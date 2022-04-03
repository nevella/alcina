package cc.alcina.framework.servlet.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.servlet.job.TreeProcess.Node;

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

	private List<Generation> generations = new ArrayList<>();

	public void addSelector(Selector selector) {
		selectors.add(currentGeneration, selector);
	}

	public Generation getCurrentGeneration() {
		return this.currentGeneration;
	}

	public List<Generation> getGenerations() {
		return this.generations;
	}

	public Selection getRootSelection() {
		return this.rootSelection;
	}

	public void setCurrentGeneration(Generation currentGeneration) {
		this.currentGeneration = currentGeneration;
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
			List<Selection> toProcess = selections.get(generation);
			for (Selection selection : toProcess) {
				try {
					enterSelectionContext(selection);
					selection.processNode().select(null);
					List<Selector> processors = selectors.get(generation);
					for (Selector processor : processors) {
						if (processor.handles(selection)) {
							processor.process(this, selection);
						}
					}
				} finally {
					exitSelectionContext(selection);
					selection.processNode().setSelfComplete(true);
					releaseCompletedSelections(selection);
				}
			}
			List<Generation> currentSelectionKeys = selections.keySet().stream()
					.collect(Collectors.toList());
		}
	}

	private void enterSelectionContext(Selection selection) {
		selection.processNode().asNodePath().stream()
				.<Selection> map(Node::typedValue)
				.forEach(Selection::enterContext);
	}

	private void exitSelectionContext(Selection selection) {
		selection.processNode().asNodePath().stream()
				.<Selection> map(Node::typedValue)
				.forEach(Selection::exitContext);
	}

	/*
	 * Intended for release of DOM references, particularly
	 */
	private void releaseCompletedSelections(Selection selection) {
		selection.processNode().setSelfComplete(true);
		Selection cursor = selection;
		while (cursor != null) {
			if (cursor.processNode().evaluateDescendantsComplete()) {
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
