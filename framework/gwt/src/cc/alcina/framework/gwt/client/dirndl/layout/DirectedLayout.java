package cc.alcina.framework.gwt.client.dirndl.layout;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.InsertPanel.ForIsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DepthFirstTraversal;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.InsertionPoint.Point;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelEvent.TopicListeners;
import cc.alcina.framework.gwt.client.dirndl.model.HasBind;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 *
 * <p>
 * A generative, expressive algorithm that transforms an arbitrary object into a
 * live layout tree of {@link DirectedLayout.Node} objects which encapsulate a
 * render tree (currently implemented only for HTML DOM, but easily extended to
 * arbitrary native widgets)
 *
 * <p>
 * Dirndl bears some resemblance to xslt - they both use annotations to
 * transform an object tree into markup, but differs as follows:
 *
 * <ul>
 * <li><b>generative</b>: the intermediate transform (ModelTransform) generates
 * inputs for the algorithm rather than result nodes, leading to much richer
 * output structures from a given initial object tree
 * <li><b>expressive</b>: transforms, which are controlled by @Directed
 * annotations, can be customised in many ways by code since the annotations
 * themselves can be transformed by the {@link ContextRenderer} applicable to
 * the layout node
 * <li><b>live</b>: the DirectedLayout.Node objects are aware of changes to
 * their source model objects and mutate accordingly
 * <li><b>events</b>: UI events are transformed into NodeEvent objects, which
 * provide a generally truer-to-the-model-logic way of modelling and handling
 * interface events than dealing directly with native (DOM) events
 * </ul>
 *
 * FIXME - dirndl.perf
 *
 * Minimise annotation resolution by caching an intermediate renderer object
 * which itself caches property/class annotation tuples. Also apply to
 * reflective serializer
 *
 * @author nick@alcina.cc
 *
 */
/*
 *
 * @formatter:off
 *
 * Implementation: Dirndl 1.1
 *
 * [Transform model object to renderer input]
 * - For ContextResolver CR, model M, AnnotationLocation AL, parent DirectedLayout.Node PN,
 * retrieve the @Directed[]  annotations DL[] (most often only 1) applicable to the M at AL.
 * - Construct a RendererInput model from M, AL, CR, PN, DL[]
 *
 * [Algorithm]
 * - Transform the initial object I to RendererInput RI, push onto RI stack
 * - Pop RI from stack
 * - While RI.DL[] is non-empty, compute renderer R from DL0 (first element of RI.DL[])
 * - Apply R to RI via [DL0,CR], which (decidedly non-functional - i.e. results in multiple changes):
 * -- generates Node n which will be added to the children of PN
 * -- optionally generates Widget W which will be added as a child to the nearest parent Widget in the Node tree
 * -- optionally modifies RI.DL (TODO - examples)
 * -- can emit RI[] - the primary examples of that are:
 * --- if RI.DL[].length>1, emit a copy of RI with first element of RI.DL[] removed
 * --- applying the '[Transform model object]' algo above to the children (properties,
 * collection elements)of M, applicable only to the last @Directed in RI.DL[]
 * (Repeat until no RI stack is empty)
 *
 * Dirndl 1.1 TODO
 *
 * - Ensure collection, model transform work as expected (/)
 * - Test in a large application
 * - Remove current layout algorithm
 * - Review renderers. Mark for removal (e.g. Link -> LinkDeprecated -> Remove)(actually that's a mooel but... yup)
 * - Remove renderers
 * - Review propertychange handling in this class
 * - Remove uses of TreeResolver (but not the code) (explain why -
 *   at least for directed trees, simpler & better to have lowest-imperative win)
 * - Plan a ContextResolver cleanup
 * - Plan Registry.Context
 * - Implement the ContextResolver cleanup
 *
 *
 * - Goals:
 *   - Is ContextResolver clear?
 *   - Is event propagation clear?
 *   - Justify eventpump (or not eventpump) for node, transformed node events
 *   - Document dirndl 1x2 - widget removal
 *
 * - Phases:
 * 	 a. Implement TODO above to 'remove renderers'
 *   b. rest of TODO
 *   c. FIXMEs marked as 'c'
 *
 *
 *
 *  @formatter:on
 */
public class DirectedLayout {
	static Logger logger = LoggerFactory.getLogger(DirectedLayout.class);
	static {
		AlcinaLogUtils.sysLogClient(DirectedLayout.class, Level.INFO);
	}

	public static boolean trace = false;

	// FIXME - remove
	public static Node current = null;

