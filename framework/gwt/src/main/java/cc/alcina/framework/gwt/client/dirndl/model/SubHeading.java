package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

/**
 * A simple panel sub-heading
 */
public class SubHeading extends Model.Fields {
	@Binding(type = Type.INNER_TEXT)
	public String value;

	public SubHeading() {
	}

	public SubHeading(String value) {
		this.value = value;
	}

	@Directed(tag = "sub-heading")
	public static class Container extends Model.All {
		public Model model;

		public Container(Model model) {
			this.model = model;
		}
	}
}
