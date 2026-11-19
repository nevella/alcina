package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * A simple binding from the value property to the inner text of the rendered
 * model (default: a span element). Note that here is no listening <i>on</i> the
 * inner text of the label (binding is one-way)
 *
 *
 *
 *
 */
@Directed(tag = "span")
public class StringValue extends Model {
	public static final transient String VALUE = "value";

	private String value;

	public StringValue() {
	}

	public StringValue(String value) {
		setValue(value);
	}

	@Binding(type = Type.INNER_TEXT)
	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		set("value", this.value, value, () -> this.value = value);
	}
}