package cc.alcina.framework.servlet.transform.fragment.pos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.RootToken.HasFragmentDocumentRoot;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerPeer;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.servlet.transform.fragment.html.TableLayer.TableSelection;
import cc.alcina.framework.servlet.transform.fragment.pos.BlockIndentLayer.IndentBlock;
import cc.alcina.framework.servlet.transform.fragment.pos.IndentRootLayer.IndentRoot;

/*
 * Merge indents of the same horizontal indent level, list level and quote level. This is complicated slightly
 * because there can be structures like:
 * 
 * @formatter:off
 * block.t0
 *     block.t1
 * block.t0
 * 			block.t2
 * 			block.t2
 * 
 * i.e. jumps in the visual indent structure which still need to be represented by [t0[t1[t2]]] nested measures
 * 
 * Also, tables/grids must be outside this structure (at least initially) - and, to be even tricksier, td/th define a root 'indent context'
 * 
 * Implementation: 
 * - for each indentblock (which is a tuple of a block and its indent root), 
 *  - indentBlock.indentRoot: compute the block indents relative to the root
 * - 
 * 
 * at the end of processing, insert a 'whole document' run to ensure there's a stable containment for all runs
 * 
 * @formatter:on
 */
public class MergedIndentLayer extends Layer<IndentBlock> {
	/*
	 * per-indent-root runs
	 */
	class RootIndents {
		RootIndents(IndentRoot root) {
			this.root = root;
		}

		IndentRoot root;

		List<Run> openRuns = new ArrayList<>();

		IndentBlock closingBlock;

		void closeTo(int level) {
			int maxLevel = openRuns.size() - 1;
			// end in ascending order
			for (int idx = level + 1; idx <= maxLevel; idx++) {
				Run run = openRuns.get(idx);
				run.end();
			}
			openRuns = openRuns.stream().limit(level + 1)
					.collect(Collectors.toList());
		}

		IndentBlock firstBlock;

		void process(IndentBlock block) {
			if (firstBlock == null) {
				firstBlock = block;
			}
			/*
			 * Ignore a pure-container block - transformation logic ignores
			 * source structure, generating its own structure from leaf blocks
			 */
			if (block.isContainer()) {
				return;
			}
			/*
			 * close any runs with level > block.level
			 */
			int level = block.styledNode().styles().getIndents().getLevel();
			String ntc = block.get().ntc();
			int closeLevel = level;
			if (closeLevel < 0) {
				// outdent past 0?
				closeLevel = 0;
			}
			/*
			 * close run at current level if different list/quote value
			 * (list->not-list, not-list->list)
			 */
			if (openRuns.size() > closeLevel) {
				Run current = openRuns.get(closeLevel);
				if (!current.canContinue(block)) {
					closeLevel--;
				}
			}
			/*
			 * Close all if a different indent context was entered/exited
			 */
			if (lastProcessedBlock != null
					&& lastProcessedBlock.indentRoot != root
					&& closeLevel > 0) {
				closeLevel = 0;
			}
			closeTo(closeLevel);
			this.closingBlock = block;
			/*
			 * handle non-markup indents due to fancy list handling
			 */
			if (root.lefts != null && level == root.lefts.size()) {
				root.addSyntheticIndent();
			}
			/*
			 * create runs as needed up to level == block.level, and add the
			 * block to all open runs
			 */
			openTo(level);
			openRuns.forEach(run -> run.add(block, level));
		}

		/* open in ascending order is important for containment */
		void openTo(int level) {
			int insertAt = openRuns.size();
			for (int idx = insertAt; idx <= level; idx++) {
				Run run = new Run(idx);
				openRuns.add(run);
			}
		}

		Set<Location.Range> selectedRanges = new LinkedHashSet<>();

		class Run {
			List<IndentBlock> blocks = new ArrayList<>();

			final int level;

			Run(int level) {
				this.level = level;
			}

			boolean list;

			boolean quote;

			boolean canContinue(IndentBlock block) {
				return list == block.isList() && quote == block.isQuote();
			}

			void end() {
				IndentBlock indentBlock = Ax.first(blocks);
				createMeasure(index(), list, quote, false);
			}

			Run nextRun() {
				return openRuns.get(index() + 1);
			}

			int index() {
				return openRuns.indexOf(this);
			}

