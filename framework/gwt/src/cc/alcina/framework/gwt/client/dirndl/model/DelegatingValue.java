package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

@Directed.Delegating
@Registration({ Model.Value.class, FormModel.Viewer.class, Object.class })
@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class DelegatingValue extends Model.Value<Object> {
	Object value;

	@Override
	@Directed
	public Object getValue() {
		return this.value;
	}

	@Override
	public void setValue(Object value) {
		set("value", this.value, value, () -> this.value = value);
	}
}