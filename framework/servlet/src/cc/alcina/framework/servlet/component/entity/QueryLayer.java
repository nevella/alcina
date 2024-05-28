package cc.alcina.framework.servlet.component.entity;

import java.util.Comparator;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.InputsFromPreviousSibling;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.servlet.component.entity.EntityTypeLayer.EntitySelection;

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
				// throw new UnsupportedOperationException();
			} else {
			}
		}
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
			((Set) value).stream().limit(25).forEach(elem -> {
				if (elem instanceof Entity) {
					Entity childEntity = (Entity) elem;
					select(new EntitySelection(selection, childEntity));
				}
			});
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
}
