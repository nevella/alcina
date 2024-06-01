package cc.alcina.framework.servlet.component.entity;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMatches;
import cc.alcina.framework.common.client.util.StringMatches.PartialSubstring;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes.Filter;

class PropertyFilterParser {
	List<StandardLayerAttributes.Filter>
			proposeFilters(Class<? extends Entity> entityType, String query) {
		if (query.equals("10")) {
			int debug = 3;
		}
		List<Filter> list = Registry.query(MatcherPart.class).implementations()
				.flatMap(
						part -> part.proposeFilters(entityType, query).stream())
				.toList();
		return list;
	}

	enum PropertyValueType {
		NUMERIC, ENUM, BOOLEAN, STRING, UNMATCHABLE;
	}

	static PropertyValueType getValueType(Property property) {
		Class type = property.getType();
		if (Number.class.isAssignableFrom(type) || type == long.class
				|| type == int.class || Entity.class.isAssignableFrom(type)) {
			return PropertyValueType.NUMERIC;
		}
		if (type == Boolean.class || type == boolean.class) {
			return PropertyValueType.BOOLEAN;
		}
		if (CommonUtils.isEnumOrEnumSubclass(type)) {
			return PropertyValueType.ENUM;
		}
		if (type == String.class) {
			return PropertyValueType.STRING;
		}
		return PropertyValueType.UNMATCHABLE;
	}

	static class PropertyProposer {
		List<StandardLayerAttributes.Filter> proposePropertyFilters(
				Class<? extends Entity> entityType, String propertyNamePart,
				FilterOperator op, String propertyValuePart) {
			List<Property> properties = Reflections.at(entityType).properties()
					.stream().sorted(Comparator.comparing(Property::getName))
					.toList();
			List<Property> candidates = new StringMatches.PartialSubstring<Property>()
					.match(properties, Property::getName, propertyNamePart)
					.stream().map(PartialSubstring.Match::getValue).toList();
			return candidates.stream()
					.flatMap(p -> propertyFilter(p, op, propertyValuePart))
					.filter(Objects::nonNull).toList();
		}

		Stream<StandardLayerAttributes.Filter> propertyFilter(Property property,
				FilterOperator op, String propertyValuePart) {
			Class type = property.getType();
			if (op == null) {
				if (type == String.class) {
					op = FilterOperator.MATCHES;
				} else {
					op = FilterOperator.EQ;
				}
			}
			FilterOperator _op = op;
			Stream<ValueProposal> valueProposals = proposeValue(property,
					propertyValuePart);
			return valueProposals.map(p -> Filter.of(property.getName(), _op,
					String.valueOf(p.value)));
		}

		Stream<ValueProposal> proposeValue(Property property,
				String propertyValuePart) {
			String lcQuery = propertyValuePart.toLowerCase();
			switch (getValueType(property)) {
			case NUMERIC:
				if (propertyValuePart.matches("\\d+")) {
					return Stream.of(new ValueProposal(
							Long.parseLong(propertyValuePart)));
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

	@Registration(MatcherPart.class)
	static abstract class MatcherPart implements Registration.AllSubtypes {
		abstract Pattern getPattern();

		abstract List<StandardLayerAttributes.Filter> proposeFilters(
				Class<? extends Entity> entityType, String query);

		static class _Id extends MatcherPart {
			@Override
			Pattern getPattern() {
				return Pattern.compile("\\d+");
			}

			@Override
			List<Filter> proposeFilters(Class<? extends Entity> entityType,
					String query) {
				Matcher matcher = getPattern().matcher(query);
				if (!matcher.matches()) {
					return List.of();
				}
				return List.of(Filter.of("id", FilterOperator.EQ, query));
			}
		}

		static class _PropOpValue extends MatcherPart {
			@Override
			Pattern getPattern() {
				String ops = Arrays.stream(FilterOperator.values())
						.map(Object::toString).collect(Collectors.joining("|"));
				return Pattern
						.compile(Ax.format("(?i)(\\S.*) (%s) (\\S.*)", ops));
			}

			@Override
			List<Filter> proposeFilters(Class<? extends Entity> entityType,
					String query) {
				Matcher matcher = getPattern().matcher(query);
				if (!matcher.matches()) {
					return List.of();
				}
				return proposePropertyFilters(entityType, matcher.group(1),
						CommonUtils.getEnumValueOrNull(FilterOperator.class,
								matcher.group(2)),
						matcher.group(3));
			}
		}

		static class _PropValue extends MatcherPart {
			@Override
			Pattern getPattern() {
				return Pattern.compile("(\\S.*) (\\S.*)");
			}

			@Override
			List<Filter> proposeFilters(Class<? extends Entity> entityType,
					String query) {
				Matcher matcher = getPattern().matcher(query);
				if (!matcher.matches()) {
					return List.of();
				}
				return proposePropertyFilters(entityType, matcher.group(1),
						null, matcher.group(2));
			}
		}

		List<Filter> proposePropertyFilters(Class<? extends Entity> entityType,
				String propertyNamePart, FilterOperator op,
				String propertyValuePart) {
			return new PropertyProposer().proposePropertyFilters(entityType,
					propertyNamePart, op, propertyValuePart);
		}
	}
}
