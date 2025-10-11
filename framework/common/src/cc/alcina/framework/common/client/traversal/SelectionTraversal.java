package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.process.AlcinaProcess;
import cc.alcina.framework.common.client.process.ContextObservers;
import cc.alcina.framework.common.client.process.ProcessContextProvider;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.reflection.ReflectionUtils;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.Selection.DuplicateSelectionException;
import cc.alcina.framework.common.client.traversal.SelectionTraversal.State.SelectionLayers.LayerSelections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IdCounter;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.MultikeyMap;
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
	/**
	 * An exception suppressed [to keep the traversal flow/logs clean], but
	 * required for end-of-traversal exception wrangling
	 */
	public static class SuppressedException
			implements ContextObservers.Observable {
		Exception e;

		public SuppressedException(Exception e) {
			this.e = e;
		}
	}

	/**
	 * A non-exception 'short-circuit traversal' message
	 */
	public static class ExitTraversal implements ContextObservers.Observable {
		String reason;

		public ExitTraversal(String reason) {
			this.reason = reason;
		}
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

		void awaitCompletion(boolean serialExecution);

		void submit(Runnable runnable);
	}

	public class LayerEntry implements ProcessObservable {
		public Layer getLayer() {
			return layers().getCurrent();
		}

		@Override
		public String toString() {
			return layers().getCurrent().getClass().getName() + "::"
					+ layers().getCurrent().toString();
		}
	}

	public class LayerExit implements ProcessObservable {
		public Layer getLayer() {
			return layers().getCurrent();
		}

		@Override
		public String toString() {
			return layers().getCurrent().getClass().getName() + "::"
					+ layers().getCurrent().toString();
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
		public Topic<Selection> topicAdded = Topic.create();

		public Topic<Selection> topicProcessed = Topic.create();

		public Topic<SelectionException> topicException = Topic.create();

		public Topic<Selection> topicBeforeProcessed = Topic.create();

		public Selection root;

		public Map<Selection, Exception> exceptions = new ConcurrentHashMap<>();

		public boolean hasExceptions() {
			return exceptions.size() > 0;
		}

		/*
		 * These selections are available as inputs to subsequent, type-specific
		 * layers. So, the multiset is not populated by outputs which are
		 * ambiguous (could be routed to multiple) receiving layers
		 */
		Multiset<Class<? extends Selection>, Set<Selection>> byClassInputs = new Multiset<>();

		// all selectioons, by type
		Multiset<Class<? extends Selection>, Set<Selection>> byClass = new Multiset<>();

		Map<Layer, Map<Selection, Integer>> byLayerCounts = AlcinaCollections
				.newLinkedHashMap();

		Map<Selection, Layer> selectionLayer = AlcinaCollections
				.newLinkedHashMap();

		// layer/segment
		MultikeyMap<Selection> byLayerSegments = new UnsortedMultikeyMap<>(2);

		// class/segment
		MultikeyMap<Selection> byClassSegments = new UnsortedMultikeyMap<>(2);

		int size;

		public Stream<Selection> allSelections() {
			return selectionLayer.keySet().stream();
		}

		/*
		 * A selection may be selected in multiple layers, in which case it
		 * would only appear once in 'getAllSelections'
		 */
		public Stream<Selection> allLayerSelections() {
			return byLayerCounts.values().stream()
					.flatMap(m -> m.keySet().stream());
		}

		public Map<Selection, Integer> byLayerCounts(Layer layer) {
			return byLayerCounts.computeIfAbsent(layer,
					l -> AlcinaCollections.newLinkedHashMap());
		}

		/**
		 * Return all selections matching exactly the given type
		 * 
		 * @param <S>
		 *            The filtering {@link Selection} subtype
		 * @param clazz
		 *            The filtering {@link Selection} subtype
		 * @return The matching Selection instances
		 */
		public synchronized <S extends Selection> List<S>
				get(Class<? extends S> clazz) {
			return get(clazz, false);
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
		synchronized <S extends Selection> List<S>
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

		public <T extends Selection> T getSingleSelection(Class<T> clazz) {
			List<T> selections = get(clazz, false);
			Preconditions.checkState(selections.size() == 1);
			return selections.get(0);
		}

		public <T extends Selection> boolean has(Class<T> clazz) {
			List<T> selections = get(clazz, false);
			return selections.size() > 0;
		}

		public int size() {
			return size;
		}

		synchronized boolean add(Selection selection) {
			if (!testFilter(selection)) {
				return false;
			}
			if (!checkSelectionPath(selection)) {
				return false;
			}
			if (root == null) {
				root = selection;
			}
			Map<Selection, Integer> byCurrentLayer = byLayerCounts(
					layers().getCurrent());
			byCurrentLayer.put(selection, byCurrentLayer.size());
			byLayerSegments.put(layers().getCurrent(),
					selection.getPathSegment(), selection);
			byClass.add(selection.getClass(), selection);
			selectionLayer.put(selection, layers().getCurrent());
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
			}
			return add;
		}

		Layer getLayer(Selection selection) {
			return byLayerCounts.entrySet().stream()
					.filter(e -> e.getValue().containsKey(selection))
					.map(Entry::getKey).findFirst().get();
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
			Selection existing = byLayerSegments.get(layers().getCurrent(),
					selection.getPathSegment());
			if (existing != null) {
				existing.onDuplicatePathSelection(layers().getCurrent(),
						selection);
				return false;
			} else {
				return true;
			}
		}

		synchronized IntPair getSelectionPosition(Selection value) {
			Layer layer = selectionLayer.get(value);
			Map<Selection, Integer> layerSelections = byLayerCounts.get(layer);
			Integer layerPosition = layerSelections.get(value);
			if (layerPosition != null) {
				return new IntPair(layerPosition + 1, layerSelections.size());
			}
			return null;
		}

		boolean isLayerOnly() {
			return layers().getCurrent() == null ? false
					: layers().getCurrent().hasReceivingLayer();
		}

		boolean testFilter(Selection selection) {
			if (filter == null) {
				return true;
			}
			if (layers().getCurrent() == null) {
				return true;
			}
			if (filter.maxExceptions > 0
					&& selections().exceptions.size() >= filter.maxExceptions) {
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

		public Collection<Selection> byLayer(Layer layer) {
			return byLayerCounts(layer).keySet();
		}

		void addException(Selection selection, Exception exception) {
			exceptions.put(selection, exception);
		}
	}

	/*
	 * Note that Layer.State is reset each time the layer is processed, so
	 * per-layer state that lasts the entire traversal lifetime is stored here
	 * 
	 * CLEAN - it'd be cleaner to have per-layer persistent state attached to
	 * the layer
	 */
	public class State {
		/*
		 * handles groups of layers which receive the same input type, tracking
		 * their dirty state (for rewind). If more than one layer receives type
		 * X, all such layers must have a sibling or ancestor/descendant
		 * relationship with other layers receiving type X
		 */
		class SelectionLayers {
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

			boolean immutableInput;

			Map<Layer, LayerSelections> layers = new LinkedHashMap<>();

			void add(Layer layer) {
				if (layers.containsKey(layer)) {
					return;
				}
				/*
				 * Note that normally if a selection could cause a loop, it
				 * *should if possible* implement Selection.ImmutableInput and
				 * emit a replacement selection of the same type - unbounded
				 * loops are discouraged (but sometimes the only way)
				 */
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
		}

		public Layer<?> currentLayer;

		DepthFirstTraversal<Layer> layerTraversal;

		Layer rootLayer;

		Map<Layer, Integer> layerIndex = new LinkedHashMap<>();

		Map<Integer, Layer> indexLayer = new LinkedHashMap<>();

		public Selections selections = new Selections();

		Map<Class<? extends Selection>, SelectionLayers> layersByInput = new LinkedHashMap<>();

		Map<Layer, SelectionLayers.LayerSelections> selectionsByLayer = new LinkedHashMap<>();

		ExitTraversal exitTraversal;

		public <T> T context(Class<T> clazz) {
			return SelectionTraversal.this.context(clazz);
		}

		public SelectionTraversal getTraversal() {
			return SelectionTraversal.this;
		}

		void releaseLastLayerResources() {
			Layer last = layerIndex.keySet().stream().reduce(Ax.last())
					.orElse(null);
			selections.byLayerCounts.getOrDefault(last, new LinkedHashMap<>())
					.keySet().forEach(
							SelectionTraversal.this::releaseCompletedSelections);
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
			Integer index = state.layerIndex.computeIfAbsent(currentLayer,
					l -> state.layerIndex.size());
			state.indexLayer.put(index, currentLayer);
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
			selections.topicAdded.publish(selection);
		}

		synchronized void onSelectionProcessed(Selection selection) {
			LayerSelections layerSelections = selectionsByLayer
					.get(currentLayer);
			if (layerSelections != null) {
				layerSelections.onSelectionProcessed(selection);
			}
			selections.topicProcessed.publish(selection);
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
	}

	public static class StatsLogger {
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
				int size = selectionTraversal.state.selections
						.byLayerCounts(layer).size();
				if (size != 0) {
					return String.valueOf(size);
				}
				Layer firstLeaf = layer.firstLeaf();
				int firstLeafSize = selectionTraversal.state.selections
						.byLayerCounts(firstLeaf).size();
				if (firstLeafSize != 0) {
					return "-";
				} else {
					return "0";
				}
			}
		}

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
	}

	public class TraversalComplete implements ProcessObservable {
	}

	/*
	 * publishes an exception, but does not break code flow
	 */
	class SuppressedExceptionObserver
			implements ProcessObserver<SuppressedException> {
		int observed = 0;

		@Override
		public void topicPublished(SuppressedException message) {
			if (observed++ < 100) {
				/*
				 * Duplicate selections are quite possibly an issue with
				 * external data rather than traversal logic - so don't
				 * interrupt traversal
				 */
				Ax.simpleExceptionOut(message.e);
			}
			publishException(message.e);
		}
	}

	class ExitTraversalObserver implements ProcessObserver<ExitTraversal> {
		@Override
		public void topicPublished(ExitTraversal message) {
			SelectionTraversal.this.state.exitTraversal = message;
		}
	}

	static LooseContext.Key<SelectionTraversal> CONTEXT_TRAVERSAL = LooseContext
			.key(SelectionTraversal.class, "CONTEXT_TRAVERSAL");

	static LooseContext.Key<Selection> CONTEXT_SELECTION = LooseContext
			.key(SelectionTraversal.class, "CONTEXT_SELECTION");

	public static Topic<SelectionTraversal> topicTraversalComplete = Topic
			.create();

	/*
	 * Each traversal in the VM lifetime is assigned a unique id
	 */
	static IdCounter counter = new IdCounter();

	public static <S extends Selection> S contextSelection() {
		return (S) CONTEXT_SELECTION.getTyped();
	}

	public static SelectionTraversal contextTraversal() {
		return CONTEXT_TRAVERSAL.getTyped();
	}

	public Topic<Layer> topicBeforeLayerTraversal = Topic.create();

	State state = new State();

	Map<Selection, Integer> selectionIndicies = new ConcurrentHashMap<>();

	SelectionFilter filter;

	Executor executor = new Executor.CurrentThreadExecutor();

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

	/**
	 * API grouping sugar class - provides access to layer operations
	 */
	public class Layers {
		public Layer<?> getCurrent() {
			return state.currentLayer;
		}

		public Layer getRoot() {
			return state.rootLayer;
		}

		public Layer get(Selection selection) {
			if (selection == selections().root) {
				return null;
			} else {
				return state.selections.getLayer(selection);
			}
		}

		public Layer get(int selectedLayerIndex) {
			return getVisited().stream()
					.filter(layer -> layer.index == selectedLayerIndex)
					.findFirst().orElse(null);
		}

		public void setRoot(Layer rootLayer) {
			state.rootLayer = rootLayer;
		}

		public List<Layer> getVisited() {
			return state.layerIndex.keySet().stream()
					.collect(Collectors.toList());
		}

		/**
		 * Better - if in a layer, just use {@link Layer#layerContext(Class)}
		 * 
		 * @param <T>
		 * @param clazz
		 * @return
		 */
		public <T extends Layer> T visitedLayer(Class<T> clazz) {
			return (T) state.layerIndex.keySet().stream()
					.filter(layer -> layer.getClass() == clazz).findFirst()
					.get();
		}

		Layer getPrecedingLayer(Layer relativeTo) {
			int idx = state.layerIndex.get(relativeTo);
			return idx > 0 ? state.indexLayer.get(idx - 1) : null;
		}
	}

	Layers layers = new Layers();

	public Layers layers() {
		return layers;
	}

	@Override
	public String flatPosition(Node node) {
		FormatBuilder position = new FormatBuilder().separator(" > ");
		List<Node> selectionPath = node.asNodePath();
		Node last = CommonUtils.last(selectionPath);
		Selection value = (Selection) last.getValue();
		// Layer layer = layers().get(value);
		/*
		 * a little counterintuitively, emit the current layer (using the
		 * selection as an input), not the layer the selection was generated in
		 */
		Layer layer = layers().getCurrent();
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

	public Executor getExecutor() {
		return this.executor;
	}

	public SelectionFilter getFilter() {
		return this.filter;
	}

	public Selections selections() {
		return state.selections;
	}

	public void logTraversalStats() {
		new StatsLogger(this).execute();
	}

	public SelectionFilter provideExceptionSelectionFilter() {
		List<Selection> exceptionKeys = selections().exceptions.keySet()
				.stream().toList();
		int skip = Math.max(0, exceptionKeys.size() - 10);
		List<Selection> last5 = exceptionKeys.stream().skip(skip).toList();
		return SelectionFilter.ofSelections(last5);
	}

	public void select(Selection selection) {
		boolean added = false;
		try {
			added = state.selections.add(selection);
		} catch (DuplicateSelectionException e) {
			new SuppressedException(e).publish();
		}
		/*
		 * this will never be processed, so mark as released
		 */
		selection.processNode().setReleasedResources(true);
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

	public void throwExceptions() {
		if (selections().exceptions.size() > 0) {
			throw new UmbrellaException(selections().exceptions.values()
					.stream().collect(AlcinaCollectors.toLinkedHashSet()));
		}
	}

	public void traverse() {
		try {
			LooseContext.push();
			CONTEXT_TRAVERSAL.set(this);
			new SuppressedExceptionObserver().bind();
			new ExitTraversalObserver().bind();
			traverse0();
		} catch (Throwable t) {
			t.printStackTrace();
			// very unexpected
			throw t;
		} finally {
			LooseContext.pop();
		}
	}

	public <T> T context(Class<T> clazz) {
		return traversalContext != null && Reflections.isAssignableFrom(clazz,
				traversalContext.getClass()) ? (T) traversalContext : null;
	}

	void traverse0() {
		if (id == null) {
			id = Ax.format("%s.%s",
					ClientInstance.current() == null ? 0
							: ClientInstance.current().getId(),
					counter.nextId());
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
				try {
					layer.onBeforeIteration();
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
				if (layer.isComplete() || isExit()) {
					layer.onAfterInputsProcessed();
					break;
				}
			}
			state.onAfterTraversal();
			ProcessObservers.publish(LayerExit.class, () -> new LayerExit());
			state.releaseLastLayerResources();
			if (isExit()) {
				break;
			}
		}
		topicTraversalComplete.publish(this);
		ProcessObservers.publish(TraversalComplete.class,
				() -> new TraversalComplete());
	}

	boolean isExit() {
		return state.exitTraversal != null;
	}

	void enterSelectionContext(Selection<?> selection) {
		Iterator<Selection> itr = selection.ancestorIterator();
		while (itr.hasNext()) {
			itr.next().enterContext();
		}
	}

	void exitSelectionContext(Selection<?> selection) {
		Iterator<Selection> itr = selection.ancestorIterator();
		while (itr.hasNext()) {
			itr.next().exitContext();
		}
	}

	void publishException(Exception e) {
		Selection selection = CONTEXT_SELECTION.getTyped();
		selections().addException(selection, e);
		selection.processNode().onException(e);
		selections().topicException
				.publish(new SelectionException(selection, e));
		if (context(TraversalContext.ThrowOnException.class) != null) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	void processSelection(Selection selection) {
		try {
			LooseContext.push();
			CONTEXT_SELECTION.set(selection);
			Layer layer = layers().getCurrent();
			enterSelectionContext(selection);
			selection.processNode().select(null, this);
			if (!layer.testFilter(selection) || !testLayerFilter(selection)) {
				// skip processing if, for instance, the traversal has hit max
				// exceptions
				return;
			}
			try {
				selections().topicBeforeProcessed.publish(selection);
				ProcessObservers.publish(BeforeLayerSelection.class,
						() -> new BeforeLayerSelection(layer, selection));
				layer.process(selection);
			} catch (Exception e) {
				logger.warn(Ax.format("Selection exception :: %s",
						Ax.trimForLogging(selection)), e);
				publishException(e);
			} finally {
				layer.onAfterProcess(selection);
			}
		} finally {
			try {
				exitSelectionContext(selection);
				releaseCompletedSelections(selection);
				state.onSelectionProcessed(selection);
			} catch (Throwable e) {
				logger.warn("DEVEX-0 :: selection cleanup", e);
				throw e;
			} finally {
				LooseContext.pop();
			}
		}
	}

	/*
	 * Intended for release of DOM references, particularly
	 */
	void releaseCompletedSelections(Selection selection) {
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

	boolean testLayerFilter(Selection selection) {
		if (filter == null) {
			return true;
		}
		if (filter.maxExceptions > 0
				&& selections().exceptions.size() >= filter.maxExceptions) {
			return false;
		}
		int allLayersLimit = filter.allLayersLimit;
		return allLayersLimit == 0 || allLayersLimit > state.selections.size();
	}
}
