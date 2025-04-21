package cc.alcina.framework.servlet.component.entity.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.SearchCriterion.Direction;
import cc.alcina.framework.common.client.traversal.InitialTextSelection;
import cc.alcina.framework.common.client.traversal.PlainTextSelection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.StringMatches;
import cc.alcina.framework.common.client.util.StringMatches.PartialSubstring;
import cc.alcina.framework.servlet.component.entity.property.QueryPartLayer.EntryToken;
import cc.alcina.framework.servlet.component.entity.property.QueryPartLayer.PartSelection;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes.Filter;
import cc.alcina.framework.servlet.job.JobContext;

/**
 * <p>
 * Uses a traversal parser to parse a string such as 'nam "bruce le"', to
 * generate property proposals 'name EQ "bruce le"' and 'name MATCHES "bruce
 * le"'
 * 
 * <p>
 * Parts of this are context/branch dependent (valid property names for the
 * class, valid operators for the property)
 */
public class PropertyFilterParser {
	static Logger logger = LoggerFactory.getLogger(PropertyFilterParser.class);

	public List<StandardLayerAttributes.Filter>
			proposeFilters(Class<? extends Entity> entityType, String query) {
		parse(entityType, query);
		traversal.throwExceptions();
		List<PartSelection> parts = traversal.selections()
				.get(QueryPartLayer.PartSelection.class);
		logger.info("{} parts", parts.size());
		parts.forEach(i -> logger.debug(
				"==========================================\n{}\n",
				i.getBranch().toResult().toStructuredString()));
		List<StandardLayerAttributes.Filter> result = new ArrayList<>();
		parts.forEach(part -> {
			BranchingParser.Result partResult = part.getBranch().toResult();
			QueryPartLayer.EntryToken token = (EntryToken) partResult
					.rootMeasure().getToken();
			PropertyProposer proposer = new PropertyProposer();
			token.proposePropertyFilters(entityType, partResult, proposer)
					.forEach(result::add);
		});
		logger.info("{} results", result.size());
		result.forEach(filter -> logger.info("{}", filter));
		return result;
	}

	public enum PropertyValueType {
		NUMERIC, ENUM, BOOLEAN, STRING, UNMATCHABLE;
	}

	static class PropertyProposer {
		List<StandardLayerAttributes.Filter> proposePropertyFilters(
				Class<? extends Entity> entityType, String propertyNamePart,
				FilterOperator op, String propertyValuePart, String sortKey,
				Direction sortDirection) {
			List<Property> properties = Reflections.at(entityType).properties()
					.stream().sorted(Comparator.comparing(Property::getName))
					.collect(Collectors.toList());
			List<Property> candidates = new StringMatches.PartialSubstring<Property>()
					.withMatchPartialsIfExactMatch(true)
					.match(properties, Property::getName, propertyNamePart)
					.stream().map(PartialSubstring.Match::getValue)
					.collect(Collectors.toList());
			List<Property> sortCandidates = sortKey == null ? List.of()
					: new StringMatches.PartialSubstring<Property>()
							.withMatchPartialsIfExactMatch(true)
							.match(properties, Property::getName, sortKey)
							.stream().map(PartialSubstring.Match::getValue)
							.limit(3).collect(Collectors.toList());
			List<Filter> filters = candidates.stream()
					.flatMap(p -> propertyFilter(p, op, propertyValuePart))
					.filter(Objects::nonNull).collect(Collectors.toList());
			if (sortCandidates.size() > 0) {
				filters = filters.stream().flatMap(f -> sortCandidates.stream()
						.map(sort -> f.withSort(sort.getName(), sortDirection)))
						.collect(Collectors.toList());
			}
			return filters;
		}