	/**
	 * <p>
	 * Input for the renderer, which transforms a model found at an
	 * annotationlocation and a list of (usually a singleton) directed
	 * annotations into (0,1) widgets and (0,n) RendererInputs
	 *
	 * <p>
	 * When a node is removed, if a widget was generated during rendering, that
	 * widget is removed from its parent, otherwise (recursively) its child
	 * nodes' widgets are removed (TODO - explain with reasoning/motivation)
	 *
	 *
	 * @author nick@alcina.cc
	 *
	 */
	private Node root = null;

	DepthFirstTraversal<RendererInput> rendererInputs;

	boolean inLayout = false;

	/**
	 * Render a model object and add top-level output widgets to the parent
	 * widget
	 */
	public Widget render(ContextResolver resolver, Object model) {
		resolver.layout = this;
		AnnotationLocation location = new AnnotationLocation(model.getClass(),
				null);
		enqueueInput(resolver, model, location, null, null);
		layout();
		return root.firstDescendantWidget();
	}

	RendererInput enqueueInput(ContextResolver resolver, Object model,
			AnnotationLocation location, List<Directed> directeds,
			Node parentNode) {
		// Even if model == null (so no widget will be emitted), nodes must be
		// added to the structure for a later change to non-null
		// if (model == null) {
		// return;
		// }
		RendererInput input = new RendererInput(resolver, model, location,
				directeds, parentNode);
		// beginning of a layout
		if (rendererInputs == null) {
			rendererInputs = new DepthFirstTraversal<>(input, o -> null, false);
		} else {
			rendererInputs.add(input);
		}
		return input;
	}

	// The algorithm. Gosh.
	void layout() {
		try {
			inLayout = true;
			do {
				// depth-first traversal
				RendererInput input = rendererInputs.next();
				if (root == null) {
					root = input.node;
				}
				input.render();
			} while (rendererInputs.hasNext());
		} finally {
			inLayout = false;
			rendererInputs = null;
		}
	}

	/**
	 * An interface that supports lazy model population before render
	 *
	 * @author nick@alcina.cc
	 *
	 */
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
	 *
	 * <p>
	 * FIXME - dirndl 1.1 - change documentation (since there's less
	 * correspondence, but also fewer renderers)(and only [0,1] widgets per
	 * node)
	 *
	 * Also: changeSource/property/annotationLocation can all possibly be
	 * combined (or documented)
	 *
	 * <p>
	 * FIXME - dirndl 1.2 - optimise: speicalise leafnode for performance (these
	 * are heavyweight, and leaves need not be so much so)
	 */
	public static class Node {
		private ContextResolver resolver;

		final Object model;

		Directed directed;

		List<Node> children = new ArrayList<>();

		Node parent;

		// FIXME - dirndl 1x1b - ensure no null checks (guaranteed non null)
		final AnnotationLocation annotationLocation;

		public List<NodeEventBinding> eventBindings;

		public List<PropertyBinding> propertyBindings;

		private ChildReplacer replacementListener;

		Widget widget;

		private InsertionPoint insertionPoint;

		protected Node(ContextResolver resolver, Node parent,
				AnnotationLocation annotationLocation, Object model) {
			this.resolver = resolver;
			this.parent = parent;
			this.annotationLocation = annotationLocation;
			this.model = resolver.resolveModel(model);
			current = this;
		}

		// FIXME - dirndl1x1 - remove
		public <T> T ancestorModel(Class<T> clazz) {
			return ancestorModel(model -> Reflections.isAssignableFrom(clazz,
					model.getClass()));
		}

		// FIXME - dirndl1x1 - remove
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
			return annotationLocation.getAnnotation(clazz);
		}

		public Node childBefore(Node child) {
			int idx = children.indexOf(child);
			return idx == 0 ? null : children.get(idx - 1);
		}

		// FIXME - dirndl 1x2 (use models for form intermediates) (remove, let
		// the form node handle focus itself)
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

		public void fireEvent(ModelEvent topicEvent) {
			if (eventBindings != null) {
				eventBindings.forEach(bb -> bb.onTopicEvent(topicEvent));
			}
		}

		public <T> T getModel() {
			return (T) this.model;
		}

		public ContextResolver getResolver() {
			return this.resolver;
		}

		public Widget getWidget() {
			return widget;
		}

		public <A extends Annotation> boolean has(Class<A> clazz) {
			return annotation(clazz) != null;
		}

		public boolean hasWidget() {
			return widget != null;
		}

		public <A extends Annotation> Optional<A> optional(Class<A> clazz) {
			return Optional.ofNullable(annotation(clazz));
		}

