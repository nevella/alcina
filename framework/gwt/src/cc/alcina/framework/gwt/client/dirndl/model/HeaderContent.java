package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.StandardModels.PageCssClass;

@Directed(
	tag = "page",
	bindings = @Binding(
		from = "className",
		type = Type.CLASS_PROPERTY,
		transform = ToStringFunction.ExplicitIdentity.class))
@TypeSerialization(reflectiveSerializable = false)
public class HeaderContent extends Model {
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
		propertyChangeSupport().firePropertyChange("className", old_className,
				className);
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
	 * This is a WIP and may well be reverted - it's really the responsibility
	 * of the HeaderContentModel subclass (or a subcomponent) to track
	 * ModelEvents and adjust its style accordingly.
	 *
	 * </p>
	 */
	public static class BoundToPageCssClass extends HeaderContent {
		public BoundToPageCssClass() {
			bindings().add("className", PageCssClass.get(), "pageClassName");
		}
	}
}