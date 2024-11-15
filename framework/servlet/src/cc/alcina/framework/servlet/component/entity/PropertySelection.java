package cc.alcina.framework.servlet.component.entity;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.servlet.component.traversal.SelectionTableArea;

class PropertySelection extends AbstractSelection<Property>
		implements Selection.HasTableRepresentation.Children {
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