		// FIXME - dirndl1x1 - check this (whether a resolver should apply to
		// *this* input or just child inputs)
		public void pushChildResolver(ContextResolver resolver) {
			// this.childResolver = resolver;
			throw new UnsupportedOperationException();
		}

		public <T> T resolveRenderContextProperty(String key) {
			return resolver.resolveRenderContextProperty(key);
		}

		public String toParentStack() {
			return path();
		}

		@Override
		public String toString() {
			return pathSegment();
		}

		private void bindBehaviours() {
			if (model == null || directed.receives().length == 0) {
				return;
			}
			eventBindings = new ArrayList<>();
			for (int idx = 0; idx < directed.receives().length; idx++) {
				Class<? extends NodeEvent> clazz = directed.receives()[idx];
				eventBindings.add(new NodeEventBinding(clazz, idx));
			}
			eventBindings.forEach(NodeEventBinding::bind);
		}

		private void bindModel() {
			if (model == null) {
				return;
			}
			if (hasWidget()) {
				propertyBindings = Arrays.stream(directed.bindings())
						.map(PropertyBinding::new).collect(Collectors.toList());
			}
			if (model instanceof HasBind) {
				((HasBind) model).bind();
			}
		}

		private void bindParentProperty() {
			Property property = getProperty();
			if (property == null || property.isReadOnly() || parent == null
					|| parent.model == null) {
				return;
			}
			// The property (and annotationlocation) may not refer to the parent
			// model, rather a more distant ancestor (if there is an intervening
			// transformation). In which case do not listen.
			//
			if (property.getOwningType() != parent.model.getClass()) {
				return;
			}
			// Also, in the case of wrapping, the parent and grandparent models
			// (and annotation locations) are the same, so do not listen on the
			// parent model (since an ancestor listens)
			//
			if (parent.parent != null && parent.model == parent.parent.model
					&& parent.getProperty() == parent.parent.getProperty()) {
				return;
			}
			// even though the parent handles changes,
			// binding/unbinding on node removal is the responsibility of the
			// created child node corresponding to the property, so we track on
			// the child
			//
			// FIXME - dirndl.1 - don't add this to form/table cells
			replacementListener = new ChildReplacer((Bindable) parent.model,
					property.getName());
		}

		private Widget provideWidgetOrLastDescendantChildWidget() {
			DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<>(
					this, node -> node.children, true);
			for (Node node : traversal) {
				if (node.widget != null) {
					return node.widget;
				}
			}
			return null;
		}

		private void remove() {
			resolveRenderedWidgets().forEach(Widget::removeFromParent);
			if (parent != null) {
				parent.children.remove(this);
			}
			unbind();
		}

		private void resolveRenderedWidgets0(List<Widget> list) {
			if (hasWidget()) {
				list.add(getWidget());
			} else {
				for (Node child : children) {
					child.resolveRenderedWidgets0(list);
				}
			}
		}

		private void unbind() {
			if (model instanceof Model) {
				((Model) model).unbind();
			}
			children.forEach(Node::unbind);
			if (replacementListener != null) {
				replacementListener.unbind();
				replacementListener = null;
			}
			if (propertyBindings != null) {
				propertyBindings.forEach(PropertyBinding::unbind);
			}
		}

		Widget firstDescendantWidget() {
			DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<>(
					this, node -> node.children, false);
			for (Node node : traversal) {
				if (node.widget != null) {
					return node.widget;
				}
			}
			return null;
		}

		Optional<Widget> firstSelfOrAncestorWidget(boolean includeSelf) {
			Node cursor = this;
			do {
				if ((includeSelf || cursor != this) && cursor.widget != null) {
					return Optional.of(cursor.widget);
				} else {
					cursor = cursor.parent;
				}
			} while (cursor != null);
			return Optional.empty();
		}

		Property getProperty() {
			return annotationLocation.property;
		}

		String path() {
			String thisLoc = pathSegment();
			if (parent == null) {
				return thisLoc;
			} else {
				return parent.path() + " =>\n" + thisLoc;
			}
		}

		String pathSegment() {
			FormatBuilder fb = new FormatBuilder();
			String propertyName = annotationLocation.property == null
					? "[no property]"
					: annotationLocation.property.getName();
			fb.appendPadRight(20, propertyName);
			fb.append(" ");
			String typeName = annotationLocation.classLocation.getSimpleName();
			fb.appendPadRight(30, typeName);
			fb.append(" ");
			// FIXME - dirndl 1x1a - elide defaults in @Directed, add
			// DirectedRenderer, fromTransform, indent to 50 if > say 50
			// charwidth
			fb.append(directed);
			return fb.toString();
		}

