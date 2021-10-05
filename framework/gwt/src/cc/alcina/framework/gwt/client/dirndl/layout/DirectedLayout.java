package cc.alcina.framework.gwt.client.dirndl.layout;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.google.gwt.event.shared.EventHandler;
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
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.DirectedResolver;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent.TopicListeners;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * FIXME - dirndl.perf
 * 
 * Minimise annotation resolution by caching an intermediate renderer object
 * which itself caches property/reflector annotation tuples. Also apply to
 * reflective serializer
 * 
 * @author nick@alcina.cc
 *
 */
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

	public interface Lifecycle {
		public void beforeRender();
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
		private ContextResolver<?> resolver;

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
			return ancestorModel(model -> Reflections.isAssignableFrom(clazz,
					model.getClass()));
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
			A annotation = resolver.resolveAnnotation(clazz, location);
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

		public Node childWithModel(Predicate<Object> test) {
			if (test.test(this.model)) {
				return this;
			}
			for (Node child : children) {
				Node childWithModel = child.childWithModel(test);
				if (childWithModel != null) {
					return childWithModel;
				}
			}
			return null;
		}

		public void fireEvent(TopicEvent topicEvent) {
			if (rendered.eventBindings != null) {
				rendered.eventBindings
						.forEach(bb -> bb.onTopicEvent(topicEvent));
			}
		}

		public <T> T getModel() {
			return (T) this.model;
		}

		public ContextResolver getResolver() {
			return this.resolver;
		}

		public Widget getWidget() {
			return rendered.verifySingleWidget();
		}

		public void pushResolver(ContextResolver resolver) {
			this.resolver = resolver;
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
				Class clazz = model.getClass();
				AnnotationLocation annotationLocation = new AnnotationLocation(
						clazz, propertyReflector, resolver);
				directed = new DirectedResolver(
						getResolver().getTreeResolver(Directed.class),
						annotationLocation);
			}
			Class<? extends DirectedNodeRenderer> rendererClass = directed
					.renderer();
			if (rendererClass == ModelClassNodeRenderer.class) {
				rendererClass = Registry.get().lookupSingle(
						DirectedNodeRenderer.class, model.getClass());
			}
			renderer = Reflections.newInstance(rendererClass);
			return renderer;
		}

		public Widget resolveWidget(String path) {
			return resolveNode(path).rendered.verifySingleWidget();
		}

		public Node root() {
			Node cursor = this;
			while (cursor.parent != null) {
				cursor = cursor.parent;
			}
			return cursor;
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
			if (directed == null || directed.receives().length == 0) {
				return;
			}
			rendered.bindBehaviours();
		}

		private void bindProperties() {
			if (directed == null || directed.bindings().length == 0
					|| model == null) {
				return;
			}
			/*
			 * TODO - can probably relax this (just apply to outermost widget)
			 */
			/*
			 * FIXME - index - actions.{ArrayList}. actions.{ArrayList}(div).
			 * actions.{ArrayList}(ul). actions.{ArrayList} - child annotation
			 * being applied to parent?
			 * 
			 */
			if (rendered.widgets.size() != 1) {
				directed.bindings();
				return;
			}
			Preconditions.checkState(rendered.widgets.size() == 1);
			rendered.bindings = Arrays.stream(directed.bindings())
					.map(PropertyBinding::new).collect(Collectors.toList());
		}

		private void populateWidgets(boolean intermediateChild) {
			if (model == null) {
				return;
			}
			this.descriptor = Reflections.beanDescriptorProvider()
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
				Collection<PropertyReflector> propertyReflectors = Reflections
						.classLookup().getPropertyReflectors((model.getClass()))
						.values();
				if (propertyReflectors != null) {
					for (PropertyReflector propertyReflector : propertyReflectors) {
						if (resolver.hasDirectedAnnotation(propertyReflector)) {
							Object childModel = propertyReflector
									.getPropertyValue(model);
							addChild(childModel, propertyReflector,
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
			if (model == null) {
				return;
			}
			if (model instanceof DirectedLayout.Lifecycle) {
				// FIXME - dirndl 1.0 - lifecycle -> abstract class,
				// HasLifecycle, yadda
				// beforeRRender -> (maybe) first time render
				((DirectedLayout.Lifecycle) model).beforeRender();
			}
			resolver.beforeRender();
			current = this;
			DirectedContextResolver directedContextResolver = annotation(
					DirectedContextResolver.class);
			if (directedContextResolver != null) {
				resolver = Reflections
						.newInstance(directedContextResolver.value());
				((ContextResolver) resolver).setModel(model);
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
				Collection<PropertyReflector> propertyReflectors = Reflections
						.classLookup().getPropertyReflectors((model.getClass()))
						.values();
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

			public List<NodeEventBinding> eventBindings;

			public List<PropertyBinding> bindings;

			private Map<Class<? extends NodeEvent>, NodeEventBinding> preRenderListeners;

			public void addBehaviourBinding(
					Class<? extends NodeEvent> eventType,
					NodeEventBinding behaviourBinding) {
				if (eventBindings != null) {
					eventBindingFor(eventType).topicListeners
							.addListener(behaviourBinding);
				} else {
					if (preRenderListeners == null) {
						preRenderListeners = new LinkedHashMap<>();
					}
					preRenderListeners.put(eventType, behaviourBinding);
				}
			}

			public NodeEventBinding
					eventBindingFor(Class<? extends NodeEvent> eventType) {
				return eventBindings.stream().filter(bb -> bb.type == eventType)
						.findFirst().get();
			}

			public int getChildIndex(Widget childWidget) {
				FlowPanel panel = verifyContainer();
				return panel.getWidgetIndex(childWidget);
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

			void bindBehaviours() {
				Preconditions.checkState(widgets.size() == 1);
				eventBindings = new ArrayList<>();
				for (int idx = 0; idx < directed.receives().length; idx++) {
					Class<? extends NodeEvent> clazz = directed.receives()[idx];
					eventBindings.add(new NodeEventBinding(clazz, idx));
				}
				eventBindings.forEach(NodeEventBinding::bind);
				if (preRenderListeners != null) {
					preRenderListeners
							.forEach((behaviour, behaviourBinding) -> {
								eventBindingFor(behaviour).topicListeners
										.addListener(behaviourBinding);
							});
					preRenderListeners = null;
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

		class NodeEventBinding implements NodeEventReceiver {
			Class<? extends NodeEvent> type;

			// FIXME - dirndl 1.3 - why binding? why not bound event?
			// also .... shouldn't we create these events on demand, and just
			// use type? or call it 'template'?
			NodeEvent<? extends EventHandler> eventTemplate;

			TopicListeners topicListeners = new TopicListeners();

			private int receiverIndex;

			public NodeEventBinding(Class<? extends NodeEvent> type, int idx) {
				this.type = type;
				this.receiverIndex = idx;
			}

			@Override
			public void onEvent(GwtEvent event) {
				Context context = new NodeEvent.Context();
				context.gwtEvent = event;
				fireEvent(context, Node.this.getModel());
			}

			@Override
			public String toString() {
				return Ax.format("Binding :: %s :: %s",
						model.getClass().getSimpleName(), type);
			}

			private void bindEvent(boolean bind) {
				if (bind) {
					if (eventTemplate == null) {
						eventTemplate = Reflections.newInstance(type);
						eventTemplate.setEventReceiver(this);
					}
					eventTemplate.bind(getBindingWidget(), true);
				} else {
					eventTemplate.bind(null, false);
				}
			}

			private void fireEvent(Context context, Object model) {
				NodeEvent nodeEvent = Reflections.newInstance(type);
				context.setNodeEvent(nodeEvent);
				nodeEvent.setModel(model);
				context.node = Node.this;
				// FIXME - dirndl 1.3 - do we still need to pass topicListeners?
				context.topicListeners = topicListeners;
				Class<? extends EventHandler> handlerClass = eventTemplate
						.getHandlerClass();
				NodeEvent.Handler handler = null;
				if (Reflections.isAssignableFrom(handlerClass,
						context.node.renderer.getClass())) {
					handler = (NodeEvent.Handler) context.node.renderer;
				} else if (Reflections.isAssignableFrom(handlerClass,
						context.node.model.getClass())) {
					handler = (NodeEvent.Handler) context.node.model;
				} else {
					// fire a logical topic event, based on correspondence
					// between Directed.reemits and receives
					Context eventContext = NodeEvent.Context
							.newTopicContext(context, Node.this);
					Preconditions.checkState(directed
							.receives().length == directed.reemits().length);
					Class<? extends TopicEvent> emitTopic = (Class<? extends TopicEvent>) directed
							.reemits()[receiverIndex];
					TopicEvent event = Reflections.newInstance(emitTopic);
					TopicEvent.fire(eventContext, emitTopic,
							Node.this.getModel());
					return;
				}
				logger.trace("Firing behaviour {} on {} to {}",
						eventTemplate.getClass().getSimpleName(),
						Node.this.pathSegment(), handlerClass.getSimpleName());
				nodeEvent.dispatch(handler);
			}

			void bind() {
				bindEvent(true);
				// FIXME - dirndl.emit
				/// go up the node tree until we find an emitter
				//
				// but - is this even used? maybe for search/form filters - but
				// not sure it's needed/wanted
				// for (TopicBehaviour topicBehaviour : type.topics()) {
				// if (Behaviour.Util.isListenerTopic(topicBehaviour)) {
				// Node cursor = Node.this;
				// while (cursor != null) {
				// Behaviour behaviour = Behaviour.Util.getEmitter(
				// cursor.directed, topicBehaviour.topic());
				// if (behaviour != null) {
				// cursor.rendered.addBehaviourBinding(behaviour,
				// this);
				// logger.warn(
				// "Binding topic behaviour {} on {} to {}\n",
				// topicBehaviour.topic().getSimpleName(),
				// Node.this.pathSegment(),
				// cursor.pathSegment());
				// break;
				// }
				// cursor = cursor.parent;
				// }
				// }
				// }
			}

			Widget getBindingWidget() {
				return rendered.verifySingleWidget();
			}

			void onTopicEvent(TopicEvent topicEvent) {
				if (topicEvent.getClass() == type) {
					Context context = NodeEvent.Context.newTopicContext(
							topicEvent.getContext(), Node.this);
					// set before we dispatch to the handler, so the handler can
					// unset
					topicEvent.setHandled(true);
					fireEvent(context, topicEvent.getModel());
				}
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
					 * requires from. If both transform and literal are
					 * undefined, uses the de-infixed form of the property name
					 */
					Preconditions.checkArgument(binding.from().length() > 0);
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
				if (binding.from().length() > 0 && model instanceof Bindable) {
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
				String stringValue = value == null ? "null" : value.toString();
				Element element = rendered.widgets.get(0).getElement();
				switch (binding.type()) {
				case INNER_HTML:
					if (value != null) {
						element.setInnerHTML(stringValue);
					}
					break;
				case INNER_TEXT:
					if (value != null) {
						element.setInnerText(stringValue);
					}
					break;
				case PROPERTY:
					String propertyName = binding.to().isEmpty()
							? binding.from()
							: binding.to();
					if (value == null) {
						element.removeAttribute(propertyName);
					} else {
						element.setAttribute(propertyName, stringValue);
					}
					break;
				case CSS_CLASS: {
					if (hasTransform) {
						element.setClassName(value == null ? "" : stringValue);
					} else {
						String cssClass = binding.literal().isEmpty()
								? CommonUtils.deInfixCss(binding.from())
								: binding.literal();
						boolean present = (boolean) value;
						element.setClassName(cssClass, present);
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
	}

	public interface NodeEventReceiver {
		public void onEvent(GwtEvent event);
	}
}
