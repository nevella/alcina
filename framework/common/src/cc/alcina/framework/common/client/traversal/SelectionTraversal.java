package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.web.bindery.event.shared.UmbrellaException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.process.AlcinaProcess;
import cc.alcina.framework.common.client.process.ProcessContextProvider;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.reflection.ReflectionUtils;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.SelectionTraversal.State.SelectionLayers.LayerSelections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IdCounter;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * A generalised engine for rule-based transformation.
 *
 * From an initial set of selection seeds (such as urls), and a tree of layers l
 * iterate the following:
 *
 * <pre>
 *  {
 *      traverse tree of layers l
 * 		for each l
 *          for all inputs appropriate to l (computedInputs)
 *              for each input, produce new selections 
 *          if new inputs are generated for a previous layer, repeat for that layer
 *           otherwise continue to the next layer
 *  }
 * </pre>
 * 
 * <h3>Context and service support</h3>
 * <p>
 * An instance exposes a {@link TraversalContext} context object, which can be
 * cast to provide whole-process context support to descendant objects such as
 * layers and selections.
 * <p>
 * The current pattern is to have the TraversalContext instance - essentially a
 * whole-process customiser - implement the service interface provider classes
 * required by the process. As an example:
 * 
 * <pre>
 * <code>
 public class TraversalExample1 {
	public static class MySelection extends AbstractSelection<String> {
		public MySelection(Node parentNode, String value, String pathSegment) {
			super(parentNode, value, pathSegment);
		}
	}

	// must be public, since Peer is
	public static class Layer1 extends Layer<MySelection> {
		public void process(MySelection selection) {
			state.traversalContext(Layer1.Peer.Has.class).getLayer1Peer()
					.checkSelection(selection);
		}

		public static class Peer {
			void checkSelection(MySelection selection) {
				// noop
			}

			public interface Has {
				Peer getLayer1Peer();
			}
		}
	}

	public static class MyTraversalPeerBase
			implements TraversalContext, Layer1.Peer.Has {
		public Layer1.Peer getLayer1Peer() {
			return new Layer1.Peer();
		}
	}

	public static class MyTraversalPeerSpecialisation1
			extends MyTraversalPeerBase {
		public Layer1.Peer getLayer1Peer() {
			return new PeerImpl();
		}

		static class PeerImpl extends Layer1.Peer {
			void checkSelection(MySelection selection) {
				if (selection.toString().contains("bruh")) {
					throw new IllegalArgumentException("No bruh");
				}
			}
		}
	}

	public void run() {
		// create and execute a dummy traversal
		TreeProcess process = new TreeProcess(this);
		SelectionTraversal traversal = new SelectionTraversal(
				new MyTraversalPeerSpecialisation1());
		TreeProcess.Node parentNode = process.getSelectedNode();
		traversal.select(new MySelection(parentNode, "bruh", "root"));
		traversal.traverse();
	}
}

 * </code>
 * </pre>
 *
 * <p>
 * Note that if a process checks for a traversalContext interface, the
 * traversalContext must exist and implement that interface. Use noop default
 * implementations here - it makes coding much easier
 * <p>
 * This design pattern - the process/peer pattern - makes composition much
 * easier, and greatly reduces the use of subclasses in these processes.
 * Subclassed <i>selection</i> types are very useful, but because traversals
 * (and their layer trees) are themselves potentially deep and wide trees,
 * customising via potentially subclassing the creation of each layer at layer
 * tree creation time would be more cumbersome and more brittle
 */
