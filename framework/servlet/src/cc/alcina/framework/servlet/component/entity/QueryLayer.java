package cc.alcina.framework.servlet.component.entity;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.InputsFromPreviousSibling;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables.Single.PropertyValues;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.entity.EntityGraphView.Ui;
import cc.alcina.framework.servlet.component.entity.EntityTypesLayer.TypeSelection;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes.Filter;

/*
 * inputs can be any of several types, so non-typed
 */
class QueryLayer extends Layer implements InputsFromPreviousSibling {
	@Override
	public void process(Selection selection) throws Exception {
		if (!EntityGraphView.peer().isSelected(selection)) {
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
		String segmentPath = Ui.place().viewPath().nthSegmentPath(index);
		Entity selected = segmentPath != null
				? Domain.find(selection.get(), Long.parseLong(segmentPath))
				: null;
		Stream<Entity> stream = Stream
				.concat(Stream.of(selected), Domain.stream(selection.get()))
				.distinct().filter(Objects::nonNull);
		addStream(selection, stream);
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
			addStream(selection, ((Set) value).stream());
		}
	}

	void addStream(Selection selection, Stream stream) {
		Layer layer = Ui.traversal().getLayer(selection);
		Filter filter = Ui.place().attributesOrEmpty(layer.index + 1)
				.get(Filter.class);
		if (filter != null) {
			stream = applyFilter(stream, filter);
		}
		AtomicInteger valueCounter = new AtomicInteger();
		stream.limit(25).forEach(elem -> {
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
				Property property = Reflections.at(o).property(filter.key);
				Object propertyValue = property.get(o);
				if (propertyValue == null) {
					return false;
				}
				if (filter.op == FilterOperator.MATCHES) {
					return matchesOrContains(propertyValue.toString(),
							filter.normalisedValue());
				}
				Object value = getTypedValue(property,
						filter.normalisedValue());
				return Objects.equals(value, propertyValue);
			}
		}

		boolean matchesOrContains(String string, String match) {
			return string.matches(filter.value)
					|| string.matches(Ax.format("(?i).*%s.*", match));
		}

		Object getTypedValue(Property property, String propertyValuePart) {
			String lcQuery = propertyValuePart.toLowerCase();
			Class type = property.getType();
			switch (PropertyFilterParser.getValueType(property)) {
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
				.sorted(Comparator.comparing(Property::getName))
				.map(p -> new PropertySelection(selection, p))
				.forEach(this::select);
	}

	static class PropertySelection extends AbstractSelection<Property> {
		private Object propertyValue;

		public PropertySelection(Selection<?> parent, Property value) {
			super(parent, value, value.getName());
			EntitySelection entitySelection = parent
					.ancestorSelection(EntitySelection.class);
			Entity entity = entitySelection.get();
			propertyValue = get().get(entity);
		}

		@Override
		public String toString() {
			return String.valueOf(propertyValue);
		}

		static class View extends AbstractSelection.View<PropertySelection> {
			public String computeText(PropertySelection selection) {
				return selection.toString();
			}
		}
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

	public static class EntitySelection extends AbstractSelection<Entity> {
		public EntitySelection(Selection parent, Entity entity) {
			super(parent, entity, String.valueOf(entity.getId()));
		}

		static class View extends AbstractSelection.View<EntitySelection> {
			@Override
			public Model getExtended(EntitySelection selection) {
				return new Container(new EntityExtended(selection));
			}

			static class Container extends Model.All {
				Model model;

				Container(Model model) {
					this.model = model;
				}
			}

			static class EntityExtended extends Model.All {
				String id;

				@Directed.Transform(Tables.Single.class)
				Tables.Single.PropertyValues propertyValues = new Tables.Single.PropertyValues();

				EntityExtended(EntitySelection selection) {
					Entity entity = selection.get();
					if (entity == null) {
						return;
					}
					entity.domain().ensurePopulated();
					id = entity.toStringId();
					Reflections.at(selection.entityType()).properties().stream()
							.sorted(Comparator.comparing(Property::getName))
							.map(p -> new PropertyValue(selection, p))
							.forEach(pv -> pv.addTo(propertyValues));
				}

				static class PropertyValue extends Model.Fields
						implements DomEvents.Click.Handler {
					@Directed
					String value;

					private EntitySelection selection;

					private Property property;

					PropertyValue(EntitySelection selection,
							Property property) {
						this.selection = selection;
						this.property = property;
						Entity entity = selection.get();
						Object v = property.get(entity);
						value = Ax.trim(String.valueOf(v), 100);
					}

					void addTo(PropertyValues propertyValues) {
						propertyValues.add(property, this);
					}

					@Override
					public void onClick(Click event) {
						Ax.out("click :: %s", selection.get());
					}
				}
			}
		}

		public Class<? extends Entity> entityType() {
			return (Class<? extends Entity>) Domain
					.resolveEntityClass(get().getClass());
		}
	}
}
