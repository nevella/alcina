package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.FocusOnAttach;
import cc.alcina.framework.gwt.client.dirndl.behaviour.GwtEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.GwtEvents.Attach;

public class Editable {
	@Directed(tag = "input", bindings = {
			@Binding(type = Type.PROPERTY, from = "value"),
			@Binding(type = Type.PROPERTY, from = "placeholder") }, receives = GwtEvents.Attach.class)
	public static class StringInput extends Model implements FocusOnAttach {
		private String value;

		private String placeholder;

		private boolean focusOnAttach;

		public void setFocusOnAttach(boolean focusOnAttach) {
			this.focusOnAttach = focusOnAttach;
		}

		public String getPlaceholder() {
			return this.placeholder;
		}

		public void setPlaceholder(String placeholder) {
			this.placeholder = placeholder;
		}

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			String old_value = this.value;
			this.value = value;
			propertyChangeSupport().firePropertyChange("value", old_value,
					value);
		}

		@Override
		public boolean isFocusOnAttach() {
			return focusOnAttach;
		}
	}
}
