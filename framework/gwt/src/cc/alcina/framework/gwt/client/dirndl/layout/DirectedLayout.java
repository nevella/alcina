package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.BeanDescriptor;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.TypedParameter;
import cc.alcina.framework.common.client.logic.reflection.TypedParameter.Accessor;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.DirectedResolver;

public class DirectedLayout {
	Widget parent;

	public Widget render(Widget parent, Object model) {
		this.parent = parent;
		Node root = new Node();
		root.model = model;
		List<Widget> rendered = root.render();
		Preconditions.checkState(rendered.size() == 1);
		return rendered.get(0);
	}

	public static class Node {
		Object model;

		DirectedNodeRenderer renderer;

		BeanDescriptor descriptor;

		Directed directed;

		List<Node> children = new ArrayList<>();

		Node parent;

		private PropertyReflector propertyReflector;

		private Accessor accessor;

		List<Widget> render() {
			return render(false);
		}

		// FIXME - change from List to render result (mostly single widget)...
		private List<Widget> render(boolean intermediateChild) {
			this.descriptor = model == null ? null
					: Reflections.beanDescriptorProvider()
							.getDescriptorOrNull(model);
			renderer = resolveRenderer();
			if (renderer instanceof NotRenderedNodeRenderer) {
				// to avoid adding model children
				return Collections.emptyList();
			}
			/*
			 * allow insertion of multiple nodes for one model object - loop
			 * without adding model children until the final Directed
			 */
			if (renderer instanceof HasWrappingDirecteds) {
				List<Directed> wrappers = ((HasWrappingDirecteds) renderer)
						.getWrappingDirecteds(this);
				Widget rootResult = null;
				Widget cursor = null;
				Node nodeCursor = this;
				List<Widget> widgets = null;
				for (Directed directed : wrappers) {
					Node wrapperChild = nodeCursor.addChild(model);
					wrapperChild.propertyReflector = propertyReflector;
					wrapperChild.directed = directed;
					boolean intermediate = directed != Ax.last(wrappers);
					widgets = wrapperChild.render(intermediate);
					if (directed == Ax.last(wrappers) && widgets.size() != 1) {
						if (cursor != null) {
							widgets.forEach(((Panel) cursor)::add);
						}
					} else {
						Preconditions.checkState(widgets.size() == 1);
						Widget widget = widgets.get(0);
						if (rootResult == null) {
							rootResult = widget;
						}
						if (cursor != null) {
							((Panel) cursor).add(widget);
						}
						cursor = widget;
						nodeCursor = wrapperChild;
					}
				}
				return Collections.singletonList(rootResult);
			}
			if (intermediateChild) {
				// will be handled by the calling loop
				return renderer.renderWithDefaults(this);
			}
			if (model instanceof BaseBindable
					&& !(renderer instanceof HasDirectedModel)) {
				List<PropertyReflector> propertyReflectors = Reflections
						.classLookup()
						.getPropertyReflectors((model.getClass()));
				if (propertyReflectors != null) {
					for (PropertyReflector propertyReflector : propertyReflectors) {
						Node child = addChild(
								propertyReflector.getPropertyValue(model));
						child.propertyReflector = propertyReflector;
					}
				}
			}
			List<Widget> result = renderer.renderWithDefaults(this);
			return result;
		}

		Node addChild(Object childModel) {
			Node child = new Node();
			child.parent = this;
			child.model = childModel;
			children.add(child);
			return child;
		}

		private DirectedNodeRenderer resolveRenderer() {
			if (model == null && propertyReflector
					.getAnnotation(Directed.class) == null) {
				return new NullNodeRenderer();
			}
			if (directed == null) {
				Class clazz = model == null ? void.class : model.getClass();
				directed = Registry.impl(DirectedResolver.class, clazz);
				((DirectedResolver) directed).setLocation(
						new AnnotationLocation(clazz, propertyReflector));
			}
			Class<? extends DirectedNodeRenderer> rendererClass = directed
					.renderer();
			if (rendererClass == VoidNodeRenderer.class) {
				if (model.getClass() == String.class) {
					int debug = 3;
				}
				rendererClass = Registry.get().lookupSingle(
						DirectedNodeRenderer.class, model.getClass());
			}
			renderer = Reflections.classLookup().newInstance(rendererClass);
			return renderer;
		}

		String path() {
			String thisLoc = Ax.format("[%s]", model == null ? "null model"
					: model.getClass().getSimpleName());
			if (propertyReflector != null) {
				thisLoc = propertyReflector.getPropertyName() + "." + thisLoc;
			}
			if (parent == null) {
				return thisLoc;
			} else {
				return parent.path() + "." + thisLoc;
			}
		}

		@Override
		public String toString() {
			return path();
		}

		public Accessor parameters() {
			if (accessor == null) {
				accessor = new TypedParameter.Accessor(directed.parameters());
			}
			return accessor;
		}

		public <A extends Annotation> A annotation(Class<A> clazz) {
			// TODO - dirndl - do we resolve? I think...not - just directed
			// (hardcode there)
			A annotation = new AnnotationLocation(
					model == null ? null : model.getClass(), propertyReflector)
							.getAnnotation(clazz);
			if (annotation == null && parent != null
					&& parent.renderer instanceof CollectionNodeRenderer) {
				return parent.annotation(clazz);
			}
			return annotation;
		}
	}
}
