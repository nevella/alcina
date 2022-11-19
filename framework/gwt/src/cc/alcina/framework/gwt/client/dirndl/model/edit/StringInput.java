package cc.alcina.framework.gwt.client.dirndl.model.edit;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.behaviour.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.behaviour.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Model.FocusOnBind;

@Directed(
	bindings = { @Binding(type = Type.PROPERTY, from = "value"),
			@Binding(type = Type.PROPERTY, from = "placeholder"),
			@Binding(type = Type.PROPERTY, from = "type") },
	receives = { DomEvents.Change.class, DomEvents.Input.class })
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
public class StringInput extends Model.WithNode implements FocusOnBind, HasTag,
		DomEvents.Change.Handler, DomEvents.Input.Handler {
	private String value;

	private String currentValue;

	private String placeholder;

	private String type = "text";

	private boolean focusOnBind;

	private String tag = "input";

	private boolean selectAllOnBind;

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

	public String getType() {
		return this.type;
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
			Widget widget = event.getContext().node.getWidget();
			Element elem = widget.getElement();
			TextBoxImpl.setTextBoxSelectionRange(elem, 0,
					elem.getPropertyString("value").length());
		}
	}

	@Override
	public void onChange(Change event) {
		currentValue = elementValue();
		setValue(currentValue);
		NodeEvent.Context.newModelContext(event, node)
				.fire(ModelEvents.Change.class);
	}

	@Override
	public void onInput(Input event) {
		currentValue = elementValue();
		NodeEvent.Context.newModelContext(event, node)
				.fire(ModelEvents.Input.class);
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

	public void setType(String type) {
		// must set before attach
		Preconditions.checkState(node == null);
		this.type = type;
	}

	public void setValue(String value) {
		String old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	private String elementValue() {
		return provideElement().getPropertyString("value");
	}
}