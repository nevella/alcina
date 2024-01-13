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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.web.bindery.event.shared.UmbrellaException;

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
 *          if new inputs are generated for a previous layer, rewind to that layer
 *           otherwise continue to the next layer
 *  }
 * </pre>
 * 
 * <p>
 * An instance also exposes a traversal context, which can be cast to provide
 * whole-process context support by descendant objects such as layers
 *
 */
public class SelectionTraversal
		implements ProcessContextProvider, AlcinaProcess {
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

	public static Topic<SelectionTraversal> topicTraversalComplete = Topic
			.create();

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
			public synchronized void awaitCompletion() {
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

		void awaitCompletion();

		void submit(Runnable runnable);
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

		Multiset<Class<? extends Selection>, Set<Selection>> byClass = new Multiset<>();

		Map<Layer, Map<Selection, Integer>> byLayer = AlcinaCollections
				.newLinkedHashMap();

		// layer/segment
		MultikeyMap<Selection> byLayerSegments = new UnsortedMultikeyMap<>(2);

		// class/segment
		MultikeyMap<Selection> byClassSegments = new UnsortedMultikeyMap<>(2);

		int size;

		public Map<Selection, Integer> byLayer(Layer layer) {
			return byLayer.computeIfAbsent(layer,
					l -> AlcinaCollections.newLinkedHashMap());
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

		boolean isLayerOnly() {
			return currentLayer() == null ? false
					: currentLayer().hasReceivingLayer();
		}

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
			boolean add = true;
			if (isLayerOnly()) {
			} else {
				add = byClass.add(selection.getClass(), selection);
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

		synchronized IntPair getSelectionPosition(Selection value) {
			for (Entry<Layer, Map<Selection, Integer>> entry : byLayer
					.entrySet()) {
				Map<Selection, Integer> layerSelections = entry.getValue();
				if (layerSelections.containsKey(value)) {
					return new IntPair(layerSelections.get(value) + 1,
							layerSelections.size());
				}
			}
			return null;
		}

		public <T extends Selection> T getSingleSelection(Class<T> clazz) {
			List<T> selections = getSelections(clazz);
			Preconditions.checkState(selections.size() == 1);
			return selections.get(0);
		}
	}

	public Layer getRootLayer() {
		return state.rootLayer;
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

		public <T> T typedContext(Class<T> clazz) {
			return (T) traversalContext;
		}

		public Selections selections = new Selections();

		public Layer findLayerHandlingInput(Selection value) {
			return rootLayer.findHandlingLayer(value.getClass());
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

		public SelectionTraversal getTraversal() {
			return SelectionTraversal.this;
		}

		public <S extends Selection> List<S>
				getSelections(Class<? extends S> clazz) {
			return getSelections(clazz, false);
		}

		public <S extends Selection> List<S> getSelections(
				Class<? extends S> clazz, boolean includeSubclasses) {
			return selections.get(clazz, includeSubclasses);
		}

		Map<Class<? extends Selection>, SelectionLayers> layersByInput = new LinkedHashMap<>();

		Map<Layer, SelectionLayers.LayerSelections> selectionsByLayer = new LinkedHashMap<>();

		void onBeforeLayerTraversal() {
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

		/*
		 * handles groups of layers which receive the same input type, tracking
		 * their dirty state (for rewind)
		 */
		class SelectionLayers {
			void add(Layer layer) {
				if (layers.containsKey(layer)) {
					return;
				}
				Preconditions.checkState(layers.isEmpty() || layers.keySet()
						.stream().anyMatch(l -> layer.parent == l
								|| layer.parent == l.parent));
				LayerSelections layerSelections = new LayerSelections(layer);
				layers.put(layer, layerSelections);
				selectionsByLayer.put(layer, layerSelections);
			}

			void onSelectionAdded(Selection selection) {
				layers.values().forEach(ls -> ls.dirty = true);
			}

			Map<Layer, LayerSelections> layers = new LinkedHashMap<>();

			class LayerSelections {
				LayerSelections(Layer layer) {
					this.layer = layer;
				}

				void onSelectionProcessed(Selection selection) {
					processed.add(selection);
				}

				Layer layer;

				Set<Selection> processed = AlcinaCollections.newUniqueSet();

				boolean dirty;
			}
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

		boolean wasProcessed(Selection selection) {
			LayerSelections layerSelections = state.selectionsByLayer
					.get(currentLayer);
			return layerSelections != null
					&& layerSelections.processed.contains(selection);
		}

		Collection<Selection> getSelections(Layer layer) {
			return selections.byLayer(layer).keySet();
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

	public Topic<Selection> selectionAdded = Topic.create();

	public Topic<Selection> selectionProcessed = Topic.create();

	public Topic<SelectionException> selectionException = Topic.create();

	public Topic<Selection> beforeSelectionProcessed = Topic.create();

	private State state = new State();

	Map<Selection, Integer> selectionIndicies = new ConcurrentHashMap<>();

	Map<Selection, Exception> selectionExceptions = new ConcurrentHashMap<>();

	private SelectionFilter filter;

	private Executor executor = new Executor.CurrentThreadExecutor();

	Logger logger = LoggerFactory.getLogger(getClass());

	public final TraversalContext traversalContext;

	public SelectionTraversal() {
		this(null);
	}

	public SelectionTraversal(TraversalContext traversalContext) {
		this.traversalContext = traversalContext;
	}

	@Override
	public String flatPosition(Node node) {
		FormatBuilder position = new FormatBuilder().separator(" > ");
		List<Node> selectionPath = node.asNodePath();
		Node last = CommonUtils.last(selectionPath);
		Selection value = (Selection) last.getValue();
		{
			Layer layer = state.findLayerHandlingInput(value);
			if (layer != null) {
				position.format("Layer: [%s/%s]", layer.layerPath(),
						layer.root().getChildren().size());
				IntPair pair = state.selections.getSelectionPosition(value);
				position.append(pair);
				position.separator(" :: ");
				position.append(layer);
				position.append(last.displayName());
				String positionMessage = position.toString();
				return positionMessage;
			}
		}
		// unsupported?
		return "[Unknown position]";
	}

	public Executor getExecutor() {
		return this.executor;
	}

	public SelectionFilter getFilter() {
		return this.filter;
	}

	public Selection getRootSelection() {
		return state.selections.rootSelection;
	}

	public <S extends Selection> List<S>
			getSelections(Class<? extends S> clazz) {
		return state.getSelections(clazz);
	}

	public Collection<Selection> getSelections(Layer layer) {
		return state.getSelections(layer);
	}

	public <S extends Selection> List<S> getSelections(Class<? extends S> clazz,
			boolean includeSubclasses) {
		return state.getSelections(clazz, includeSubclasses);
	}

	public void logTraversalStats() {
		new StatsLogger(this).execute();
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

	public void select(Selection selection) {
		state.selections.add(selection);
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

	public void throwExceptions() {
		if (selectionExceptions.size() > 0) {
			throw new UmbrellaException(selectionExceptions.values().stream()
					.collect(AlcinaCollectors.toLinkedHashSet()));
		}
	}

	public String id;

	static IdCounter counter = new IdCounter();

	public void traverse() {
		id = Ax.format("%s.%s", ClientInstance.self() == null ? 0
				: ClientInstance.self().getId(), counter.nextId());
		state.layerTraversal = new DepthFirstTraversal<Layer>(state.rootLayer,
				Layer::getChildren);
		/*
		 * layers with sublayers will compute their outputs after sublayer
		 * traversal
		 *
		 */
		state.layerTraversal.topicNodeExit.add(Layer::onAfterTraversal);
		for (Layer<?> layer : state.layerTraversal) {
			Layer untyped = layer;
			state.currentLayer = layer;
			state.onBeforeLayerTraversal();
			layer.onBeforeTraversal(state);
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
					executor.awaitCompletion();
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
		topicTraversalComplete.publish(this);
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

	private void processSelection(Selection selection) {
		try {
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
				layer.process(selection);
			} catch (Exception e) {
				selectionExceptions.put(selection, e);
				selection.processNode().onException(e);
				selectionException
						.publish(new SelectionException(selection, e));
				logger.warn(Ax.format("Selection exception :: %s",
						Ax.trimForLogging(selection)), e);
			} finally {
				layer.onAfterProcess(selection);
			}
		} finally {
			exitSelectionContext(selection);
			selection.processNode().setSelfComplete(true);
			releaseCompletedSelections(selection);
			state.onSelectionProcessed(selection);
			selectionProcessed.publish(selection);
		}
	}

	/*
	 * Intended for release of DOM references, particularly
	 */
	private void releaseCompletedSelections(Selection selection) {
		selection.processNode().setSelfComplete(true);
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

	public Object outputContainer;

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

	public <T extends Selection> T getSingleSelection(Class<T> clazz) {
		return state.selections.getSingleSelection(clazz);
	}
}
