package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.process.AlcinaProcess;
import cc.alcina.framework.common.client.process.ProcessContextProvider;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.reflection.ReflectionUtils;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.NestedNameProvider;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * A generalised engine for rule-based transformation.
 *
 * From an initial set of selection seeds (such as urls), set generation g to
 * ordinal zero and iterate the following:
 *
 * <pre>
 *  {
 *      for each selector in generation g
 *          match against selections in generation g.
 *              for each match, produce new selections (in generation >=g).
 *              clear processed selections
 *          if no selections in generation g, increment generation.
 *              exit if no selectors in generation
 *  }
 * </pre>
 *
 * Current naive implementation requires produced selections be in generation
 * g+1 - that may be all that's needed
 *
 * <h2>Notes</h2>
 *
 * <ul>
 * <li>The selection ancestor chain (and assoc process node tree) is *different*
 * to the generation levels - because selectors can assign selections to the
 * current levels (or >1 lower), the ancestor chain is 'how did i get here' -
 * wheras the generation is 'what stage of the process are we at/what selectors
 * can be applied'
 * </ul>
 *
 *
 */
public class SelectionTraversal
		implements ProcessContextProvider, AlcinaProcess {
	public Topic<Selection> selectionAdded = Topic.create();

	public Topic<Selection> selectionProcessed = Topic.create();

	public Topic<SelectionException> selectionException = Topic.create();

	public Topic<Selection> beforeSelectionProcessed = Topic.create();

	private State state = new State();

	Selection rootSelection;

	Map<Generation, GenerationTraversal> generations = new LinkedHashMap<>();

	Map<Selection, Integer> generationIndicies = new ConcurrentHashMap<>();

	Map<Selection, Exception> selectionExceptions = new ConcurrentHashMap<>();

	private Generation currentGeneration;

	private Generation nextGeneration;

	private SelectionFilter filter;

	private Executor executor = new Executor.CurrentThreadExecutor();

	private Selector currentSelector;

	Logger logger = LoggerFactory.getLogger(getClass());

	public SelectionTraversal() {
	}

	public void addSelector(Generation generation, Selector selector) {
		generations.get(generation).selectors.add(selector);
	}

	public GenerationTraversal currentGenerationTraversal() {
		return generations.get(currentGeneration);
	}

	@Override
	public String flatPosition(Node node) {
		FormatBuilder position = new FormatBuilder().separator(" > ");
		List<Node> selectionPath = node.asNodePath();
		Node last = CommonUtils.last(selectionPath);
		Object value = last.getValue();
		List<Generation> list = generations.keySet().stream()
				.collect(Collectors.toList());
		for (int idx = 0; idx < list.size(); idx++) {
			Generation generation = list.get(idx);
			GenerationTraversal generationData = generations.get(generation);
			Set<Selection> selections = generationData.selections;
			if (selections.contains(value)) {
				int level = idx;
				position.format("Generation: [%s/%s]", idx + 1, list.size());
				IntPair pair = new IntPair(generationIndicies.get(value),
						selections.size());
				position.append(pair);
				position.separator(" :: ");
				position.append(generation);
				position.append(last.displayName());
				String positionMessage = position.toString();
				return positionMessage;
			}
		}
		// unsupported?
		return "[Unknown position]";
	}

	public Selector getCurrentSelector() {
		return this.currentSelector;
	}

	public Executor getExecutor() {
		return this.executor;
	}

	public SelectionFilter getFilter() {
		return this.filter;
	}

	public Selection getRootSelection() {
		return this.rootSelection;
	}

	public void logTraversalStats() {
		new StatsLogger(this).execute();
	}

	public GenerationTraversal nextGenerationTraversal() {
		return generations.get(nextGeneration);
	}

	public synchronized String pathSegment(Generation generation,
			Class<? extends Selection> clazz, Object value) {
		GenerationTraversal generationTraversal = generations.get(generation);
		int size = generationTraversal.selectionsByClassValue
				.asMapEnsure(true, clazz, value).size();
		return Ax.format("%s::%s::%s", clazz.getSimpleName(), value.toString(),
				size);
	}

	public <G extends Generation> void populateGenerations(Class<G> clazz) {
		Arrays.stream(clazz.getEnumConstants())
				.forEach(g -> generations.put(g, new GenerationTraversal(g)));
	}

	public SelectionFilter provideExceptionSelectionFilter() {
		Multimap<Generation, List<String>> generationSegments = new Multimap<>();
		selectionExceptions.keySet().forEach(selection -> {
			Selection cursor = selection;
			while (cursor != null) {
				Generation generation = computeSubmittedGeneration(cursor);
				String pathSegment = cursor.getPathSegment();
				Preconditions.checkState(Ax.notBlank(pathSegment));
				generationSegments.add(generation, pathSegment);
				cursor = cursor.parentSelection();
			}
		});
		SelectionFilter filter = new SelectionFilter();
		generationSegments.forEach((k, v) -> {
			String pathSegmentRegex = Ax.format("^(%s)$",
					v.stream().distinct().map(CommonUtils::escapeRegex)
							.collect(Collectors.joining("|")));
			filter.addGenerationFilter(k.toString(), pathSegmentRegex);
		});
		return filter;
	}

	public synchronized void select(Generation generation,
			Selection selection) {
		GenerationTraversal generationTraversal = generations.get(generation);
		generationTraversal.select(selection);
	}

	/*
	 * Add to both new and old (1,2) selection trackers
	 */
	public void select(Selection selection) {
		if (nextGeneration != null) {
			select(nextGeneration, selection);
		} else {
			state.selections.add(selection);
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

	public void setRootSelection(Selection rootSelection) {
		this.rootSelection = rootSelection;
	}

	public void traverse1() {
		Preconditions.checkState(generations.size() > 0);
		GenerationTraversal firstGeneration = generations.values().iterator()
				.next();
		// prime the pump
		select(firstGeneration.generation, rootSelection);
		for (GenerationTraversal generationTraversal : generations.values()) {
			currentGeneration = generationTraversal.generation;
			ProcessObservers.publish(GenerationEntry.class,
					() -> new GenerationEntry());
			nextGeneration = Ax.next(generations.keySet(), currentGeneration);
			// this logic (looping on the current generation until there's a
			// pass with no submitted tasks) allows selectors to add to the
			// current generation (as well as subsequent)
			int selectorPass = 0;
			for (;;) {
				int submitted = 0;
				for (Selector selector : generationTraversal.selectors) {
					int submittedBySelector = 0;
					try {
						this.currentSelector = selector;
						selector.onBeforeTraversal(generationTraversal,
								selectorPass == 0);
						generationTraversal.onBeforeSelectorTraversal(selector);
						for (Selection selection : generationTraversal
								.selectionIterator()) {
							if (generationTraversal.submitted.add(selector,
									selection)) {
								submitted++;
								submittedBySelector++;
								executor.submit(() -> processSelection1(
										generationTraversal, selector,
										selection));
							}
						}
						executor.awaitCompletion();
					} finally {
						selector.onAfterTraversal(generationTraversal,
								submittedBySelector != 0);
					}
				}
				if (submitted == 0) {
					break;
				}
				selectorPass++;
			}
			ProcessObservers.publish(GenerationExit.class,
					() -> new GenerationExit());
		}
	}

	public void traverse2() {
		state.layerTraversal = new DepthFirstTraversal<Layer>(state.rootLayer,
				Layer::getChildren, false);
		/*
		 * layers with sublayers will compute their outputs after sublayer
		 * traversal
		 */
		state.layerTraversal.topicNodeExit.add(Layer::onAfterTraversal);
		/*
		 * @formatter:off
		 *
		 * - get rid of current, next generation
		 * - use layer state
		 * - layer *parser* emits slices, layer *selector* emits selections
		 *
		 * @formatter:on
		 *
		 */
		for (Layer<?> layer : state.layerTraversal) {
			Layer untyped = layer;
			state.currentLayer = layer;
			layer.onBeforeTraversal(state);
			// FIXME
			ProcessObservers.publish(GenerationEntry.class,
					() -> new GenerationEntry());
			for (;;) {
				layer.onBeforeIteration();
				try {
					for (Selection selection : layer) {
						if (untyped.submit(selection)) {
							executor.submit(() -> processSelection(selection));
						}
					}
					executor.awaitCompletion();
				} finally {
					layer.onAfterIteration();
				}
				if (layer.isComplete()) {
					layer.onAfterInputsProcessed();
					break;
				}
			}
		}
		// FIXME
		ProcessObservers.publish(GenerationExit.class,
				() -> new GenerationExit());
	}

	private Generation computeSubmittedGeneration(Selection selection) {
		return generations.values().stream()
				.filter(generation -> generation.wasSubmitted(selection))
				.findFirst().get().generation;
	}

	private void enterSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::enterContext);
	}

	private void exitSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::exitContext);
	}

	private void processSelection(Selection selection) {
		try {
			Layer layer = state.currentLayer;
			enterSelectionContext(selection);
			selection.processNode().select(null, this);
			if (!layer.testFilter(selection)) {
				// skip processing if, for instance, the traversal has hit max
				// exceptions
				return;
			}
			try {
				beforeSelectionProcessed.publish(selection);
				// FIXME - upa - remove 'this'
				((Layer) layer).process(this, selection);
			} catch (Exception e) {
				selectionExceptions.put(selection, e);
				selection.processNode().onException(e);
				selectionException
						.publish(new SelectionException(selection, e));
				logger.warn(Ax.format("Selection exception :: %s", selection),
						e);
			}
		} finally {
			exitSelectionContext(selection);
			selection.processNode().setSelfComplete(true);
			releaseCompletedSelections(selection);
			selectionProcessed.publish(selection);
		}
	}

	private void processSelection1(GenerationTraversal generationTraversal,
			Selector selector, Selection selection) {
		try {
			enterSelectionContext(selection);
			selection.processNode().select(null, this);
			if (!generationTraversal.testFilter(selection)) {
				// skip processing if, for instance, the traversal has hit max
				// exceptions
				return;
			}
			if (selector.handles(selection)) {
				try {
					beforeSelectionProcessed.publish(selection);
					selector.process(this, selection);
				} catch (Exception e) {
					selectionExceptions.put(selection, e);
					selection.processNode().onException(e);
					selectionException
							.publish(new SelectionException(selection, e));
					// TODO blah blah
					e.printStackTrace();
				}
			}
		} finally {
			exitSelectionContext(selection);
			selection.processNode().setSelfComplete(true);
			releaseCompletedSelections(selection);
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

	public interface Context {
	}

	/**
	 * Because the runnables will generally require Alcina contexts, use this
	 * (custom) approach rather than java.util.concurrent.Executor
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public interface Executor {
		void awaitCompletion();

		void submit(Runnable runnable);

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
	}

	/*
     * Here, 'generation' means an evolutionary generation - corresponding to one
     * level of the TreeProcess node tree - e.g:
     *
     * @formatter:off
     *
     * [Process top level web pages :: 0 :: root] ->
     * [Process intermediate web pages :: 1 :: child] ->
     * [Emit journal resource page :: 2 :: grandchild] ->
     * [Emit journal resource :: 3 :: g-grandchild]
     *
     * @formatter:on
     *
     */
	public interface Generation {
	}

	public class GenerationEntry implements ProcessObservable {
		public Generation getGeneration() {
			return currentGeneration;
		}

		@Override
		public String toString() {
			return currentGeneration.getClass().getName() + "::"
					+ currentGeneration.toString();
		}
	}

	public class GenerationExit implements ProcessObservable {
		public Generation getGeneration() {
			return currentGeneration;
		}

		@Override
		public String toString() {
			return currentGeneration.getClass().getName() + "::"
					+ currentGeneration.toString();
		}
	}

	public class GenerationTraversal {
		public Generation generation;

		List<Selector> selectors = new ArrayList<>();

		Set<Selection> selections = new LinkedHashSet<>();

		MultikeyMap<Selection> selectionsByClassValue = new UnsortedMultikeyMap<>(
				3);

		Multimap<Selector, List<Selection>> selectionsBySelector = new Multimap<>();

		Multiset<Selector, Set<Selection>> submitted = new Multiset<>();

		Map<String, Selection> selectionPaths = new LinkedHashMap<>();

		GenerationTraversal(Generation generation) {
			this.generation = generation;
		}

		public PriorGenerationSelections
				getPriorGenerationSelections(boolean includeCurrentGeneration) {
			return new PriorGenerationSelections(
					includeCurrentGeneration ? null : this);
		}

		public Set<Selection> getSelections() {
			return this.selections;
		}

		public SelectionTraversal getSelectionTraversal() {
			return SelectionTraversal.this;
		}

		public boolean hasSelections(Class<? extends Selection> clazz,
				Object value) {
			return selectionsByClassValue.containsKey(clazz, value);
		}

		public void onBeforeSelectorTraversal(Selector selector) {
			selectionsBySelector.getAndEnsure(selector);
		}

		public Stream<Selection> provideEmittedSelections() {
			return selectionsBySelector.allValues().stream();
		}

		public Iterable<Selection> selectionIterator() {
			return currentSelector.selectionIterator(this);
		}

		public boolean wasSubmitted(Selection selection) {
			return submitted.values().stream()
					.anyMatch(set -> set.contains(selection));
		}

		private void select(Selection selection) {
			if (testFilter(selection)) {
				if (checkSelectionPath(selection)) {
					return;
				}
				int index = selections.size();
				selections.add(selection);
				selectionsByClassValue.put(selection.getClass(),
						selection.get(), selection, selection);
				generationIndicies.put(selection, index);
				selectionAdded.publish(selection);
				// stats
				GenerationTraversal currentGenerationTraversal = currentGenerationTraversal();
				if (currentGenerationTraversal != null) {
					currentGenerationTraversal.selectionsBySelector
							.add(currentSelector, selection);
				}
			}
		}

		private boolean testFilter(Selection selection) {
			if (filter == null) {
				return true;
			}
			if (filter.getMaxExceptions() > 0 && selectionExceptions
					.size() >= filter.getMaxExceptions()) {
				return false;
			}
			String generationName = generation.toString();
			if (filter.hasGenerationFilter(generationName)) {
				if (filter.matchesGenerationFilter(generationName,
						selection.getFilterableSegments())) {
					return true;
				} else {
					return false;
				}
			} else {
				int allGenerationsLimit = filter.getAllGenerationsLimit();
				return allGenerationsLimit == 0
						|| allGenerationsLimit > selections.size();
			}
		}

		/**
		 * Selection paths must be unique per generation - the traverser either
		 * throws on a duplicate (default) or ignores (selection-specific
		 * onDuplicatePathSelection override) -- an example of ignoreable
		 * duplication would be if a pathsegment is reachable from multiple
		 * ancestral routes/paths)
		 *
		 * @return Return true if selection should be ignored (duplicate)
		 */
		boolean checkSelectionPath(Selection selection) {
			synchronized (selectionPaths) {
				Selection atPath = selectionPaths
						.get(selection.getPathSegment());
				if (atPath != null) {
					atPath.onDuplicatePathSelection(generation, selection);
					return true;
				} else {
					selectionPaths.put(selection.getPathSegment(), selection);
					return false;
				}
			}
		}
	}

	public class PriorGenerationSelections {
		public List<TypeAndSelections> data = new ArrayList<>();

		public PriorGenerationSelections(GenerationTraversal stop) {
			for (GenerationTraversal traversal : generations.values()) {
				if (traversal == stop) {
					break;
				}
				Multimap<Class<? extends Selection>, List<Selection>> byClass = traversal.selectionsBySelector
						.allValues().stream().collect(AlcinaCollectors
								.toKeyMultimap(Selection::getClass));
				byClass.entrySet().stream().map(e -> {
					TypeAndSelections element = new TypeAndSelections();
					element.generation = traversal.generation;
					element.type = e.getKey();
					element.selections = e.getValue();
					return element;
				}).forEach(data::add);
			}
		}

		public class TypeAndSelections {
			public Generation generation;

			public Class<? extends Selection> type;

			public List<Selection> selections;
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
		private Multiset<Class<? extends Selection>, Set<Selection>> byClass = new Multiset<>();

		private Multiset<Layer, Set<Selection>> byLayer = new Multiset<>();

		public Set<Selection> byLayer(Layer layer) {
			return byLayer.getAndEnsure(layer);
		}

		synchronized boolean add(Selection selection) {
			byLayer.add(state.currentLayer, selection);
			return byClass.add(selection.getClass(), selection);
		}

		synchronized <S extends Selection> List<S>
				get(Class<? extends S> clazz) {
			return byClass.getAndEnsure(clazz).stream().map(clazz::cast)
					.collect(Collectors.toList());
		}
	}

	public class State {
		public Layer<?> currentLayer;

		DepthFirstTraversal<Layer> layerTraversal;

		Layer rootLayer;

		Selections selections = new Selections();

		public void select(Selection selection) {
			SelectionTraversal.this.select(selection);
		}
	}

	public static class StatsLogger {
		private SelectionTraversal selectionTraversal;

		public StatsLogger(SelectionTraversal selectionTraversal) {
			this.selectionTraversal = selectionTraversal;
		}

		void execute() {
			if (selectionTraversal.state.layerTraversal == null) {
				// V1
				List<GenerationEntry> entries = new ArrayList<>();
				entries.add(
						new GenerationEntry(selectionTraversal.rootSelection));
				selectionTraversal.generations.values().forEach(traversal -> {
					entries.add(new GenerationEntry(traversal));
					if (traversal.selectionsBySelector.keySet().size() > 1) {
						traversal.selectionsBySelector.keySet()
								.forEach(selector -> {
									entries.add(new GenerationEntry(traversal,
											selector));
								});
					}
				});
				String log = ReflectionUtils.logBeans(GenerationEntry.class,
						entries);
				Ax.out(log);
			} else {
				DepthFirstTraversal<Layer> debugTraversal = new DepthFirstTraversal<Layer>(
						selectionTraversal.state.rootLayer, Layer::getChildren,
						false);
				List<LayerEntry> entries = debugTraversal.stream()
						.map(LayerEntry::new).collect(Collectors.toList());
				String log = ReflectionUtils.logBeans(LayerEntry.class,
						entries);
				Ax.out(log);
			}
		}

		@PropertyOrder({ "key", "outputs" })
		public static class GenerationEntry {
			private GenerationTraversal traversal;

			private Selector selector;

			private Selection rootSelection;

			public GenerationEntry(GenerationTraversal traversal) {
				this(traversal, null);
			}

			public GenerationEntry(GenerationTraversal traversal,
					Selector selector) {
				this.traversal = traversal;
				this.selector = selector;
			}

			public GenerationEntry(Selection rootSelection) {
				this.rootSelection = rootSelection;
			}

			public Object getKey() {
				if (rootSelection != null) {
					return Ax.format("[%s]",
							NestedNameProvider.get(rootSelection.getClass()));
				} else if (selector == null) {
					return traversal.generation;
				} else {
					return "  " + NestedNameProvider.get(selector.getClass());
				}
			}

			public int getOutputs() {
				if (rootSelection != null) {
					return 1;
				} else if (selector == null) {
					return traversal.selectionsBySelector.itemSize();
				} else {
					return traversal.selectionsBySelector.get(selector).size();
				}
			}
		}

		@PropertyOrder({ "key", "outputs" })
		public class LayerEntry {
			private Layer layer;

			public LayerEntry(Layer layer) {
				this.layer = layer;
			}

			public String getKey() {
				FormatBuilder keyBuilder = new FormatBuilder();
				keyBuilder.indent(layer.depth());
				keyBuilder.append(layer.name);
				return keyBuilder.toString();
			}

			public int getOutputs() {
				return selectionTraversal.state.selections.byLayer(layer)
						.size();
			}
		}
	}
}
