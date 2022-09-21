package cc.alcina.framework.gwt.client.dirndl.layout;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.InsertPanel.ForIsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
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
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.DirectedResolver;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent.Context;
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
	private static Logger logger = LoggerFactory
			.getLogger(DirectedLayout.class);
	static {
		AlcinaLogUtils.sysLogClient(DirectedLayout.class, Level.OFF);
	}

	public static boolean trace = false;

	// FIXME - remove
	public static Node current = null;

	static void trace(Supplier<String> messageSupplier) {
		if (!GWT.isScript() && trace) {
			logger.trace(messageSupplier.get());
		}
	}

	private Node root = null;

	// TODO - to preserve transformation order, collection elements and
	// properties are added in reverse order. Make a structure that removes the
	// need for a reverse()
	Deque<RendererInput> rendererInputs = new LinkedList();

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
		return root.firstDescendantWidget().orElse(null);
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
		rendererInputs.add(input);
		return input;
	}

	// The algorithm. Gosh.
	void layout() {
		do {
			// Inputs are added to the end of the deque, so removeLast() is a
			// depth-first traversal
			RendererInput input = rendererInputs.removeLast();
			if (root == null) {
				root = input.node;
			}
			input.render();
		} while (rendererInputs.size() > 0);
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

		Rendered rendered = new Rendered();

		DirectedNodeRenderer renderer;

		Directed directed;

		List<Node> children = new ArrayList<>();

		Node parent;

		List<RemovablePropertyChangeListener> listeners = new ArrayList<>();

		public Property changeSource;

		private boolean intermediate;

		// FIXME - remove
		private ContextResolver childResolver;

		// FIXME - dirndl 1x1b - ensure no null checks (guaranteed non null)
		final AnnotationLocation annotationLocation;

		public List<NodeEventBinding> eventBindings;

		public List<PropertyBinding> bindings;

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

		// FIXME - dirndlx1a - should just be annotationLocation.get(clazz)
		public <A extends Annotation> A annotation(Class<A> clazz) {
			Class locationClass = model == null ? null : model.getClass();
			// FIXME - dirndl 1.1 - check this behaviour. Note this
			// implementation 'incorrectly' skips resolution - because it
			// happens potentially before we resolve the @Directed. Possible
			// (probable)
			// solution is to have @DirectedContentResolver be resolved normally
			// (i.e. via the ContextResolver)
			//
			// (in 1.1 we get @Directeds earlier so probably already solved)
			// - all this and more in 1.1
			if ((directed != null
					&& directed.renderer() == ModelTransformNodeRenderer.class)
					|| (getProperty() != null
							&& getProperty().has(Directed.class)
							&& getProperty().annotation(Directed.class)
									.renderer() == ModelTransformNodeRenderer.class)) {
				// *don't* resolve against the model if it will be transformed
				// (resolution against the transform result, with dirndl 1.1,
				// will be resolution against the child node)
				if (clazz != Directed.Transform.class) {
					locationClass = null;
				}
			}
			if (locationClass != null || getProperty() != null) {
				AnnotationLocation location = new AnnotationLocation(
						locationClass, getProperty(), resolver);
				A annotation = location.getAnnotation(clazz);
				if (annotation != null) {
					return annotation;
				}
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

		public <A extends Annotation> boolean has(Class<A> clazz) {
			return annotation(clazz) != null;
		}

		public boolean hasWidget() {
			return rendered.widget != null;
		}

		public <A extends Annotation> Optional<A> optional(Class<A> clazz) {
			return Optional.ofNullable(annotation(clazz));
		}

		// FIXME - dirndl1x1 - check this (whether a resolver should apply to
		// *this* input or just child inputs)
		public void pushChildResolver(ContextResolver resolver) {
			this.childResolver = resolver;
		}

		public void remove() {
			if (parent != null) {
				parent.children.remove(this);
			}
			unbind();
		}

		public <T> T resolveRenderContextProperty(String key) {
			return resolver.resolveRenderContextProperty(key);
		}

		public DirectedNodeRenderer resolveRenderer() {
			DirectedNodeRenderer renderer = null;
			if (directed == null) {
				Class locationClass = model.getClass();
				// see annotation() above, goes away in 1.1
				if ((directed != null && directed
						.renderer() == ModelTransformNodeRenderer.class)
						|| (getProperty() != null
								&& getProperty().has(Directed.class)
								&& getProperty().annotation(Directed.class)
										.renderer() == ModelTransformNodeRenderer.class)) {
					// *don't* resolve against the model if it will be
					// transformed
					// (resolution against the transform result, with dirndl
					// 1.1,
					// will be resolution against the child node)
					locationClass = null;
				}
				AnnotationLocation annotationLocation = new AnnotationLocation(
						locationClass, getProperty(), resolver);
				// FIXME - dirndl1.1 - remove (switched to a strategy)
				directed = new DirectedResolver(
						getResolver().getTreeResolver(Directed.class),
						annotationLocation);
			}
			// TODO - distinguish between stateful and non-stateful
			Class<? extends DirectedNodeRenderer> rendererClass = directed
					.renderer();
			if (rendererClass == ModelClassNodeRenderer.class) {
				rendererClass = resolver.resolveModelRenderer(model);
			}
			renderer = Reflections.newInstance(rendererClass);
			return renderer;
		}

		@Override
		public String toString() {
			return path();
		}

		private void addListeners(Node child) {
			if (child.changeSource == null) {
				return;
			}
			// Object childModel = child.model;
			// // even though this (the parent) handles changes,
			// // binding/unbinding on node removal is the responsibility of the
			// // child, so we add to the child's listeners list
			// if (!child.changeSource.isReadOnly() && model instanceof
			// Bindable) {
			// // FIXME - dirndl.1 - don't add this to form/table cells
			// ChildReplacer listener = new ChildReplacer((Bindable) model,
			// child.changeSource.getName(), );
			// trace(() -> Ax.format("added listener :: {} :: {} :: {} :: {}",
			// child.pathSegment(), child.hashCode(),
			// child.changeSource.getName(), listener.hashCode()));
			// child.listeners.add(listener);
			// }
			// if (childModel instanceof Model) {
			// ChildReplacer listener = new ChildReplacer(
			// (Bindable) childModel, null, child);
			// trace(() -> Ax.format("added listener :: {} :: {} :: {} :: {}",
			// child.pathSegment(), child.hashCode(), "(fireUpdate)",
			// listener.hashCode()));
			// child.listeners.add(listener);
			// }
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
			bindings = Arrays.stream(directed.bindings())
					.map(PropertyBinding::new).collect(Collectors.toList());
			if (model instanceof HasBind) {
				((HasBind) model).bind();
			}
		}

		private void bindParentProperty() {
			Property property = getProperty();
			if (property == null || property.isReadOnly()) {
				return;
			}
			if (property.getName().equals("activity")) {
				int debug = 3;
			}
			// only listen on changes to the topmost Node corresponding to a
			// property. property != null guarantees parent != null, and
			// parent.model instanceof Bindable
			if (parent.getProperty() == property) {
				return;
			}
			// FIXME - dirndl.1 - don't add this to form/table cells
			ChildReplacer listener = new ChildReplacer((Bindable) parent.model,
					property.getName());
			// even though the parent handles changes,
			// binding/unbinding on node removal is the responsibility of the
			// child, so we add to the child's listeners list
			listeners.add(listener);
		}

		private void populateWidgets(boolean intermediateChild) {
			if (model == null) {
				return;
			}
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
							getProperty(), null);
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
				Collection<Property> properties = Reflections
						.at((model.getClass())).properties();
				for (Property property : properties) {
					Property directedProperty = resolver
							.resolveDirectedProperty(property);
					if (directedProperty != null) {
						Object childModel = property.get(model);
						addChild(childModel, directedProperty, property);
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
			children.remove(child);
		}

		private void render(boolean intermediateChild) {
			if (model == null) {
				return;
			}
			// FIXME - dndl1x1a - move to 1.1
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
				resolver.setModel(model);
			}
			populateWidgets(intermediateChild);
			bindBehaviours();
			bindModel();
			current = null;
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
			listeners.forEach(RemovablePropertyChangeListener::unbind);
			if (rendered.bindings != null) {
				rendered.bindings.forEach(PropertyBinding::unbind);
			}
		}

		// FIXME - will go
		protected Node createChild(Object childModel,
				Property definingReflector, Property changeSource) {
			// FIXME - this should probably be via rendererinputs
			Node child = new Node(
					childResolver != null ? childResolver : resolver, this,
					null, childModel);
			// child.property = definingReflector;
			child.changeSource = changeSource;
			child.resolver = resolver;
			addListeners(child);
			return child;
		}

		Node addChild(Object childModel, Property definingReflector,
				Property changeSource) {
			Node child = createChild(childModel, definingReflector,
					changeSource);
			// child.index = children.size();
			children.add(child);
			return child;
		}

		Optional<Widget> firstAncestorWidget() {
			Node cursor = this;
			do {
				Widget widget = cursor.rendered.widget;
				if (widget != null) {
					return Optional.of(widget);
				} else {
					cursor = cursor.parent;
				}
			} while (cursor != null);
			return Optional.empty();
		}

		Optional<Widget> firstDescendantWidget() {
			Node cursor = this;
			for (;;) {
				Widget widget = cursor.rendered.widget;
				if (widget != null) {
					return Optional.of(widget);
				} else {
					if (cursor.children.size() == 1) {
						cursor = cursor.children.get(0);
					} else {
						return Optional.empty();
					}
				}
			}
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

		Rendered render() {
			render(false);
			return rendered;
		}

		// this node will disappear, so refer to predecessor nodes
		//
		// walks the node tree backwards, looking for a rendered widget
		//
		// if the first encountereed widget is the insertion ancestor, there are
		// no
		// previous sibling widget (so return null)
		//
		// TODO - optimise use of index/indexOf
		Widget resolveInsertAfter() {
			Node cursor = this;
			Widget ancestorWidget = firstAncestorWidget().get();
			while (true) {
				if (cursor != this) {
					if (cursor.hasWidget()) {
						Widget widget = cursor.getWidget();
						if (widget == ancestorWidget) {
							return null;
						} else {
							return widget;
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

		public class Rendered {
			Widget widget;

			List<Widget> widgets = new ArrayList<>();

			public List<NodeEventBinding> eventBindings;

			public List<PropertyBinding> bindings;

			public NodeEventBinding
					eventBindingFor(Class<? extends NodeEvent> eventType) {
				return eventBindings.stream().filter(bb -> bb.type == eventType)
						.findFirst().get();
			}

			public int getChildIndex(Widget childWidget) {
				ComplexPanel panel = verifyContainer();
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
				// FIXME - dirndl 1.4 - this isn't optimal, but swapping
				// probably needs a larger structure to optimise anyway. A
				// class...?
				ComplexPanel container = verifyContainer();
				// FIXME - ui2 1.1 (remove null check? better representation of
				// insertion context?)
				if (container == null) {
					container = RootPanel.get();
				}
				for (Widget oldChild : oldChildWidgets) {
					if (insertAfterChildWidget.isPresent()
							&& insertAfterChildWidget.get() == oldChild) {
						int oldChildIndex = container.getWidgetIndex(oldChild);
						if (oldChildIndex > 0) {
							insertAfterChildWidget = Optional
									.of(container.getWidget(oldChildIndex - 1));
						} else {
							insertAfterChildWidget = Optional.empty();
						}
					}
					oldChild.removeFromParent();
				}
				for (int idx = newChildWidgets.size() - 1; idx >= 0; idx--) {
					int index = insertAfterChildWidget.map(this::getChildIndex)
							.orElse(-1) + 1;
					((InsertPanel) container).insert(newChildWidgets.get(idx),
							index);
				}
			}

			public Widget verifySingleWidget() {
				if (widget != null) {
					return widget;
				}
				Preconditions.checkState(widgets.size() == 1);
				return widgets.get(0);
			}

			private ComplexPanel verifyContainer() {
				if (renderer instanceof RendersToParentContainer) {
					if (parent == null) {
						// swapping top, delegating @Directed
						ComplexPanel viaWidget = (ComplexPanel) rendered.widgets
								.get(0).getParent();
						return viaWidget == null ? RootPanel.get() : viaWidget;
					} else {
						return parent.rendered.verifyContainer();
					}
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
			}
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
					trace(() -> Ax.format("Firing behaviour {} on {} to {}",
							eventTemplate.getClass().getSimpleName(),
							Node.this.pathSegment(),
							handlerClass.getSimpleName()));
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

			// FIXME - dirndl 1.1 - only required if Dom (or Gwt/InferredDom)
			// event, *not* ModelEvent
			Widget getBindingWidget() {
				return rendered.verifySingleWidget();
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
				Element element = rendered.verifySingleWidget().getElement();
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
	class RendererInput {
		ContextResolver resolver;

		final Object model;

		final AnnotationLocation location;

		final List<Directed> directeds;

		final Node parentNode;

		final Node node;

		Node replace;

		private Widget insertAfter;

		RendererInput(ContextResolver resolver, Object model,
				AnnotationLocation location, List<Directed> directeds,
				Node parentNode) {
			this.resolver = resolver;
			this.model = model;
			this.location = location;
			this.directeds = directeds != null ? directeds
					: location.getAnnotations(Directed.class);
			this.parentNode = parentNode;
			// generate the node (1-1 with input)
			node = new Node(resolver, parentNode, location, model);
			if (parentNode != null) {
				parentNode.children.add(node);
			}
			node.directed = firstDirected();
		}

		public DirectedRenderer provideRenderer() {
			return directeds.size() > 1 ? new DirectedRenderer.Container()
					: resolver.getRenderer(node.directed, location, model);
		}

		@Override
		public String toString() {
			return Ax.format("Node:\n%s\n\nRenderer: %s", node,
					location.toString());
		}

		private Directed firstDirected() {
			return directeds.get(0);
		}

		void enqueueInput(ContextResolver resolver, Object model,
				AnnotationLocation location, List<Directed> directeds,
				Node parentNode) {
			DirectedLayout.this.enqueueInput(resolver, model, location,
					directeds, parentNode);
		}

		Optional<Widget> firstAncestorWidget() {
			return parentNode == null ? Optional.empty()
					: parentNode.firstAncestorWidget();
		}

		void postRender() {
			// FIXME - dirndl 1.1 - this is *probably* a point guard, will need
			// to handle replace early
			//
			// replace will cause panel.insert (probably want an
			// InsertionLocation abstraction) and will possibly cause >1
			// removals (if this is a delegating widget)
			node.postRender();
			if (node.rendered.widget != null) {
				Optional<Widget> firstAncestorWidget = firstAncestorWidget();
				if (firstAncestorWidget.isPresent()) {
					ComplexPanel panel = (ComplexPanel) firstAncestorWidget
							.get();
					if (insertAfter != null) {
						int insertAfterIndex = panel
								.getWidgetIndex(insertAfter);
						if (insertAfterIndex < panel.getWidgetCount() - 1) {
							((ForIsWidget) panel).insert(node.rendered.widget,
									insertAfterIndex + 1);
						} else {
							panel.add(node.rendered.widget);
						}
						// panel;
					} else {
						panel.add(node.rendered.widget);
					}
				}
			}
			if (directeds.size() > 1) {
				enqueueInput(resolver, model, location,
						directeds.subList(1, directeds.size()), node);
			}
		}

		void preRender() {
			if (model instanceof DirectedLayout.Lifecycle) {
				// FIXME - dirndl 1.0 - lifecycle -> abstract class,
				// HasLifecycle, yadda
				// beforeRRender -> (maybe) first time render
				((DirectedLayout.Lifecycle) model).beforeRender();
			}
			resolver.beforeRender();
			DirectedContextResolver directedContextResolver = location
					.getAnnotation(DirectedContextResolver.class);
			if (directedContextResolver != null) {
				resolver = Reflections
						.newInstance(directedContextResolver.value());
				resolver.setModel(model);
			}
		}

		void render() {
			if (replace != null) {
				insertAfter = replace.resolveInsertAfter();
				replace.remove();
			}
			preRender();
			if (model != null) {
				DirectedRenderer renderer = provideRenderer();
				renderer.render(this);
			}
			postRender();
		}

		Directed soleDirected() {
			Preconditions.checkState(directeds.size() == 1);
			return directeds.get(0);
		}
	}
}
