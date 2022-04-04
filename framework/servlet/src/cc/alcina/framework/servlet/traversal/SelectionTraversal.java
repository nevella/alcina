package cc.alcina.framework.servlet.traversal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.servlet.job.JobContext;

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
 */
public class SelectionTraversal {
	GenerationStore generationStore = new GenerationStore();

	public Topic<Selection> selectionAdded = Topic.local();

	Selection rootSelection;

	Multimap<Generation, List<Selector>> selectors = new Multimap<>();

	Multimap<Generation, List<Selection>> selections = new Multimap<>();

	private Generation currentGeneration;

	private Generation nextGeneration;

	private List<Generation> generations = new ArrayList<>();

	public void addSelector(Generation generation, Selector selector) {
		selectors.add(generation, selector);
	}

	public List<Generation> getGenerations() {
		return this.generations;
	}

	public Selection getRootSelection() {
		return this.rootSelection;
	}

	public <G extends Generation> void populateGenerations(Class<G> clazz) {
		Arrays.stream(clazz.getEnumConstants()).forEach(generations::add);
	}

	public synchronized void select(Generation generation,
			Selection selection) {
		selections.add(generation, selection);
	}

	public synchronized void select(Selection selection) {
		select(nextGeneration, selection);
	}

	public void setRootSelection(Selection rootSelection) {
		this.rootSelection = rootSelection;
	}

	public void traverse() {
		Objects.requireNonNull(selectors.firstKey());
		// prime the pump
		selections.add(selectors.firstKey(), rootSelection);
		for (Generation generation : generations) {
			currentGeneration = generation;
			nextGeneration = Ax.next(generations, currentGeneration);
			List<Selection> toProcess = selections.getAndEnsure(generation);
			for (Selection selection : toProcess) {
				try {
					enterSelectionContext(selection);
					selection.processNode().select(null);
					List<Selector> processors = selectors
							.getAndEnsure(generation);
					for (Selector processor : processors) {
						if (processor.handles(selection)) {
							try {
								processor.process(this, selection);
							} catch (Exception e) {
								// TODO blah blah
								e.printStackTrace();
							}
						}
					}
				} finally {
					exitSelectionContext(selection);
					selection.processNode().setSelfComplete(true);
					releaseCompletedSelections(selection);
				}
				JobContext.checkCancelled();
			}
			List<Generation> currentSelectionKeys = selections.keySet().stream()
					.collect(Collectors.toList());
		}
		selections.asCountingMap().entrySet().forEach(Ax::out);
	}

	private void enterSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::enterContext);
	}

	private void exitSelectionContext(Selection<?> selection) {
		selection.ancestorSelections().forEach(Selection::exitContext);
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

	/*
     * Here, 'generation' means an evoltionary generation - corresponding to one
     * level of the TreeProcess node tree - e.g:
     *
     * @formatter:off
     *
     * [Process top level web pages :: 0 :: root] ->
     * [Process intermediate web pages :: 1 :: child] ->
     * [Emit journal resource :: 2 :: grandchild] ->
     * [Emit journal resource metadata :: 3 :: g-grandchild]
     *
     * @formatter:on
     *
     */
	public interface Generation {
	}

	static class GenerationStore {
		Multimap<Class, List<Selection>> selectionByClass = new Multimap<>();

		public <T> List<Selection<T>> getSelections(Class<T> clazz) {
			return (List) selectionByClass.getAndEnsure(clazz);
		}
	}
}
