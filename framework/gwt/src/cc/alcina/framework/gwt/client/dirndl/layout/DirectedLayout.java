package cc.alcina.framework.gwt.client.dirndl.layout;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.InsertPanel.ForIsWidget;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.process.AlcinaProcess;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.common.client.util.traversal.OneWayTraversal;
import cc.alcina.framework.common.client.util.traversal.Traversable;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Impl;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.InsertionPoint.Point;

/**
 *
 * <p>
 * A generative, expressive algorithm that transforms an arbitrary object into a
 * live layout tree of {@link DirectedLayout.Node} objects. These nodes
 * encapsulate a render tree (currently implemented only for HTML DOM, but
 * easily extended to arbitrary native widgets)
 *
 * <p>
 * Dirndl bears some resemblance to xslt - they both use annotations to
 * transform an object tree into markup, but the differences are what allows it
 * to function as a UI renderer:
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
 * their source model objects and mutate both structure and attributes
 * <li><b>events</b>: UI events are transformed into NodeEvent objects, which
 * provide a generally truer-to-the-model-logic way of modelling and handling
 * interface events than dealing directly with native (DOM) events
 * </ul>
 *
 * FIXME - dirndl 1xg - performance
 *
 * Minimise annotation resolution by caching an intermediate renderer object
 * which itself caches property/class annotation tuples (very similar to
 * reflective serializer typenode/propertynode implementation), scoped to
 * resolver but by default copying from parent
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
 * - Transform the initial object I to RendererInput RI, push onto RI (depth-first-traversal) queue
 * - Pop next RI from queue
 * - While RI.DL[] is non-empty, compute renderer R from DL0 (first element of RI.DL[])
 * - Apply R to RI via [DL0,CR], which (decidedly non-functional - i.e. results in multiple changes):
 * -- generates Node n which will be added to the children of PN
 * -- optionally generates Widget W which will be added as a child to the nearest parent Widget in the Node tree
 * -- optionally modifies RI.DL (TODO - examples)
 * -- can emit RI[] - the primary examples of that are:
 * --- if RI.DL[].length>1, emit a copy of RI with first element of RI.DL[] removed
 * --- applying the '[Transform model object]' algo above to the children (properties,
 * collection elements)of M, applicable only to the last @Directed in RI.DL[]
 * (Repeat until no RI queue is empty)
 *
 * -
 *
 * - Goals:
 *   - Is ContextResolver clear?
 *   - Is event propagation clear?
 *   - Plan Registry.Context (in fact, no, discuss/document why not)
 *   - Justify eventpump (or not eventpump) for node, transformed node events
 *   - Document dirndl 1x2 - widget removal
 *   - Documentation, demo app, comparison to react/angular/flutter/switfUI (that may take a while)
 *
 * - Phases :
 *   1x1b rest of TODO (complete)
 *   1x1c categorise FIXMEs, then d -> e -> f  etc (current, also working on [d])
 *   1x1d initial FIXMEs
 *   1x1e do a big localdom issue block - FIXMEs, improve tracking (with fully reproducible exceptions)
 *   1x1f reflectiveserializer: integrate into GWT serializer framework
 *   1x1g performance (possibly before 1x1e)
 *   1x1h docs (ditto)
 *   1x1j overlay/preview events redux
 *   1x2 switch table/form rendering to pure model - adjunct transformmanager
 *   1x3 low priority fixmes
 *   1x4 consider removing widget entirely (localdom)
 *   1x5 non-critical but desirable larger refactorings
 *
 *
 *
 *  @formatter:on
 */
public class DirectedLayout implements AlcinaProcess {
	static Logger logger = LoggerFactory.getLogger(DirectedLayout.class);
	static {
		AlcinaLogUtils.sysLogClient(DirectedLayout.class, Level.INFO);
	}

	// FIXME - dirndl 1x2 - remove (required until decoupling from
	// RnederContext)
	public static Node current = null;

	public static void dispatchModelEvent(ModelEvent modelEvent) {
		/*
		 * Bubble until event is handled or we've reached the top of the node
		 * tree
		 */
		Node cursor = modelEvent.getContext().node;
		while (cursor != null && !modelEvent.isHandled()) {
			cursor.dispatchEvent(modelEvent);
			cursor = cursor.parent;
		}
	}

