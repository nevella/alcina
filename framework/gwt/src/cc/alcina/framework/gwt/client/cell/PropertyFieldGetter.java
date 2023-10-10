package cc.alcina.framework.gwt.client.cell;

import java.util.function.Function;

import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;

public class PropertyFieldGetter<O> implements Function<O, Object> {
	private Field field;

	public PropertyFieldGetter(String propertyName, Class clazz) {
		this.field = BeanFields.query().forClass(clazz)
				.forPropertyName(propertyName).asEditable(true).getField();
		// FIXME - reflection.post-gwittir - use field.property
	}

	@Override
	public Object apply(O object) {
		Entity entity = (Entity) object;
		Object value = field.getProperty().get(entity);
		if (field.getValidator() != null) {
			try {
				value = field.getValidator().validate(value);
			} catch (Exception e) {
				ClientNotifications.get().log(Ax.format(
						"warn - invalid value in property get - %s - %s - %s",
						object, value, e.getMessage()));
				// throw new WrappedRuntimeException(e);
			}
		}
		if (field.getConverter() != null) {
			value = field.getConverter().convert(value);
		}
		return value;
	}
}