		void postRender() {
			bindBehaviours();
			bindModel();
			bindParentProperty();
		}

		// this node will disappear, so refer to predecessor nodes
		//
		// walks the node tree backwards, looking for a rendered widget
		//
		// if the first encountereed widget is the insertion ancestor, there are
		// no
		// previous sibling widget (so return null)
		//
		// but...
		//
		// TODO - optimise use of index/indexOf
		InsertionPoint resolveInsertionPoint() {
			InsertionPoint result = new InsertionPoint();
			result.point = Point.FIRST;
			result.pending = true;
			Node cursor = this;
			result.container = firstSelfOrAncestorWidget(false).get();
			while (true) {
				if (cursor != this) {
					Widget widget = cursor
							.provideWidgetOrLastDescendantChildWidget();
					if (widget != null) {
						if (widget == result.container) {
							// no preceding widget, insert first
							return result;
						} else {
							result.point = Point.AFTER;
							result.after = widget;
							return result;
						}
					}
				}
				int index = cursor.parent.children.indexOf(cursor);
				if (index > 0) {
					cursor = cursor.parent.children.get(index - 1);
				} else {
					cursor = cursor.parent;
				}
			}
		}

		/*
		 * Either self.optionalWidget, or
		 * sum(children.resolveRenderedWidgets()), recursive
		 *
		 * Used only for node.replace
		 *
		 * That's the delegation logic -
		 */
		List<Widget> resolveRenderedWidgets() {
			List<Widget> list = new ArrayList<>();
			// visitor, minimises list creation
			resolveRenderedWidgets0(list);
			return list;
		}

		// i.e. that the node doesn't correspond to @Directed.Delegating
		Widget verifySingleWidget() {
			Preconditions.checkState(widget != null);
			return widget;
		}

