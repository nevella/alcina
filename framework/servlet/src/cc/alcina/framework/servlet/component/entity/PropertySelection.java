package cc.alcina.framework.servlet.component.entity;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.CommonUtils;

class PropertySelection extends AbstractSelection<Property>
		implements Selection.HasTableRepresentation.Children {
	private Object propertyValue;

	public PropertySelection(Selection<?> parent, Property value) {
		super(parent, value, value.getName());
		EntitySelection entitySelection = parent
				.ancestor(EntitySelection.class);
		Entity entity = entitySelection.get();
		propertyValue = get().safeGet(entity);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PropertySelection) {
			PropertySelection o = (PropertySelection) obj;
			return CommonUtils.equals(get(), o.get(), propertyValue,
					o.propertyValue);
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(get(), propertyValue);
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