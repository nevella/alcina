package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * This class models an editable boolean field, rendering as a Checkbox DOM
 * element.
 *
 *
 * <p>
 * It fires the <code>&lt;Change&gt;</code> model event , wrapping the
 * corresponding DOM event
 *
 *
 *
 *
 */
@Directed(
	tag = "input",
	bindings = @Binding(
		type = Type.PROPERTY,
		to = "type",
		literal = "checkbox"),
	emits = { ModelEvents.Change.class })
@Registration({ Model.Value.class, FormModel.Editor.class, Boolean.class })
@Registration({ Model.Value.class, FormModel.Editor.class, boolean.class })
public class CheckboxInput extends Model.Value<Boolean>
		implements DomEvents.Change.Handler {
	public static final transient String VALUE = "value";

	private Boolean value;

	public CheckboxInput() {
	}

	public CheckboxInput(boolean value) {
		setValue(value);
	}

	private boolean elementValue() {
		return provideElement().getPropertyBoolean("checked");
	}

	@Override
	@Binding(type = Type.PROPERTY, to = "checked")
	public Boolean getValue() {
		return this.value;
	}

	@Override
	public void onChange(Change event) {
		setValue(elementValue());
		event.reemitAs(this, ModelEvents.Change.class);
	}

	@Override
	public void setValue(Boolean value) {
		set(VALUE, this.value, value, () -> this.value = value);
	}
}