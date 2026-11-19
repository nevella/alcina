package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

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

	@Directed(tag = "heading")
	public static class Container extends Model.All {
		public Model model;

		public Container(Model model) {
			this.model = model;
		}
	}
}
