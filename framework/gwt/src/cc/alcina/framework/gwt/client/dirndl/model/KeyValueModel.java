package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

@Directed(tag = "key-value")
@Directed.AllProperties
public class KeyValueModel extends Model.Fields {
	public static KeyValueModel objectValue(String key, Object value) {
		return new KeyValueModel(key, value);
	}

	public static KeyValueModel stringValue(String key, Object value) {
		return new KeyValueModel(key,
				value == null ? "\u00A0" : value.toString());
	}

	public final String key;

	public final Object value;

	@Binding(type = Type.CLASS_PROPERTY)
	public String className;

	KeyValueModel(String key, Object value) {
		this.key = Ax.isBlank(key) ? "\u00A0" : key;
		this.value = value;
	}

	public KeyValueModel withClassName(String className) {
		this.className = className;
		return this;
	}

	public void addTo(List<KeyValueModel> list) {
		list.add(this);
	}
}
