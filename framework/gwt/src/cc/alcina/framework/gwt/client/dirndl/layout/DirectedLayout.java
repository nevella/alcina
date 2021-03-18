package cc.alcina.framework.gwt.client.dirndl.layout;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
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
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour.TopicBehaviour;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.DirectedResolver;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Handler;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent.TopicListeners;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class DirectedLayout {
	private static Logger logger = LoggerFactory
			.getLogger(DirectedLayout.class);
	static {
		AlcinaLogUtils.sysLogClient(DirectedLayout.class, Level.OFF);
	}

	public static Node current = null;

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
	public static class ContextResolver implements AnnotationLocation.Resolver {
		public ContextResolver() {
		}

		public Object resolveModel(Object model) {
			return model;
		}

		public <T> T resolveRenderContextProperty(String key) {
			return null;
		}

		@Override
		public <A extends Annotation> A getAnnotation(Class<A> annotationClass,
				AnnotationLocation location) {
			return AnnotationLocation.Resolver.super.getAnnotation(
					annotationClass, location);
		}
	}

	private static class DelegatingContextResolver extends ContextResolver {
		private ContextResolver parent = null;

		private Resolver locationResolver;

		public DelegatingContextResolver(ContextResolver parent,
				Resolver locationResolver) {
			this.parent = parent;
			this.locationResolver = locationResolver;
		}

		@Override
		public Object resolveModel(Object model) {
			return parent.resolveModel(model);
		}

		@Override
		public <T> T resolveRenderContextProperty(String key) {
			return parent.resolveRenderContextProperty(key);
		}

		@Override
		public <A extends Annotation> A getAnnotation(Class<A> annotationClass,
				AnnotationLocation location) {
			return locationResolver.getAnnotation(annotationClass, location);
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
		private ContextResolver resolver;

		public ContextResolver getResolver() {
			return this.resolver;
		}

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
			this.model = resolver.resolveModel(model);
			current = this;
		}

		public <T> T ancestorModel(Class<T> clazz) {
			return ancestorModel(model -> model.getClass() == clazz);
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

		public <A extends Annotation> A annotation(Class<A> clazz) {
			AnnotationLocation location = new AnnotationLocation(
					model == null ? null : model.getClass(), propertyReflector);
			A annotation = resolver.getAnnotation(clazz, location);
			if (annotation != null) {
				return annotation;
			}
			if (parent != null
					&& parent.renderer instanceof CollectionNodeRenderer) {
				return parent.annotation(clazz);
			} else {
				return null;
			}
		}

		public Node childBefore(Node child) {
			int idx = children.indexOf(child);
			return idx == 0 ? null : children.get(idx - 1);
		}

		public <T> T getModel() {
			return (T) this.model;
		}

		public Node resolveNode(String path) {
			String[] segments = path.split("/");
			Node firstSegment = resolveNodeSegment(segments[0]);
			if (segments.length == 1) {
				Node cursor = firstSegment;
				while (true) {
					Preconditions
							.checkState(cursor.rendered.widgets.size() == 1);
					if (cursor.intermediate) {
						cursor = cursor.children.get(0);
					} else {
						return cursor;
					}
				}
			}
			return firstSegment.resolveNodeSegment(
					path.substring(segments[0].length() + 1));
		}

		public <T> T resolveRenderContextProperty(String key) {
			return resolver.resolveRenderContextProperty(key);
		}

		public DirectedNodeRenderer resolveRenderer() {
			DirectedNodeRenderer renderer = null;
			if (directed == null) {
				// FIXME - dirndl.0 - no, resolver is from the node tree, not
				// the class
				Class clazz = model == null ? void.class : model.getClass();
				/*
				 * if the property has a simple @Directed annotation, and the
				 * class has a non-simple @Directed, use the class
				 */
				directed = Registry.impl(DirectedResolver.class, clazz);
				AnnotationLocation annotationLocation = new AnnotationLocation(
						clazz, propertyReflector);
				if (propertyReflector != null
						&& propertyReflector
								.getAnnotation(Directed.class) != null
						&& isDefault(
								propertyReflector.getAnnotation(Directed.class))
						&& clazz != null
						&& Reflections.classLookup().getAnnotationForClass(
								clazz, Directed.class) != null) {
					annotationLocation = new AnnotationLocation(clazz, null);
				}
				((DirectedResolver) directed).setLocation(annotationLocation);
			}
			Class<? extends DirectedNodeRenderer> rendererClass = directed
					.renderer();
			if (rendererClass == ModelClassNodeRenderer.class) {
				if (model != null) {
					rendererClass = Registry.get().lookupSingle(
							DirectedNodeRenderer.class, model.getClass());
				} else {
					rendererClass = DefaultNodeRenderer.class;
				}
			}
			renderer = Reflections.classLookup().newInstance(rendererClass);
			return renderer;
		}

		private boolean isDefault(Directed annotation) {
			return annotation.renderer() == ModelClassNodeRenderer.class
					&& annotation.cssClass().isEmpty()
					&& annotation.tag().isEmpty()
					&& annotation.behaviours().length == 0
					&& annotation.bindings().length == 0;
		}

		public Widget resolveWidget(String path) {
			return resolveNode(path).rendered.verifySingleWidget();
		}

		@Override
		public String toString() {
			return path();
		}

		private void addListeners(Node child) {
			if (child.changeSource == null) {
				return;
			}
			Object childModel = child.model;
			// even though this (the parent) handles changes,
			// binding/unbinding on node removal is the responsibility of the
			// child, so we add to the child's listeners list
			if (!child.changeSource.isReadOnly() && model instanceof Bindable) {
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

		private void bindBehaviours() {
			if (directed == null || directed.behaviours().length == 0) {
				return;
			}
			rendered.bindBehaviours();
		}

		private void bindProperties() {
			if (directed == null || directed.bindings().length == 0) {
				return;
			}
			/*
			 * TODO - can probably relax this (just apply to outermost widget)
			 */
			Preconditions.checkState(rendered.widgets.size() == 1);
			rendered.bindings = Arrays.stream(directed.bindings())
					.map(PropertyBinding::new).collect(Collectors.toList());
		}

		private void populateWidgets(boolean intermediateChild) {
			this.descriptor = model == null ? null
					: Reflections.beanDescriptorProvider()
							.getDescriptorOrNull(model);
			renderer = resolveRenderer();
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
						if (propertyReflector
								.getAnnotation(Directed.class) != null) {
							Object childModel = propertyReflector
									.getPropertyValue(model);
							if (childModel != null && childModel.getClass()
									.getName().contains("GlobalHeader")) {
								int debug = 3;
							}
							Node child = addChild(childModel, propertyReflector,
									propertyReflector);
						}
					}
				}
			}
			current = this;// after leaving child traverse
			rendered.widgets = renderer.renderWithDefaults(this);
			return;
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
			current = this;
			DirectedContextResolver directedContextResolver = annotation(
					DirectedContextResolver.class);
			if (directedContextResolver != null) {
				resolver = Reflections
						.newInstance(directedContextResolver.value());
			}
			populateWidgets(intermediateChild);
			bindBehaviours();
			bindProperties();
			current = null;
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
			if (rendered.bindings != null) {
				rendered.bindings.forEach(PropertyBinding::unbind);
			}
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

		String pathSegment() {
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

		Rendered render() {
			render(false);
			return rendered;
		}

		public class Rendered {
			List<Widget> widgets = new ArrayList<>();

			public List<BehaviourBinding> behaviours;

			public List<PropertyBinding> bindings;

			public int getChildIndex(Widget childWidget) {
				FlowPanel panel = verifyContainer();
				return panel.getWidgetIndex(childWidget);
			}

			void bindBehaviours() {
				Preconditions.checkState(widgets.size() == 1);
				behaviours = Arrays.stream(directed.behaviours())
						.map(BehaviourBinding::new)
						.collect(Collectors.toList());
				behaviours.forEach(BehaviourBinding::bind);
				if (preRenderListeners != null) {
					preRenderListeners
							.forEach((behaviour, behaviourBinding) -> {
								behaviourBindingFor(behaviour).topicListeners
										.addListener(behaviourBinding);
							});
					preRenderListeners = null;
				}
			}

			public Optional<Widget> lastWidgetOrPredecessorLastWidget() {
				Node cursor = Node.this;
				while (cursor != null) {
					Widget last = Ax.last(cursor.rendered.widgets);
					if (last != null) {
						return Optional.of(last);
					}
					cursor = parent.childBefore(cursor);
				}
				return Optional.empty();
			}

			public void swapChildWidgets(
					Optional<Widget> insertAfterChildWidget,
					List<Widget> oldChildWidgets,
					List<Widget> newChildWidgets) {
				for (int idx = newChildWidgets.size() - 1; idx >= 0; idx--) {
					int index = insertAfterChildWidget.map(this::getChildIndex)
							.orElse(-1) + 1;
					verifyContainer().insert(newChildWidgets.get(idx), index);
				}
				oldChildWidgets.forEach(Widget::removeFromParent);
			}

			public Widget verifySingleWidget() {
				Preconditions.checkState(widgets.size() == 1);
				return widgets.get(0);
			}

			private FlowPanel verifyContainer() {
				if (renderer instanceof RendersToParentContainer) {
					return parent.rendered.verifyContainer();
				}
				return (FlowPanel) verifySingleWidget();
			}

			public BehaviourBinding behaviourBindingFor(Behaviour behaviour) {
				return behaviours.stream()
						.filter(bb -> bb.behaviour == behaviour).findFirst()
						.get();
			}

			private Map<Behaviour, BehaviourBinding> preRenderListeners;

			public void addBehaviourBinding(Behaviour behaviour,
					BehaviourBinding behaviourBinding) {
				if (behaviours != null) {
					behaviourBindingFor(behaviour).topicListeners
							.addListener(behaviourBinding);
				} else {
					if (preRenderListeners == null) {
						preRenderListeners = new LinkedHashMap<>();
					}
					preRenderListeners.put(behaviour, behaviourBinding);
				}
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
				if (child.propertyReflector == null
						&& evt.getPropertyName() != null) {
					// the 'null name' listener and 'non-null' are listening at
					// different layers of the model
					//
					// FIXME - dirndl.1 - elaborate this
					return;
				}
				if (propertyName == null && evt.getPropertyName() != null) {
					// whole-object/model listeners shouldn't fire on named
					// changes
					return;
				}
				logger.info("removed listener :: {} :: {}  :: {}",
						child.pathSegment(), child.hashCode(), this.hashCode());
				Node newChild = addChild(
						child.propertyReflector == null ? evt.getNewValue()
								: child.propertyReflector
										.getPropertyValue(getModel()),
						child.propertyReflector, child.changeSource);
				newChild.render();
				List<Widget> oldChildWidgets = this.child.rendered.widgets;
				Optional<Widget> insertBeforeChildWidget = this.child.rendered
						.lastWidgetOrPredecessorLastWidget();
				List<Widget> newChildWidgets = newChild.rendered.widgets;
				rendered.swapChildWidgets(insertBeforeChildWidget,
						oldChildWidgets, newChildWidgets);
				newChild.index = child.index;
				removeChild(this.child);
				children.add(newChild.index, newChild);
				this.child = newChild;
				// unbind();
				// no need to unbind - removeChild will have done this
			}
		}

		/*
		 * Similar to Gwittir Binding - but simpler
		 */
		class PropertyBinding {
			Binding binding;

			private RemovablePropertyChangeListener listener;

			PropertyBinding(Binding binding) {
				this.binding = binding;
				switch (binding.type()) {
				case CSS_CLASS:
					/*
					 * requires from/transform or from/literal
					 */
					Preconditions.checkArgument(
							binding.from().length() > 0 && (binding
									.transform() != ToStringFunction.Identity.class
									^ binding.literal().length() > 0));
					break;
				case SWITCH_CSS_CLASS:
					/*
					 * requires both 'inputs', literal must be | separated
					 */
					Preconditions.checkArgument(binding.from().length() > 0
							&& binding.literal().length() > 0
							&& binding.literal().split("\\|").length == 2);
					break;
				default:
					/*
					 * exactly one of from() and value() must be non-empty
					 */
					Preconditions.checkArgument(binding.from().length() > 0
							^ binding.literal().length() > 0);
					break;
				}
				if (binding.from().length() > 0) {
					this.listener = new RemovablePropertyChangeListener(
							(Bindable) model, binding.from(), evt -> set());
				}
				set();
			}

			void set() {
				Object value = binding.from().length() > 0
						? Reflections.propertyAccessor().getPropertyValue(model,
								binding.from())
						: binding.literal();
				boolean hasTransform = (Class) binding
						.transform() != ToStringFunction.Identity.class;
				if (hasTransform) {
					value = Reflections.newInstance(binding.transform())
							.apply(value);
				}
				String stringValue = value.toString();
				Element element = rendered.widgets.get(0).getElement();
				switch (binding.type()) {
				case INNER_HTML:
					element.setInnerHTML(stringValue);
					break;
				case INNER_TEXT:
					element.setInnerText(stringValue);
					break;
				case PROPERTY:
					element.setAttribute(binding.to(), stringValue);
					break;
				case CSS_CLASS: {
					if (hasTransform) {
						element.setClassName(stringValue);
					} else {
						boolean present = (boolean) value;
						element.setClassName(binding.literal(), present);
					}
				}
					break;
				case SWITCH_CSS_CLASS: {
					String[] parts = binding.literal().split("\\|");
					boolean part1 = (boolean) value;
					if (parts[0].length() > 0) {
						element.setClassName(parts[0], !part1);
					}
					if (parts[1].length() > 0) {
						element.setClassName(parts[1], part1);
					}
				}
					break;
				case TOGGLE_CSS_CLASS:
					element.setClassName(stringValue,
							element.hasClassName(stringValue));
					break;
				case STYLE_ATTRIBUTE:
					element.getStyle().setProperty(binding.to(), stringValue);
					break;
				default:
					throw new UnsupportedOperationException();
				}
			}

			void unbind() {
				if (listener != null) {
					listener.unbind();
				}
			}
		}

		class BehaviourBinding implements NodeEventReceiver {
			Behaviour behaviour;

			NodeEvent eventBinding;

			TopicListeners topicListeners = new TopicListeners();

			public BehaviourBinding(Behaviour behaviour) {
				this.behaviour = behaviour;
			}

			@Override
			public void onEvent(GwtEvent event) {
				Context context = new NodeEvent.Context();
				context.gwtEvent = event;
				fireEvent(context);
			}

			void onTopicEvent(TopicEvent topicEvent) {
				if (Behaviour.Util.hasActivationTopic(behaviour,
						topicEvent.topic)) {
					bindEvent(true);
				}
				if (Behaviour.Util.hasListenerTopic(behaviour,
						topicEvent.topic)) {
					Context context = new NodeEvent.Context();
					context.topicEvent = topicEvent;
					fireEvent(context);
				}
			}

			@Override
			public String toString() {
				return Ax.format("Binding :: %s :: %s",
						model.getClass().getSimpleName(), behaviour);
			}

			private void fireEvent(Context context) {
				context.nodeEvent = eventBinding;
				context.behaviour = behaviour;
				context.node = Node.this;
				context.topicListeners = topicListeners;
				Class<? extends Handler> handlerClass = behaviour.handler();
				Handler handler = null;
				// allow context-sensitive handlers
				if (context.node.renderer.getClass() == handlerClass) {
					handler = (Handler) context.node.renderer;
				} else if (context.node.model.getClass() == handlerClass) {
					handler = (Handler) context.node.model;
				} else {
					Handler ancestorHandler = context.node
							.ancestorModel(handlerClass);
					if (ancestorHandler == null) {
						handler = Reflections.newInstance(handlerClass);
					} else {
						handler = ancestorHandler;
					}
				}
				logger.trace("Firing behaviour {} on {} to {}",
						eventBinding.getClass().getSimpleName(),
						Node.this.pathSegment(), handlerClass.getSimpleName());
				handler.onEvent(context);
			}

			private void bindEvent(boolean bind) {
				if (bind) {
					if (eventBinding == null) {
						eventBinding = Reflections
								.newInstance(behaviour.event());
						eventBinding.setReceiver(this);
					}
					eventBinding.bind(getBindingWidget(), true);
				} else {
					eventBinding.bind(null, false);
				}
			}

			void bind() {
				if (!Behaviour.Util.hasActivationTopic(behaviour)) {
					bindEvent(true);
				}
				for (TopicBehaviour topicBehaviour : behaviour.topics()) {
					if (Behaviour.Util.isListenerTopic(topicBehaviour)) {
						Node cursor = Node.this;
						while (cursor != null) {
							Behaviour behaviour = Behaviour.Util.getEmitter(
									cursor.directed, topicBehaviour.topic());
							if (behaviour != null) {
								cursor.rendered.addBehaviourBinding(behaviour,
										this);
								logger.warn(
										"Binding topic behaviour {} on {} to {}\n",
										topicBehaviour.topic().getSimpleName(),
										Node.this.pathSegment(),
										cursor.pathSegment());
								break;
							}
							cursor = cursor.parent;
						}
					}
				}
			}

			Widget getBindingWidget() {
				return rendered.verifySingleWidget();
			}
		}

		public AnnotationLocation.Resolver contextResolver() {
			return resolver;
		}

		public void pushResolver(AnnotationLocation.Resolver locationResolver) {
			resolver = new DelegatingContextResolver(resolver,
					locationResolver);
		}

		public void fireEvent(TopicEvent topicEvent) {
			if (rendered.behaviours != null) {
				rendered.behaviours.forEach(bb -> bb.onTopicEvent(topicEvent));
			}
		}

		public Node root() {
			Node cursor = this;
			while (cursor.parent != null) {
				cursor = cursor.parent;
			}
			return cursor;
		}

		public Widget getWidget() {
			return rendered.verifySingleWidget();
		}
	}

	public interface NodeEventReceiver {
		public void onEvent(GwtEvent event);
	}
}