	private Node root = null;

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
	OneWayTraversal<RendererInput> rendererInputs;

	boolean inLayout = false;

	Map<Class, Class<? extends DirectedRenderer>> modelRenderers = new LinkedHashMap<>();

	public InsertionPoint insertionPoint;

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

	public Widget render(Object model) {
		return render(ContextResolver.Default.get().createResolver(), model);
	}

	/*
	 * very simple caching, but lowers allocation *a lot*
	 */
	private Class<? extends DirectedRenderer>
			resolveModelRenderer(Object model) {
		return modelRenderers.computeIfAbsent(model.getClass(), clazz -> {
			try {
				Class<? extends DirectedRenderer> registration = Registry
						.query(DirectedRenderer.class).addKeys(clazz)
						.registration();
				return registration;
			} catch (RuntimeException e) {
				throw new RendererNotFoundException(Ax.format(
						"Renderer for %s not found - if a class to be rendered does not extend Model.class"
								+ ", it will require a registered DirectedRenderer class "
								+ "- for examples of such classes, see the nested classes of LeafRenderer.class",
						clazz.getSimpleName()), e);
			}
		});
	}

	RendererInput enqueueInput(ContextResolver resolver, Object model,
			AnnotationLocation location, List<Directed> directeds,
			Node parentNode) {
		// Even if model == null (so no widget will be emitted), nodes must be
		// added to the structure for a later change to non-null
		// if (model == null) {
		// return;
		// }
		RendererInput input = null;
		if (rendererInputs == null) {
			input = new RendererInput();
			// beginning of a layout
			rendererInputs = new OneWayTraversal<DirectedLayout.RendererInput>(
					input, RendererInput::new);
		} else {
			input = rendererInputs.add();
		}
		input.init(resolver, model, location, directeds, parentNode);
		return input;
	}

	// The layout loop
	void layout() {
		try {
			Preconditions.checkState(!inLayout);
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
			if (insertionPoint != null) {
				insertionPoint.clear();
				insertionPoint = null;
			}
		}
	}

	DirectedRenderer resolveRenderer(Directed directed,
			AnnotationLocation location, Object model) {
		Class<? extends DirectedRenderer> rendererClass = directed.renderer();
		if (rendererClass == DirectedRenderer.ModelClass.class) {
			// default - see Directed.Transform
			boolean transform = location
					.hasAnnotation(Directed.Transform.class);
			if (transform && !(model instanceof Collection)) {
				rendererClass = DirectedRenderer.TransformRenderer.class;
			} else {
				// Object.class itself (not as the root of the class hieararchy)
				// resolves to a Container. It's generally used for images or
				// other CSS placeholders
				if (model.getClass() == Object.class) {
					rendererClass = DirectedRenderer.Container.class;
				} else {
					rendererClass = resolveModelRenderer(model);
				}
			}
		}
		return Reflections.newInstance(rendererClass);
	}

	public static class EventObservable implements ProcessObservable {
		Class<? extends NodeEvent> type;

		Context context;

		Object model;

		public EventObservable(Class<? extends NodeEvent> type, Context context,
				Object model) {
			this.type = type;
			this.context = context;
			this.model = model;
		}

		@Override
		public String toString() {
			FormatBuilder fb = new FormatBuilder().separator("\n");
			fb.append("EVENT\n=============");
			fb.append(type.getSimpleName());
			fb.append(context.node.toParentStack());
			fb.append("");
			return fb.toString();
		}
	}

	/**
	 *
	 * <p>
	 * ...shades of the DOM render tree...
	 * </p>
	 *
	 * Also: changeSource/property/annotationLocation can all possibly be
	 * combined (or documented)
	 *
	 * <p>
	 * FIXME - dirndl 1x1g - optimise: specialise leafnode for performance
	 * (these are heavyweight, and leaves need not be so much so). Note that
	 * most optimisation is probably already done/implicit (see childreplacer) -
	 * if fields are never set (e.g. simple leaves), .js compilers will emit
	 * simpler objects anyway
	 */
	public static class Node {
		// not necessarily unchanged during the Node's lifetime - the renderer
		// can change it if required
		ContextResolver resolver;

