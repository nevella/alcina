package cc.alcina.framework.gwt.client.dirndl.layout;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Annotation;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.collections.NotifyingList;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.process.AlcinaProcess;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.AttributeTemplate;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.ToStringFunction;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.common.client.util.traversal.OneWayTraversal;
import cc.alcina.framework.common.client.util.traversal.Traversable;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Bidi;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Impl;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent.DescendantEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent.Emitter;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.Context;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent.DirectlyInvoked;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.InsertionPoint.Point;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer.RendersNull;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.HasNode;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.util.ClassNames;
import cc.alcina.framework.gwt.client.util.StyleUtil;

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
 * <p>
 * <b>Note - ProcessObservers</b> Dirndl currently emits one observable, before
 * a received event is dispatched - here's an example of how to log those to
 * devmode stdout:
 *
 * <pre>
 * <code>
 *
 ProcessObservers.observe(DirectedLayout.EventObservable.class, Ax::out, true);
	</code>
 * </pre>
 *
 * <p>
 * FIXME - dirndl 1xg - performance
 *
 * <p>
 * Minimise annotation resolution by caching an intermediate renderer object
 * which itself caches property/class annotation tuples (very similar to
 * reflective serializer typenode/propertynode implementation), scoped to
 * resolver but by default copying from parent
 *
 * <h2>Model uniqueness</h2>
 * <p>
 * For a model to be assigned a Node, it must have a 1-1 correspondence with a
 * node (i.e. not be used at multiple points in the UI). If it *is* used at
 * multiple points, set @Directed(bindToModel=false) -- but that will prevent
 * the model from firing/reemitting events.
 * <p>
 * To workaround <i>that</i> - the easiest solution is to clone the model
 * (assuming it's invariant)
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
 * -- optionally generates Rendered W which will be added as a child to the nearest parent Rendered in the Node tree
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
 *   1x1c categorise FIXMEs, then d -> e -> f  etc (current, also working on [d])(complete)
 *   1x1d initial FIXMEs
 *   1x1e do a big localdom issue block - FIXMEs, improve tracking (with fully reproducible exceptions)
 *   1x1f reflectiveserializer: integrate into GWT serializer framework
 *   1x1g performance (possibly before 1x1e)
 *   1x1h docs (ditto)
 *   1x1j overlay/preview events redux
 *   1x2 switch table/form rendering to pure model - adjunct transformmanager (in fact - client transactions)
 *   1x2a Java Records! (done)
 *   1x2b Java Beans 2! Not public -- also consider removal of @Bean (but @ReflectionSerializable is required) (done)
 *  (note beans manifesto)
 *   1x2c Fragment Node fixes (FN)
 *   1x3 low priority fixmes
 *   1x3a consider consort rework
 *   1x4 consider removing widget entirely (localdom) (basically done)
 *   1x5 non-critical but desirable larger refactorings
 *
 *
 *  @formatter:on
 */
public class DirectedLayout implements AlcinaProcess {
	static Logger logger = LoggerFactory.getLogger(DirectedLayout.class);
	static {
		AlcinaLogUtils.sysLogClient(DirectedLayout.class, Level.INFO);
	}

	public static void dispatchModelEvent(ModelEvent modelEvent) {
		Object sourceModel = modelEvent.getContext().node.getModel();
		if (sourceModel instanceof ModelEvent.DelegatesDispatch) {
			Model dispatchDelegate = ((ModelEvent.DelegatesDispatch) sourceModel)
					.provideDispatchDelegate();
			if (dispatchDelegate != null) {
				if (modelEvent instanceof ModelEvent.DescendantEvent) {
					Context delegateDescentContext = NodeEvent.Context
							.fromEvent(modelEvent,
									dispatchDelegate.provideNode());
					ModelEvent delegateDescentEvent = Reflections
							.newInstance(modelEvent.getClass());
					delegateDescentContext.setNodeEvent(delegateDescentEvent);
					delegateDescentEvent.setModel(modelEvent.getModel());
					ModelEventDispatch.dispatchDescent(delegateDescentEvent);
				} else {
					modelEvent.reemitAs(dispatchDelegate, modelEvent.getClass(),
							modelEvent.getModel());
				}
				return;
			}
		}
		if (modelEvent instanceof ModelEvent.DescendantEvent) {
			ModelEventDispatch.dispatchDescent(modelEvent);
		} else {
			ModelEventDispatch.dispatchAscent(modelEvent);
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
	 *
	 *
	 */
	OneWayTraversal<RendererInput> rendererInputs;

	boolean inLayout = false;

	Map<Class, Class<? extends DirectedRenderer>> modelRenderers = new LinkedHashMap<>();

	public InsertionPoint insertionPoint;

	/**
	 * This guards against recursive (infinite) node generation, forcing input
	 * checks when node depth > maxDepth
	 */
	public int maxDepth = 99;

	boolean checkedRecursion = false;

	/**
	 * <p>
	 * The application may require that change listeners be dispatched in a
	 * particular context. By default they're not (just dispatched in the
	 * changing thread), which is fine for a single-threaded application.
	 *
	 * <p>
	 * It's assumed that the initial render is in the correct context,
	 * mutationDispatch is only used for non-initial renders
	 */
	public Consumer<Runnable> mutationDispatch = Runnable::run;

	RendererInput enqueueInput(ContextResolver resolver, Object model,
			Property property, AnnotationLocation location,
			List<Directed> directeds, Node parentNode) {
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
		input.init(resolver, model, property, location, directeds, parentNode);
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

	// remove the root node (unbind all listeners following removal from the
	// dom)
	public void remove() {
		root.remove(false);
		root = null;
	}

	public LayoutResult layoutResult;

	/**
	 * Render a model object and add top-level output widgets to the parent
	 * widget
	 */
	public LayoutResult render(ContextResolver resolver, Object model) {
		if (resolver == null) {
			resolver = ContextResolver.Default.get().createResolver();
		}
		resolver.layout = this;
		AnnotationLocation location = new AnnotationLocation(model.getClass(),
				null, resolver);
		enqueueInput(resolver, model, null, location, null, null);
		layout();
		this.layoutResult = new LayoutResult();
		return layoutResult;
	}

	public LayoutResult render(Object model) {
		return render(null, model);
	}

	/*
	 * very simple caching, but lowers allocation *a lot*
	 */
	Class<? extends DirectedRenderer> resolveModelRenderer(Object model,
			boolean collection, AnnotationLocation location) {
		return modelRenderers.computeIfAbsent(model.getClass(), clazz -> {
			// Object.class itself (not as the root of the class hieararchy)
			// resolves to a Container. It's generally used for images or
			// other CSS placeholders
			if (clazz == Object.class) {
				return DirectedRenderer.Container.class;
			}
			try {
				Class<? extends DirectedRenderer> registration = Registry
						.query(DirectedRenderer.class).addKeys(clazz)
						.registrationOrNull();
				if (registration == null && collection) {
					// the registry doesn't ascend interface hierarchies, so the
					// CollectionRenderer registration renders
					// AbstractCollection subtypes,
					// not Collection subtypes
					registration = Registry.query(DirectedRenderer.class)
							.addKeys(AbstractCollection.class).registration();
				}
				Preconditions.checkNotNull(registration);
				return registration;
			} catch (RuntimeException e) {
				throw new RendererNotFoundException(Ax.format(
						"Renderer for %s not found at %s - if a class to be rendered does not extend Model.class"
								+ ", it will require a registered DirectedRenderer class "
								+ "- for examples of such classes, see the nested classes of LeafRenderer.class",
						clazz.getSimpleName(), location), e);
			}
		});
	}

	DirectedRenderer resolveRenderer(Directed directed,
			AnnotationLocation location, Object model) {
		Class<? extends DirectedRenderer> rendererClass = directed.renderer();
		if (model == null && !Reflections.isAssignableFrom(RendersNull.class,
				directed.renderer())) {
			// @Directed.RenderNull
			return new LeafRenderer.Blank();
		}
		if (rendererClass == DirectedRenderer.ModelClass.class) {
			rendererClass = resolveModelRenderer(model,
					model instanceof Collection, location);
		}
		return Reflections.newInstance(rendererClass);
	}

	/**
	 * Usage:
	 *
	 * ProcessObservers.observe(DirectedLayout.EventObservable.class, Ax::out,
	 * true);
	 *
	 *
	 *
	 */
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
	 * A resolved location in the widget tree relative to which a widget should
	 * be inserted
	 *
	 *
	 *
	 */
	static class InsertionPoint {
		Point point = Point.LAST;

		Rendered after;

		Rendered container;

		void clear() {
			// clear refs to possibly removed widgets
			after = null;
			container = null;
		}

		enum Point {
			FIRST, AFTER, LAST
		}
	}

	public class LayoutResult {
		public Rendered getRendered() {
			return root.firstDescendantRendered();
		}

		public Node getRoot() {
			return root;
		}

		public void remove() {
			DirectedLayout.this.remove();
		}

		public DirectedLayout getLayout() {
			return DirectedLayout.this;
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
	 *
	 * <p>
	 * Style note - *try* not to access this class from outside this package. An
	 * example of a reasonable usage (because it requires access to the overlay
	 * context) is the retrieval of the ancestor Overlay in
	 * {@code Overlay.onBind()}
	 * <p>
	 * Note - fragmentnode ops - these follow DomNode (and w3c Node) ops -
	 * append, insertbefore, insertAsFirstChild. They manipulate several trees,
	 * at the moment via logical (DirectedLayout.Node) then Rendered(DOM) - this
	 * *could* be done via direct rendered manipulation and then flush of local
	 * changes, but it's more performant this way. But it may be better to
	 * eventually switch to the latter for consistency
	 */
	public class Node {
		// not necessarily unchanged during the Node's lifetime - the renderer
		// can change it if required
		ContextResolver resolver;

		Directed directed;

		final AnnotationLocation annotationLocation;

		final Property property;

		// below are nullable
		Node parent;

		int depth = -1;

		final Object model;

		// many below may be null if a 'simple' node (particularly a leaf)
		NotifyingList<Node> children;

		Rendered rendered;

		List<NodeEventBinding> eventBindings;

		PropertyBindings propertyBindings;

		ChildReplacer replacementListener;

		InsertionPoint insertionPoint;

		Runnable onUnbind;

		boolean lastForModel;

		boolean bindingsDisabled;

		public boolean isBindingsDisabled() {
			return bindingsDisabled;
		}

		public void setBindingsDisabled(boolean bindingsDisabled) {
			this.bindingsDisabled = bindingsDisabled;
		}

		protected Node(ContextResolver resolver, Node parent, Property property,
				AnnotationLocation annotationLocation, Object model,
				boolean lastForModel) {
			this.resolver = resolver;
			this.parent = parent;
			this.property = property;
			this.annotationLocation = annotationLocation;
			this.model = model;
			this.lastForModel = lastForModel;
			if (depth() > maxDepth && !checkedRecursion) {
				checkRecursion();
			}
		}

		NodeEventBinding
				addDescentEventBinding(Class<? extends NodeEvent> type) {
			Preconditions
					.checkArgument(NodeEventBinding.isDescendantBinding(type));
			NodeEventBinding newBinding = new NodeEventBinding(this, type);
			eventBindings.add(newBinding);
			return newBinding;
		}

		public <A extends Annotation> A annotation(Class<A> clazz) {
			return annotationLocation.getAnnotation(clazz);
		}

		public void append(FragmentNode child) {
			insertBefore(child, null);
		}

		public void applyReverseBindings() {
			if (propertyBindings != null) {
				propertyBindings.setLeft();
			}
		}

		public <T extends ContextService> T service(Class<T> serviceType) {
			return getResolver().getService(serviceType).get();
		}

		void bind(boolean modelToRendered) {
			bindModel(modelToRendered);
			bindParentProperty();
			/*
			 * During normal rendering there will be no children - this handles
			 * reverse-model node movements (e.g. wrapping, re-parenting)
			 */
			if (children != null && !modelToRendered) {
				children.forEach(c -> c.bind(false));
			}
		}

		private void bindEvents() {
			if (model == null) {
				return;
			}
			ReceivesEmitsEvents.ClassData classData = ReceivesEmitsEvents
					.get(ClassUtil.resolveEnumSubclassAndSynthetic(model));
			if (classData.receives.isEmpty()
					&& classData.emitsDescendant.isEmpty()
					&& directed.reemits().length == 0) {
				return;
			}
			/*
			 * Directed.Wrap (or equivalents) should not bind, only the last
			 * directed derived from a model
			 */
			if (!lastForModel) {
				return;
			}
			eventBindings = new ArrayList<>();
			classData.receives.forEach(clazz -> eventBindings
					.add(new NodeEventBinding(this, clazz)));
			for (int idx = 0; idx < directed.reemits().length; idx += 2) {
				Class<? extends NodeEvent> clazz = directed.reemits()[idx];
				NodeEventBinding reemitBinding = new NodeEventBinding(this,
						clazz);
				reemitBinding.reemitAs = (Class<? extends ModelEvent>) directed
						.reemits()[idx + 1];
				eventBindings.add(reemitBinding);
			}
			if (!directed.bindDomEvents()) {
				eventBindings.removeIf(NodeEventBinding::isDomBinding);
			}
			eventBindings.forEach(NodeEventBinding::bind);
			classData.emitsDescendant.forEach(this::addDescentEventBinding);
		}

		private void bindModel(boolean modelToRendered) {
			if (model == null) {
				return;
			}
			if (hasRendered()) {
				List<Binding> bindings = resolver.getBindings(directed, model);
				if (bindings.size() > 0) {
					propertyBindings = new PropertyBindings();
					propertyBindings.bind(modelToRendered);
				}
			}
			if (model instanceof LayoutEvents.Bind.Handler) {
				boolean bind = true;
				if (!lastForModel || !directed.bindToModel()) {
					// only bind if last of a multiple node -> single model
					// chain
					bind = false;
				} else if (!modelToRendered && model instanceof HasNode
						&& ((HasNode) model).provideIsBound()) {
					/*
					 * model.node will already be set in most cases (exceptions
					 * are like 'FragmentNode.replaceWith', which regenerates
					 * child node bindings)
					 */
					bind = false;
				}
				if (bind) {
					((LayoutEvents.Bind.Handler) model)
							.onBind(new LayoutEvents.Bind(this, true));
					fireEvent(new LayoutEvents.Bound());
				}
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
				// make an exception for the first level of an AttributeTemplate
				if (!Reflections.isAssignableFrom(AttributeTemplate.class,
						property.getOwningType()) || model == parent.model) {
					return;
				}
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
			if (!(parent.model instanceof Bindable)) {
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

		private void checkRecursion() {
			checkedRecursion = true;
			Ax.err("Checking recursion. To remove this check, increase DirectedLayout.maxDepth - depth %s - %s - %s",
					depth(), annotationLocation, model);
			Node cursor = this;
			CountingMap<RecursionTest> ancestorCount = new CountingMap<>();
			while (cursor != null) {
				ancestorCount.add(new RecursionTest(cursor));
				cursor = cursor.parent;
			}
			LinkedHashMap<RecursionTest, Integer> counts = ancestorCount
					.toLinkedHashMap(true);
			Entry<RecursionTest, Integer> first = Ax.first(counts.entrySet());
			if (first.getValue() >= 3) {
				throw new IllegalStateException(Ax.format(
						"Probable recursive layout generation - %s", first));
			}
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

		int depth() {
			if (depth == -1) {
				if (parent == null) {
					depth = 0;
				} else {
					depth = parent.depth() + 1;
				}
			}
			return depth;
		}

		public DomNode domNode() {
			return rendered.asDomNode();
		}

		public void dispatch(Class<? extends ModelEvent> modelEventClass,
				Object data) {
			Context context = NodeEvent.Context.fromNode(this);
			context.dispatch(modelEventClass, data);
		}

		NotifyingList<Node> ensureChildren() {
			if (children == null) {
				children = new NotifyingList<>(new ArrayList<>());
			}
			return children;
		}

		Emitter findEmitter(Class<? extends NodeEvent> type) {
			/*
			 * See signature of DescendantEvent
			 */
			Class<? extends Emitter> emitterType = Reflections.at(type)
					.getGenericBounds().bounds.get(2);
			Node cursor = this;
			while (cursor != null) {
				if (cursor.model != null && Reflections.isAssignableFrom(
						emitterType, cursor.model.getClass())) {
					return (Emitter) cursor.model;
				}
				cursor = cursor.parent;
			}
			return null;
		}

		void fireEvent(ModelEvent modelEvent) {
			if (eventBindings != null) {
				eventBindings.forEach(eventBinding -> eventBinding
						.fireEventIfType(modelEvent));
			}
		}

		Rendered firstDescendantRendered() {
			DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<>(
					this, Node::readOnlyChildren);
			for (Node node : traversal) {
				if (node.rendered != null) {
					return node.rendered;
				}
			}
			return null;
		}

		Optional<Rendered> firstSelfOrAncestorRendered(boolean includeSelf) {
			Node cursor = this;
			do {
				if ((includeSelf || cursor != this)
						&& cursor.rendered != null) {
					return Optional.of(cursor.rendered);
				} else {
					cursor = cursor.parent;
				}
			} while (cursor != null);
			return Optional.empty();
		}

		public AnnotationLocation getAnnotationLocation() {
			return this.annotationLocation;
		}

		NodeEventBinding getEventBinding(Class<? extends NodeEvent> type) {
			return eventBindings.stream().filter(eb -> eb.type == type)
					.findFirst()
					.orElseThrow(() -> new IllegalStateException(Ax.format(
							"Type %s does not implement the %s Emitter",
							NestedName.get(this.model), NestedName.get(type))));
		}

		public <T> T getModel() {
			return (T) this.model;
		}

		/*
		 * annotationLocation.property will *normally* be the bound property,
		 * unless annotations are sourced from a different property (the
		 * original property in that case will be the Node.property field)
		 * 
		 * 
		 */
		Property getProperty() {
			return property != null ? property : annotationLocation.property;
		}

		public DirectedLayout.Rendered getRendered() {
			return rendered;
		}

		public ContextResolver getResolver() {
			return resolver;
		}

		public <A extends Annotation> boolean has(Class<A> clazz) {
			return annotation(clazz) != null;
		}

		public boolean hasRendered() {
			return rendered != null;
		}

		public void insertAfter(FragmentNode newChildModel,
				FragmentNode refModel) {
			int nextNodeIndex = children.indexOf(refModel.provideNode()) + 1;
			FragmentNode beforeModel = nextNodeIndex == children.size() ? null
					: (FragmentNode) children.get(nextNodeIndex).model;
			insertBefore(newChildModel, beforeModel);
		}

		/**
		 * Another bidi model/render (via dl.node) structure call, this calls an
		 * immediate layout. model-model ownership is maintained by the
		 * fragmentmodel rather than model fields
		 *
		 * Note this is a reparent of an existing node
		 */
		public void insertAsFirstChild(FragmentNode newChildModel) {
			ensureChildren();
			insertBefore(newChildModel, Ax.firstOptional(children)
					.<FragmentNode> map(Node::getModel).orElse(null));
		}

		public void insertBefore(FragmentNode newChildModel,
				FragmentNode refChildModel) {
			ensureChildren();
			Node oldNode = newChildModel.provideNode();
			/*
			 * This doesn't remove (with recursion) existing rendered dom - we
			 * move that rather than requiring a rerender
			 */
			removeChildNode(newChildModel, true);
			RendererInput input = getResolver().layout.enqueueInput(
					getResolver(), newChildModel,
					oldNode == null ? null : oldNode.property,
					annotationLocation.copyWithClassLocationOf(newChildModel),
					null, this);
			input.before = refChildModel == null ? null
					: refChildModel.provideNode();
			if (oldNode != null) {
				input.rendered = oldNode.rendered;
			}
			getResolver().layout.layout();
			moveChildren(oldNode, newChildModel.provideNode());
		}

		/*
		 * Note that this requires an onto dom/layout correlation (no
		 * delegated), which is fine since it's only called by dom traversal
		 */
		public Node insertFragmentChild(Model childModel,
				org.w3c.dom.Node childW3cNode) {
			Node node = new Node(resolver, this, null, new AnnotationLocation(
					childModel.getClass(), null, resolver), childModel, true);
			node.rendered = new RenderedW3cNode(childW3cNode);
			node.directed = node.annotation(Directed.class);
			if (node.directed == null) {
				node.annotation(Directed.class);
			}
			org.w3c.dom.Node previousSibling = childW3cNode
					.getPreviousSibling();
			ensureChildren();
			boolean append = previousSibling == null && children.isEmpty()
					|| previousSibling == Ax.last(children).rendered.getNode();
			if (append) {
				children.add(node);
			} else {
				int idx = 0;
				int insertAfter = -1;
				while (idx < children.size()) {
					if (children.get(idx).rendered
							.getNode() == previousSibling) {
						insertAfter = idx;
						break;
					}
					idx++;
				}
				children.add(insertAfter + 1, node);
			}
			node.bind(false);
			return node;
		}

		void moveChildren(Node from, Node to) {
			if (from == null || from.ensureChildren().isEmpty()) {
				return;
			}
			to.ensureChildren();
			Preconditions.checkState(to.children.isEmpty());
			from.children.stream().collect(Collectors.toList())
					.forEach(child -> {
						child.remove(true);
						child.parent = to;
						to.children.add(child);
						to.rendered.append(child.rendered);
						child.bind(false);
					});
		}

		/**
		 * FIMXE - dirndl - tmp, until widget system completely removed (i.e.
		 * value editors)
		 */
		public void onUnbind(Runnable runnable) {
			this.onUnbind = runnable;
		}

		public <A extends Annotation> Optional<A> optional(Class<A> clazz) {
			return Optional.ofNullable(annotation(clazz));
		}

		public <A extends Annotation> Optional<A>
				optionalAnnotation(Class<A> clazz) {
			return Optional.ofNullable(annotationLocation.getAnnotation(clazz));
		}

		String path() {
			String thisLoc = pathSegment();
			if (parent == null) {
				return thisLoc;
			} else {
				return parent.path() + " \n" + thisLoc;
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
			String rendererClassName = model == null
					? "[no renderer/null model]"
					: resolver.layout.resolveRenderer(directed,
							annotationLocation, model).getClass()
							.getSimpleName();
			fb.appendPadRight(30, rendererClassName);
			fb.append(" ");
			Impl impl = new Directed.Impl(directed);
			fb.append(impl.toStringElideDefaults());
			return fb.toString();
		}

		void postRender() {
			bindEvents();
		}

		void postDomAttach() {
			bind(true);
			if (model instanceof LayoutEvents.EmitDescent.Handler) {
				((LayoutEvents.EmitDescent.Handler) model).onEmitDescent(
						new LayoutEvents.EmitDescent(this, true));
			}
		}

		public class NodeRelative {
			public Node previousSibling() {
				if (parent == null) {
					return null;
				}
				int idx = parent.children.indexOf(Node.this);
				return idx == 0 ? null : parent.children.get(idx - 1);
			}

			// parallels DomNode.treePreviousNode
			public Node treePreviousNode() {
				Node cursor = Node.this;
				Node previous = null;
				while (cursor != null) {
					previous = cursor.relative().previousSibling();
					if (previous != null) {
						break;
					} else {
						cursor = cursor.parent;
					}
				}
				if (previous == null) {
					return null;
				} else {
					return previous.relative().lastDescendant();
				}
			}

			public Node treeSubsequentNodeNoDescent() {
				if (hasNextSibling()) {
					return nextSibling();
				}
				if (parent != null) {
					return parent.relative().treeSubsequentNodeNoDescent();
				}
				return null;
			}

			public Node treeSubsequentNode() {
				Node firstChild = firstChild();
				if (firstChild != null) {
					return firstChild;
				}
				return treeSubsequentNodeNoDescent();
			}

			public boolean hasNextSibling() {
				return nextSibling() != null;
			}

			public Node lastDescendant() {
				Node cursor = Node.this;
				while (cursor != null) {
					Node last = cursor.relative().lastChild();
					if (last == null) {
						return cursor;
					}
					cursor = last;
				}
				return null;
			}

			Node firstChild() {
				if (children == null) {
					return null;
				}
				return Ax.first(children);
			}

			Node lastChild() {
				if (children == null) {
					return null;
				}
				return Ax.last(children);
			}

			public Node nextSibling() {
				if (parent == null) {
					return null;
				}
				int idx = parent.children.indexOf(Node.this);
				return idx == parent.children.size() - 1 ? null
						: parent.children.get(idx + 1);
			}
		}

		public NodeRelative relative() {
			return new NodeRelative();
		}

		public Node provideMostSpecificNodeForModel() {
			Node cursor = this;
			while (true) {
				if (cursor.children.size() == 1) {
					Node firstChild = cursor.children.get(0);
					if (firstChild.model == model) {
						cursor = firstChild;
						continue;
					}
				}
				return cursor;
			}
		}

		private Rendered provideRenderedOrLastDescendantChildRenderedPriorTo() {
			DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<>(
					this, Node::readOnlyChildren, true);
			for (Node node : traversal) {
				if (node.rendered != null) {
					return node.rendered;
				}
			}
			return null;
		}

		List<Node> readOnlyChildren() {
			return children != null ? children : Collections.emptyList();
		}

		/*
		 * Note that removeFromRendered==true calls a 'deep remove' - all
		 * subnodes of the tree will be removed from their parent. This is
		 * *normally* desired behaviour - to remove stray bindings in descendant
		 * nodes, particularly. But it is different to say DOM behaviour where
		 * removal just detaches the subtree.
		 * 
		 * Note further that this doesn't detach all the leaves of the
		 * associated domNode (if any) - only the rootremoved
		 */
		void remove(boolean willReattach) {
			remove(!willReattach, !willReattach);
		}

		void remove(boolean removeFromRendered, boolean removeParentBindings) {
			if (removeFromRendered) {
				/*
				 * We want to minimize dom mutations, but have to allow for
				 * delegation - so descend all branches until a DomNode
				 * (hasRendered) is reached
				 */
				Deque<Node> removeFromDom = new ArrayDeque<>();
				removeFromDom.push(this);
				while (removeFromDom.size() > 0) {
					Node node = removeFromDom.pop();
					if (node.hasRendered()) {
						if (node.rendered.asDomNode().isAttached()) {
							node.rendered.removeFromParent();
						}
					} else {
						if (node.children != null) {
							node.children.forEach(removeFromDom::push);
						}
					}
				}
			}
			if (removeParentBindings) {
				if (children != null) {
					/*
					 * This *may* not be necessary for FragmentModel where
					 * LocalMutations are operating - since Dom tree removal
					 * would remove the descendant tree - but doesn't hurt
					 * 
					 * 
					 */
					// slightly more efficient in reverse
					for (int idx = children.size() - 1; idx >= 0; idx--) {
						Node child = children.get(idx);
						child.remove(false, true);
					}
				}
			}
			if (parent != null) {
				parent.children.remove(this);
			}
			unbind();
		}

		public void removeChildNode(Model child, boolean willReattach) {
			Node childNode = child.provideNode();
			if (childNode != null) {
				childNode.remove(willReattach);
			}
		}

		public void replaceChild(Model oldModel, Model newModel) {
			Node oldNode = oldModel.provideNode();
			RendererInput input = getResolver().layout.enqueueInput(
					getResolver(), newModel, oldNode.property,
					annotationLocation.copyWithClassLocationOf(newModel), null,
					this);
			input.replace = oldNode;
			input.removeReplaced = false;
			getResolver().layout.layout();
			moveChildren(oldNode, newModel.provideNode());
			oldNode.remove(false);
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
			Optional<Rendered> firstSelfOrAncestorRendered = firstSelfOrAncestorRendered(
					false);
			if (firstSelfOrAncestorRendered.isEmpty()) {
				return result;// default, append
			}
			result.point = Point.FIRST;
			Node cursor = this;
			result.container = firstSelfOrAncestorRendered.get();
			IdentityHashMap<Node, Boolean> ancestors = new IdentityHashMap<>();
			// of course, there's sure to be a mor
			cursor = cursor.parent;
			while (cursor != null) {
				ancestors.put(cursor, true);
				cursor = cursor.parent;
			}
			cursor = this;
			while (true) {
				// test for rendered, if there is one return it - either AFTER
				// or FIRST, depending on its relation to the node
				if (cursor != this) {
					if (ancestors.containsKey(cursor)) {
						if (cursor.rendered != null) {
							// no preceding widget, insert first
							return result;
						}
					} else {
						Rendered rendered = cursor
								.provideRenderedOrLastDescendantChildRenderedPriorTo();
						if (rendered != null) {
							result.point = Point.AFTER;
							result.after = rendered;
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

		// Rare - but crucial - called by a DirectedRenderer
		// (DirectedRenderer.Transform transform ), imperatively setup a child
		// renderer
		public void setResolver(ContextResolver resolver) {
			this.resolver = resolver;
		}

		public void strip() {
			List<Node> oldChildren = children.stream()
					.collect(Collectors.toList());
			int insertionIndex = parent.children.indexOf(this);
			Node insertAfterCursor = this;
			// FIXME - fragmentmodel - this doesn't allow for delegating etc (in
			// fact all the mutation methods don't)
			for (Node child : oldChildren) {
				child.parent = parent;
				parent.children.add(insertionIndex++, child);
				parent.rendered.insertAfter(child.rendered,
						insertAfterCursor.rendered);
				insertAfterCursor = child;
			}
			/*
			 * Performing a direct reparent in the loop, so need to clear
			 * children.
			 */
			children.removeAll(oldChildren);
			parent.children.remove(this);
			rendered.removeFromParent();
		}

		public String toParentStack() {
			return path();
		}

		/*
		 * for debugging, return a list of all Nodes in the ancestry chain of
		 * this Node
		 */
		List<Node> toNodeStack() {
			Node cursor = this;
			List<Node> result = new ArrayList<>();
			while (cursor != null) {
				result.add(cursor);
				cursor = cursor.parent;
			}
			Collections.reverse(result);
			return result;
		}

		@Override
		public String toString() {
			return pathSegment();
		}

		void unbind() {
			if (model instanceof LayoutEvents.Bind.Handler) {
				if (parent != null && parent.model == model) {
					// noop (to prevent multiple bind event firings on the same
					// model)
				} else {
					((LayoutEvents.Bind.Handler) model)
							.onBind(new LayoutEvents.Bind(this, false));
					fireEvent(new LayoutEvents.Unbound());
				}
			}
			if (onUnbind != null) {
				onUnbind.run();
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

		// i.e. that the node doesn't correspond to @Directed.Delegating
		Rendered verifySingleRendered() {
			Preconditions.checkState(rendered != null);
			return rendered;
		}

		/**
		 * <p>
		 * Replaces a child node following a property change
		 *
		 * <p>
		 * Note that this is only added to the topmost Node corresponding to a
		 * given source/property
		 *
		 * <p>
		 * FIXME - dirndl - possibly enqueue changes rather than throwing if in
		 * layout
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
				mutationDispatch.accept(() -> {
					RendererInput input = getResolver().layout
							.enqueueInput(getResolver(), newValue,
									getProperty(), annotationLocation
											.copyWithClassLocationOf(newValue),
									null, parent);
					input.replace = Node.this;
					getResolver().layout.layout();
				});
			}
		}

		/*
		 * Binds a model property to an aspect of the rendered DOM
		 */
		class PropertyBinding {
			Binding binding;

			private RemovablePropertyChangeListener listener;

			private String lastValue;

			boolean innerTextWasSet;

			boolean innerHtmlWasSet;

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
			}

			void bind() {
				if (this.binding.from().length() > 0
						&& model instanceof Bindable) {
					this.listener = new RemovablePropertyChangeListener(
							(Bindable) model, this.binding.from(), evt -> {
								if (!bindingsDisabled) {
									setRight();
								}
							});
					this.listener.bind();
				}
			}

			Property getProperty() {
				Property property = null;
				if (Ax.notBlank(binding.from())) {
					property = Reflections.at(model).property(binding.from());
					if (property == null) {
						throw new IllegalArgumentException(
								Ax.format("No property %s for model %s",
										binding.from(), NestedName.get(model)));
					}
				}
				return property;
			}

			void setLeft() {
				Property property = getProperty();
				if (property == null) {
					// literatl
					return;
				}
				if (property.isReadOnly()) {
					return;
				}
				String stringValue = null;
				Rendered rendered = verifySingleRendered();
				Element element = rendered.isElement() ? rendered.asElement()
						: null;
				org.w3c.dom.Node node = rendered.getNode();
				org.w3c.dom.Element w3cElement = node
						.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE
								? (org.w3c.dom.Element) node
								: null;
				switch (binding.type()) {
				case INNER_HTML:
					stringValue = element.getInnerHTML();
					break;
				case INNER_TEXT:
					if (element == null) {
						stringValue = node.getNodeValue();
					} else {
						stringValue = element.getInnerText();
					}
					break;
				case PROPERTY:
				case CLASS_PROPERTY: {
					if (!rendered.hasAttributes()) {
						break;
					}
					String propertyName = binding.type() == Type.CLASS_PROPERTY
							? "class"
							: binding.to().isEmpty() ? Ax.cssify(binding.from())
									: binding.to();
					if (w3cElement.hasAttribute(propertyName)) {
						stringValue = w3cElement.getAttribute(propertyName);
					}
					break;
				}
				case CSS_CLASS:
				case SWITCH_CSS_CLASS:
					throw new UnsupportedOperationException();
				case STYLE_ATTRIBUTE:
					if (w3cElement.hasAttribute("style")) {
						StringMap styleMap = StyleUtil.styleAttributeToMap(
								w3cElement.getAttribute("style"));
						String key = Ax.cssify(property.getName());
						stringValue = styleMap.get(key);
					}
					break;
				default:
					throw new UnsupportedOperationException();
				}
				Object value = null;
				boolean hasTransform = (Class) binding
						.transform() != ToStringFunction.Identity.class;
				if (hasTransform) {
					ToStringFunction.Bidi bidiTransform = (Bidi) Registry
							.newInstanceOrImpl(binding.transform());
					Function<String, ?> transform = bidiTransform.rightToLeft();
					if (transform instanceof Binding.ContextSensitiveReverseTransform) {
						((Binding.ContextSensitiveReverseTransform) transform)
								.withContextNode(Node.this);
					}
					if (transform instanceof Binding.ContextPropertySensitiveReverseTransform) {
						((Binding.ContextPropertySensitiveReverseTransform) transform)
								.putContextProperty(property);
					}
					value = transform.apply(stringValue);
				} else {
					value = stringValue;
					if (Ax.notBlank(stringValue)) {
						if (property.getType() == Boolean.class) {
							value = Boolean.valueOf(stringValue);
						} else if (property.getType() == Integer.class) {
							value = Integer.parseInt(stringValue);
						} else if (property.getType() == Double.class) {
							value = Double.parseDouble(stringValue);
						}
						if (property.getType() == boolean.class) {
							value = Boolean.valueOf(stringValue);
						} else if (property.getType() == int.class) {
							value = Integer.parseInt(stringValue);
						} else if (property.getType() == double.class) {
							value = Double.parseDouble(stringValue);
						}
					} else {
						if (property.getType() == boolean.class) {
							value = false;
						} else if (property.getType() == int.class) {
							value = 0;
						} else if (property.getType() == double.class) {
							value = 0.0;
						}
					}
				}
				property.set(model, value);
			}

			void setRight() {
				Property property = getProperty();
				Object value = binding.from().length() > 0 ? property.get(model)
						: binding.literal();
				boolean hasTransform = (Class) binding
						.transform() != ToStringFunction.Identity.class;
				if (hasTransform) {
					ToStringFunction transform = Registry
							.newInstanceOrImpl(binding.transform());
					if (transform instanceof Binding.ContextSensitiveTransform) {
						((Binding.ContextSensitiveTransform) transform)
								.withContextNode(Node.this);
					}
					value = transform.apply(value);
				}
				String stringValue = value == null ? "null" : value.toString();
				boolean nullNoTransform = value == null && !hasTransform;
				Rendered rendered = verifySingleRendered();
				Element element = rendered.isElement() ? rendered.asElement()
						: null;
				org.w3c.dom.Node node = rendered.getNode();
				org.w3c.dom.Element w3cElement = node
						.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE
								? (org.w3c.dom.Element) node
								: null;
				switch (binding.type()) {
				case INNER_HTML:
					if (value != null) {
						element.setInnerHTML(stringValue);
						innerHtmlWasSet = true;
					} else {
						if (innerHtmlWasSet) {
							element.removeAllChildren();
							innerHtmlWasSet = false;
						}
					}
					break;
				case INNER_TEXT:
					if (value != null) {
						if (element == null) {
							if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
								node.appendChild(node.getOwnerDocument()
										.createTextNode(stringValue));
							} else {
								// COMMENT, TEXT, PROCESSING_INSTRUCTION
								node.setNodeValue(stringValue);
							}
						} else {
							element.setInnerText(stringValue);
						}
						innerTextWasSet = true;
					} else {
						if (innerTextWasSet) {
							if (element == null) {
								int count = node.getChildNodes().getLength();
								Preconditions.checkState(count <= 1);
								if (count == 1) {
									node.removeChild(
											node.getChildNodes().item(0));
								}
							} else {
								element.removeAllChildren();
							}
							innerTextWasSet = false;
						}
					}
					break;
				case PROPERTY:
				case CLASS_PROPERTY: {
					if (!rendered.hasAttributes()) {
						// generic containers which may be rendered as
						// non-element nodes
						break;
					}
					String propertyName = binding.type() == Type.CLASS_PROPERTY
							? "class"
							: binding.to().isEmpty() ? Ax.cssify(binding.from())
									: binding.to();
					if (value == null || (value instanceof Boolean
							&& !((Boolean) value).booleanValue())) {
						// if ++ 'class', don't remove (special-case)
						if (Ax.isBlank(lastValue)
								&& Objects.equals(propertyName, "class")) {
							// don't overwrite @Directed.cssClass
						} else {
							w3cElement.removeAttribute(propertyName);
						}
					} else {
						if (element != null) {
							element.setPropertyString(propertyName,
									stringValue);
						} else {
							w3cElement.setAttribute(propertyName, stringValue);
						}
						lastValue = stringValue;
					}
					break;
				}
				case CSS_CLASS: {
					/*
					 * Note (and probably fixme) - if the property value (or
					 * transformed value) is a string, this implementation
					 * currently assumes the string is invariant.
					 * 
					 * That could be changed - but there's a risk of classname
					 * collision in that case (prop A + B both
					 * transforming/representing CN C). Almost certainly better
					 * if the string is variant would be to map to a
					 * variant-valued property rather than a classname
					 */
					boolean present = false;
					String cssClass = null;
					// almost (see class_property) only place we need to
					// store the last value
					// We'd either have to store the "added" value, or
					// assume readonly
					if (hasTransform) {
						cssClass = stringValue;
						present = value != null && cssClass != null;
					} else {
						if (value instanceof Boolean) {
							cssClass = binding.literal().isEmpty()
									? Ax.cssify(binding.from())
									: binding.literal();
							present = (boolean) value;
						} else if (value instanceof String) {
							cssClass = (String) value;
							present = cssClass != null;
						} else {
							Preconditions.checkArgument(value == null);
						}
					}
					if (present) {
						if (cssClass.length() > 0) {
							if (element != null) {
								element.setClassName(cssClass, true);
							} else {
								String existingClassNames = w3cElement
										.getAttribute("class");
								String updated = ClassNames.addClassName(
										existingClassNames, cssClass);
								w3cElement.setAttribute("class", updated);
							}
							lastValue = cssClass;
						}
					} else {
						if (Ax.notBlank(lastValue)) {
							if (element != null) {
								element.setClassName(cssClass, false);
							} else {
								String existingClassNames = w3cElement
										.getAttribute("class");
								String updated = ClassNames.removeClassName(
										existingClassNames, cssClass);
								w3cElement.setAttribute("class", updated);
							}
							lastValue = null;
						}
					}
					if (element != null) {
						if (element.getClassName().isEmpty()) {
							element.removeAttribute("class");
						}
					} else {
						if (w3cElement.getAttribute("class").isEmpty()) {
							w3cElement.removeAttribute("class");
						}
					}
					break;
				}
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
					if (element != null) {
						if (Ax.notBlank(stringValue) && !nullNoTransform) {
							element.getStyle().setProperty(attributeName,
									stringValue);
						} else {
							element.getStyle().removeProperty(attributeName);
						}
					} else {
						StringMap styleMap = StyleUtil.styleAttributeToMap(
								w3cElement.getAttribute("style"));
						String key = Ax.cssify(attributeName);
						if (Ax.notBlank(stringValue) && !nullNoTransform) {
							Preconditions.checkArgument(
									stringValue.matches("[^'\":;=\n]+"));
							styleMap.put(key, stringValue);
						} else {
							styleMap.remove(key);
						}
						w3cElement.setAttribute("style",
								StyleUtil.styleMapToAttribute(styleMap));
						if (w3cElement.getAttribute("style").isEmpty()) {
							w3cElement.removeAttribute("style");
						}
					}
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

			/*
			 * FIXME - dirndl - validate (only 1 innertext/innerhtml, no
			 * duplicate property anns)
			 */
			void bind(boolean leftToRight) {
				resolver.getBindings(directed, model).stream()
						.map(PropertyBinding::new).forEach(binding -> {
							if (leftToRight) {
								binding.setRight();
							} else {
								binding.setLeft();
							}
							binding.bind();
						});
			}

			public void setLeft() {
				bindings.forEach(PropertyBinding::setLeft);
			}

			void unbind() {
				bindings.forEach(PropertyBinding::unbind);
			}
		}

		/*
		 * Use for debugging model/node bindings across layouts
		 */
		public String provideLayoutId() {
			return String.valueOf(DirectedLayout.this.hashCode());
		}

		/**
		 * Use to dispatch a runnable if it may cause layout (but simplifies the
		 * sequencing by non-deferrred execution if layout is not in progress)
		 * 
		 * @param runnable
		 *            the runnable to execute which may cause layout
		 */
		public void deferIfFiring(Runnable runnable) {
			DirectedLayout.this.deferIfFiring(runnable);
		}
	}

	void deferIfFiring(Runnable runnable) {
		if (inLayout) {
			Scheduler.get().scheduleFinally(runnable::run);
		} else {
			runnable.run();
		}
	}

	static class ReceivesEmitsEvents {
		static volatile ReceivesEmitsEvents instance;

		static ClassData get(Class clazz) {
			if (instance == null) {
				synchronized (ReceivesEmitsEvents.class) {
					if (instance == null) {
						instance = new ReceivesEmitsEvents();
					}
				}
			}
			return instance.get0(clazz);
		}

		Map<Class, ClassData> classData;

		Map<Class<? extends NodeEvent.Handler>, Class<? extends NodeEvent>> handlerEvents;

		Map<Class<? extends ModelEvent.Emitter>, Class<? extends ModelEvent>> emitterEvents;

		ReceivesEmitsEvents() {
			classData = CollectionCreators.Bootstrap.createConcurrentClassMap();
			// FIXME - reflection - rebuild on registry change
			handlerEvents = (Map) CollectionCreators.Bootstrap
					.createConcurrentClassMap();
			emitterEvents = (Map) CollectionCreators.Bootstrap
					.createConcurrentClassMap();
			Registry.query(NodeEvent.class).registrations()
					.filter(clazz -> !Reflections
							.isAssignableFrom(DirectlyInvoked.class, clazz))
					.filter(clazz -> Reflections.at(clazz)
							.hasNoArgsConstructor())
					.forEach(eventClass -> {
						ClassReflector<? extends NodeEvent> reflector = Reflections
								.at(eventClass);
						Class<? extends NodeEvent.Handler> handlerClass = reflector
								.hasNoArgsConstructor()
										? reflector.newInstance()
												.getHandlerClass()
										: reflector.getGenericBounds().bounds
												.get(0);
						handlerEvents.put(handlerClass, eventClass);
						if (Reflections.isAssignableFrom(DescendantEvent.class,
								eventClass)) {
							Class<? extends ModelEvent.Emitter> emitterClass = ((DescendantEvent) reflector
									.newInstance()).getEmitterClass();
							emitterEvents.put(emitterClass,
									(Class<? extends ModelEvent>) eventClass);
						}
					});
		}

		ClassData get0(Class clazz) {
			return classData.computeIfAbsent(clazz, ClassData::new);
		}

		class ClassData {
			List<Class<? extends NodeEvent>> receives = new ArrayList<>();

			List<Class<? extends NodeEvent>> emitsDescendant = new ArrayList<>();

			Class<?> clazz;

			ClassData(Class<?> clazz) {
				this.clazz = clazz;
				Reflections.at(clazz).provideAllImplementedInterfaces()
						.<Class<? extends NodeEvent>> map(handlerEvents::get)
						.filter(Objects::nonNull).forEach(receives::add);
				Reflections.at(clazz).provideAllImplementedInterfaces()
						.<Class<? extends NodeEvent>> map(emitterEvents::get)
						.filter(Objects::nonNull).forEach(emitsDescendant::add);
			}
		}
	}

	class RecursionTest {
		private Object model;

		private AnnotationLocation annotationLocation;

		RecursionTest(Node node) {
			this.model = node.model;
			this.annotationLocation = node.annotationLocation;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof RecursionTest) {
				RecursionTest test = (RecursionTest) obj;
				return CommonUtils.equals(test.annotationLocation,
						annotationLocation, test.model, model);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(model, annotationLocation);
		}

		Object model() {
			return model;
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("location", annotationLocation,
					"model", model);
		}
	}

	/**
	 * The output of RendererInputs - for Uis with events the default is a GWT
	 * element
	 *
	 *
	 *
	 */
	public interface Rendered {
		void append(Rendered rendered);

		boolean hasAttributes();

		void appendToRoot();

		<T> T as(Class<T> class1);

		default DomNode asDomNode() {
			return DomNode.from(getNode());
		}

		Element asElement();

		org.w3c.dom.Element asW3cElement();

		int getChildCount();

		int getChildIndex(Rendered after);

		org.w3c.dom.Node getNode();

		void insertAsFirstChild(Rendered rendered);

		void insertChild(Rendered rendered, int i);

		void insertAfter(Rendered rendered, Rendered afterRendered);

		boolean isElement();

		void removeFromParent();
	}

	/**
	 * Instances act as an input and process state token for the
	 * layout/transformation algorithm
	 *
	 * Note that the resolver is modified (if at all) *after* init,
	 * so @DirectedContextResolver applies to children, not the node itself.
	 * This simplifies processing, but makes customisation a little more work in
	 * certain cases - see {@link Choices.Select}
	 *
	 *
	 *
	 *
	 */
	class RendererInput implements Traversable {
		Node before;

		ContextResolver resolver;

		// effectively final
		Object model;

		Property property;

		AnnotationLocation location;

		List<Directed> directeds;

		Node parentNode;

		Node node;

		Node replace;

		/*
		 * preserve previous render (e.g. dom nodes) if possible (when moving
		 * nodes due to a FragmentModel mutation)
		 */
		Rendered rendered;

		boolean removeReplaced = true;

		private RendererInput() {
		}

		void afterRender() {
			// must occur after insertion (onBind expects dom attach)
			node.postRender();
			if (node.hasRendered()) {
				Optional<Rendered> firstAncestorRendered = firstAncestorRendered();
				if (firstAncestorRendered.isPresent()) {
					Rendered container = firstAncestorRendered.get();
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
						container.insertChild(node.rendered, 0);
						// bump insertion point
						insertionPoint.point = Point.AFTER;
						insertionPoint.after = node.rendered;
						break;
					case AFTER:
						int insertAfterIndex = container
								.getChildIndex(insertionPoint.after);
						if (insertAfterIndex < container.getChildCount() - 1) {
							container.insertChild(node.rendered,
									insertAfterIndex + 1);
							// bump insertion point
							insertionPoint.after = node.rendered;
						} else {
							container.append(node.rendered);
							// bump insertion point
							insertionPoint.point = Point.LAST;
						}
						break;
					case LAST:
						container.append(node.rendered);
						break;
					}
					// the node *did* provide a widget, so its children will
					// just insert normally (LAST), no insertionpoint required
					node.insertionPoint = null;
				} else {
					// root can't have a sibling
					Preconditions.checkState(before == null);
					// root - if this is a replace, append to root panel
					if (replace != null) {
						resolver.replaceRoot(node.rendered);
					}
				}
			}
			node.postDomAttach();
			if (directeds.size() > 1) {
				enqueueInput(resolver, model, property, location,
						directeds.subList(1, directeds.size()), node);
			}
		}

		void beforeRender() {
			/*
			 * This event is fired directly (as method calls), not via
			 * bubbling/dispatch
			 */
			LayoutEvents.BeforeRender beforeRender = new LayoutEvents.BeforeRender(
					node, model);
			resolver.onBeforeRender(beforeRender);
			if (model instanceof LayoutEvents.BeforeRender.Handler) {
				((LayoutEvents.BeforeRender.Handler) model)
						.onBeforeRender(beforeRender);
			}
		}

		@Override
		public Iterator children() {
			throw new UnsupportedOperationException();
		}

		void enqueueInput(ContextResolver resolver, Object model,
				Property property, AnnotationLocation location,
				List<Directed> directeds, Node parentNode) {
			DirectedLayout.this.enqueueInput(resolver, model, property,
					location, directeds, parentNode);
		}

		@Override
		public void enter() {
		}

		@Override
		public void exit() {
		}

		Optional<Rendered> firstAncestorRendered() {
			return parentNode == null ? Optional.empty()
					: parentNode.firstSelfOrAncestorRendered(true);
		}

		private Directed firstDirected() {
			return directeds.get(0);
		}

		void init(ContextResolver resolver, Object model, Property property,
				AnnotationLocation location, List<Directed> directeds,
				Node parentNode) {
			/*
			 * compute the new resolver, if any
			 */
			ContextResolver newResolver = null;
			this.property = property;
			if (model != null && model instanceof ContextResolver.Has) {
				newResolver = ((ContextResolver.Has) model)
						.getContextResolver(location);
			}
			if (newResolver == null) {
				DirectedContextResolver resolverAnnotation = location
						.getAnnotation(DirectedContextResolver.class);
				if (resolverAnnotation != null) {
					newResolver = Reflections
							.newInstance(resolverAnnotation.value());
				}
			}
			if (newResolver != null) {
				newResolver.init(resolver, resolver.layout, model);
				resolver = newResolver;
				// legal (modifying the location's resolver)! note that new
				// resolver will have an empty resolution
				// cache
				location.setResolver(resolver);
			}
			this.resolver = resolver;
			/*
			 * Now compute other fields (using the resolver)
			 */
			this.model = resolver.resolveModel(location, model);
			if (this.model != model) {
				if (this.model instanceof Model.ResetDirecteds) {
					location = new AnnotationLocation(this.model.getClass(),
							null, resolver);
				}
			}
			this.location = location;
			this.parentNode = parentNode;
			this.directeds = directeds != null ? directeds
					: location.getAnnotations(Directed.class);
			// generate the node (1-1 with input)
			node = new Node(resolver, parentNode, property, location,
					this.model, this.directeds.size() == 1);
			node.directed = firstDirected();
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
			before = null;
		}

		void render() {
			ProcessObservers.publish(RenderObservable.class,
					() -> new RenderObservable(node));
			if (replace != null) {
				int indexInParentChildren = parentNode.children
						.indexOf(replace);
				if (indexInParentChildren == -1) {
					/*
					 * double modification - bail. Node will be created but
					 * never attached to parent (ditto rendered)
					 */
					return;
				}
				node.insertionPoint = replace.resolveInsertionPoint();
				DirectedLayout.this.insertionPoint = node.insertionPoint;
				if (removeReplaced) {
					replace.remove(false);
				}
				parentNode.children.add(indexInParentChildren, node);
			} else if (before != null) {
				node.insertionPoint = before.resolveInsertionPoint();
				int indexInParentChildren = parentNode.children.indexOf(before);
				DirectedLayout.this.insertionPoint = node.insertionPoint;
				parentNode.children.add(indexInParentChildren, node);
			} else {
				if (parentNode != null) {
					// add fairly late, to ensure we're in insertion order
					parentNode.ensureChildren();
					parentNode.children.add(node);
					// complexities of delegation and child replacement
					if (parentNode.insertionPoint != null) {
						node.insertionPoint = parentNode.insertionPoint;
					}
				}
			}
			beforeRender();
			if (model != null
					|| Reflections.isAssignableFrom(RendersNull.class,
							node.directed.renderer())
					|| node.has(Directed.RenderNull.class)) {
				if (rendered != null) {
					// preserve domNode/model identity. Note relationship with
					// #moveChildren()
					node.rendered = rendered;
				} else {
					DirectedRenderer renderer = resolveRenderer();
					renderer.render(this);
				}
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

		@Override
		public String toString() {
			return Ax.format("Node:\n%s\n\nLocation: %s", node.toParentStack(),
					location.toString());
		}
	}

	static class RendererNotFoundException extends RuntimeException {
		public RendererNotFoundException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * Usage:
	 *
	 * <code>
	 * <pre>
	public class MyClientObservers extends ProcessObserver.AppDebug {
	public MyClientObservers() {j
		ProcessObservers.observe(DirectedLayout.RenderObservable.class, o -> {
			if (o.node.getModel() instanceof MyModel) {
				boolean breakpointHere = true;
			}
		}, true);
	}
	}
	 * </pre>
	 *</code>
	 *
	 *
	 *
	 *
	 */
	public static class RenderObservable implements ProcessObservable {
		public Node node;

		public RenderObservable(Node node) {
			this.node = node;
		}

		@Override
		public String toString() {
			FormatBuilder fb = new FormatBuilder().separator("\n");
			fb.append(node);
			return fb.toString();
		}
	}
}