		/**
		 * Replaces a child node following a property change
		 *
		 * Note that this is only added to the topmost Node corresponding to a
		 * given source/property
		 *
		 * @author nick@alcina.cc
		 *
		 */
		private class ChildReplacer extends RemovablePropertyChangeListener {
			private ChildReplacer(SourcesPropertyChangeEvents bound,
					String propertyName) {
				super(bound, propertyName);
			}

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Preconditions.checkState(!resolver.getLayout().inLayout);
				// The input can mostly be constructed from this node (only the
				// model differs)
				Object newValue = evt.getNewValue();
				RendererInput input = resolver.getLayout().enqueueInput(
						resolver, newValue,
						annotationLocation.copyWithClassLocationOf(newValue),
						null, parent);
				input.replace = Node.this;
				resolver.getLayout().layout();
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
						context.node.model.getClass())) {
					handler = (NodeEvent.Handler) context.node.model;
					nodeEvent.dispatch(handler);
				} else {
					// fire a logical topic event, based on correspondence
					// between Directed.reemits and receives
					Context eventContext = NodeEvent.Context
							.newTopicContext(context, Node.this);
					Preconditions.checkState(directed
							.receives().length == directed.reemits().length);
					Class<? extends ModelEvent> emitTopic = (Class<? extends ModelEvent>) directed
							.reemits()[receiverIndex];
					ModelEvent.fire(eventContext, emitTopic,
							Node.this.getModel());
				}
			}

			void bind() {
				bindEvent(true);
			}

			// FIXME - dirndl 1.1 - only required if Dom (or Gwt/InferredDom)
			// event, *not* ModelEvent.
			Widget getBindingWidget() {
				return verifySingleWidget();
			}

			void onTopicEvent(ModelEvent topicEvent) {
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
						? Reflections.at(model.getClass())
								.property(binding.from()).get(model)
						: binding.literal();
				boolean hasTransform = (Class) binding
						.transform() != ToStringFunction.Identity.class;
				if (hasTransform) {
					value = Registry.newInstanceOrImpl(binding.transform())
							.apply(value);
				}
				String stringValue = value == null ? "null" : value.toString();
				Element element = verifySingleWidget().getElement();
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
					if (value == null || (value instanceof Boolean
							&& !((Boolean) value).booleanValue())) {
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
					String attributeName = binding.to().isEmpty()
							? binding.from()
							: binding.to();
					element.getStyle().setProperty(attributeName, stringValue);
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

	static class InsertionPoint {
		Point point = Point.LAST;

		Widget after;

		Widget container;

		boolean pending = false;

		public void consume() {
			after = null;
			container = null;
			pending = false;
		}

		enum Point {
			FIRST, AFTER, LAST
		}
	}

	class RendererInput {
		ContextResolver resolver;

		final Object model;

		final AnnotationLocation location;

		final List<Directed> directeds;

		final Node parentNode;

		final Node node;

		Node replace;

		RendererInput(ContextResolver resolver, Object model,
				AnnotationLocation location, List<Directed> directeds,
				Node parentNode) {
			this.resolver = resolver;
			this.model = model;
			this.location = location;
			this.parentNode = parentNode;
			this.directeds = directeds != null ? directeds
					: location.getAnnotations(Directed.class);
			// generate the node (1-1 with input)
			node = new Node(resolver, parentNode, location, model);
			// don't add to parents yet (out of order) - but once we have a
			// better queue, do
			// if (parentNode != null) {
			// parentNode.children.add(node);
			// }
			node.directed = firstDirected();
		}

		public DirectedRenderer provideRenderer() {
			return resolver.getRenderer(node.directed, location, model);
		}

		@Override
		public String toString() {
			return Ax.format("Node:\n%s\n\nLocation: %s\n\nRenderer: %s",
					node.toParentStack(), location.toString(),
					provideRenderer().getClass().getSimpleName());
		}

		private Directed firstDirected() {
			return directeds.get(0);
		}

		void afterRender() {
			node.postRender();
			resolver.postRender();
			if (node.hasWidget()) {
				Optional<Widget> firstAncestorWidget = firstAncestorWidget();
				if (firstAncestorWidget.isPresent()) {
					ComplexPanel panel = (ComplexPanel) firstAncestorWidget
							.get();
					// in most cases, insertionPoint will be the default (LAST),
					// so don't set the field in that case . But use here to
					// make the logic clearer
					InsertionPoint insertionPoint = node.insertionPoint != null
							? node.insertionPoint
							: new InsertionPoint();
					switch (insertionPoint.point) {
					case FIRST:
						((ForIsWidget) panel).insert(node.widget, 0);
						break;
					case AFTER:
						int insertAfterIndex = panel
								.getWidgetIndex(insertionPoint.after);
						if (insertAfterIndex < panel.getWidgetCount() - 1) {
							((ForIsWidget) panel).insert(node.widget,
									insertAfterIndex + 1);
						} else {
							panel.add(node.widget);
						}
						break;
					case LAST:
						panel.add(node.widget);
						break;
					}
					insertionPoint.consume();
				}
			}
			if (directeds.size() > 1) {
				enqueueInput(resolver, model, location,
						directeds.subList(1, directeds.size()), node);
			}
		}

		void beforeRender() {
			DirectedContextResolver directedContextResolver = location
					.getAnnotation(DirectedContextResolver.class);
			if (directedContextResolver != null) {
				resolver = ContextResolver.create(
						directedContextResolver.value(), resolver,
						resolver.layout);
				resolver.setRootModel(model);
				// legal! note that new resolver will have an empty resolution
				// cache
				location.setResolver(resolver);
			}
			if (model instanceof DirectedLayout.Lifecycle) {
				((DirectedLayout.Lifecycle) model).beforeRender();
			}
			resolver.beforeRender(model);
		}

		void enqueueInput(ContextResolver resolver, Object model,
				AnnotationLocation location, List<Directed> directeds,
				Node parentNode) {
			DirectedLayout.this.enqueueInput(resolver, model, location,
					directeds, parentNode);
		}

		Optional<Widget> firstAncestorWidget() {
			return parentNode == null ? Optional.empty()
					: parentNode.firstSelfOrAncestorWidget(true);
		}

		void render() {
			if (replace != null) {
				node.insertionPoint = replace.resolveInsertionPoint();
				int indexInParentChildren = parentNode.children
						.indexOf(replace);
				replace.remove();
				parentNode.children.add(indexInParentChildren, node);
			} else {
				if (parentNode != null) {
					// add fairly late, to ensure we're in insertion order
					parentNode.children.add(node);
					// complexities of delegation and child replacement
					if (parentNode.insertionPoint != null
							&& parentNode.insertionPoint.pending) {
						node.insertionPoint = parentNode.insertionPoint;
					}
				}
			}
			beforeRender();
			if (model != null) {
				DirectedRenderer renderer = provideRenderer();
				renderer.render(this);
			}
			afterRender();
		}

		Directed soleDirected() {
			Preconditions.checkState(directeds.size() == 1);
			return directeds.get(0);
		}
	}
}