		Directed directed;

		final AnnotationLocation annotationLocation;

		// below are nullable
		Node parent;

		final Object model;

		// many below may be null if a 'simple' node (particularly a leaf)
		private List<Node> children;

		Widget widget;

		List<NodeEventBinding> eventBindings;

		PropertyBindings propertyBindings;

		private ChildReplacer replacementListener;

		private InsertionPoint insertionPoint;

		protected Node(ContextResolver resolver, Node parent,
				AnnotationLocation annotationLocation, Object model) {
			this.resolver = resolver;
			this.parent = parent;
			this.annotationLocation = annotationLocation;
			this.model = model;
			current = this;
		}

		public <A extends Annotation> A annotation(Class<A> clazz) {
			return annotationLocation.getAnnotation(clazz);
		}

		// FIXME - dirndl 1x2 (use models for form intermediates) (remove, let
		// the form node handle focus itself)
		public Node childWithModel(Predicate<Object> test) {
			if (test.test(this.model)) {
				return this;
			}
			if (children != null) {
				for (Node child : children) {
					Node childWithModel = child.childWithModel(test);
					if (childWithModel != null) {
						return childWithModel;
					}
				}
			}
			return null;
		}

		public void dispatchEvent(ModelEvent modelEvent) {
			if (eventBindings != null) {
				eventBindings.forEach(bb -> bb.dispatchEventIfType(modelEvent));
			}
		}

		public AnnotationLocation getAnnotationLocation() {
			return this.annotationLocation;
		}

		public <T> T getModel() {
			return (T) this.model;
		}

		public ContextResolver getResolver() {
			return resolver;
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

		public <T> T resolveRenderContextProperty(String key) {
			return getResolver().resolveRenderContextProperty(key);
		}

		// Rare - but crucial - called by a DirectedRenderer
		// (DirectedRenderer.Transform transform ), imperatively setup a child
		// renderer
		public void setResolver(ContextResolver resolver) {
			this.resolver = resolver;
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
			if (hasWidget() && directed.bindings().length > 0) {
				propertyBindings = new PropertyBindings();
				propertyBindings.bind();
			}
			if (model instanceof LayoutEvents.Bind.Handler) {
				((LayoutEvents.Bind.Handler) model)
						.onBind(new LayoutEvents.Bind(this, true));
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
			// parent model at getProperty() for replacement changes (since the
			// parent, or a more remote
			// ancestor listens on that modelproperty for precisely that)
			//
			if (parent.parent != null && parent.model == parent.parent.model
					&& getProperty() == parent.getProperty()) {
				return;
			}
			// even though the parent handles changes,
			// binding/unbinding on node removal is the responsibility of the
			// created child node corresponding to the property, so we track on
			// the child
			//
			// FIXME - dirndl 1x2 - don't add this to form/table cells
			// (why? not sure - optimisation of r/o tables? make the originating
			// bean setterless I reckon)(so I reckon no, but check performance)
			replacementListener = new ChildReplacer((Bindable) parent.model,
					property.getName());
			replacementListener.bind();
		}

		private Widget provideWidgetOrLastDescendantChildWidget() {
			DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<>(
					this, Node::readOnlyChildren, true);
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
				if (children != null) {
					for (Node child : children) {
						child.resolveRenderedWidgets0(list);
					}
				}
			}
		}

		private void unbind() {
			if (model instanceof LayoutEvents.Bind.Handler) {
				((LayoutEvents.Bind.Handler) model)
						.onBind(new LayoutEvents.Bind(this, false));
			}
			if (children != null) {
				children.forEach(Node::unbind);
			}
			if (replacementListener != null) {
				replacementListener.unbind();
				replacementListener = null;
			}
			if (eventBindings != null) {
				eventBindings.forEach(NodeEventBinding::unbind);
			}
			if (propertyBindings != null) {
				propertyBindings.unbind();
			}
		}

		Widget firstDescendantWidget() {
			DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<>(
					this, Node::readOnlyChildren, false);
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
			DirectedRenderer renderer = resolver.layout
					.resolveRenderer(directed, annotationLocation, model);
			fb.appendPadRight(30, renderer.getClass().getSimpleName());
			fb.append(" ");
			Impl impl = new Directed.Impl(directed);
			fb.append(impl.toStringElideDefaults());
			return fb.toString();
		}

		void postRender() {
			bindBehaviours();
			bindModel();
			bindParentProperty();
		}

		List<Node> readOnlyChildren() {
			return children != null ? children : Collections.emptyList();
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
			Optional<Widget> firstSelfOrAncestorWidget = firstSelfOrAncestorWidget(
					false);
			if (firstSelfOrAncestorWidget.isEmpty()) {
				return result;// default, append
			}
			result.point = Point.FIRST;
			Node cursor = this;
			result.container = firstSelfOrAncestorWidget.get();
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
				Preconditions.checkState(!getResolver().layout.inLayout);
				// The input can mostly be constructed from this node (only the
				// model differs)
				Object newValue = evt.getNewValue();
				RendererInput input = getResolver().layout.enqueueInput(
						getResolver(), newValue,
						annotationLocation.copyWithClassLocationOf(newValue),
						null, parent);
				input.replace = Node.this;
				getResolver().layout.layout();
			}
		}