			void createMeasure(int index, boolean list, boolean quote,
					boolean ignoreRedundant) {
				if (blocks.isEmpty()) {
					nextRun().createMeasure(index, false, false, false);
				} else {
					/*
					 * the logical parent of this selection is the first block
					 * in the selection
					 */
					IndentBlock indentBlock = Ax.first(blocks);
					Location start = Ax.first(blocks).get().start;
					Location end = closingBlock.get().end;
					Measure measure = new Measure(start, end,
							LayoutTokens.IndentRun.TYPE);
					/*
					 * Compute the relative left of this run (for use in final
					 * output) - see the package javadoc for reasoning
					 */
					float normalisedLeft = 0.0F;
					float parentNormalisedLeft = 0.0F;
					if (root.documentRoot) {
						normalisedLeft = root.lefts.get(level);
						parentNormalisedLeft = level == 0 ? 0
								: root.lefts.get(level - 1);
					} else {
						normalisedLeft = indentBlock.styledNode().styles()
								.getIndents().normalisedLeft;
						if (level > 0) {
							Run parentRun = openRuns.get(level - 1);
							parentNormalisedLeft = parentRun.blocks.get(0)
									.styledNode().styles()
									.getIndents().normalisedLeft;
						}
					}
					double relativeLeft = (double) (normalisedLeft
							- parentNormalisedLeft);
					IndentRun indentRun = new IndentRun(indentBlock, measure,
							list, quote, index, relativeLeft,
							root.documentRoot);
					if (ignoreRedundant) {
						if (selectedRanges.contains(measure.toRange())) {
							return;
						}
					}
					boolean hasTableOverlap = selections(TableSelection.class)
							.stream()
							.anyMatch(tableSelection -> tableSelection
									.overlapsWith(indentRun)
									|| tableSelection.contains(indentRun));
					if (hasTableOverlap) {
						// bail - e.g. C2004A04997/2025-12-19
						// return;
					}
					selectedRanges.add(measure.toRange());
					select(indentRun);
				}
			}

			void add(IndentBlock block, int currentLevel) {
				if (blocks.isEmpty()) {
					// if this is an intermediate indent - i.e. the dom
					// structure is indent{1,n}/list - only the most specific
					// run is of type list
					if (currentLevel == level) {
						list = block.isList();
						quote = block.isQuote();
					}
				}
				blocks.add(block);
			}

			@Override
			public String toString() {
				return Ax.format("Run :: %s => %s", Ax.first(blocks),
						Ax.last(blocks));
			}
		}

		void ensureContainerRun() {
			Run containerRun = new Run(0);
			containerRun.blocks.add(firstBlock);
			containerRun.createMeasure(0, false, false, true);
		}
	}

	IndentBlock lastProcessedBlock;

	CachingMap<IndentRoot, RootIndents> perRootIndents = new CachingMap<>(
			RootIndents::new);

	@Override
	public void process(IndentBlock block) throws Exception {
		perRootIndents.get(block.indentRoot).process(block);
		lastProcessedBlock = block;
	}

	@Override
	protected void onAfterIteration() {
		perRootIndents.values().forEach(ri -> {
			ri.closeTo(-1);
			ri.ensureContainerRun();
		});
		Iterator<RootIndents> itr = perRootIndents.values().iterator();
		if (itr.hasNext()) {
			IndentBlock firstBlock = itr.next().firstBlock;
			StyledDocument styledDocument = StyledDocument
					.ancestorDocument(firstBlock);
			if (styledDocument.isStyleBlockIndents()) {
				// Location start = firstBlock.get().start;
				// Location end = start.clone();
				// Measure measure = new Measure(start, end,
				// LayoutTokens.IndentStyles.TYPE);
				// IndentStyles indentStyles = new
				// IndentStyles(lastProcessedBlock,
				// measure);
				// select(indentStyles);
				new IndentStylesObservable(indentsToDocVersionCss()).publish();
			}
			if (BlockIndentLayer.debug.is()) {
				new FormatBuilder().dashedLine().out();
				selections(IndentRun.class).forEach(IndentRun::toDebugOut);
				new FormatBuilder().dashedLine().out();
				selections(IndentStyles.class)
						.forEach(IndentStyles::toDebugOut);
			}
		}
		super.onAfterIteration();
	}

	public static class IndentStylesObservable implements ContextObservable {
		public IndentStylesObservable(String css) {
			this.css = css;
		}

		public String css;
	}

	String indentsToDocVersionCss() {
		FormatBuilder format = new FormatBuilder();
		IndentRoot root = perRootIndents.getMap().keySet().iterator().next();
		HasFragmentDocumentRoot hasFdRoot = state
				.traversalContext(HasFragmentDocumentRoot.class);
		String cssSelector = hasFdRoot.provideRoot().provideCssSelector();
		for (int level = 1; level < root.lefts.size(); level++) {
			for (int idx = 0; idx < level; idx++) {
				if (idx == 0) {
					// minor crossing of boundaries here (really the style
					// node contents should be generated during output - but
					// that'd need a mechanism to pass the indent data to the
					// output layer, and just end up with the same result)
					format.format("%s ", cssSelector);
				} else {
					format.append(" ");
				}
				format.append("block.indent");
			}
			format.append("\n{");
			format.line("\tpadding-left: %spx;",
					root.lefts.get(level) - root.lefts.get(level - 1));
			format.append("}\n");
		}
		format.line(
				"%s block.indent[ignore-indent]\n{padding-left: 0 !important;}",
				cssSelector);
		String css = format.toString();
		return css;
	}

