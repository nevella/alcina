package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

/**
 * A simple panel heading
 */
@Directed(tag = "heading")
public class HeadingModel extends Model.Fields {
	@Binding(type = Type.INNER_TEXT)
	public String value;

	public HeadingModel() {
	}

	public HeadingModel(String value) {
		this.value = value;
	}
}
