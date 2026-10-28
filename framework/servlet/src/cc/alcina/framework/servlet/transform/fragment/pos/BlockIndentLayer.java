package cc.alcina.framework.servlet.transform.fragment.pos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformer;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerPeer;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles.Indents;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.servlet.transform.fragment.pos.BlockIndentLayer.IndentationContext.Block;
import cc.alcina.framework.servlet.transform.fragment.pos.IndentRootLayer.IndentRoot;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.entity.Configuration;

/*
 * Overlay indent block measures. These overlay blocks and define the per-block
 * indent level.
 *
 * A later layer (MergedIndentLayer) collates these into larger indentation and
 * list structures
 * 
 * Note that the purpose of this layer is to produce a logical indent model of
 * the document - which means it makes sense for the styleddocumentpeer to
 * override a physical indent in the source doc if necessary, where that indent
 * is purely decorative and (say in the case of indented section headers) would
 * break the indentation logic.
 * 
 * That override is provided by StyledDocumentPeer.getIndentLeft(DomNode node,
 * float marginLeft)
 *
 */
public class BlockIndentLayer extends Layer<StyledNode> {
	static Configuration.Key<Boolean> debug = Configuration.key("debug");

	Map<IndentRootLayer.IndentRoot, IndentationContext> rootContexts = new LinkedHashMap<>();

	List<Block> documentBlocks = new ArrayList<>();

	PositioningLayer positioningLayer;

	PositioningLayer.Peer positioningLayerPeer;

	MeasureContainment.ContainmentMap<IndentRoot> indentRoots;

	public static class IndentLogObservable implements ContextObservable {
		public IndentLogObservable(String log) {
			this.log = log;
		}

		public String log;
	}

	static class CapturingLogger {
		Logger logger;

		StringBuilder capture = new StringBuilder();

		public CapturingLogger(Logger logger) {
			this.logger = logger;
		}

		void append(String level, String template, Object... args) {
			String message = Ax.format(translateLogToFormatTemplate(template),
					args);
			capture.append(message);
			capture.append("\n");
		}

		String translateLogToFormatTemplate(String template) {
			return template.replace("{}", "%s");
		}

		public void debug(String template, Object... args) {
			append("DEBUG", template, args);
			logger.info(template, args);
		}
	}

	CapturingLogger logger;

	Peer peer;

