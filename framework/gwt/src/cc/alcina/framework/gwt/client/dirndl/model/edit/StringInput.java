package cc.alcina.framework.gwt.client.dirndl.model.edit;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.Model.FocusOnBind;

@Directed(
	bindings = { @Binding(type = Type.PROPERTY, from = "value"),
			@Binding(type = Type.PROPERTY, from = "placeholder"),
			@Binding(type = Type.PROPERTY, from = "type") })
/*
 * FIXME - dirndl 1x1d - should handle DOM input + change events, have r/o
 * currentvalue (from input)
 *
 * More specifically: emit inputchanged, changed events (from DOM input/change).
 * Don't reemit those transformed events
 */
public class StringInput extends Model.WithNode implements FocusOnBind, HasTag {
	private String value;

	private String placeholder;

	private String type = "text";

	private boolean focusOnBind;

	private String tag = "input";

	private boolean selectAllOnBind;

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
		// remove post FIXME - dirndl 1x1d
		if (node != null) {
			sync();
		}
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
		this.type = type;
	}

	public void setValue(String value) {
		String old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	// temp
	public void sync() {
		setValue(node.getWidget().getElement().getPropertyString("value"));
	}
}