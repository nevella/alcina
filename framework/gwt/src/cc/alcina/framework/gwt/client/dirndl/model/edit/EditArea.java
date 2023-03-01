package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusout;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Model.FocusOnBind;

/**
 * <p>
 * This class models an editable text field, rendering as either an
 * <code>&lt;input&gt;</code> or <code>&lt;textarea&gt;</code> DOM element.
 *
 * <p>
 * It maintains a 'currentValue' r/o property, tracking the current edited (but
 * not committed) value of the element (an element is committed - and the value
 * property changed - on focus loss or [enter] in the case of input elements -
 * not on every change to the visible text) - see
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/input_event
 *
 * <p>
 * Note that the StringInput.value property may be out of sync with the
 * element.value property - since the StringInput.value property tracks the
 * committed value
 *
 * <p>
 * It fires modelevents <code>&lt;Change&gt;</code> and
 * <code>&lt;Input&gt;</code>, wrapping the corresponding DOM eventt.s
 *
 *
 * @author nick@alcina.cc
 *
 */
/*
 * FIXME - dirndl 1x1d - should handle DOM input + change events, have r/o
 * currentvalue (from input)
 *
 * More specifically: emit inputchanged, changed events (from DOM input/change).
 * Don't reemit those transformed events
 */
@Directed(
	bindings = { @Binding(type = Type.INNER_HTML, from = "value"),
			@Binding(type = Type.PROPERTY, from = "placeholder"),
			@Binding(
				type = Type.PROPERTY,
				literal = "true",
				to = "contenteditable") },
	receives = { DomEvents.Input.class, DomEvents.Focusout.class })
public class EditArea extends Model
		implements FocusOnBind, HasTag, DomEvents.Input.Handler,
		LayoutEvents.BeforeRender.Handler, DomEvents.Focusout.Handler {
	private String value;

	private String currentValue;

	private String placeholder;

	private boolean focusOnBind;

	private String tag = "edit";

	private boolean selectAllOnBind;

	public EditArea() {
	}

	public EditArea(String value) {
		setValue(value);
	}

	public String getCurrentValue() {
		if (currentValue == null) {
			currentValue = elementValue();
		}
		return this.currentValue;
	}

	public String getPlaceholder() {
		return this.placeholder;
	}

	public String getTag() {
		return this.tag;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public boolean isFocusOnBind() {
		return focusOnBind;
	}

	public boolean isSelectAllOnBind() {
		return this.selectAllOnBind;
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (isSelectAllOnBind()) {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void onFocusout(Focusout event) {
		setValue(elementValue());
	}

	@Override
	public void onInput(Input event) {
		currentValue = elementValue();
		event.reemitAs(this, ModelEvents.Input.class);
	}

	@Override
	public String provideTag() {
		return getTag();
	}

	public void setFocusOnBind(boolean focusOnBind) {
		this.focusOnBind = focusOnBind;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	public void setSelectAllOnBind(boolean selectAllOnBind) {
		this.selectAllOnBind = selectAllOnBind;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void setValue(String value) {
		String old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	private String elementValue() {
		if (!provideIsShowingPlaceholder()) {
			return provideElement().getInnerHTML();
		} else {
			return null;
		}
	}

	private boolean provideIsShowingPlaceholder() {
		// TODO - disallow placeholder in normalisation
		return node().children.firstNode().tagIs("placeholder");
	}

	DomNode node() {
		return DomNode.from(provideElement());
	}
}