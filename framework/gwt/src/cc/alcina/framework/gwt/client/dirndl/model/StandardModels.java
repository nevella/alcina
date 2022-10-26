package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

public class StandardModels {
	@Directed(
		tag = "page",
		bindings = @Binding(
			from = "className",
			type = Type.CSS_CLASS,
			transform = ToStringFunction.ExplicitIdentity.class))
	@TypeSerialization(reflectiveSerializable = false)
	public static class HeaderContent extends Model.WithBinding {
		private Object header;

		private Object content;

		private String className;

		public HeaderContent() {
		}

		public String getClassName() {
			return this.className;
		}

		@Directed
		public Object getContent() {
			return this.content;
		}

		@Directed
		public Object getHeader() {
			return this.header;
		}

		public void setClassName(String className) {
			String old_className = this.className;
			this.className = className;
			propertyChangeSupport().firePropertyChange("className",
					old_className, className);
		}

		public void setContent(Object content) {
			var old_content = this.content;
			this.content = content;
			propertyChangeSupport().firePropertyChange("content", old_content,
					content);
		}

		public void setHeader(Object header) {
			var old_header = this.header;
			this.header = header;
			propertyChangeSupport().firePropertyChange("header", old_header,
					header);
		}

		/**
		 * <p>
		 * This is a WIP and may well be reverted - it's really the
		 * responsibility of the HeaderContentModel subclass (or a subcomponent)
		 * to track ModelEvents and adjust its style accordingly.
		 *
		 * </p>
		 */
		public static class BoundToPageCssClass extends HeaderContent {
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
