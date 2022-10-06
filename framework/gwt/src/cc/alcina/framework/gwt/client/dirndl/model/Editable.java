package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model.FocusOnBind;

public class Editable {
	@Directed(
		tag = "input",
		bindings = { @Binding(type = Type.PROPERTY, from = "value"),
				@Binding(type = Type.PROPERTY, from = "placeholder"),
				@Binding(type = Type.PROPERTY, from = "type") })
	public static class StringInput extends Model.WithNode
			implements FocusOnBind {
		private String value;

		private String placeholder;

		private String type = "text";

		private boolean focusOnBind;

		public String getPlaceholder() {
			return this.placeholder;
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

		public void setFocusOnBind(boolean focusOnBind) {
			this.focusOnBind = focusOnBind;
		}

		public void setPlaceholder(String placeholder) {
			this.placeholder = placeholder;
		}

		public void setType(String type) {
			// must set before attach
			this.type = type;
		}

		public void setValue(String value) {
			String old_value = this.value;
			this.value = value;
			propertyChangeSupport().firePropertyChange("value", old_value,
					value);
		}

		// temp
		public void sync() {
			setValue(node.getWidget().getElement().getPropertyString("value"));
		}
	}
}
