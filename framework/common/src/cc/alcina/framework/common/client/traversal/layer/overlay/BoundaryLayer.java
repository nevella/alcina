package cc.alcina.framework.common.client.traversal.layer.overlay;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.LayerParserPeer;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.overlay.BoundaryTraversals.Unit;
import cc.alcina.framework.common.client.traversal.layer.overlay.MeasureOverlay.DocumentElement;
import cc.alcina.framework.common.client.traversal.layer.overlay.MeasureOverlay.ExtendMeasureSelection;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multiset;

/**
 * Transforms a Document into a set of boundary traversal selections
 */
class BoundaryLayer extends Layer<ExtendMeasureSelection> {
	public enum Token implements BranchToken {
		DOCUMENT_BOUNDARY {
			@Override
			public Measure match(ParserState state) {
				ParserPeer peer = peer(state);
				/*
				 * if !after + fowards (or after + backwards), return null. So
				 * effectively return 'is start'
				 */
				if (!(state.getLocation().after
						^ peer.isForwardsTraversalOrder())) {
					return null;
				}
				boolean match = false;
				int treeIndex = state.getLocation().getTreeIndex();
				if (state.getLocation().getIndex() == 0) {
					match = treeIndex == 2;
				} else {
					Range documentRange = state.getLocation()
							.getLocationContext().getDocumentRange();
					if (state.getLocation().getIndex() == documentRange
							.length()) {
						DomNode lastNode = state.getLocation()
								.getContainingNode().children.lastNode();
						if (lastNode != null && lastNode.asLocation()
								.getTreeIndex() == treeIndex) {
							match = true;
						}
					}
				}
				if (match) {
					return matchMeasure(state);
				} else {
					return null;
				}
			}
		},
		BLOCK_BOUNDARY {
			@Override
			public Measure match(ParserState state) {
				boolean match = DOCUMENT_BOUNDARY.match(state) != null;
				if (!match) {
					ParserPeer peer = peer(state);
					/*
					 * if after + fowards (or !after + backwards), return null.
					 * So effectively return 'is start'
					 */
					if (state.getLocation().after
							^ peer.isForwardsTraversalOrder()) {
						return null;
					}
					DomNode containingNode = state.getLocation()
							.getContainingNode();
					match = peer.styleResolver().isBlock(containingNode);
				}
				if (match) {
					return matchMeasure(state);
				} else {
					return null;
				}
			}
		},
		SENTENCE_BOUNDARY {
			@Override
			public Measure match(ParserState state) {
				boolean match = BLOCK_BOUNDARY.match(state) != null;
				if (!match) {
					match = Objects.equals(nextChar(state), ".")
							&& Objects.equals(previousChar(state), " ");
				}
				if (match) {
					return matchMeasure(state);
				} else {
					return null;
				}
			}
		},
		WORD_BOUNDARY {
			@Override
			public Measure match(ParserState state) {
				boolean match = BLOCK_BOUNDARY.match(state) != null;
				if (!match) {
					match = Objects.equals(nextChar(state), " ");
				}
				if (match) {
					return matchMeasure(state);
				} else {
					return null;
				}
			}
		};

		Measure matchMeasure(ParserState state) {
			return Measure.fromRange(state.getLocation().asRange(), this);
		}

		ParserPeer peer(ParserState state) {
			return (ParserPeer) state.peer();
		}

		String nextChar(ParserState state) {
			return relativeChar(state, 1);
		}

		String previousChar(ParserState state) {
			return relativeChar(state, -1);
		}

		String relativeChar(ParserState state, int delta) {
			ParserPeer peer = peer(state);
			int absDelta = (peer.isForwardsTraversalOrder() ? 1 : -1) * delta;
			Location location = state.getLocation();
			IntPair pair = new IntPair(location.getIndex(),
					location.getIndex() + absDelta).toLowestFirst();
			IntPair documentPair = location.getLocationContext()
					.getDocumentRange().toIntPair();
			if (documentPair.containsExEnd(pair)) {
				Location end = location.createTextRelativeLocation(absDelta,
						false);
				Range range = new Location.Range(location, end);
				return location.getLocationContext().textContent(range);
			} else {
				return null;
			}
		}

		Unit toUnit() {
			switch (this) {
			case DOCUMENT_BOUNDARY:
				return Unit.document;
			case BLOCK_BOUNDARY:
				return Unit.block;
			case SENTENCE_BOUNDARY:
				return Unit.sentence;
			case WORD_BOUNDARY:
				return Unit.word;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	class Parser extends LayerParser {
		public Parser(MeasureSelection selection, LayerParserPeer parserPeer) {
			super(selection, parserPeer);
		}

		@Override
		public DomNode getContainerNode() {
			return super.getContainerNode().document.getDocumentElementNode();
		}
	}

	class ParserPeer extends LayerParserPeer {
		BoundaryTraversals quota;

		public ParserPeer(SelectionTraversal selectionTraversal) {
			super(selectionTraversal);
			add(Token.DOCUMENT_BOUNDARY);
			add(Token.BLOCK_BOUNDARY);
			add(Token.SENTENCE_BOUNDARY);
			add(Token.WORD_BOUNDARY);
		}

		@Override
		public boolean lookaheadMatcherIsNormalizeSpaces() {
			return false;
		}

		@Override
		protected Location computeParserStartLocation() {
			// init
			this.quota = selection.quota;
			Measure measure = selection.get();
			return isForwardsTraversalOrder() ? measure.end : measure.start;
		}

		MeasureOverlay.StyleResolver styleResolver() {
			return boundaryParser.measureOverlay.styleResolver;
		}

		boolean isForwardsTraversalOrder() {
			return parser.isForwardsTraversalOrder();
		}

		/*
		 * Tracks ignore of - say - multiple block boundaries at a given index
		 */
		Multiset<Token, Set<Integer>> indexMatches = new Multiset<>();

		@Override
		public void onSentenceMatched(Branch bestMatch,
				List<Branch> matchedSentenceBranches) {
			for (Branch branch : matchedSentenceBranches) {
				Measure rootMeasure = branch.toResult().rootMeasure();
				Token token = (Token) rootMeasure.token;
				int index = rootMeasure.start.getIndex();
				if (indexMatches.add(token, index)) {
					if (!quota.decrement(token.toUnit())) {
						parser.getParserState().finished = true;
					}
				}
			}
		}

		Measure computeExtendedMeasure() {
			if (quota.isExhausted()) {
				Range range = new Location.Range(computeParserStartLocation(),
						parser.getParserState().getLocation());
				return Measure.fromRange(range,
						MeasureOverlay.ExtendedToken.TYPE);
			} else {
				return null;
			}
		}
	}

	static class ExtendedMeasureSelection extends MeasureSelection {
		public ExtendedMeasureSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}

	BoundaryParser boundaryParser;

	ExtendMeasureSelection selection;

	DocumentElement documentSelection;

	@Override
	public void process(ExtendMeasureSelection selection) throws Exception {
		ParserPeer parserPeer = new ParserPeer(state.getTraversal());
		boundaryParser = state.getTraversal().context(BoundaryParser.class);
		this.selection = selection;
		this.documentSelection = MeasureOverlay.DocumentElement.of(selection);
		LayerParser layerParser = new Parser(documentSelection, parserPeer);
		layerParser.setForwardsTraversalOrder(selection.forwards);
		layerParser.setLookahead(false);
		layerParser.parse();
		Measure extendedMeasure = parserPeer.computeExtendedMeasure();
		if (extendedMeasure != null) {
			select(new ExtendedMeasureSelection(selection, extendedMeasure));
		}
	}
}