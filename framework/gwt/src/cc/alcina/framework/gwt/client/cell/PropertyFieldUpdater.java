package cc.alcina.framework.gwt.client.cell;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.client.Window;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.validator.ValidationException;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;

public class PropertyFieldUpdater implements FieldUpdater {
	private Field field;

	private Property property;

	public PropertyFieldUpdater(String propertyName, Field field, Class clazz) {
		// FIXME - reflection.post-gwittir - use field.property
		this.property = Reflections.at(clazz).property(propertyName);
		this.field = field;
	}

	@Override
	public void update(int index, Object object, Object value) {
		Entity entity = (Entity) object;
		TransformManager.get().registerDomainObject(entity);
		if (field != null) {
			if (field.getValidator() != null) {
				try {
					value = field.getValidator().validate(value);
				} catch (ValidationException e) {
					Window.alert("Cannot update");
					return;
				}
			}
			BoundWidgetProvider cellProvider = field.getCellProvider();
			if (cellProvider instanceof ListBoxEnumProvider) {
				ListBoxEnumProvider listBoxEnumProvider = (ListBoxEnumProvider) cellProvider;
				Class<? extends Enum> enumClass = listBoxEnumProvider
						.getEnumClass();
				value = CommonUtils.getEnumValueOrNull(enumClass,
						CommonUtils.nullSafeToString(value));
			}
		}
		property.set(entity, value);
	}
}
