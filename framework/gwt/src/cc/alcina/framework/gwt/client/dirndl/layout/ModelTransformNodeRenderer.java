package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class ModelTransformNodeRenderer extends DirectedNodeRenderer
		implements HasDirectedModel {
	@Override
	public List<Widget> renderWithDefaults(Node node) {
		List<Widget> result = new ArrayList<>();
		Node child = node.addChild(getDirectedModel(node));
		result.addAll(child.render());
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
		return Reflections.newInstance(args.value())
				.apply((BaseBindable) node.model);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface ModelTransformNodeRendererArgs {
		Class<? extends ModelTransform> value();
	}

	@ClientInstantiable
	public abstract static class ModelTransform<A extends BaseBindable, B extends BaseBindable>
			implements Function<A, B> {
	}
}
