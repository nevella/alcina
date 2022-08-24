package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.log.TreeProcess.Node;
import cc.alcina.framework.common.client.log.TreeProcess.ProcessContextProvider;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.reflection.ReflectionUtils;
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
public class SelectionTraversal implements ProcessContextProvider {
	public Topic<Selection> selectionAdded = Topic.create()
			.withThrowExceptions();

	public Topic<Selection> selectionProcessed = Topic.create()
			.withThrowExceptions();

	public Topic<Selection> beforeSelectionProcessed = Topic.create()
			.withThrowExceptions();

	Selection rootSelection;

	Map<Generation, GenerationTraversal> generations = new LinkedHashMap<>();

	Map<Selection, Integer> generationIndicies = new ConcurrentHashMap<>();

	Map<Selection, Exception> selectionExceptions = new ConcurrentHashMap<>();

	private Generation currentGeneration;

	private Generation nextGeneration;

	private SelectionFilter filter;

	private Executor executor = new Executor.CurrentThreadExecutor();

	private Selector currentSelector;

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

	public void select(Selection selection) {
		select(nextGeneration, selection);
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

	public void setRootSelection(Selection rootSelection) {
		this.rootSelection = rootSelection;
	}

	public void traverse() {
		Preconditions.checkState(generations.size() > 0);
		GenerationTraversal firstGeneration = generations.values().iterator()
				.next();
		// prime the pump
		select(firstGeneration.generation, rootSelection);
		for (GenerationTraversal generationTraversal : generations.values()) {
			currentGeneration = generationTraversal.generation;
			nextGeneration = Ax.next(generations.keySet(), currentGeneration);
			// this logic (looping on the current generation until there's a
			// pass with no submitted tasks) allows selectors to add to the
			// current generation (as well as subsequent)
			for (;;) {
				int submitted = 0;
				for (Selector selector : generationTraversal.selectors) {
					try {
						this.currentSelector = selector;
						selector.beforeTraversal(generationTraversal);
						generationTraversal.beforeSelectorTraversal(selector);
						for (Selection selection : generationTraversal
								.selectionIterator()) {
							if (generationTraversal.submitted.add(selector,
									selection)) {
								submitted++;
								executor.submit(() -> processSelection(
										generationTraversal, selector,
										selection));
							}
						}
						executor.awaitCompletion();
					} finally {
						selector.afterTraversal(generationTraversal);
					}
				}
				if (submitted == 0) {
					break;
				}
			}
		}
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

	private void processSelection(GenerationTraversal generationTraversal,
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

		public void beforeSelectorTraversal(Selector selector) {
			selectionsBySelector.getAndEnsure(selector);
		}

		public SelectionTraversal getSelectionTraversal() {
			return SelectionTraversal.this;
		}

		public boolean hasSelections(Class<? extends Selection> clazz,
				Object value) {
			return selectionsByClassValue.containsKey(clazz, value);
		}

		public boolean isForwards() {
			return currentSelector.isForwards();
		}

		public Iterable<Selection> selectionIterator() {
			if (isForwards()) {
				return selections;
			} else {
				// TODO - make selections a class which combines (to a degree)
				// List & Set - see JEP for SequencedCollection - and has a
				// non-copying reverse iterator. Probably plenty in FastUtil
				List<Selection> list = selections.stream()
						.collect(Collectors.toList());
				Collections.reverse(list);
				return list;
			}
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

	public static class StatsLogger {
		private SelectionTraversal selectionTraversal;

		public StatsLogger(SelectionTraversal selectionTraversal) {
			this.selectionTraversal = selectionTraversal;
		}

		void execute() {
			List<Entry> entries = new ArrayList<>();
			entries.add(new Entry(selectionTraversal.rootSelection));
			selectionTraversal.generations.values().forEach(traversal -> {
				entries.add(new Entry(traversal));
				if (traversal.selectionsBySelector.keySet().size() > 1) {
					traversal.selectionsBySelector.keySet()
							.forEach(selector -> {
								entries.add(new Entry(traversal, selector));
							});
				}
			});
			String log = ReflectionUtils.logBeans(Entry.class, entries);
			Ax.out(log);
		}

		@PropertyOrder({ "key", "outgoing" })
		public static class Entry {
			private GenerationTraversal traversal;

			private Selector selector;

			private Selection rootSelection;

			public Entry(GenerationTraversal traversal) {
				this(traversal, null);
			}

			public Entry(GenerationTraversal traversal, Selector selector) {
				this.traversal = traversal;
				this.selector = selector;
			}

			public Entry(Selection rootSelection) {
				this.rootSelection = rootSelection;
			}

			public Object getKey() {
				NestedNameProvider nestedNameProvider = NestedNameProvider
						.get();
				if (rootSelection != null) {
					return Ax.format("[%s]", nestedNameProvider
							.getNestedSimpleName(rootSelection.getClass()));
				} else if (selector == null) {
					return traversal.generation;
				} else {
					return "  " + nestedNameProvider
							.getNestedSimpleName(selector.getClass());
				}
			}

			public int getOutgoing() {
				if (rootSelection != null) {
					return 1;
				} else if (selector == null) {
					return traversal.selectionsBySelector.itemSize();
				} else {
					return traversal.selectionsBySelector.get(selector).size();
				}
			}
		}
	}
}
