package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

@Directed(tag = "key-value")
@Directed.AllProperties
public class KeyValueModel extends Model.Fields {
	public static KeyValueModel objectValue(String key, Object value) {
		return new KeyValueModel(key, value);
	}

	public static KeyValueModel stringValue(String key, Object value) {
		return new KeyValueModel(key, value.toString());
	}

	public final String key;

	public final Object value;

	KeyValueModel(String key, Object value) {
		this.key = Ax.isBlank(key) ? "\u00A0" : key;
		this.value = value;
	}

	public void addTo(List<KeyValueModel> list) {
		list.add(this);
	}
}
