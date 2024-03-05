package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;

@Directed(tag = "bean-editor")
public class BeanEditor extends Model {
	private String header;

	private String description;

	private Bindable bindable;

	private String className;

	private boolean inert;

	@Directed.Transform(FormTransform.class)
	public Bindable getBindable() {
		return this.bindable;
	}

	@Binding(type = Type.CLASS_PROPERTY)
	public String getClassName() {
		return this.className;
	}

	@Directed
	public String getDescription() {
		return this.description;
	}

	@Directed
	public String getHeader() {
		return this.header;
	}

	@Binding(type = Type.PROPERTY)
	public boolean isInert() {
		return this.inert;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		Classes classes = event.getContext().node.annotation(Classes.class);
		if (classes != null) {
			className = Arrays.stream(classes.value()).map(Ax::cssify)
					.collect(Collectors.joining(" "));
		}
		super.onBeforeRender(event);
	}

	public void setBindable(Bindable bindable) {
		this.bindable = bindable;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void setInert(boolean inert) {
		set("inert", this.inert, inert, () -> this.inert = inert);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface Classes {
		ClassName[] value();
	}

	@Reflected
	public enum ClassName {
		grid, wide, horizontal_validation, vertical_validation, vertical,
		label_over
	}

	/**
	 * this indirection allows (requires) an app to define its own bean viewer
	 * implementations
	 */
	@Reflected
	public static class FormTransform extends
			AbstractContextSensitiveModelTransform<BaseSourcesPropertyChangeEvents, Bindable> {
		private Impl delegate;

		public FormTransform() {
			delegate = Registry.impl(Impl.class);
		}

		@Override
		public Bindable apply(BaseSourcesPropertyChangeEvents t) {
			return delegate.apply(t);
		}

		@Override
		public AbstractContextSensitiveModelTransform<BaseSourcesPropertyChangeEvents, Bindable>
				withContextNode(Node node) {
			return delegate.withContextNode(node);
		}

		@Registration(Impl.class)
		public abstract static class Impl extends
				AbstractContextSensitiveModelTransform<BaseSourcesPropertyChangeEvents, Bindable> {
		}
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	public @interface Actions {
		Class<? extends ModelEvent>[] value();
	}

	public static class NonAdjunct extends BeanEditor {
		@Override
		@BeanViewModifiers(adjunct = false)
		public Bindable getBindable() {
			return super.getBindable();
		}
	}

	public static class Viewer extends BeanEditor
			implements ModelTransform<Bindable, Viewer> {
		@Override
		public Viewer apply(Bindable t) {
			setBindable(t);
			return this;
		}

		@Override
		@BeanViewModifiers(editable = false, nodeEditors = true)
		public Bindable getBindable() {
			return super.getBindable();
		}
	}
}
