package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class ModelTransformNodeRenderer extends DirectedNodeRenderer implements
		HasDirectedModel, HandlesModelBinding, RendersToParentContainer {
	@Override
	public Object getDirectedModel(Node node) {
		ModelTransformNodeRendererArgs args = node
				.annotation(ModelTransformNodeRendererArgs.class);
		if (node.model == null && !args.transformsNull()) {
			return null;
		}
		ModelTransform transform = Reflections.newInstance(args.value());
		if (transform instanceof ContextSensitiveTransform) {
			((ContextSensitiveTransform) transform).withContextNode(node);
		}
		return transform.apply(node.model);
	}

	// Not called
	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Widget> renderWithDefaults(Node node) {
		List<Widget> result = new ArrayList<>();
		Object directedModel = getDirectedModel(node);
		if (directedModel == null) {
			return Collections.emptyList();
		}
		Node child = node.addChild(directedModel, null, node.propertyReflector);
		/*
		 * add node css
		 */
		List<Widget> widgets = child.render().widgets;
		widgets.forEach(w -> this.renderDefaults(node, w));
		result.addAll(widgets);
		return result;
	}

	public abstract static class AbstractContextSensitiveModelTransform<A, B extends Bindable>
			extends AbstractModelTransform<A, B>
			implements ContextSensitiveTransform<A, B> {
		protected Node node;

		@Override
		public AbstractContextSensitiveModelTransform<A, B>
				withContextNode(Node node) {
			this.node = node;
			return this;
		}
	}

	@ClientInstantiable
	public abstract static class AbstractModelTransform<A, B extends Bindable>
			implements ModelTransform<A, B> {
	}

	public interface ContextSensitiveTransform<A, B extends Bindable>
			extends ModelTransform<A, B> {
		public ContextSensitiveTransform<A, B>
				withContextNode(DirectedLayout.Node node);
	}

	public interface ModelTransform<A, B extends Bindable>
			extends Function<A, B> {
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface ModelTransformNodeRendererArgs {
		boolean transformsNull() default false;

		Class<? extends ModelTransform> value();
	}

	@Directed(renderer = DelegatingNodeRenderer.class)
	public static class PlaceholderModel extends Model {
		private String value = "Placeholder";

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class PlaceholderModelTransform
			extends AbstractModelTransform<Object, Bindable> {
		@Override
		public Bindable apply(Object t) {
			PlaceholderModel model = new PlaceholderModel();
			return model;
		}
	}
}
