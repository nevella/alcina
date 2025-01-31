package cc.alcina.framework.servlet.component.entity.property;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Result;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.LayerParserPeer;
import cc.alcina.framework.common.client.traversal.layer.Measure;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.servlet.component.entity.property.PropertyFilterParser.PropertyProposer;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes.Filter;

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
			add(EntryToken.NAME_VALUE);
			add(EntryToken.ID);
		}
	}

	enum IntermediateToken implements BranchToken {
		PROPERTY_NAME {
			Pattern pattern = Pattern.compile("\\S+");

			@Override
			public Measure match(ParserState state) {
				Measure match = state.patternMatcher().match(this, pattern);
				if (match != null) {
					PropertyFilterParser.Query query = state.getDocument()
							.ancestorSelection(
									PropertyFilterParser.Query.class);
					if (query.getMatchingProperties(match.text()).isEmpty()) {
						match = null;
					}
				}
				return match;
			}
		},
		OPERATOR {
			Pattern pattern = Pattern.compile(
					Arrays.stream(FilterOperator.values()).map(Object::toString)
							.collect(Collectors.joining("|")),
					Pattern.CASE_INSENSITIVE);

			@Override
			public Measure match(ParserState state) {
				return state.patternMatcher().match(this, pattern);
			}
		},
		SORT {
			Pattern pattern = Pattern.compile("sort", Pattern.CASE_INSENSITIVE);

			@Override
			public Measure match(ParserState state) {
				return state.patternMatcher().match(this, pattern);
			}
		},
		DIRECTION {
			Pattern pattern = Pattern.compile("desc|asc",
					Pattern.CASE_INSENSITIVE);

			@Override
			public Measure match(ParserState state) {
				return state.patternMatcher().match(this, pattern);
			}
		},
		SORT_PROPERTY_NAME {
			@Override
			public Group getGroup() {
				return Group.of(BranchToken.Standard.NON_WHITESPACE);
			}
		},
		PROPERTY_VALUE {
			@Override
			public Group getGroup() {
				return Group.of(BranchToken.Strings.STRING_VALUE);
			}
		};
	}

	enum EntryToken implements BranchToken {
		NAME_VALUE {
			@Override
			public Group getGroup() {
				Group operatorClause = Group.of(BranchToken.Standard.WHITESPACE,
						IntermediateToken.OPERATOR);
				Group sortClause = Group.of(BranchToken.Standard.WHITESPACE,
						IntermediateToken.SORT, BranchToken.Standard.WHITESPACE,
						IntermediateToken.SORT_PROPERTY_NAME,
						BranchToken.Standard.WHITESPACE,
						IntermediateToken.DIRECTION);
				return Group.of(BranchToken.Standard.OPTIONAL_WHITESPACE,
						IntermediateToken.PROPERTY_NAME,
						Group.optional(operatorClause),
						BranchToken.Standard.WHITESPACE,
						IntermediateToken.PROPERTY_VALUE,
						Group.optional(sortClause));
			}

			@Override
			List<Filter> proposePropertyFilters(
					Class<? extends Entity> entityType, Result parseResult,
					PropertyProposer proposer) {
				String name = parseResult
						.measure(IntermediateToken.PROPERTY_NAME).text();
				FilterOperator operator = null;
				String sortKey = null;
				Direction sortDirection = null;
				Measure operatorMeasure = parseResult
						.measure(IntermediateToken.OPERATOR);
				if (operatorMeasure != null) {
					operator = CommonUtils.getEnumValueOrNull(
							FilterOperator.class, operatorMeasure.text(), true,
							null);
				}
				Measure sortMeasure = parseResult
						.measure(IntermediateToken.SORT);
				if (sortMeasure != null) {
					sortKey = parseResult
							.measure(IntermediateToken.SORT_PROPERTY_NAME)
							.text();
					String sortDirectionText = parseResult
							.measure(IntermediateToken.DIRECTION).text();
					sortDirection = Direction.valueOfAbbrev(sortDirectionText);
				}
				String value = parseResult
						.measure(BranchToken.Strings.STRING_VALUE).text();
				return proposer.proposePropertyFilters(entityType, name,
						operator, value, sortKey, sortDirection);
			}
		},
		ID {
			@Override
			public Group getGroup() {
				return Group.of(BranchToken.Standard.DIGITS);
			}

			@Override
			List<Filter> proposePropertyFilters(
					Class<? extends Entity> entityType, Result parseResult,
					PropertyProposer proposer) {
				String value = parseResult.rootMeasure().text();
				return proposer.proposePropertyFilters(entityType, "id", null,
						value, null, null);
			}
		};

		abstract List<StandardLayerAttributes.Filter> proposePropertyFilters(
				Class<? extends Entity> entityType,
				BranchingParser.Result parseResult, PropertyProposer proposer);
	}
}