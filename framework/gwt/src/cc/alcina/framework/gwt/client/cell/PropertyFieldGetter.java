package cc.alcina.framework.gwt.client.cell;

import java.util.function.Function;

import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

public class PropertyFieldGetter<O> implements Function<O, String> {
	private String propertyName;

	private Field field;

	public PropertyFieldGetter(String editablePropertyName, Class clazz) {
		this.propertyName = editablePropertyName;
		this.field = GwittirBridge.get().getField(clazz, propertyName, true,
				false);
	}

	@Override
	public String apply(O object) {
		HasIdAndLocalId hili = (HasIdAndLocalId) object;
		Object value = Reflections.propertyAccessor().getPropertyValue(hili,
				propertyName);
		if (field.getValidator() != null) {
			try {
				value = field.getValidator().validate(value);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		if (field.getConverter() != null) {
			value = field.getConverter().convert(value);
		}
		return CommonUtils.nullSafeToString(value);
	}
}