	BlockIndentLayer(PositioningLayer indentLayer) {
		this.positioningLayer = indentLayer;
		logger = new CapturingLogger(LoggerFactory.getLogger(getClass()));
	}

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		peer = state.traversalContext(Peer.Has.class)
				.getBlockIndentNormalisationPeer();
		Order order = state.traversalContext(Order.Has.class).getOrder();
		positioningLayerPeer = state
				.traversalContext(PositioningLayer.Peer.Has.class)
				.getIndentLayerPeer();
		indentRoots = new MeasureContainment.ContainmentMap<>(order,
				selections(IndentRoot.class));
	}

	@Override
	public void process(StyledNode node) throws Exception {
		if (positioningLayerPeer.excludeFromLevelComputations(node)) {
			return;
		}
		DomNode domNode = node.get();
		if (domNode.isElement()) {
			Styles styles = node.styles();
			if (styles.isBlock() && domNode.isNonWhitespaceTextContent()) {
				getContext(node).add(node);
			}
		}
	}

	@Override
	protected void onAfterIteration() {
		indentationContexts().forEach(IndentationContext::computeLevels);
		Map<String, IndentBlock> dedupe = new LinkedHashMap<>();
		documentBlocks.forEach(block -> {
			StyledNode node = block.node;
			Measure measure = Measure.fromNode(node.get(),
					LayoutTokens.IndentData.TYPE);
			Indents indents = node.styles().getIndents();
			String data = Ax.format("[Lvl:%s:%s:%s:%s]", indents.getLevel(),
					CommonUtils.tf(indents.list), CommonUtils.tf(indents.quote),
					indents.left());
			measure.setData(data);
			String duplicateCheck = Ax.format("%s:%s", measure.toIntPair(),
					data);
			IndentBlock existing = dedupe.get(duplicateCheck);
			if (existing == null) {
				IndentBlock indentBlock = new IndentBlock(node, measure);
				indentBlock.indentRoot = indentRoots
						.getLowestContainingMeasure(indentBlock, false);
				measure.setData(Ax.format("[Root:%s]%s",
						indentBlock.indentRoot.idx, data));
				select(indentBlock);
				dedupe.put(duplicateCheck, indentBlock);
			} else {
				if (measure.start.getIndex() == 0
						|| DocumentTransformer.measureFilterKey.has()) {
					// multiple document containers (html/body/div, say)
				} else {
					throw new UnsupportedOperationException();
				}
			}
		});
		if (debug.is()) {
			selections(IndentBlock.class).forEach(IndentBlock::toDebugOut);
		}
		ProcessObservers.context()
				.publish(new IndentLogObservable(logger.capture.toString()));
		super.onAfterIteration();
	}

	IndentationContext getContext(StyledNode block) {
		Measure measure = Measure.fromRange(block.get().asRange(),
				LayoutTokens.LeafBlock.TYPE);
		StyledMeasureSelection testSelection = new StyledMeasureSelection(block,
				measure);
		IndentRoot indentRoot = indentRoots
				.getLowestContainingMeasure(testSelection, false);
		return rootContexts.computeIfAbsent(indentRoot,
				IndentationContext::new);
	}

	Collection<IndentationContext> indentationContexts() {
		return rootContexts.values();
	}

	class IndentationContext {
		IndentRoot root;

		IndentationContext(IndentRoot root) {
			this.root = root;
		}

		List<Block> blocks = new ArrayList<>();

		Multimap<Float, List<Block>> byLeft = new Multimap<>();

		Multimap<Float, List<Float>> leftBybyGap = new Multimap<>();

		Map<Float, Integer> levelByLeft = new LinkedHashMap<>();

		List<Float> lefts = null;

		void add(StyledNode node) {
			Block block = new Block(node);
			blocks.add(block);
			documentBlocks.add(block);
		}

		void computeLevels() {
			Collections.sort(blocks);
			blocks.forEach(b -> byLeft.add(b.left, b));
			/*
			 * normalise gaps
			 */
			float normaliseIfGapLt = 5.0F;
			boolean delta = false;
			do {
				delta = false;
				lefts = byLeft.keySet().stream().sorted()
						.collect(Collectors.toList());
				for (int idx = 0; idx < lefts.size() - 1; idx++) {
					Float left0 = lefts.get(idx);
					Float left1 = lefts.get(idx + 1);
					float gap = left1 - left0;
					if (gap < normaliseIfGapLt) {
						List<Block> to = byLeft.get(left0);
						List<Block> from = byLeft.remove(left1);
						to.addAll(from);
						delta = true;
						break;
					}
				}
			} while (delta);
			if (lefts.size() == 1) {
				// all level 0 - not handling all level > 0 yet
				// Preconditions.checkArgument(lefts.get(0) == 0.0);
				// return;
				root.lefts = List.of(0.0F);
				return;
			}
			// compute gaps between lefts
			for (int idx = 0; idx < lefts.size() - 1; idx++) {
				Float left0 = lefts.get(idx);
				Float left1 = lefts.get(idx + 1);
				float gap = left1 - left0;
				leftBybyGap.add(gap, left1);
			}
			/*
			 * Code below checks for consistent gaps - but real life ain't like
			 * that.
			 * 
			 * As a first approx we just create list levels [0, 1, 2 etc] event
			 * if the gaps are say [40px 40px 120px]
			 * 
			 * 
			 */
			// List<Float> gaps = leftBybyGap.keySet().stream().sorted()
			// .collect(Collectors.toList());
			// // check gaps are all multiples of each other - they *won't* be
			// in
			// // the general case (will need to handle fuzzys), but this is a
			// // first approximation
			// List<Float> testMutiples = gaps.stream().filter(g -> g != 0.0)
			// .collect(Collectors.toList());
			// float first = testMutiples.get(0);
			// for (int idx = 1; idx < testMutiples.size(); idx++) {
			// float testMultiple = testMutiples.get(idx);
			// float testRatio = testMultiple / first;
			// float nonIntegral = Math.abs(testRatio - Math.round(testRatio));
			// Preconditions.checkArgument(nonIntegral < 0.02);
			// }
			normaliseLevels();
			// assign levels
			byLeft.forEach((left, blocks) -> {
				int level = lefts.indexOf(left);
				if (root.documentRoot) {
					logger.debug(
							"Indent :: level: {} - left: {} - blocks: {} :: {}",
							level, Ax.fourPlaces(left), blocks.size(),
							styleGrouping(blocks));
				}
				blocks.forEach(block -> block.node.styles().getIndents()
						.setLevel(state, level, left));
			});
			root.lefts = lefts;
		}

		void normaliseLevels() {
			/*
			 * Only normalise the root context
			 */
			if (!root.documentRoot) {
				return;
			}
			logger.debug("Computing levels: {}", root.get().toIntPair());
			/*
			 * Custom normalise gaps
			 */
			Map<Float, Float> normalisations = peer.getNormalisations(state,
					(Multimap) byLeft);
			Float roundTo = peer.roundIndentsToMultiplesOf(state);
			if (normalisations.size() > 0 || roundTo != null) {
				logger.debug("pre-normalisation levels:");
				byLeft.forEach((left, blocks) -> {
					int level = lefts.indexOf(left);
					logger.debug(
							"Indent :: level: {} - left: {} - blocks: {} :: {}",
							level, Ax.fourPlaces(left), blocks.size(),
							styleGrouping(blocks));
				});
				logger.debug("Indent normalisations: {}", normalisations);
				normalisations.forEach((from, to) -> {
					if (byLeft.containsKey(from) && byLeft.containsKey(to)) {
						List<Block> fromBlocks = byLeft.remove(from);
						byLeft.get(to).addAll(fromBlocks);
					} else {
						String issue = Ax.format(
								"Indent normalisations :: %s - missing at least one of {%s,%s}",
								byLeft.keySet(), from, to);
						peer.onNormalisationIssue(issue);
					}
				});
				lefts.removeAll(normalisations.keySet());
				if (roundTo != null) {
					logger.debug("Indent roundTo: {}", roundTo);
					byLeft.keySet().stream().toList().forEach(left -> {
						float rounded = Math.round(left / roundTo) * roundTo;
						if (left != rounded) {
							byLeft.addCollection(rounded, byLeft.remove(left));
						}
					});
					lefts = byLeft.keySet().stream().toList();
				}
				// Integer alignToListLevel = peer.getAlignToListLevel(node);
				// if(alignToListLevel==null){
				// return;
				// }
			}
		}

		String styleGrouping(List<Block> blocks) {
			CountingMap<String> map = new CountingMap<>();
			blocks.forEach(b -> {
				map.add(b.node.get().getClassName());
			});
			String summary = map.toAlignedReverseString(true);
			summary = Ax.ntrim(summary.replace("\n", "; "));
			return summary;
		}

		Logger logger = LoggerFactory.getLogger(IndentationContext.class);

		public class Block implements Comparable<Block> {
			StyledNode node;

			float left;

			Block(StyledNode node) {
				this.node = node;
				String ntc = node.get().ntc();
				left = node.styles().getIndents().left();
				// tmp - FIXME - move to more general normalisation
				if (left < 10) {
					left = 0;
				}
				Integer alignToListLevel = peer.getAlignToListLevel(node);
				if (alignToListLevel == null) {
					return;
				}
				DomNode cursor = node.get();
				int maxNodes = 100;
				while (cursor != cursor.document.getDocumentElementNode()
						&& maxNodes-- > 0) {
					if (!cursor.isElement()) {
						continue;
					}
					StyledNode styledCursor = node.getStyledDocument()
							.styledNode(cursor);
					Integer listLevel = peer.getListLevel(styledCursor);
					if (listLevel != null
							&& Objects.equals(listLevel, alignToListLevel)) {
						float from = left;
						left = styledCursor.styles().getIndents().left();
						node.styles().getIndents().overrideLeft = left;
						node.styles().getIndents().list = true;
						logger.info(
								"List-continue - adjust left :: {} :: {} -> {}",
								node.get().getClassName(), from, left);
						return;
					}
					cursor = cursor.relative().treePreviousNode();
				}
				logger.warn("No list match - {}", alignToListLevel);
				/*
				 * FIXME - DEVEX-2 - notify parser warning
				 */
			}

			@Override
			public int compareTo(Block o) {
				return CommonUtils.compareFloats(left, o.left);
			}

			@Override
			public String toString() {
				return node.toString();
			}
		}
	}

	public static class IndentBlock extends StyledMeasureSelection implements
			MeasureSelection.IgnoreOverlaps, MeasureSelection.Intermediate {
		public IndentRoot indentRoot;

		IndentBlock(Selection parent, Measure measure) {
			super(parent, measure);
		}

		boolean isList() {
			return styledNode().styles().getIndents().list;
		}

		@Override
		public String toDebugString() {
			return Ax.format("left: %s :: %s",
					styledNode().styles().getIndents().left(),
					super.toDebugString());
		}

		boolean isQuote() {
			return styledNode().styles().getIndents().quote;
		}

		public boolean isContainer() {
			// currently just hard-code this check (could be for others, but div
			// is the only one so far)
			if (domNode().tagIs("div")) {
				Optional<StyledNode> nonContainerChild = domNode().children
						.nodes().stream().map(dn -> styledNode()
								.getStyledDocument().styledNode(dn))
						.filter(sn -> {
							if (sn == null) {
								/*
								 * root
								 */
								return false;
							}
							if (sn.isIgnorableWhitespace()) {
								return false;
							}
							if (sn.isBlock()) {
								return false;
							}
							return true;
						}).findFirst();
				return nonContainerChild.isEmpty();
			} else {
				return false;
			}
		}
	}

	public static class Peer {
		public interface Has {
			Peer getBlockIndentNormalisationPeer();
		}

		public boolean applyMsoClassNameNormalisations() {
			return true;
		}

		public Integer getListLevel(StyledNode node) {
			return null;
		}

		Pattern classNameIgnoreablePattern;

		boolean isClassNameIgnoreable(List<Block> blocks) {
			buildClassNameIgnoreableRegex();
			CountingMap<String> map = new CountingMap<>();
			blocks.forEach(b -> {
				map.add(b.node.get().getClassName());
			});
			String ignoreableRegex = "(?i)" + buildClassNameIgnoreableRegex();
			String nonIgnoreableRegex = "(?i)"
					+ buildClassNameNonIgnoreableRegex();
			int ignoreableCount = map.entrySet().stream()
					.filter(s -> s.getKey().matches(ignoreableRegex))
					.collect(Collectors.summingInt(Map.Entry::getValue));
			int nonIgnoreableCount = map.entrySet().stream()
					.filter(s -> s.getKey().matches(nonIgnoreableRegex))
					.collect(Collectors.summingInt(Map.Entry::getValue));
			int nonMsoTocCount = map.entrySet().stream()
					.filter(s -> !s.getKey().matches(ignoreableRegex))
					.collect(Collectors.summingInt(Map.Entry::getValue));
			return nonIgnoreableCount == 0
					&& ignoreableCount > nonMsoTocCount * 2;
		}

		protected String buildClassNameNonIgnoreableRegex() {
			return ".*(section|sub).*";
		}

		protected String buildClassNameIgnoreableRegex() {
			if (applyMsoClassNameNormalisations()) {
				return ".*MsoToc.*";
			} else {
				return ".*zzz-nomatch.*";
			}
		}

		Map<Float, Float> getNormalisations(Layer<?>.State state,
				Multimap<Float, List<?>> byLeft) {
			return Map.of();
		}

		public void onNormalisationIssue(String issue) {
			LoggerFactory.getLogger(getClass()).warn("CONVERSION_STRONG");
			LoggerFactory.getLogger(getClass()).warn(issue);
		}

		public Float roundIndentsToMultiplesOf(Layer<?>.State state) {
			return null;
		}

		public Integer getAlignToListLevel(StyledNode node) {
			return null;
		}
	}
}