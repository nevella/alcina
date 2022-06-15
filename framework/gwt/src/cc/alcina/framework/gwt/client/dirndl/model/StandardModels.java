package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

public class StandardModels {
	@Directed(tag = "page", bindings = @Binding(from = "pageClassName", type = Type.CSS_CLASS, transform = ToStringFunction.ExplicitIdentity.class))
	public static class HeaderContentModel extends Model.WithBinding {
		private Object headerModel;

		private Object contentModel;

		private String pageClassName;

		public HeaderContentModel() {
			addBinding("pageClassName", PageCssClass.get(), "pageClassName");
		}

		@Directed
		public Object getContentModel() {
			return this.contentModel;
		}

		@Directed
		public Object getHeaderModel() {
			return this.headerModel;
		}

		public String getPageClassName() {
			return this.pageClassName;
		}

		public void setContentModel(Object contentModel) {
			Object old_contentModel = this.contentModel;
			this.contentModel = contentModel;
			propertyChangeSupport().firePropertyChange("contentModel",
					old_contentModel, contentModel);
		}

		public void setHeaderModel(Object headerModel) {
			Object old_headerModel = this.headerModel;
			this.headerModel = headerModel;
			propertyChangeSupport().firePropertyChange("headerModel",
					old_headerModel, headerModel);
		}

		public void setPageClassName(String pageClassName) {
			var old_pageClassName = this.pageClassName;
			this.pageClassName = pageClassName;
			propertyChangeSupport().firePropertyChange("pageClassName",
					old_pageClassName, pageClassName);
		}

		public enum Property implements PropertyEnum {
			pageClassName
		}
	}

	@Registration.Singleton
	public static class PageCssClass extends Bindable {
		public static StandardModels.PageCssClass get() {
			return Registry.impl(StandardModels.PageCssClass.class);
		}

		private String pageClassName;

		public String getPageClassName() {
			return this.pageClassName;
		}

		public void setPageClassName(String pageClassName) {
			String old_pageClassName = this.pageClassName;
			this.pageClassName = pageClassName;
			propertyChangeSupport().firePropertyChange("pageClassName",
					old_pageClassName, pageClassName);
		}
	}
}
