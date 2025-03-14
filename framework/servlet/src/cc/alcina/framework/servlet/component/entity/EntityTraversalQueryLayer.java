package cc.alcina.framework.servlet.component.entity;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.collections.PropertyFilter;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.InputsFromPreviousSibling;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.servlet.component.entity.EntityBrowser.Ui;
import cc.alcina.framework.servlet.component.entity.EntityTypesLayer.TypeSelection;
import cc.alcina.framework.servlet.component.entity.property.PropertyFilterParser;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes.Filter;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;

/*
 * inputs can be any of several types, so non-typed
 */
class EntityTraversalQueryLayer extends Layer
		implements InputsFromPreviousSibling {
	// for debugging
	Selection selection;

	@Override
	public void process(Selection selection) throws Exception {
		this.selection = selection;
		if (!EntityBrowser.peer().isSelected(selection)) {
			return;
		} else {
			if (selection instanceof EntitySelection) {
				processEntitySelection((EntitySelection) selection);
			} else if (selection instanceof PropertySelection) {
				processPropertySelection((PropertySelection) selection);
			} else if (selection instanceof EntityTypesLayer.TypeSelection) {
				processEntityTypeSelection(
						(EntityTypesLayer.TypeSelection) selection);
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	void processEntityTypeSelection(TypeSelection selection) {
		String segmentPath = Ui.traversingPlace().viewPath()
				.nthSegmentPath(index);
		Class<? extends Entity> entityClass = selection.get();
		Entity selected = segmentPath != null
				? Domain.find(entityClass, Long.parseLong(segmentPath))
				: null;
		Layer layer = getTraversal().getLayer(selection);
		Filter filter = Ui.traversingPlace().attributesOrEmpty(layer.index + 1)
				.get(Filter.class);
		DomainFilter domainFilter = filter == null ? null
				: filter.toDomainFilter(entityClass);
		String cacheMarker = filter != null && domainFilter == null
				? Ui.traversingPlace().truncateTo(layer.index + 1)
						.toTokenString()
				: null;
		// note this code double filters - but the second application will be
		// already filtered, so low perf impact
		Stream domainStream = domainFilter == null ? Domain.stream(entityClass)
				: Domain.query(entityClass).filter(domainFilter).stream();
		Stream<Entity> stream = Stream.concat(Stream.of(selected), domainStream)
				.distinct().filter(Objects::nonNull);
		addStream(selection, stream, cacheMarker);
	}

	void processPropertySelection(PropertySelection selection) {
		EntitySelection entitySelection = selection
				.ancestorSelection(EntitySelection.class);
		Entity entity = entitySelection.get();
		Object value = selection.get().get(entity);
		if (value instanceof Entity) {
			Entity childEntity = (Entity) value;
			select(new EntitySelection(selection, childEntity));
		} else if (value instanceof Set) {
			// don't try and cache this (won't help)
			addStream(selection, ((Set) value).stream(), null);
		}
	}

	void addStream(Selection selection, Stream stream, String cacheMarker) {
		Layer layer = getTraversal().getLayer(selection);
		Filter filter = Ui.traversingPlace().attributesOrEmpty(layer.index + 1)
				.get(Filter.class);
		if (filter != null) {
			stream = applyFilter(stream, filter);
			if (cacheMarker != null) {
				List list = Ui.cast().cache.get(cacheMarker);
				if (list != null) {
					stream = list.stream();
				}
			}
		}
		AtomicInteger valueCounter = new AtomicInteger();
		int limit = TraversalSettings.get().tableRows;
		List results = stream.limit(limit).toList();
		if (cacheMarker != null) {
			Ui.cast().cache.put(cacheMarker, results);
		}
		results.forEach(elem -> {
			if (elem instanceof Entity) {
				Entity childEntity = (Entity) elem;
				select(new EntitySelection(selection, childEntity));
			} else {
				select(new ValueSelection(selection, elem,
						valueCounter.getAndIncrement()));
			}
		});
	}

	Stream applyFilter(Stream stream, Filter filter) {
		stream = stream.filter(new StreamFilter(filter));
		return filter.sortKey == null ? stream
				: stream.sorted(new StreamSort(filter));
	}

	class StreamSort implements Comparator {
		Filter filter;

		Comparator cmp;

		StreamSort(Filter filter) {
			this.filter = filter;
			cmp = Comparator.nullsFirst(Comparator.naturalOrder());
		}

		@Override
		public int compare(Object o1, Object o2) {
			Object v1 = getValue(o1);
			Object v2 = getValue(o2);
			if (v1 != null && !(v1 instanceof Comparable)) {
				return 0;
			}
			if (v2 != null && !(v2 instanceof Comparable)) {
				return 0;
			}
			//
			int direction = filter.sortDirection.toComparatorMultiplier();
			return cmp.compare((Comparable) v1, (Comparable) v2) * direction;
		}

		Object getValue(Object o) {
			Property property = Reflections.at(o).property(filter.sortKey);
			return property.get(o);
		}
	}

	class StreamFilter implements Predicate {
		Filter filter;

		Property property;

		List<?> inValues;

		StreamFilter(Filter filter) {
			this.filter = filter;
		}

		public boolean test(Object o) {
			if (o == null) {
				return false;
			}
			if (filter.key == null) {
				String string = o.toString();
				if (string == null) {
					return false;
				}
				return matchesOrContains(string, filter.normalisedValue());
			} else {
				// FIXME - very inefficient (possibly reuse domainquery)
				if (this.property == null) {
					this.property = Reflections.at(o).property(filter.key);
					if (filter.op == FilterOperator.IN) {
						inValues = TransformManager
								.idListToLongs(filter.normalisedValue());
					}
				}
				Object propertyValue = property.get(o);
				if (propertyValue == null) {
					return false;
				}
				if (filter.op == FilterOperator.MATCHES) {
					return matchesOrContains(propertyValue.toString(),
							filter.normalisedValue());
				}
				if (filter.op == FilterOperator.IN) {
					if (propertyValue instanceof Long) {
						return inValues.contains(propertyValue);
					} else if (propertyValue instanceof Entity) {
						return inValues
								.contains(((Entity) propertyValue).getId());
					} else {
						throw new IllegalArgumentException(
								"Unsupported property type");
					}
				}
				Object filterValue = getTypedValue(property,
						filter.normalisedValue());
				PropertyFilter<Object> propertyFilter = new PropertyFilter<>(
						property.getName(), filterValue, filter.op);
				Object value = getTypedValue(property,
						propertyValue.toString());
				return propertyFilter.matchesValue(value);
			}
		}

		boolean matchesOrContains(String string, String match) {
			return string.matches(filter.value)
					|| string.matches(Ax.format("(?i).*%s.*", match));
		}

		Object getTypedValue(Property property, String propertyValuePart) {
			String lcQuery = propertyValuePart.toLowerCase();
			Class type = property.getType();
			switch (PropertyFilterParser.getValueType(property,
					propertyValuePart)) {
			case NUMERIC:
				long value = Long.parseLong(propertyValuePart);
				if (Entity.class.isAssignableFrom(type)) {
					return Domain.find(type, value);
				} else {
					return value;
				}
			case BOOLEAN:
				return Boolean.parseBoolean(propertyValuePart);
			case ENUM:
				return CommonUtils.getEnumValueOrNull(type, propertyValuePart);
			case STRING:
				return propertyValuePart;
			default:
				return Stream.of();
			}
		}
	}

	void processEntitySelection(EntitySelection selection) {
		Reflections.at(selection.entityType()).properties().stream()
				.filter(Property::isReadable)
				.sorted(Comparator.comparing(Property::getName))
				.map(p -> new PropertySelection(selection, p))
				.forEach(this::select);
	}

	static class ValueSelection extends AbstractSelection<Object> {
		private Object value;

		public ValueSelection(Selection<?> parent, Object value, int index) {
			super(parent, value, String.valueOf(index));
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}

		static class View extends AbstractSelection.View<ValueSelection> {
			public String computeText(PropertySelection selection) {
				return selection.toString();
			}
		}
	}
}
