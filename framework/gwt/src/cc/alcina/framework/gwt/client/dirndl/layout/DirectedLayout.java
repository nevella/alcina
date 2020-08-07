package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import cc.alcina.framework.gwt.client.dirndl.layout.Directed.DirectedResolver;

public class DirectedLayout {
	Widget parent;

	public Widget render(Widget parent, Object model) {
		this.parent = parent;
		Node root = new Node();
		root.model = model;
		return root.render().orElse(null);
	}

	public class Node {
		Object model;

		DirectedNodeRenderer renderer;

		BeanDescriptor descriptor;

		Directed directed;

		Object childSource;

		List<Node> children = new ArrayList<>();

		Node parent;

		private PropertyReflector propertyReflector;

		private Accessor accessor;

		Optional<Widget> render() {
			return render(false);
		}

		private Optional<Widget> render(boolean intermediateChild) {
			this.descriptor = model == null ? null
					: Reflections.beanDescriptorProvider().getDescriptor(model);
			renderer = resolveRenderer();
			/*
			 * allow insertion of multiple nodes for one model object - loop
			 * without adding model children until the final Directed
			 */
			if (renderer instanceof HasWrappingDirecteds) {
				List<Directed> wrappers = ((HasWrappingDirecteds) renderer)
						.getWrappingDirecteds(this);
				Optional<Widget> rootResult = null;
				Optional<Widget> cursor = null;
				Node nodeCursor = this;
				for (Directed directed : wrappers) {
					Node wrapperChild = new Node();
					wrapperChild.parent = nodeCursor;
					wrapperChild.model = model;
					wrapperChild.propertyReflector = propertyReflector;
					wrapperChild.directed = directed;
					nodeCursor.children.add(wrapperChild);
					boolean intermediate = directed != Ax.last(wrappers);
					Optional<Widget> widget = wrapperChild.render(intermediate);
					if (rootResult == null) {
						rootResult = widget;
					}
					if (cursor != null) {
						((Panel) cursor.get()).add(widget.get());
					}
					cursor = widget;
					nodeCursor = wrapperChild;
				}
				return rootResult;
			}
			if (intermediateChild) {
				// will be handled by the calling loop
				return renderer.renderWithDefaults(this);
			}
			childSource = model;
			if (renderer instanceof HasDirectedModel) {
				childSource = ((HasDirectedModel) renderer).getDirectedModel();
			}
			if (childSource instanceof BaseBindable) {
				List<PropertyReflector> propertyReflectors = Reflections
						.classLookup()
						.getPropertyReflectors((childSource.getClass()));
				if (propertyReflectors != null) {
					for (PropertyReflector propertyReflector : propertyReflectors) {
						Node child = new Node();
						child.parent = this;
						child.propertyReflector = propertyReflector;
						child.model = propertyReflector
								.getPropertyValue(childSource);
						children.add(child);
					}
				}
			}
			Optional<Widget> result = renderer.renderWithDefaults(this);
			if (!result.isPresent()) {
				/*
				 * passthrough wrapper model
				 */
				Preconditions
						.checkArgument(descriptor.getProperties().length == 1);
				return children.get(0).render();
			}
			return result;
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
			return new AnnotationLocation(
					model == null ? null : model.getClass(), propertyReflector)
							.getAnnotation(clazz);
		}
	}
}
