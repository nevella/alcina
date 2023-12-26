package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;

/*
 * Typed for serialization
 * 
 * Serialiability is tested in the constructor
 */
@ReflectiveSerializer.Checks(ignore = true)
public class KeyValue<T> extends Model.All {
	public static <T> KeyValue<T> objectValue(String key, T value) {
		return new KeyValue(key, value);
	}

	public static KeyValue<String> stringValue(String key, Object value) {
		return new KeyValue(key, value == null ? "\u00A0" : value.toString());
	}

	public String key;

	public T value;

	@Binding(type = Type.CLASS_PROPERTY)
	public String className;

	// serialization constructor
	// FIXME - ser - remove, revert key/value to final
	KeyValue() {
	}

	KeyValue(String key, T value) {
		this.key = Ax.isBlank(key) ? "\u00A0" : key;
		if (value != null) {
			Preconditions.checkState(
					ReflectiveSerializer.isSerializable(value.getClass()));
		}
		this.value = value;
	}

	public KeyValue withClassName(String className) {
		this.className = className;
		return this;
	}

	public void addTo(List<KeyValue> list) {
		list.add(this);
	}
}
