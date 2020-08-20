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
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class ModelTransformNodeRenderer extends DirectedNodeRenderer
		implements HasDirectedModel,HandlesModelBinding {
	@Override
	public List<Widget> renderWithDefaults(Node node) {
		List<Widget> result = new ArrayList<>();
		if (node.getModel() == null) {
			return Collections.emptyList();
		}
		Object directedModel = getDirectedModel(node);
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
		if (node.model == null) {
			return null;
		}
		ModelTransformNodeRendererArgs args = node
				.annotation(ModelTransformNodeRendererArgs.class);
		return Reflections.newInstance(args.value()).apply(node.model);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface ModelTransformNodeRendererArgs {
		Class<? extends ModelTransform> value();
	}

	@ClientInstantiable
	public abstract static class ModelTransform<A, B extends Bindable>
			implements Function<A, B> {
	}
}