		/**
		 * <p>
		 * Note that dom/inferred-dom events (NodeEvent *not* subclass
		 * ModelEvent) and model events have quite different event propagation
		 * mechanisms, so there's essentially two event propagation mechanisms:
		 *
		 * <p>
		 * DOM: model -> widget -> element.addListener(x) -- Model implements
		 * the handler mechanism, event propagation is DOM propagation, so up
		 * the widget tree (which mostly corresponds to the DL node tree). (TODO
		 * - actually explain this - possibly in javadoc)
		 *
		 * <p>
		 * Model: ModelEvent.fire(...) - event fires on the current Model if it
		 * implements the Model.Handler class, and propagation finishes at the
		 * first Node that handles the event (implements the Handler class)
		 * unless explicitly permitted via
		 * NodeEvent.Context.markCauseEventAsNotHandled()
		 *
		 * <h3>An example</h3>
		 *
		 * <pre>
		 * <code>
		 *
		 *
		 * @Override
		 * public void onSubmitted(Submitted event) {
		 * 	// this occurs when enter is clicked, so handle here, but also propagate
		 * 	// to the containing form
		 * 	event.getContext().markCauseEventAsNotHandled();
		 * 	String value = textArea.getValue();
		 * 	setDirty(!Objects.equals(originalOrLastSubmittedValue, value));
		 * 	originalOrLastSubmittedValue = value;
		 * }
		 *
		 * </code>
		 * </pre>
		 *
		 * <p>
		 * TODO - should these in fact be two different bindings - say a base
		 * class and subclass?
		 */
		class NodeEventBinding {
			/*
			 * This may be a superclass of the event type (see ActionEvent).
			 * TODO - Doc (it makes event binding slightly more complicated, but
			 * allows a really useful inversion for one-off event handling
			 */
			Class<? extends NodeEvent> type;
			// FIXME - dirndl 1x1d - why binding? why not bound event?
			// also .... shouldn't we create these events on demand, and just
			// use type? or call it 'template'?
			//
			// Actually, the event provides access to - essentially - metadata
			// about the event. So that'd need to either go elsewhere, or keep
			// the current behaviour (with maybe more specific naming)
			// NodeEvent<? extends EventHandler> eventInstance;

			private int receiverIndex;

			DomBinding domBinding;

			public NodeEventBinding(Class<? extends NodeEvent> type, int idx) {
				this.type = type;
				this.receiverIndex = idx;
			}

			public void onEvent(GwtEvent event) {
				Context context = NodeEvent.Context.newModelContext(event,
						Node.this);
				dispatchEvent(type, context, Node.this.getModel());
			}

			@Override
			public String toString() {
				return Ax.format("Binding :: %s :: %s",
						model.getClass().getSimpleName(), type);
			}

