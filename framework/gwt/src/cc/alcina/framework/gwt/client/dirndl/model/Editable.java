package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.GwtEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvents;

public class Editable {
	@Directed(tag = "input", bindings = {
			@Binding(type = Type.PROPERTY, from = "value"),
			@Binding(type = Type.PROPERTY, from = "placeholder") })
	public static class StringInput extends Model {
		private String value;

		private String placeholder;

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
	}
}
