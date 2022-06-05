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

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.log.TreeProcess.Node;
import cc.alcina.framework.common.client.log.TreeProcess.ProcessContextProvider;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.Topic;

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

	public SelectionTraversal() {
	}

	public void addSelector(Generation generation, Selector selector) {
		generations.get(generation).selectors.add(selector);
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
		return "[Unkknown position]";
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

	public Multiset<Generation, Set<Selection>> getSelections() {
		Multiset<Generation, Set<Selection>> result = new Multiset<>();
		generations.forEach((k, v) -> result.put(k, v.selections));
		return result;
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
		if (testFilter(generationTraversal, selection)) {
			if (generationTraversal.checkSelectionPath(selection)) {
				return;
			}
			Set<Selection> selections = generationTraversal.selections;
			int index = selections.size();
			selections.add(selection);
			generationIndicies.put(selection, index);
			selectionAdded.publish(selection);
		}
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
				for (Selection selection : generationTraversal.selections) {
					if (generationTraversal.submitted.add(selection)) {
						submitted++;
						executor.submit(() -> processSelection(
								generationTraversal, selection));
					}
				}
				if (submitted == 0) {
					break;
				}
				executor.awaitCompletion();
			}
		}
	}

	private Generation computeSubmittedGeneration(Selection selection) {
		return generations.values().stream()
				.filter(generation -> generation.submitted.contains(selection))
				.findFirst().get().generation;
	}

	private void enterSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::enterContext);
	}

	private void exitSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::exitContext);
	}

	private void processSelection(GenerationTraversal generationTraversal,
			Selection selection) {
		try {
			enterSelectionContext(selection);
			selection.processNode().select(null, this);
			if (!testFilter(generationTraversal, selection)) {
				// skip processing if, for instance, the traversal has hit max
				// exceptions
				return;
			}
			for (Selector processor : generationTraversal.selectors) {
				if (processor.handles(selection)) {
					try {
						beforeSelectionProcessed.publish(selection);
						processor.process(this, selection);
					} catch (Exception e) {
						selectionExceptions.put(selection, e);
						selection.processNode().onException(e);
						// TODO blah blah
						e.printStackTrace();
					}
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

	private boolean testFilter(GenerationTraversal generationTraversal,
			Selection selection) {
		if (filter == null) {
			return true;
		}
		if (filter.getMaxExceptions() > 0
				&& selectionExceptions.size() >= filter.getMaxExceptions()) {
			return false;
		}
		String generationName = generationTraversal.generation.toString();
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
					|| allGenerationsLimit > generationTraversal.selections
							.size();
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
			public void awaitCompletion() {
				for (Runnable runnable : runnables) {
					runnable.run();
				}
			}

			@Override
			public void submit(Runnable runnable) {
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

	static class GenerationTraversal {
		Generation generation;

		List<Selector> selectors = new ArrayList<>();

		Set<Selection> selections = new LinkedHashSet<>();

		Set<Selection> submitted = new LinkedHashSet<>();

		Map<String, Selection> selectionPaths = new LinkedHashMap<>();

		public GenerationTraversal(Generation generation) {
			this.generation = generation;
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
		public boolean checkSelectionPath(Selection selection) {
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
}
