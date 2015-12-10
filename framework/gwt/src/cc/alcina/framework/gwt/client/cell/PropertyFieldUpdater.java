package cc.alcina.framework.gwt.client.cell;

import com.google.gwt.cell.client.FieldUpdater;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

public class PropertyFieldUpdater implements FieldUpdater {
	private String propertyName;

	public PropertyFieldUpdater(String editablePropertyName) {
		this.propertyName = editablePropertyName;
	}

	@Override
	public void update(int index, Object object, Object value) {
		HasIdAndLocalId hili = (HasIdAndLocalId) object;
		TransformManager.get().registerDomainObject(hili);
		Reflections.propertyAccessor().setPropertyValue(hili, propertyName,
				value);
	}
}