public class SelectionTraversal
		implements ProcessContextProvider, AlcinaProcess {
	public static final String CONTEXT_SELECTION = SelectionTraversal.class
			.getName() + ".CONTEXT_SELECTION";

	public static <S extends Selection> S contextSelection() {
		return LooseContext.get(CONTEXT_SELECTION);
	}

	public static Topic<SelectionTraversal> topicTraversalComplete = Topic
			.create();

	static IdCounter counter = new IdCounter();

	public Topic<Selection> selectionAdded = Topic.create();

	public Topic<Selection> selectionProcessed = Topic.create();

	public Topic<SelectionException> selectionException = Topic.create();

	public Topic<Selection> beforeSelectionProcessed = Topic.create();

	public Topic<Layer> topicBeforeLayerTraversal = Topic.create();

	private State state = new State();

	Map<Selection, Integer> selectionIndicies = new ConcurrentHashMap<>();

	Map<Selection, Exception> selectionExceptions = new ConcurrentHashMap<>();

	private SelectionFilter filter;

	private Executor executor = new Executor.CurrentThreadExecutor();

	Logger logger = LoggerFactory.getLogger(getClass());

	final TraversalContext traversalContext;

	public String id;

	public Object outputContainer;

	public boolean serialExecution = false;

	/*
	 * Set true to release resources once the layer (and potentially children)
	 * are complete
	 */
	public boolean releaseResources = false;

	public SelectionTraversal() {
		this(null);
	}

	public SelectionTraversal(TraversalContext traversalContext) {
		this.traversalContext = traversalContext;
	}

	public Layer<?> currentLayer() {
		return state.currentLayer;
	}

	private void enterSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::enterContext);
	}

	private void exitSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::exitContext);
	}

	@Override
	public String flatPosition(Node node) {
		FormatBuilder position = new FormatBuilder().separator(" > ");
		List<Node> selectionPath = node.asNodePath();
		Node last = CommonUtils.last(selectionPath);
		Selection value = (Selection) last.getValue();
		Layer layer = getLayer(value);
		if (layer != null) {
			position.format("Layer: [%s/%s]", layer.layerPath(),
					layer.root().getChildren().size());
			IntPair pair = state.selections.getSelectionPosition(value);
			position.append(pair);
			position.separator(" :: ");
			position.append(layer);
			position.append(last.pathDisplayName());
			String positionMessage = position.toString();
			return positionMessage;
		}
		// unsupported?
		return "[Root position]";
	}

	public String getDocumentMarkup(boolean input) {
		if (input) {
			Object rootValue = getRootSelection().get();
			return rootValue instanceof HasMarkup
					? ((HasMarkup) rootValue).provideMarkup()
					: null;
		} else {
			return outputContainer instanceof HasMarkup
					? ((HasMarkup) outputContainer).provideMarkup()
					: null;
		}
	}

	public Executor getExecutor() {
		return this.executor;
	}

	public SelectionFilter getFilter() {
		return this.filter;
	}

	public Layer getLayer(Selection selection) {
		if (selection == getRootSelection()) {
			return null;
		} else {
			return state.selections.getLayer(selection);
		}
	}

	public Layer getRootLayer() {
		return state.rootLayer;
	}

	public Selection getRootSelection() {
		return state.selections.rootSelection;
	}

	public <S extends Selection> List<S>
			getSelections(Class<? extends S> clazz) {
		return state.getSelections(clazz);
	}

	public <S extends Selection> List<S> getSelections(Class<? extends S> clazz,
			boolean includeSubclasses) {
		return state.getSelections(clazz, includeSubclasses);
	}

	public Collection<Selection> getSelections(Layer layer) {
		return state.getSelections(layer);
	}

	public <T extends Selection> T getSingleSelection(Class<T> clazz) {
		return state.selections.getSingleSelection(clazz);
	}

	public void logTraversalStats() {
		new StatsLogger(this).execute();
	}

	private void processSelection(Selection selection) {
		try {
			LooseContext.pushWithKey(CONTEXT_SELECTION, selection);
			Layer layer = currentLayer();
			enterSelectionContext(selection);
			if (layer.state.traversalCancelled) {
				return;
			}
			selection.processNode().select(null, this);
			if (!layer.testFilter(selection) || !testLayerFilter(selection)) {
				// skip processing if, for instance, the traversal has hit max
				// exceptions
				return;
			}
			try {
				beforeSelectionProcessed.publish(selection);
				ProcessObservers.publish(BeforeLayerSelection.class,
						() -> new BeforeLayerSelection(layer, selection));
				layer.process(selection);
			} catch (Exception e) {
				selectionExceptions.put(selection, e);
				selection.processNode().onException(e);
				selectionException
						.publish(new SelectionException(selection, e));
				logger.warn(Ax.format("Selection exception :: %s",
						Ax.trimForLogging(selection)), e);
				if (context(TraversalContext.ThrowOnException.class) != null) {
					throw WrappedRuntimeException.wrap(e);
				}
			} finally {
				layer.onAfterProcess(selection);
			}
		} finally {
			exitSelectionContext(selection);
			releaseCompletedSelections(selection);
			state.onSelectionProcessed(selection);
			selectionProcessed.publish(selection);
			LooseContext.pop();
		}
	}

	public SelectionFilter provideExceptionSelectionFilter() {
		SelectionFilter filter = new SelectionFilter();
		Multimap<Class<? extends Selection>, List<String>> selectionTypeSegments = new Multimap<>();
		selectionExceptions.keySet().forEach(selection -> {
			Selection cursor = selection;
			while (cursor != null) {
				String pathSegment = cursor.getPathSegment();
				Preconditions.checkState(Ax.notBlank(pathSegment));
				selectionTypeSegments.add(cursor.getClass(), pathSegment);
				cursor = cursor.parentSelection();
			}
		});
		selectionTypeSegments.forEach((k, v) -> {
			String pathSegmentRegex = Ax.format("^(%s)$",
					v.stream().distinct().map(CommonUtils::escapeRegex)
							.collect(Collectors.joining("|")));
			filter.addLayerFilter(k, pathSegmentRegex);
		});
		return filter;
	}

	/*
	 * Intended for release of DOM references, particularly
	 */
	private void releaseCompletedSelections(Selection selection) {
		selection.processNode().setSelfComplete(true);
		if (!releaseResources) {
			return;
		}
		Selection cursor = selection;
		while (cursor != null) {
			if (cursor.processNode().evaluateReleaseResources()) {
				cursor.releaseResources();
				cursor = cursor.parentSelection();
			} else {
				break;
			}
		}
	}

	public void select(Selection selection) {
		if (!state.selections.add(selection)) {
			/*
			 * this will never be processed, so mark as released
			 */
			selection.processNode().setReleasedResources(true);
		}
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public void setFilter(SelectionFilter filter) {
		if (filter != null) {
			filter.prepareToFilter();
		}
		this.filter = filter;
	}

	public void setRootLayer(Layer rootLayer) {
		state.rootLayer = rootLayer;
	}

	private boolean testLayerFilter(Selection selection) {
		if (filter == null) {
			return true;
		}
		if (filter.maxExceptions > 0
				&& selectionExceptions.size() >= filter.maxExceptions) {
			return false;
		}
		int allLayersLimit = filter.allLayersLimit;
		return allLayersLimit == 0 || allLayersLimit > state.selections.size();
	}

	public void throwExceptions() {
		if (selectionExceptions.size() > 0) {
			throw new UmbrellaException(selectionExceptions.values().stream()
					.collect(AlcinaCollectors.toLinkedHashSet()));
		}
	}

	public void traverse() {
		if (id == null) {
			id = Ax.format("%s.%s", ClientInstance.self() == null ? 0
					: ClientInstance.self().getId(), counter.nextId());
		}
		state.layerTraversal = new DepthFirstTraversal<Layer>(state.rootLayer,
				Layer::getChildren);
		/*
		 * layers with sublayers will compute their outputs after sublayer
		 * traversal
		 *
		 */
		state.layerTraversal.topicBeforeNodeExit.add(Layer::onAfterTraversal);
		for (Layer<?> layer : state.layerTraversal) {
			Layer untyped = layer;
			state.currentLayer = layer;
			state.onBeforeLayerTraversal();
			layer.onBeforeTraversal(state);
			topicBeforeLayerTraversal.publish(layer);
			ProcessObservers.publish(LayerEntry.class, () -> new LayerEntry());
			for (;;) {
				layer.onBeforeIteration();
				try {
					for (Selection selection : layer) {
						if (!state.wasProcessed(selection)
								&& untyped.submit(selection)) {
							executor.submit(() -> processSelection(selection));
						}
					}
					executor.awaitCompletion(serialExecution);
				} catch (RuntimeException e) {
					e.printStackTrace();
					throw e;
				} finally {
					layer.onAfterIteration();
				}
				if (layer.isComplete()) {
					layer.onAfterInputsProcessed();
					break;
				}
			}
			state.onAfterTraversal();
			ProcessObservers.publish(LayerExit.class, () -> new LayerExit());
		}
		state.releaseLastLayerResources();
		topicTraversalComplete.publish(this);
		ProcessObservers.publish(TraversalComplete.class,
				() -> new TraversalComplete());
	}

	/**
	 * Usage: this example uses the selection observer to activate a layer
	 * parser observer when the selection string matches the 'observe-foo'
	 *
	 * <code>
	 * <pre>
	
	class SelectionObserver implements
	ProcessObserver<SelectionTraversal.BeforeLayerSelection> {
	@Override
	public void topicPublished(BeforeLayerSelection message) {
	if(message.layer.getClass()==ParserLayer.class&&message.selection.get().toString().contains("observe-foo")){
		companionLayerParserObserver.active=true;
	}else{
		companionLayerParserObserver.active=false;
	}
	}
	}
	
	 * </pre>
	 *</code>
	 *
	 *
	 *
	 *
	 */
	public static class BeforeLayerSelection implements ProcessObservable {
		public Layer layer;

		public Selection selection;

		public BeforeLayerSelection(Layer layer, Selection selection) {
			this.layer = layer;
			this.selection = selection;
		}
	}

	public interface Context {
	}

	/**
	 * Because the runnables will generally require Alcina contexts, use this
	 * (custom) approach rather than java.util.concurrent.Executor
	 *
	 *
	 *
	 */
	public interface Executor {
		void awaitCompletion(boolean serialExecution);

		void submit(Runnable runnable);

		public static class CurrentThreadExecutor implements Executor {
			private List<Runnable> runnables = new ArrayList<>();

			@Override
			public synchronized void awaitCompletion(boolean serialExecution) {
				List<Runnable> current = runnables;
				runnables = new ArrayList<>();
				for (Runnable runnable : current) {
					runnable.run();
				}
			}

			@Override
			public synchronized void submit(Runnable runnable) {
				runnables.add(runnable);
			}
		}
	}

	public class LayerEntry implements ProcessObservable {
		public Layer getLayer() {
			return currentLayer();
		}

		@Override
		public String toString() {
			return currentLayer().getClass().getName() + "::"
					+ currentLayer().toString();
		}
	}

	public class LayerExit implements ProcessObservable {
		public Layer getLayer() {
			return currentLayer();
		}

		@Override
		public String toString() {
			return currentLayer().getClass().getName() + "::"
					+ currentLayer().toString();
		}
	}

	public static class SelectionException extends Exception {
		public Selection selection;

		public Exception exception;

		public SelectionException(Selection selection, Exception exception) {
			this.selection = selection;
			this.exception = exception;
		}
	}

	public class Selections {
		Selection rootSelection;

		// these selections can be inputs to subsequent, type-specific layers
		// (so are not populated by outputs which are ambiguous for receiving
		// layers)
		Multiset<Class<? extends Selection>, Set<Selection>> byClassInputs = new Multiset<>();

		// all selectioons, by type
		Multiset<Class<? extends Selection>, Set<Selection>> byClass = new Multiset<>();

		Map<Layer, Map<Selection, Integer>> byLayer = AlcinaCollections
				.newLinkedHashMap();

		Map<Selection, Layer> selectionLayer = AlcinaCollections
				.newLinkedHashMap();

		// layer/segment
		MultikeyMap<Selection> byLayerSegments = new UnsortedMultikeyMap<>(2);

		// class/segment
		MultikeyMap<Selection> byClassSegments = new UnsortedMultikeyMap<>(2);

		int size;

		synchronized boolean add(Selection selection) {
			if (!testFilter(selection)) {
				return false;
			}
			if (!checkSelectionPath(selection)) {
				return false;
			}
			if (rootSelection == null) {
				rootSelection = selection;
			}
			Map<Selection, Integer> byCurrentLayer = byLayer(currentLayer());
			byCurrentLayer.put(selection, byCurrentLayer.size());
			byLayerSegments.put(currentLayer(), selection.getPathSegment(),
					selection);
			byClass.add(selection.getClass(), selection);
			selectionLayer.put(selection, currentLayer());
			boolean add = true;
			if (isLayerOnly()) {
			} else {
				add = byClassInputs.add(selection.getClass(), selection);
				byClassSegments.put(selection.getClass(),
						selection.getPathSegment(), selection);
			}
			if (add) {
				size++;
				state.onSelectionAdded(selection);
				selectionAdded.publish(selection);
			}
			return add;
		}

		Layer getLayer(Selection selection) {
			return byLayer.entrySet().stream()
					.filter(e -> e.getValue().containsKey(selection))
					.map(Entry::getKey).findFirst().get();
		}

		public Map<Selection, Integer> byLayer(Layer layer) {
			return byLayer.computeIfAbsent(layer,
					l -> AlcinaCollections.newLinkedHashMap());
		}

		/**
		 * Selection paths must be unique per generation - the traverser either
		 * throws on a duplicate (default) or ignores (selection-specific
		 * onDuplicatePathSelection override) -- an example of ignoreable
		 * duplication would be if a pathsegment is reachable from multiple
		 * ancestral routes/paths)
		 *
		 * @return Return false if selection should be ignored (duplicate)
		 */
		boolean checkSelectionPath(Selection selection) {
			Selection existing = byLayerSegments.get(currentLayer(),
					selection.getPathSegment());
			if (existing != null) {
				existing.onDuplicatePathSelection(currentLayer(), selection);
				return false;
			} else {
				return true;
			}
		}

		/**
		 * Return all selections matching a given type - either an exact class
		 * match to S, or any subtype of S (if includeSubclasses is true)
		 * 
		 * @param <S>
		 *            The filtering {@link Selection} subtype
		 * @param clazz
		 *            The filtering {@link Selection} subtype
		 * @param includeSubclasses
		 *            Whether to return all matching subtypes of S
		 * @return The matching Selection instances
		 */
		public synchronized <S extends Selection> List<S>
				get(Class<? extends S> clazz, boolean includeSubclasses) {
			if (includeSubclasses) {
				return (List<S>) byClass.keySet().stream()
						.filter(selectionClass -> Reflections.at(selectionClass)
								.isAssignableTo(clazz))
						.map(byClass::get).flatMap(Collection::stream)
						.collect(Collectors.toList());
			} else {
				return (List<S>) byClass.getAndEnsure(clazz).stream()
						.collect(Collectors.toList());
			}
		}

		/**
		 * Return all potential input selections (see above for info about input
		 * filtering) matching a given type - either an exact class match to S,
		 * or any subtype of S (if includeSubclasses is true)
		 * 
		 * @param <S>
		 *            The filtering {@link Selection} subtype
		 * @param clazz
		 *            The filtering {@link Selection} subtype
		 * @param includeSubclasses
		 *            Whether to return all matching subtypes of S
		 * @return The matching Selection instances
		 */
		public synchronized <S extends Selection> List<S>
				getInputs(Class<? extends S> clazz, boolean includeSubclasses) {
			if (includeSubclasses) {
				return (List<S>) byClassInputs.keySet().stream()
						.filter(selectionClass -> Reflections.at(selectionClass)
								.isAssignableTo(clazz))
						.map(byClassInputs::get).flatMap(Collection::stream)
						.collect(Collectors.toList());
			} else {
				return (List<S>) byClassInputs.getAndEnsure(clazz).stream()
						.collect(Collectors.toList());
			}
		}

		synchronized IntPair getSelectionPosition(Selection value) {
			Layer layer = selectionLayer.get(value);
			Map<Selection, Integer> layerSelections = byLayer.get(layer);
			Integer layerPosition = layerSelections.get(value);
			if (layerPosition != null) {
				return new IntPair(layerPosition + 1, layerSelections.size());
			}
			return null;
		}

		public <T extends Selection> T getSingleSelection(Class<T> clazz) {
			List<T> selections = getSelections(clazz);
			Preconditions.checkState(selections.size() == 1);
			return selections.get(0);
		}

		boolean isLayerOnly() {
			return currentLayer() == null ? false
					: currentLayer().hasReceivingLayer();
		}

		public int size() {
			return size;
		}

		boolean testFilter(Selection selection) {
			if (filter == null) {
				return true;
			}
			if (currentLayer() == null) {
				return true;
			}
			if (filter.maxExceptions > 0
					&& selectionExceptions.size() >= filter.maxExceptions) {
				return false;
			}
			if (filter.hasSelectionFilter(selection.getClass())) {
				if (filter.matchesSelectionTypeFilter(selection.getClass(),
						selection.getFilterableSegments())) {
					return true;
				} else {
					return false;
				}
			} else {
				int allLayersLimit = filter.allLayersLimit;
				return allLayersLimit == 0 || allLayersLimit > size;
			}
		}
	}

	public <T> T context(Class<T> clazz) {
		return traversalContext != null && Reflections.isAssignableFrom(clazz,
				traversalContext.getClass()) ? (T) traversalContext : null;
	}

	/*
	 * Note that Layer.State is reset each time the layer is processed, so
	 * per-layer state that lasts the entire traversal lifetime is stored here
	 * 
	 * CLEAN - it'd be cleaner to have per-layer persistent state attached to
	 * the layer
	 */
	public class State {
		public Layer<?> currentLayer;

		DepthFirstTraversal<Layer> layerTraversal;

		Layer rootLayer;

		Map<Layer, Integer> visitedLayers = new LinkedHashMap<>();

		public Selections selections = new Selections();

		Map<Class<? extends Selection>, SelectionLayers> layersByInput = new LinkedHashMap<>();

		Map<Layer, SelectionLayers.LayerSelections> selectionsByLayer = new LinkedHashMap<>();

		public <T> T context(Class<T> clazz) {
			return SelectionTraversal.this.context(clazz);
		}

		void releaseLastLayerResources() {
			Layer last = visitedLayers.keySet().stream().reduce(Ax.last())
					.orElse(null);
			selections.byLayer.getOrDefault(last, new LinkedHashMap<>())
					.keySet().forEach(
							SelectionTraversal.this::releaseCompletedSelections);
		}

		public <S extends Selection> List<S>
				getSelections(Class<? extends S> clazz) {
			return getSelections(clazz, false);
		}

		public <S extends Selection> List<S> getSelections(
				Class<? extends S> clazz, boolean includeSubclasses) {
			return selections.get(clazz, includeSubclasses);
		}

		Collection<Selection> getSelections(Layer layer) {
			return selections.byLayer(layer).keySet();
		}

		public SelectionTraversal getTraversal() {
			return SelectionTraversal.this;
		}

		/*
		 * If inputs were created for a previous layer, rewind to that layer
		 */
		void onAfterTraversal() {
			Optional<LayerSelections> firstDirty = selectionsByLayer.values()
					.stream().filter(ls -> ls.dirty).findFirst();
			if (firstDirty.isPresent()) {
				Layer layer = firstDirty.get().layer;
				logger.info("  --> Rewind :: {}", layer.getName());
				layerTraversal.setNext(layer);
			}
		}

		void onBeforeLayerTraversal() {
			Integer index = state.visitedLayers.computeIfAbsent(currentLayer,
					l -> state.visitedLayers.size());
			currentLayer.index = index;
			if (!(currentLayer instanceof InputsFromPreviousSibling)) {
				Class<?> inputType = currentLayer.inputType;
				if (Reflections.isAssignableFrom(EmittedBySoleLayer.class,
						inputType)) {
					return;
				}
				SelectionLayers layerSelections = layersByInput.get(inputType);
				if (layerSelections == null) {
					layersByInput.put(currentLayer.inputType,
							new SelectionLayers());
				}
				layersByInput.get(currentLayer.inputType).add(currentLayer);
				selectionsByLayer.get(currentLayer).dirty = false;
			}
		}

		void onSelectionAdded(Selection selection) {
			if (currentLayer != null) {
				SelectionLayers layerSelections = layersByInput
						.get(selection.getClass());
				if (layerSelections != null) {
					layerSelections.onSelectionAdded(selection);
				}
			}
		}

		synchronized void onSelectionProcessed(Selection selection) {
			LayerSelections layerSelections = selectionsByLayer
					.get(currentLayer);
			if (layerSelections != null) {
				layerSelections.onSelectionProcessed(selection);
			}
		}

		void select(Selection selection) {
			SelectionTraversal.this.select(selection);
		}

		boolean wasProcessed(Selection selection) {
			LayerSelections layerSelections = state.selectionsByLayer
					.get(currentLayer);
			return layerSelections != null
					&& layerSelections.processed.contains(selection);
		}

		/*
		 * handles groups of layers which receive the same input type, tracking
		 * their dirty state (for rewind). If more than one layer receives type
		 * X, all such layers must have a sibling or ancestor/descendant
		 * relationship with other layers receiving type X
		 */
		class SelectionLayers {
			boolean immutableInput;

			Map<Layer, LayerSelections> layers = new LinkedHashMap<>();

			void add(Layer layer) {
				if (layers.containsKey(layer)) {
					return;
				}
				this.immutableInput = Reflections.isAssignableFrom(
						Selection.ImmutableInput.class, layer.inputType);
				if (!immutableInput) {
					Preconditions.checkState(layers.isEmpty() || layers.keySet()
							.stream().anyMatch(l -> layer.parent == l
									|| layer.parent == l.parent));
				}
				LayerSelections layerSelections = new LayerSelections(layer);
				layers.put(layer, layerSelections);
				selectionsByLayer.put(layer, layerSelections);
			}

			void onSelectionAdded(Selection selection) {
				if (immutableInput) {
				} else {
					layers.values().forEach(ls -> ls.dirty = true);
				}
			}

			class LayerSelections {
				Layer layer;

				Set<Selection> processed = AlcinaCollections.newUniqueSet();

				boolean dirty;

				LayerSelections(Layer layer) {
					this.layer = layer;
				}

				void onSelectionProcessed(Selection selection) {
					processed.add(selection);
				}
			}
		}
	}

	public static class StatsLogger {
		private SelectionTraversal selectionTraversal;

		public StatsLogger(SelectionTraversal selectionTraversal) {
			this.selectionTraversal = selectionTraversal;
		}

		void execute() {
			DepthFirstTraversal<Layer> debugTraversal = new DepthFirstTraversal<Layer>(
					selectionTraversal.state.rootLayer, Layer::getChildren,
					false);
			List<LayerEntry> entries = debugTraversal.stream()
					.map(LayerEntry::new).collect(Collectors.toList());
			String log = ReflectionUtils.logBeans(LayerEntry.class, entries);
			Ax.out(log);
		}

		public class LayerEntry extends Bindable.Fields {
			private Layer layer;

			String key;

			String outputs;

			public LayerEntry(Layer layer) {
				this.layer = layer;
				FormatBuilder keyBuilder = new FormatBuilder();
				keyBuilder.indent(layer.depth());
				keyBuilder.append(layer.getName());
				key = keyBuilder.toString();
				outputs = computeOutputs();
			}

			String computeOutputs() {
				int size = selectionTraversal.state.selections.byLayer(layer)
						.size();
				if (size != 0) {
					return String.valueOf(size);
				}
				Layer firstLeaf = layer.firstLeaf();
				int firstLeafSize = selectionTraversal.state.selections
						.byLayer(firstLeaf).size();
				if (firstLeafSize != 0) {
					return "-";
				} else {
					return "0";
				}
			}
		}
	}

	public class TraversalComplete implements ProcessObservable {
	}

	public Stream<Selection> getAllSelections() {
		return state.selections.selectionLayer.keySet().stream();
	}

	public List<Layer> getVisitedLayers() {
		return state.visitedLayers.keySet().stream()
				.collect(Collectors.toList());
	}
}
