package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.BeanDescriptor;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
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

		DirectedResolver directed;

		Object childSource;

		List<Node> children = new ArrayList<>();

		Node parent;

		private PropertyReflector propertyReflector;

		Optional<Widget> render() {
			if (model == null) {
				return new NullNodeRenderer().renderWithDefaults(this);
			}
			this.descriptor = Reflections.beanDescriptorProvider()
					.getDescriptor(model);
			renderer = resolveRenderer();
			childSource = model;
			if (renderer instanceof HasDirectedModel) {
				childSource = ((HasDirectedModel) renderer).getDirectedModel();
			}
			List<PropertyReflector> propertyReflectors = Reflections
					.classLookup()
					.getPropertyReflectors((childSource.getClass()));
			if (propertyReflectors != null) {
				for (PropertyReflector propertyReflector : propertyReflectors) {
					Node child = new Node();
					child.parent = this;
					child.model = propertyReflector
							.getPropertyValue(childSource);
					children.add(child);
					child.propertyReflector = propertyReflector;
					child.model = propertyReflector
							.getPropertyValue(childSource);
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
			directed = Registry.impl(DirectedResolver.class, model.getClass());
			directed.setClassLocation(model.getClass());
			renderer = Reflections.classLookup()
					.newInstance(directed.renderer());
			return renderer;
		}

		private String path() {
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
	}
}
