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
	@Directed(tag = "page", bindings = @Binding(from = "className", type = Type.CSS_CLASS, transform = ToStringFunction.ExplicitIdentity.class))
	public static class HeaderContentModel extends Model.WithBinding {
		private Object headerModel;

		private Object contentModel;

		private String className;

		public HeaderContentModel() {
		}

		public String getClassName() {
			return this.className;
		}

		@Directed
		public Object getContentModel() {
			return this.contentModel;
		}

		@Directed
		public Object getHeaderModel() {
			return this.headerModel;
		}

		public void setClassName(String className) {
			String old_className = this.className;
			this.className = className;
			propertyChangeSupport().firePropertyChange("className",
					old_className, className);
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

		/**
		 * <p>
		 * This is a WIP and may well be reverted - it's really the
		 * responsibility of the HeaderContentModel subclass (or a subcomponent)
		 * to track ModelEvents and adjust its style accordingly.
		 *
		 * </p>
		 */
		public static class BoundToPageCssClass extends HeaderContentModel {
			public BoundToPageCssClass() {
				addBinding("className", PageCssClass.get(), "pageClassName");
			}
		}

		public enum Property implements PropertyEnum {
			className
		}
	}

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
}
