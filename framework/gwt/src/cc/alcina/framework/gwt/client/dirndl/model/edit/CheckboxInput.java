package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
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
 * <p>
 * FIXME - dirndl - this doesn't follow spec around the checked value (which
 * should only apply on initial render, subsequently value should apply)
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

	public CheckboxInput(Boolean value) {
		setValue(value);
	}

	private boolean elementValue() {
		return provideElement().getPropertyBoolean("value") || Objects
				.equals(provideElement().getPropertyString("value"), "on");
	}

	@Override
	@Binding(type = Type.PROPERTY, to = "checked")
	public Boolean getValue() {
		return this.value;
	}

	@Override
	public void onChange(Change event) {
		setValue(!CommonUtils.bv(getValue()));
		event.reemitAs(this, ModelEvents.Change.class, getValue());
	}

	@Override
	public void setValue(Boolean value) {
		set(VALUE, this.value, value, () -> this.value = value);
	}

	public static class To implements ModelTransform<Boolean, CheckboxInput> {
		@Override
		public CheckboxInput apply(Boolean t) {
			return new CheckboxInput(t);
		}
	}
}