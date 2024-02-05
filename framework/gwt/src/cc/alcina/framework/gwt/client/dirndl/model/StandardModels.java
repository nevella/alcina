package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

public class StandardModels {
	@Registration.Singleton
	public static class PageCssClass extends Bindable {
		public static StandardModels.PageCssClass get() {
			return Registry.impl(StandardModels.PageCssClass.class);
		}

		private String className;

		public String getClassName() {
			return this.className;
		}

		public void setClassName(String className) {
			String old_className = this.className;
			this.className = className;
			propertyChangeSupport().firePropertyChange("className",
					old_className, className);
		}
	}

	public static class TextTitle extends Model.Fields
			implements Directed.NonClassTag {
		@Binding(type = Type.INNER_TEXT)
		public String text;

		@Binding(type = Type.PROPERTY)
		public String title;

		public TextTitle(String text, String title) {
			this.text = text;
			this.title = title;
		}
	}

	public static class LabelText extends Model.All
			implements Directed.NonClassTag {
		public String label;

		public String text;

		public LabelText(String label, String text) {
			this.label = label;
			this.text = text;
		}
	}
}
