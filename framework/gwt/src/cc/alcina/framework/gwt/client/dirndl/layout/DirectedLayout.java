package cc.alcina.framework.gwt.client.dirndl.layout;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.DirectedResolver;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeTopic;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class DirectedLayout {
	private static Logger logger = LoggerFactory
			.getLogger(DirectedLayout.class);
	static {
		AlcinaLogUtils.sysLogClient(DirectedLayout.class, Level.OFF);
	}

	Widget parent;

	public Widget render(ContextResolver resolver, Widget parent,
			Object model) {
		this.parent = parent;
		Node root = new Node(resolver, model);
		List<Widget> rendered = root.render().widgets;
		Preconditions.checkState(rendered.size() == 1);
		return rendered.get(0);
	}

	@ClientInstantiable
	public static class ContextResolver {
		public Object resolve(Object model) {
			return model;
		}
	}

	/**
	 * <p>
	 * In most cases, there is a 1-1 correspondence between a node and a widget
	 * level (a {1,n} group of widgets with the same parent) - there are three
	 * exceptions:
	 * <ul>
	 * <li>MultipleNodeRenderer - for wrapping tags for one model element -
	 * multiple nested widgets correspond to one node
	 * <li>ModelTransformNodeRenderer - both the original model node and the
	 * transformed model node (its only child node) correspond to the same
	 * widgets
	 * <li>DelegatingNodeRenderer - as per ModelTransformNodeRenderer
	 * <p>
	 * 
	 * <p>
	 * ...shades of the DOM render tree...
	 * </p>
	 */
	public static class Node {
		public Object getModel() {
			return this.model;
		}

		private ContextResolver resolver;

		final Object model;

		Rendered rendered = new Rendered();

		DirectedNodeRenderer renderer;

		BeanDescriptor descriptor;

		Directed directed;

		LinkedList<Node> children = new LinkedList<>();

		Node parent;

		PropertyReflector propertyReflector;

		List<RemovablePropertyChangeListener> listeners = new ArrayList<>();

		private int index;

		public PropertyReflector changeSource;

		private boolean intermediate;

		protected Node(ContextResolver resolver, Object model) {
			this.resolver = resolver;
			this.model = resolver.resolve(model);
		}

		public DirectedNodeRenderer resolveRenderer() {
			DirectedNodeRenderer renderer = null;
			if (model == null && (propertyReflector == null || propertyReflector
					.getAnnotation(Directed.class) == null)) {
				return new NullNodeRenderer();
			}
			if (directed == null) {
				// FIXME - dirndl.0 - no, resolver is from the node tree, not
				// the class
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

		public void publishTopic(Class<? extends NodeTopic> topic) {
			rendered.behaviours.forEach(binding -> binding.publishTopic(topic));
		}

		public Node resolveNode(String path) {
			String[] segments = path.split("/");
			Node firstSegment = resolveNodeSegment(segments[0]);
			if (segments.length == 1) {
				Preconditions
						.checkState(firstSegment.rendered.widgets.size() == 1);
				return firstSegment;
			}
			return firstSegment.resolveNodeSegment(
					path.substring(segments[0].length() + 1));
		}

		public Widget resolveWidget(String path) {
			return resolveNode(path).rendered.verifySingleWidget();
		}

		@Override
		public String toString() {
			return path();
		}

		private void bindBehaviours() {
			if (directed == null || directed.behaviours().length == 0) {
				return;
			}
			Preconditions.checkState(rendered.widgets.size() == 1);
			rendered.behaviours = Arrays.stream(directed.behaviours())
					.map(BehaviourBinding::new).collect(Collectors.toList());
			rendered.behaviours.forEach(BehaviourBinding::bind);
		}

		private String pathSegment() {
			String thisLoc = Ax.format("{%s}", model == null ? "null model"
					: model.getClass().getSimpleName());
			if (propertyReflector != null) {
				thisLoc = propertyReflector.getPropertyName() + "." + thisLoc;
			} else {
				if (parent != null && renderer != null) {
					thisLoc = Ax.format("(%s).%s",
							renderer.getClass().getSimpleName(), thisLoc);
				} else {
					thisLoc = "---" + "." + thisLoc;
				}
			}
			if (intermediate && rendered != null
					&& rendered.widgets.size() > 0) {
				thisLoc += Ax.format("(%s)",
						rendered.widgets.get(0).getElement().getTagName());
			}
			return thisLoc;
		}

		private void populateWidgets(boolean intermediateChild) {
			this.descriptor = model == null ? null
					: Reflections.beanDescriptorProvider()
							.getDescriptorOrNull(model);
			renderer = resolveRenderer();
			if (renderer instanceof NotRenderedNodeRenderer) {
				// to avoid adding model children
				return;
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
				intermediate = true;
				for (Directed directed : wrappers) {
					// for the moment, wrapped nodes have no listeners
					// (including the leaf).
					Node wrapperChild = nodeCursor.addChild(model,
							propertyReflector, null);
					wrapperChild.directed = directed;
					wrapperChild.intermediate = directed != Ax.last(wrappers);
					wrapperChild.render(wrapperChild.intermediate);
					widgets = wrapperChild.rendered.widgets;
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
				rendered.widgets = Collections.singletonList(rootResult);
				return;
			}
			if (intermediateChild) {
				// will be handled by the calling loop
				rendered.widgets = renderer.renderWithDefaults(this);
				return;
			}
			if (model instanceof Bindable
					&& !(renderer instanceof HandlesModelBinding)) {
				List<PropertyReflector> propertyReflectors = Reflections
						.classLookup()
						.getPropertyReflectors((model.getClass()));
				if (propertyReflectors != null) {
					for (PropertyReflector propertyReflector : propertyReflectors) {
						Object childModel = propertyReflector
								.getPropertyValue(model);
						Node child = addChild(childModel, propertyReflector,
								propertyReflector);
					}
				}
			}
			rendered.widgets = renderer.renderWithDefaults(this);
			return;
		}

		private void addListeners(Node child) {
			if (child.changeSource == null) {
				return;
			}
			Object childModel = child.model;
			// even though this (the parent) handles changes,
			// binding/unbinding on node removal is the responsibility of the
			// child, so we add to the child's listeners list
			if (!child.changeSource.isReadOnly()) {
				// FIXME - dirndl.1 - don't add this to form/table cells
				ChildReplacer listener = new ChildReplacer((Bindable) model,
						child.changeSource.getPropertyName(), child);
				logger.info("added listener :: {} :: {} :: {} :: {}",
						child.pathSegment(), child.hashCode(),
						child.changeSource.getPropertyName(),
						listener.hashCode());
				child.listeners.add(listener);
			}
			if (childModel instanceof Model) {
				ChildReplacer listener = new ChildReplacer(
						(Bindable) childModel, null, child);
				logger.info("added listener :: {} :: {} :: {} :: {}",
						child.pathSegment(), child.hashCode(), "(fireUpdate)",
						listener.hashCode());
				child.listeners.add(listener);
			}
		}

		/*
		 * don't remove widgets (handled by swap, and no need to descend widget
		 * removal)
		 */
		private void removeChild(Node child) {
			child.unbind();
			children.remove(child.index);
		}

		private void render(boolean intermediateChild) {
			DirectedContextResolver directedContextResolver = annotation(
					DirectedContextResolver.class);
			if (directedContextResolver != null) {
				resolver = Reflections
						.newInstance(directedContextResolver.value());
			}
			populateWidgets(intermediateChild);
			bindBehaviours();
		}

		private Node resolveNodeSegment(String segment) {
			switch (segment) {
			case ".":
				return this;
			case "..":
				return parent;
			default:
				List<PropertyReflector> propertyReflectors = Reflections
						.classLookup()
						.getPropertyReflectors((model.getClass()));
				int idx = 0;
				for (PropertyReflector propertyReflector : propertyReflectors) {
					if (propertyReflector.getPropertyName().equals(segment)) {
						return children.get(idx);
					}
					idx++;
				}
				break;
			}
			// error/missing/unresolved
			return null;
		}

		private void unbind() {
			if (model instanceof Model) {
				((Model) model).unbind();
			}
			children.forEach(Node::unbind);
			listeners.forEach(RemovablePropertyChangeListener::unbind);
		}

		Node addChild(Object childModel, PropertyReflector definingReflector,
				PropertyReflector changeSource) {
			Node child = new Node(resolver, childModel);
			child.propertyReflector = definingReflector;
			child.changeSource = changeSource;
			child.resolver = resolver;
			child.parent = this;
			child.index = children.size();
			addListeners(child);
			children.add(child);
			return child;
		}

		String path() {
			String thisLoc = pathSegment();
			if (parent == null) {
				return thisLoc;
			} else {
				return parent.path() + ".\n" + thisLoc;
			}
		}

		Rendered render() {
			render(false);
			return rendered;
		}

		public class Rendered {
			List<Widget> widgets = new ArrayList<>();

			public List<BehaviourBinding> behaviours;

			public int getChildIndex(Widget childWidget) {
				FlowPanel panel = verifyContainer();
				return panel.getWidgetIndex(childWidget);
			}

			public Widget verifySingleWidget() {
				Preconditions.checkState(widgets.size() == 1);
				return widgets.get(0);
			}

			public Optional<Widget> verifySingleOrNullWidget() {
				Preconditions.checkState(widgets.size() <= 1);
				return Optional.ofNullable(Ax.first(widgets));
			}

			private FlowPanel verifyContainer() {
				return (FlowPanel) verifySingleWidget();
			}

			public Optional<Widget>
					verifySingleWidgetOrPredecessorSingleOrNoWidget() {
				Node cursor = Node.this;
				while (cursor != null) {
					Optional<Widget> widget = cursor.rendered
							.verifySingleOrNullWidget();
					if (widget.isPresent()) {
						return widget;
					}
					cursor = parent.childBefore(cursor);
				}
				return Optional.empty();
			}

			public void swapChildWidgets(
					Optional<Widget> insertAfterChildWidget,
					Optional<Widget> oldChildWidget,
					Optional<Widget> newChildWidget) {
				if (newChildWidget.isPresent()) {
					int index = insertAfterChildWidget.map(this::getChildIndex)
							.orElse(-1) + 1;
					verifyContainer().insert(newChildWidget.get(), index);
				}
				oldChildWidget.ifPresent(Widget::removeFromParent);
			}
		}

		private class ChildReplacer extends RemovablePropertyChangeListener {
			private Node child;

			private ChildReplacer(SourcesPropertyChangeEvents bound,
					String propertyName, Node child) {
				super(bound, propertyName);
				// almost surely this is it - overbinding
				this.child = child;
			}

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName() == null) {
					if (evt.getPropagationId() != Model.MODEL_UPDATED) {
						return;
					}
				}
				logger.info("removed listener :: {} :: {}  :: {}",
						child.pathSegment(), child.hashCode(), this.hashCode());
				Node newChild = addChild(
						child.propertyReflector.getPropertyValue(getModel()),
						child.propertyReflector, child.changeSource);
				newChild.render();
				Optional<Widget> oldChildWidget = this.child.rendered
						.verifySingleOrNullWidget();
				Optional<Widget> insertBeforeChildWidget = this.child.rendered
						.verifySingleWidgetOrPredecessorSingleOrNoWidget();
				Optional<Widget> newChildWidget = newChild.rendered
						.verifySingleOrNullWidget();
				rendered.swapChildWidgets(insertBeforeChildWidget,
						oldChildWidget, newChildWidget);
				newChild.index = child.index;
				removeChild(this.child);
				children.add(newChild.index, newChild);
				this.child = newChild;
				// unbind();
				// no need to unbind - removeChild will have done this
			}
		}

		class BehaviourBinding implements NodeEventReceiver {
			Behaviour behaviour;

			NodeEvent eventBinding;

			public BehaviourBinding(Behaviour behaviour) {
				this.behaviour = behaviour;
			}

			@Override
			public void onEvent(GwtEvent event) {
				Context context = new NodeEvent.Context();
				context.behaviour = behaviour;
				context.node = Node.this;
				context.gwtEvent = event;
				context.nodeEvent = eventBinding;
				logger.trace("Firing behaviour {} on {} to {}",
						eventBinding.getClass().getSimpleName(),
						Node.this.pathSegment(),
						behaviour.handler().getSimpleName());
				Reflections.newInstance(behaviour.handler()).onEvent(context);
			}

			private void bindEvent(boolean bind) {
				if (bind) {
					if (eventBinding == null) {
						eventBinding = Reflections
								.newInstance(behaviour.event());
						eventBinding.setReceiver(this);
					}
					Widget widgetToBind = resolveWidget(
							behaviour.eventSourcePath());
					eventBinding.bind(widgetToBind, true);
				} else {
					eventBinding.bind(null, false);
				}
			}

			void bind() {
				if (behaviour.activationTopic() == NodeTopic.VoidTopic.class) {
					bindEvent(true);
				}
			}

			void publishTopic(Class<? extends NodeTopic> topic) {
				if (behaviour.activationTopic() == topic) {
					bindEvent(true);
				}
			}
		}

		public Node childBefore(Node child) {
			int idx = children.indexOf(child);
			return idx == 0 ? null : children.get(idx - 1);
		}
		public <T> T ancestorModel(Predicate predicate) {
			if (predicate.test(model)) {
				return (T) model;
			}
			if (parent != null) {
				return parent.ancestorModel(predicate);
			}
			return null;
		}
	}

	public interface NodeEventReceiver {
		public void onEvent(GwtEvent event);
	}
}