			// this method contains devmode checks that a binding exists (if the
			// type does not implement WithoutDomBinding), and that the
			// DomBinding subclass is an inner class of the NodeEvent subclass
			private void bind() {
				Optional<DomBinding> bindingOptional = Registry
						.optional(DomBinding.class, type);
				if (!bindingOptional.isPresent()) {
					if (!GWT.isScript()) {
						Preconditions.checkState(Reflections.isAssignableFrom(
								NodeEvent.WithoutDomBinding.class, type));
					}
					return;
				}
				domBinding = bindingOptional.get();
				if (!GWT.isScript()) {
					Preconditions.checkState(domBinding.getClass().getName()
							.indexOf(type.getName()) == 0);
				}
				domBinding.nodeEventBinding = this;
				domBinding.bind(getBindingWidget(), model, true);
			}

			// FIXME - dirndl 1x1h - receive/reemit merging - document how to
			// reemit from StringInput (annotation merge should fail if
			// receive/reemit pair - instead, add just receipt and manually
			// reemit)
			//
			//
			private void dispatchEvent(
					Class<? extends NodeEvent> actualEventType, Context context,
					Object model) {
				NodeEvent nodeEvent = Reflections.newInstance(actualEventType);
				context.setNodeEvent(nodeEvent);
				nodeEvent.setModel(model);
				ProcessObservers.publish(EventObservable.class,
						() -> new EventObservable(actualEventType, context,
								model));
				Class<? extends EventHandler> handlerClass = Reflections
						.at(actualEventType).templateInstance()
						.getHandlerClass();
				NodeEvent.Handler handler = null;
				if (Reflections.isAssignableFrom(handlerClass,
						context.node.model.getClass())) {
					handler = (NodeEvent.Handler) context.node.model;
					((SimpleEventBus) Client.eventBus()).fireEventFromSource(
							nodeEvent, context.node, List.of(handler));
				} else {
					// dispatch a new ModelEvent, compute its type from the
					// correspondence between elements of @Directed.reemits and
					// receives
					Context eventContext = NodeEvent.Context
							.newModelContext(context, Node.this);
					Preconditions.checkState(directed
							.receives().length == directed.reemits().length);
					Class<? extends ModelEvent> emitType = (Class<? extends ModelEvent>) directed
							.reemits()[receiverIndex];
					ModelEvent.dispatch(eventContext, emitType,
							Node.this.getModel());
				}
			}

			private void unbind() {
				if (domBinding != null) {
					domBinding.bind(null, null, false);
				}
			}

			void dispatchEventIfType(ModelEvent event) {
				if (event.getReceiverType() == type) {
					Context context = NodeEvent.Context
							.newModelContext(event.getContext(), Node.this);
					// set before we dispatch to the handler, so the handler can
					// unset
					event.setHandled(true);
					dispatchEvent(event.getClass(), context, event.getModel());
				}
			}

			Widget getBindingWidget() {
				return verifySingleWidget();
			}
		}

		/*
		 * Binds a model property to an aspect of the rendered DOM
		 */
		class PropertyBinding {
			Binding binding;

			private RemovablePropertyChangeListener listener;

			private String lastValue;

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
					this.listener.bind();
				}
				set();
			}

			void set() {
				Property property = Reflections.at(model)
						.property(binding.from());
				Object value = binding.from().length() > 0 ? property.get(model)
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
						// only place we need to store the last value
						// We'd either have to store the "added" value, or
						// assume readonly
						boolean present = value != null;
						if (present) {
							element.setClassName(stringValue, true);
							lastValue = stringValue;
						} else {
							if (Ax.notBlank(lastValue)) {
								element.setClassName(lastValue, false);
								lastValue = null;
							}
						}
					} else {
						boolean present = (boolean) value;
						String cssClass = binding.literal().isEmpty()
								? Ax.cssify(binding.from())
								: binding.literal();
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

		class PropertyBindings {
			List<PropertyBinding> bindings = new ArrayList<>();

			PropertyBindings() {
			}

			void bind() {
				Arrays.stream(directed.bindings()).map(PropertyBinding::new)
						.forEach(bindings::add);
			}

			void unbind() {
				bindings.forEach(PropertyBinding::unbind);
			}
		}
	}