		Stream<StandardLayerAttributes.Filter> propertyFilter(Property property,
				FilterOperator op, String propertyValuePart) {
			Class type = property.getType();
			List<FilterOperator> ops = null;
			if (op == null) {
				if (type == String.class) {
					ops = List.of(FilterOperator.EQ, FilterOperator.MATCHES);
				} else if (Reflections.isAssignableFrom(Entity.class, type)) {
					ops = List.of(FilterOperator.MATCHES);
				} else if (Reflections.isAssignableFrom(Collection.class,
						type)) {
					ops = List.of(FilterOperator.MATCHES);
				} else {
					ops = List.of(FilterOperator.EQ);
				}
			} else {
				ops = List.of(op);
			}
			List<FilterOperator> f_ops = ops;
			Stream<ValueProposal> valueProposals = proposeValue(property,
					propertyValuePart);
			return valueProposals.flatMap(p -> f_ops.stream().map(_op -> Filter
					.of(property.getName(), _op, String.valueOf(p.value))));
		}

		Stream<ValueProposal> proposeValue(Property property,
				String propertyValuePart) {
			String lcQuery = propertyValuePart.toLowerCase();
			switch (getValueType(property, propertyValuePart)) {
			case NUMERIC:
				if (propertyValuePart.matches("\\d+")) {
					return Stream.of(new ValueProposal(
							Long.parseLong(propertyValuePart)));
				} else if (propertyValuePart.matches("\\(\\d+(, ?\\d+)+\\)")) {
					return Stream.of(new ValueProposal(
							TransformManager.idListToLongs(propertyValuePart)));
				} else {
					return Stream.of();
				}
			case BOOLEAN:
				return Stream.of(new ValueProposal("true".startsWith(lcQuery)));
			case ENUM:
				List<Object> enums = Arrays
						.asList(property.getType().getEnumConstants());
				Stream<Object> candidates = new StringMatches.PartialSubstring<Object>()
						.match(enums, Object::toString, lcQuery).stream()
						.map(PartialSubstring.Match::getValue);
				return candidates.map(ValueProposal::new);
			case STRING:
				return Stream.of(new ValueProposal(propertyValuePart));
			default:
				return Stream.of();
			}
		}

		static class ValueProposal {
			public ValueProposal(Object value) {
				this.value = value;
			}

			Object value;
		}
	}

	public static PropertyValueType getValueType(Property property,
			String propertyValuePart) {
		Class type = property.getType();
		if (Number.class.isAssignableFrom(type) || type == long.class
				|| type == int.class) {
			return PropertyValueType.NUMERIC;
		}
		if (type == Boolean.class || type == boolean.class) {
			return PropertyValueType.BOOLEAN;
		}
		if (ClassUtil.isEnumOrEnumSubclass(type)) {
			return PropertyValueType.ENUM;
		}
		if (type == String.class) {
			return PropertyValueType.STRING;
		}
		if (Collection.class.isAssignableFrom(type)) {
			return PropertyValueType.STRING;
		}
		if (Entity.class.isAssignableFrom(type)) {
			if (propertyValuePart.matches("\\d+")) {
				return PropertyValueType.NUMERIC;
			} else {
				return PropertyValueType.STRING;
			}
		}
		return PropertyValueType.UNMATCHABLE;
	}

	SelectionTraversal traversal;

	public void initialiseTraversal(Class<? extends Entity> entityType,
			String text) {
		traversal = new SelectionTraversal();
		TreeProcess.Node parentNode = JobContext.getSelectedProcessNode();
		traversal.select(new Query(parentNode, entityType, text));
		RootLayer rootLayer = new RootLayer();
		traversal.layers().setRoot(rootLayer);
	}

	void parse(Class<? extends Entity> entityType, String text) {
		initialiseTraversal(entityType, text);
		traversal.traverse();
	}

	static class Query extends InitialTextSelection
			implements PlainTextSelection {
		Class<? extends Entity> entityType;

		public Query(TreeProcess.Node parentNode,
				Class<? extends Entity> entityType, String text) {
			super(parentNode, text);
			this.entityType = entityType;
		}

		List<Property> getMatchingProperties(String text) {
			List<Property> properties = Reflections.at(entityType).properties()
					.stream().sorted(Comparator.comparing(Property::getName))
					.collect(Collectors.toList());
			List<Property> candidates = new StringMatches.PartialSubstring<Property>()
					.withMatchPartialsIfExactMatch(true)
					.match(properties, Property::getName, text).stream()
					.map(PartialSubstring.Match::getValue)
					.collect(Collectors.toList());
			return candidates;
		}
	}
}
