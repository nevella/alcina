package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

/**
 * A simple panel header
 */
@Directed(tag = "header")
public class HeaderModel extends Model.Fields {
	@Binding(type = Type.INNER_TEXT)
	public String value;

	public HeaderModel() {
	}

	public HeaderModel(String value) {
		this.value = value;
	}
}
