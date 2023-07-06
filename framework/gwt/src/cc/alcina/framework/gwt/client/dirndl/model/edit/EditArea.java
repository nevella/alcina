package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.BeforeInput;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Focusout;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Input;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Model.FocusOnBind;
import cc.alcina.framework.gwt.client.dirndl.model.dom.RelativeInputModel;

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
	bindings = { @Binding(type = Type.INNER_HTML, from = "value"),
			@Binding(type = Type.PROPERTY, from = "placeholder"),
			@Binding(
				type = Type.PROPERTY,
				literal = "true",
				to = "contenteditable") },
	receives = { DomEvents.Input.class, DomEvents.BeforeInput.class,
			DomEvents.Focusout.class },
	emits = { ModelEvents.Input.class })
public class EditArea extends Model implements FocusOnBind, HasTag,
		DomEvents.Input.Handler, DomEvents.BeforeInput.Handler,
		LayoutEvents.BeforeRender.Handler, DomEvents.Focusout.Handler {
	private String value;

	private String currentValue;

	private String placeholder;

	private boolean focusOnBind;

	private String tag = "edit";

	private boolean selectAllOnBind;

	boolean stripFontTagsOnInput = false;

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

	// @Feature.Ref(Feature_Dirndl_ContentDecorator.Constraint_NonSuggesting_DecoratorTag_Selection.class)
	@Override
	public void onBeforeInput(BeforeInput event) {
		stripFontTagsOnInput = provideElement().asDomNode().children
				.noElements();
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
		String elementValue = elementValue();
		if (Ax.ntrim(elementValue).isEmpty()) {
			elementValue = "";
		}
		setValue(elementValue);
	}

	@Override
	public void onInput(Input event) {
		currentValue = elementValue();
		if (stripFontTagsOnInput) {
			new RelativeInputModel().strip(provideElement(), "font");
		}
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
		DomNode firstNode = node().children.firstNode();
		return firstNode != null && firstNode.tagIs("placeholder");
	}

	DomNode node() {
		return provideElement().asDomNode();
	}
}