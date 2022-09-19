package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.Annotations;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 *
 * Transforms an input model to a renderable model, and renders. Note that the
 * renderable model class must be annotated with @Directed (or have a default
 * renderer - which classes such as String, Collection etc do)
 *
 * @author nick@alcina.cc
 *
 */
public class ModelTransformNodeRenderer extends DirectedNodeRenderer implements
		HasDirectedModel, HandlesModelBinding, RendersToParentContainer {
	@Override
	public Object getDirectedModel(Node node) {
		Directed.Transform args = node
				.annotation(Directed.Transform.class);
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
		// FIXME - dirndl - delegating renderer/simple model maybe overused?
		if (!Annotations.has(directedModel.getClass(), null, Directed.class)) {
			directedModel = new DelegatingNodeRenderer.SimpleDelegate(
					directedModel);
		}
		Node child = node.addChild(directedModel, null, node.getProperty());
		/*
		 * add node css
		 */
		List<Widget> widgets = child.render().widgets;
		widgets.forEach(w -> this.renderDefaults(node, w));
		result.addAll(widgets);
		return result;
	}

	public abstract static class AbstractContextSensitiveModelTransform<A, B>
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

	@Reflected
	public abstract static class AbstractModelTransform<A, B>
			implements ModelTransform<A, B> {
	}

	public interface ContextSensitiveTransform<A, B>
			extends ModelTransform<A, B> {
		public ContextSensitiveTransform<A, B>
				withContextNode(DirectedLayout.Node node);
	}

	@Reflected
	public static abstract class ListModelTransform<A, B>
			implements ModelTransform<List<A>, Model> {
		@Override
		public Model apply(List<A> t) {
			List<B> list = t.stream().map(this::mapElement)
					.collect(Collectors.toList());
			return new DelegatingNodeRenderer.SimpleDelegate(list);
		}

		protected abstract B mapElement(A a);
	}
	/*
	 * Note that this *must* return a result with an @Directed annotation - if
	 * transforming to (say) a list, use DelegatingNodeRenderer.SimpleDelegate
	 * to wrap
	 */

	public interface ModelTransform<A, B> extends Function<A, B> {
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