	/**
	 * A resolved location in the widget tree relative to which a widget should
	 * be inserted
	 *
	 * @author nick@alcina.cc
	 *
	 */
	static class InsertionPoint {
		Point point = Point.LAST;

		Widget after;

		Widget container;

		void clear() {
			// clear refs to possibly removed widgets
			after = null;
			container = null;
		}

		enum Point {
			FIRST, AFTER, LAST
		}
	}

	/**
	 * Instances act as an input and process state token for the
	 * layout/transformation algorithm
	 *
	 * @author nick@alcina.cc
	 *
	 */
	class RendererInput implements Traversable {
		ContextResolver resolver;

		// effectively final
		Object model;

		AnnotationLocation location;

		List<Directed> directeds;

		Node parentNode;

		Node node;

		Node replace;

		private RendererInput() {
		}

		@Override
		public Iterator children() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void enter() {
		}

		@Override
		public void exit() {
		}

		@Override
		public void release() {
			resolver = null;
			model = null;
			location = null;
			directeds = null;
			parentNode = null;
			node = null;
			replace = null;
		}

		@Override
		public String toString() {
			return Ax.format("Node:\n%s\n\nLocation: %s", node.toParentStack(),
					location.toString());
		}

		private Directed firstDirected() {
			return directeds.get(0);
		}

		void afterRender() {
			node.postRender();
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
					// FIRST and AFTER only occur during replace (and that not
					// at the end of a parent's children)
					case FIRST:
						((ForIsWidget) panel).insert(node.widget, 0);
						// bump insertion point
						insertionPoint.point = Point.AFTER;
						insertionPoint.after = node.widget;
						break;
					case AFTER:
						int insertAfterIndex = panel
								.getWidgetIndex(insertionPoint.after);
						if (insertAfterIndex < panel.getWidgetCount() - 1) {
							((ForIsWidget) panel).insert(node.widget,
									insertAfterIndex + 1);
							// bump insertion point
							insertionPoint.after = node.widget;
						} else {
							panel.add(node.widget);
							// bump insertion point
							insertionPoint.point = Point.LAST;
						}
						break;
					case LAST:
						panel.add(node.widget);
						break;
					}
					// the node *did* provide a widget, so its children will
					// just insert normally (LAST), no insertionpoint required
					node.insertionPoint = null;
				} else {
					// root - if this is a replace, append to root panel
					if (replace != null) {
						RootPanel.get().add(node.widget);
					}
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
				ContextResolver resolver = Reflections
						.newInstance(directedContextResolver.value());
				resolver.fromLayoutNode(node);
				// legal (modifying the node's resolver, etc)! note that new
				// resolver will have an empty resolution
				// cache
				node.resolver = resolver;
				this.resolver = resolver;
				location.setResolver(resolver);
			}
			if (model instanceof LayoutEvents.BeforeRender.Handler) {
				((LayoutEvents.BeforeRender.Handler) model)
						.onBeforeRender(new LayoutEvents.BeforeRender(node));
			}
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

		void init(ContextResolver resolver, Object model,
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

		void render() {
			if (replace != null) {
				node.insertionPoint = replace.resolveInsertionPoint();
				DirectedLayout.this.insertionPoint = node.insertionPoint;
				int indexInParentChildren = parentNode.children
						.indexOf(replace);
				replace.remove();
				parentNode.children.add(indexInParentChildren, node);
			} else {
				if (parentNode != null) {
					// add fairly late, to ensure we're in insertion order
					if (parentNode.children == null) {
						parentNode.children = new ArrayList<>();
					}
					parentNode.children.add(node);
					// complexities of delegation and child replacement
					if (parentNode.insertionPoint != null) {
						node.insertionPoint = parentNode.insertionPoint;
					}
				}
			}
			beforeRender();
			if (model != null) {
				DirectedRenderer renderer = resolveRenderer();
				renderer.render(this);
			}
			afterRender();
		}

		DirectedRenderer resolveRenderer() {
			return DirectedLayout.this.resolveRenderer(node.directed, location,
					model);
		}

		Directed soleDirected() {
			Preconditions.checkState(directeds.size() == 1);
			return directeds.get(0);
		}
	}

	static class RendererNotFoundException extends RuntimeException {
		public RendererNotFoundException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
