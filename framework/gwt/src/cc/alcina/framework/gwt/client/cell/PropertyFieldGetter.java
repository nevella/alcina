package cc.alcina.framework.gwt.client.cell;

import java.util.function.Function;

import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

public class PropertyFieldGetter<O> implements Function<O, Object> {
	private Field field;

	private Property property;

	public PropertyFieldGetter(String propertyName, Class clazz) {
		this.field = GwittirBridge.get().getField(clazz, propertyName, true,
				false);
		// FIXME - reflection.post-gwittir - use field.property
		this.property = Reflections.at(clazz).property(propertyName);
	}

	@Override
	public Object apply(O object) {
		Entity entity = (Entity) object;
		Object value = property.get(entity);
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