	public class IndentStyles extends StyledMeasureSelection
			implements LayoutTokens.IndentStyles.Data.Has {
		public IndentStyles(Selection parent, Measure measure) {
			super(parent, measure);
		}

		@Override
		public LayoutTokens.IndentStyles.Data getIndentStylesData() {
			LayoutTokens.IndentStyles.Data data = new LayoutTokens.IndentStyles.Data();
			String css = indentsToDocVersionCss();
			data.text = css;
			return data;
		}
	}

	public static class IndentRun extends StyledMeasureSelection
			implements LayoutTokens.HasIndentData,
			FragmentDocumentNode.NonOutput, LayoutTokens.IndentRun.Data.Has {
		boolean list;

		boolean quote;

		int logicalDepth;

		public MeasureSelection firstListItem;

		Double relativeIndent;

		boolean documentRootContext;

		IndentRoot indentRoot() {
			return ((IndentBlock) parentSelection()).indentRoot;
		}

		/*
		 * FIXME - move this logic to the list analyser classes
		 * (ListItemLabelProvider)
		 */
		@Override
		public boolean isUnorderedList() {
			DomNode parentAncestorStyledNode = parentSelection()
					.ancestor(StyledNode.class).get();
			if (parentAncestorStyledNode.tagIsOneOf("ul", "li")) {
				return parentAncestorStyledNode.tagIs("ul");
			} else {
				return firstListItem != null
						&& firstListItem.text().contains("\u2022");
			}
		}

		@Override
		public MeasureSelection truncateTo(IntPair range) {
			Location.Range locationRange = get().truncateAbsolute(range.i1,
					range.i2);
			Measure measure = Measure.fromRange(locationRange, get().token);
			return new IndentRun((IndentBlock) parentSelection(), measure, list,
					quote, logicalDepth, relativeIndent, documentRootContext);
		}

		@Override
		public int equalRangeCompare(MeasureSelection o) {
			if (o instanceof IndentRun) {
				// compare by logicalDepth
				return logicalDepth - ((IndentRun) o).logicalDepth;
			} else {
				return 0;
			}
		}

		@Override
		public boolean isQuote() {
			return quote;
		}

		IndentRun(IndentBlock parent, Measure measure, boolean list,
				boolean quote, int logicalDepth, double relativeIndent,
				boolean documentRootContext) {
			super(parent, measure);
			this.list = list;
			this.quote = quote;
			this.logicalDepth = logicalDepth;
			this.documentRootContext = documentRootContext;
			measure.setData(new IndentData(list, quote, logicalDepth,
					relativeIndent, documentRootContext));
			setOmit(!list && logicalDepth == 0);
			this.relativeIndent = relativeIndent;
		}

		static class IndentData {
			boolean list;

			boolean quote;

			int logicalDepth;

			double relativeIndent;

			boolean documentRootContext;

			IndentData(boolean list, boolean quote, int logicalDepth,
					double relativeIndent, boolean documentRootContext) {
				this.list = list;
				this.quote = quote;
				this.logicalDepth = logicalDepth;
				this.relativeIndent = relativeIndent;
				this.documentRootContext = documentRootContext;
			}

			@Override
			public String toString() {
				return Ax.format("[Lvl:%s:%s:%s:%s:%spx]", logicalDepth,
						CommonUtils.tf(list), CommonUtils.tf(quote),
						CommonUtils.tf(documentRootContext),
						relativeIndent == Math.floor(relativeIndent)
								? (int) relativeIndent
								: Ax.twoPlaces(relativeIndent));
			}
		}

		@Override
		public int getIndentLevel() {
			return styledNode().styles().getIndents().getLevel();
		}

		@Override
		public boolean isList() {
			return list;
		}

		@Override
		public String toString() {
			return Ax.format("%s %s", super.toString(), get().getData());
		}

		@Override
		public LayoutTokens.IndentRun.Data getIndentRunData() {
			LayoutTokens.IndentRun.Data data = new LayoutTokens.IndentRun.Data();
			Peer peer = SelectionTraversal.contextTraversal()
					.context(Peer.Has.class).getMergedIndentLayerPeer();
			data.relativeIndent = relativeIndent;
			data.styleBlockIndents = documentRootContext && StyledDocument
					.ancestorDocument(this).isStyleBlockIndents();
			data.quote = quote;
			return data;
		}
	}

	public static class Peer {
		public interface Has {
			Peer getMergedIndentLayerPeer();
		}
	}
}