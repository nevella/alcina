package cc.alcina.framework.common.client.traversal.layer.block;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.InputsFromPreviousSibling;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.ParserObserver.Attributes;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.LayerParserPeer;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.TextUtils;

/**
 * <p>
 * Emits visual blocks
 */
public class BlockSequenceLayer extends Layer<MeasureSelection>
		implements InputsFromPreviousSibling {
	static Logger logger = LoggerFactory.getLogger(BlockSequenceLayer.class);

	MeasureSelection selection;

	@Override
	public void process(MeasureSelection selection) throws Exception {
		this.selection = selection;
		ParserPeer parserPeer = new ParserPeer(state.getTraversal(), selection);
		LayerParser layerParser = new LayerParser(selection, parserPeer);
		layerParser.withExtendedState(new LogicalRunState(selection));
		layerParser.parse();
		layerParser.getParserResults().getSentenceMeasures()
				.filter(measure -> !measure.toIntPair().isPoint())
				.map(measure -> new BlockSelection(selection, measure))
				.forEach(this::select);
	}

	@Override
	protected void onAfterInputsProcessed() {
		List<BlockSelection> outputs = selections(BlockSelection.class);
		List<IntPair> ranges = outputs.stream().map(o -> o.get().toIntPair())
				.toList();
		if (!IntPair.isContinuous(ranges)) {
			List<IntPair> uncovered = IntPair.provideUncovered(ranges,
					selection.get().toIntPair());
			IntPair firstUncoveredPair = uncovered.get(0);
			BlockSelection firstRun = outputs.get(0);
			List<DomNode> firstUncoveredNodes = firstRun.domNode().document
					.locations()
					.getContainingNodes(firstRun.domNode().asLocation(),
							firstUncoveredPair.i1, true);
			logger.warn("First uncovered pair: {}", firstUncoveredPair);
			DomNode lowestCoveringElement = firstUncoveredNodes.stream()
					.filter(DomNode::isElement).reduce(Ax.last()).get();
			logger.warn("First uncovered element: {} {}",
					lowestCoveringElement.asRange().toIntPair(),
					lowestCoveringElement.ntc());
			logger.warn("Uncovered ranges: [{}],{}", uncovered.size(),
					uncovered.stream().limit(20).toList());
			Preconditions.checkState(IntPair.isContinuous(ranges));
		} else {
			logger.info("Logical runs range: {}", IntPair.unionOf(ranges));
		}
		outputs.forEach(output -> logger.debug(output.toString()));
	}

	class LogicalRunState extends AbstractHtmlState {
		boolean ignoreFootnotes;

		protected LogicalRunState(Selection selection) {
			super(true, selection);
		}
	}

	public static class BlockSelection extends MeasureSelection {
		int index;

		BlockSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}

		@Override
		public String toString() {
			return Ax.format("%s %s",
					CommonUtils.padStringLeft(
							String.valueOf(get().start.getIndex()), 10, ' '),
					TextUtils.normalizeWhitespace(get().text()));
		}
	}

	static class ParserPeer extends LayerParserPeer {
		ParserPeer(SelectionTraversal selectionTraversal,
				MeasureSelection selection) {
			super(selectionTraversal);
			add(LogicalRunToken.RUN);
		}

		@Override
		public Attributes getBranchingParserObserverAttributes() {
			Attributes attributes = new Attributes();
			return attributes;
		}
	}

	enum LogicalRunToken implements AbstractHtmlState.Token<LogicalRunState> {
		RUN {
			@Override
			public Group getGroup() {
				return Group.of(BLOCK_BOUNDARY_OR_IGNORE,
						Group.of(INLINE).withMatchesZeroToAny());
			}
		},
		/*
		 * IGNORE - style nodes + passthrough tags
		 */
		BLOCK_BOUNDARY_OR_IGNORE {
			@Override
			public Measure match0(LogicalRunState state) {
				if (state.isBlockOrBlockSeparator()) {
					return pointMeasure(state);
				} else {
					return null;
				}
			}

			@Override
			public MatchesBoundary matchesBoundary() {
				return MatchesBoundary.ANY;
			}
		},
		INLINE {
			/*
			 * in a fit of paranoia, handle <FONT>asdf<BR>efff</FONT> by only
			 * extending the measure of text dom nodes
			 */
			@Override
			public Measure match0(LogicalRunState state) {
				if (!state.isBlockOrBlockSeparator()) {
					Location start = state.parserState.getLocation();
					Location end = state.parserState.getLocation();
					if (end.getContainingNode().isText()) {
						end = end.toEndOfTextLocation();
					}
					return measure(start, end);
				} else {
					return null;
				}
			}

			@Override
			public MatchesBoundary matchesBoundary() {
				return MatchesBoundary.ANY;
			}
		};
	}
}