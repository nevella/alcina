package cc.alcina.framework.gwt.client.dirndl.model.edit;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
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
@Directed(
	bindings = { @Binding(type = Type.PROPERTY, from = "value"),
			@Binding(type = Type.PROPERTY, from = "placeholder"),
			@Binding(type = Type.PROPERTY, from = "type"),
			@Binding(type = Type.PROPERTY, from = "spellcheck"),
			@Binding(type = Type.PROPERTY, from = "autocomplete"),
			@Binding(type = Type.INNER_TEXT, from = "innerText") },
	receives = { DomEvents.Change.class, DomEvents.Input.class },
	emits = { ModelEvents.Change.class, ModelEvents.Input.class })
public class StringInput extends Model
		implements FocusOnBind, HasTag, DomEvents.Change.Handler,
		DomEvents.Input.Handler, LayoutEvents.BeforeRender.Handler {
	private String value;

	private String currentValue;

	private String placeholder;

	private String autocomplete;

	private String type = "text";

	private boolean focusOnBind;

	private String tag = "input";

	private boolean selectAllOnBind;

	private String spellcheck = "false";

	// used for element population if element is a textarea (dom quirk, really)
	private String innerText;

	public StringInput() {
	}

	public StringInput(String value) {
		setValue(value);
	}

	public String getAutocomplete() {
		return this.autocomplete;
	}

	public String getCurrentValue() {
		if (currentValue == null) {
			currentValue = elementValue();
		}
		return this.currentValue;
	}

	public String getInnerText() {
		return this.innerText;
	}

	public String getPlaceholder() {
		return this.placeholder;
	}

	public String getSpellcheck() {
		return this.spellcheck;
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
	public void onBeforeRender(BeforeRender event) {
		if (tag.equals("textarea") && innerText == null) {
			innerText = value;
		}
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
		event.reemitAs(this, ModelEvents.Change.class);
	}

	@Override
	public void onInput(Input event) {
		currentValue = elementValue();
		event.reemitAs(this, ModelEvents.Input.class, currentValue);
	}

	@Override
	public String provideTag() {
		return getTag();
	}

	public void setAutocomplete(String autocomplete) {
		this.autocomplete = autocomplete;
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

	public void setSpellcheck(String spellcheck) {
		this.spellcheck = spellcheck;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void setType(String type) {
		// must set before attach
		Preconditions.checkState(!provideIsBound());
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