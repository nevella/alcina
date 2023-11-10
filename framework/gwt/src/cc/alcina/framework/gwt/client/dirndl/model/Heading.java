package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;

/**
 * A simple panel heading
 */
public class Heading extends Model.Fields {
	@Binding(type = Type.INNER_TEXT)
	public String value;

	public Heading() {
	}

	public Heading(String value) {
		this.value = value;
	}
}
