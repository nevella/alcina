package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

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
 */
public class SelectionTraversal implements ProcessContextProvider {
	public Topic<Selection> selectionAdded = Topic.local();

	public Topic<Selection> selectionProcessed = Topic.local();

	Selection rootSelection;

	Multimap<Generation, List<Selector>> selectors = new Multimap<>();

	Multiset<Generation, Set<Selection>> selections = new Multiset<>();

	Map<Selection, Integer> generationIndicies = new LinkedHashMap<>();

	Map<Selection, Exception> selectionExceptions = new ConcurrentHashMap<>();

	Multiset<Generation, Set<Selection>> submitted = new Multiset<>();

	private Generation currentGeneration;

	private Generation nextGeneration;

	private List<Generation> generations = new ArrayList<>();

	private SelectionFilter filter;

	private Executor executor = new Executor.CurrentThreadExecutor();

	public SelectionTraversal() {
	}

	public void addSelector(Generation generation, Selector selector) {
		selectors.add(generation, selector);
	}

	@Override
	public String flatPosition(Node node) {
		FormatBuilder position = new FormatBuilder().separator(" > ");
		List<Node> selectionPath = node.asNodePath();
		Node last = CommonUtils.last(selectionPath);
		Object value = last.getValue();
		List<Generation> list = selections.keySet().stream()
				.collect(Collectors.toList());
		for (int idx = 0; idx < list.size(); idx++) {
			Generation generation = list.get(idx);
			Set<Selection> set = selections.get(generation);
			if (set.contains(value)) {
				int level = idx;
				position.format("Generation: [%s/%s]", idx + 1, list.size());
				IntPair pair = new IntPair(generationIndicies.get(value),
						set.size());
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

	public List<Generation> getGenerations() {
		return this.generations;
	}

	public Selection getRootSelection() {
		return this.rootSelection;
	}

	public Multiset<Generation, Set<Selection>> getSelections() {
		return selections;
	}

	public <G extends Generation> void populateGenerations(Class<G> clazz) {
		Arrays.stream(clazz.getEnumConstants()).forEach(generations::add);
		generations.forEach(selectors::getAndEnsure);
		generations.forEach(selections::getAndEnsure);
		generations.forEach(submitted::getAndEnsure);
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
					v.stream().map(CommonUtils::escapeRegex)
							.collect(Collectors.joining("|")));
			filter.addGenerationFilter(k.toString(), pathSegmentRegex);
		});
		return filter;
	}

	public synchronized void select(Generation generation,
			Selection selection) {
		if (testFilter(generation, selection)) {
			Set<Selection> set = selections.get(generation);
			int index = set.size();
			set.add(selection);
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
		Objects.requireNonNull(selectors.firstKey());
		// prime the pump
		select(selectors.firstKey(), rootSelection);
		for (Generation generation : generations) {
			currentGeneration = generation;
			nextGeneration = Ax.next(generations, currentGeneration);
			// this logic (looping on the current generation until there's a
			// pass with no submitted tasks) allows selectors to add to the
			// current generation (as well as subsequent)
			for (;;) {
				Set<Selection> submittedGeneration = submitted.get(generation);
				int submitted = 0;
				for (Selection selection : selections.get(generation)) {
					if (submittedGeneration.add(selection)) {
						submitted++;
						executor.submit(
								() -> processSelection(generation, selection));
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
		return submitted.entrySet().stream()
				.filter(e -> e.getValue().contains(selection)).findFirst().get()
				.getKey();
	}

	private void enterSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::enterContext);
	}

	private void exitSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::exitContext);
	}

	private void processSelection(Generation generation, Selection selection) {
		try {
			enterSelectionContext(selection);
			selection.processNode().select(null, this);
			if (!testFilter(generation, selection)) {
				// skip processing if, for instance, the traversal has hit max
				// exceptions
				return;
			}
			List<Selector> processors = selectors.get(generation);
			for (Selector processor : processors) {
				if (processor.handles(selection)) {
					try {
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

	private boolean testFilter(Generation generation, Selection selection) {
		if (filter == null) {
			return true;
		}
		if (filter.getMaxExceptions() > 0
				&& selectionExceptions.size() >= filter.getMaxExceptions()) {
			return false;
		}
		if (filter.hasGenerationFilter(generation.toString())) {
			if (filter.matchesGenerationFilter(generation.toString(),
					selection.getFilterableSegments())) {
				return true;
			} else {
				return false;
			}
		} else {
			int allGenerationsLimit = filter.getAllGenerationsLimit();
			return allGenerationsLimit == 0
					|| allGenerationsLimit > selections.get(generation).size();
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
}
