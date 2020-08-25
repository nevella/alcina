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
	public List<Widget> renderWithDefaults(Node node) {
		List<Widget> result = new ArrayList<>();
		Object directedModel = getDirectedModel(node);
		if (directedModel == null) {
			return Collections.emptyList();
		}
		Node child = node.addChild(directedModel, null, node.propertyReflector);
		result.addAll(child.render().widgets);
		return result;
	}

	// Not called
	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getDirectedModel(Node node) {
		ModelTransformNodeRendererArgs args = node
				.annotation(ModelTransformNodeRendererArgs.class);
		if (node.model == null && !args.transformsNull()) {
			return null;
		}
		return Reflections.newInstance(args.value()).apply(node.model);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface ModelTransformNodeRendererArgs {
		Class<? extends ModelTransform> value();
		boolean transformsNull() default false;
	}

	public interface ModelTransform<A, B extends Bindable>
			extends Function<A, B> {
	}

	@ClientInstantiable
	public abstract static class AbstractModelTransform<A, B extends Bindable>
			implements ModelTransform<A, B> {
	}

	public static class PlaceholderModelTransform
			extends AbstractModelTransform<Object, Bindable> {
		@Override
		public Bindable apply(Object t) {
			PlaceholderModel model = new PlaceholderModel();
			return model;
		}
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

	public static class BlankModelTransform
			extends AbstractModelTransform<Object, Bindable> {
		@Override
		public Bindable apply(Object t) {
			BlankModel model = new BlankModel();
			return model;
		}
	}

	@Directed(renderer = NotRenderedNodeRenderer.class)
	public static class BlankModel extends Model {
	}
}
