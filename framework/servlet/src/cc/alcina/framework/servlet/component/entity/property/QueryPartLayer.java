package cc.alcina.framework.servlet.component.entity.property;

import java.util.regex.Pattern;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.LayerParserPeer;
import cc.alcina.framework.common.client.traversal.layer.Measure;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;

/**
 * Transforms a RawIngredientSelection (a line of text) to a IngredientSelection
 * (a structured token parse result breaking the line into qty, units, nane etc)
 */
class QueryPartLayer extends Layer<DocumentLayer.Document> {
	@Override
	public void process(DocumentLayer.Document selection) throws Exception {
		ParserPeer parserPeer = new ParserPeer(state.getTraversal());
		LayerParser layerParser = new LayerParser(selection, parserPeer);
		layerParser.parse();
		layerParser.getSentences().stream()
				.map(branch -> new PartSelection(selection, branch))
				.forEach(this::select);
	}

	static class PartSelection extends MeasureSelection {
		public PartSelection(Selection parent, Branch branch) {
			super(parent, branch.match);
			branch.match.setData(branch);
		}

		public BranchingParser.Branch getBranch() {
			return (Branch) get().getData();
		}
	}

	static class ParserPeer extends LayerParserPeer {
		public ParserPeer(SelectionTraversal selectionTraversal) {
			super(selectionTraversal);
			add(Token.WITHOUT_OPERATOR);
		}
	}

	enum Token implements BranchToken {
		PROPERTY_NAME {
			Pattern PATTERN = Pattern.compile("\\S+");

			@Override
			public Measure match(ParserState state) {
				Measure match = state.patternMatcher().match(this, PATTERN);
				if (match != null) {
					PropertyFilterParser2.Query query = state.getDocument()
							.ancestorSelection(
									PropertyFilterParser2.Query.class);
					if (query.getMatchingProperties(match.text()).isEmpty()) {
						match = null;
					}
				}
				return match;
			}
		},
		COMESTIBLE {
			@Override
			public Group getGroup() {
				return Group.of(BranchToken.Standard.ANY_TEXT);
			}
		},
		WITHOUT_OPERATOR {
			@Override
			public Group getGroup() {
				return Group.of(BranchToken.Standard.OPTIONAL_WHITESPACE,
						PROPERTY_NAME, BranchToken.Standard.WHITESPACE,
						BranchToken.Strings.STRING_VALUE);
			}
		};
	}
}