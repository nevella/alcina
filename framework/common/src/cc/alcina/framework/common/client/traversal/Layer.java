package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * <p>
 * Models one layer of the traversal process tree.
 *
 *
 */
public abstract class Layer<S extends Selection> implements Iterable<S> {
	List<Layer<?>> children = new ArrayList<>();

	Layer<?> parent;

	protected State state;

	protected Class<S> inputType;

	protected Layer inputsFromLayer;

	protected Layer inputsToLayer;

	protected Logger logger = LoggerFactory.getLogger(getClass());

	String layerPath;

	private String _toString;

	// set by traversal on first visit
	public int index = -1;

	public SelectionTraversal getTraversal() {
		return state.getTraversal();
	}

	public Layer() {
		List<Class> bounds = Reflections.at(getClass())
				.getGenericBounds().bounds;
		if (bounds.size() > 0) {
			inputType = bounds.get(0);
		}
	}

	/*
	 * Marker for an output layer transformation traversals - this allows the
	 * traversal UI to determine whether a selection is 'input' or 'output'
	 */
	public interface Output {
	}

	/**
	 * Returns a (Layer) context object implementing class T, resolved by
	 * ascending the layer tree until a layer implementing T is found. This
	 * context object is logically tied to the implementing layer and its
	 * sublayers, so more specific than a traversal context
	 * 
	 * @param <T>
	 *            the type required, and returned
	 * @param clazz
	 *            the class used to query the layer tree
	 * @return an instance, or null if it does not exist
	 */
	public <T> T layerContext(Class<T> clazz) {
		Layer cursor = this;
		do {
			if (Reflections.isAssignableFrom(clazz, cursor.getClass())) {
				return (T) cursor;
			}
			cursor = cursor.parent;
		} while (cursor.parent != null);
		return null;
	}

	public void addChild(Layer child) {
		child.ensureChildren();
		child.parent = this;
		children.add(child);
		if (child instanceof InputsFromPreviousSibling) {
			child.inputsFromPreviousSiblingLayer();
		}
	}

	public Collection<S> computeInputs() {
		if (inputsFromLayer != null) {
			return (Collection<S>) state.traversalState.selections
					.byLayerCounts(inputsFromLayer).keySet();
		} else {
			return state.traversalState.selections.get(inputType, false)
					.stream().filter(Selection::hasNoReplacedBy).toList();
		}
	}

	public int depth() {
		int depth = 0;
		Layer cursor = this;
		while (cursor.parent != null) {
			cursor = cursor.parent;
			depth++;
		}
		return depth;
	}

	/**
	 * Normally child layers are populated during the constructor, this allows a
	 * builder pattern to specify attributes of the layer before child
	 * population
	 */
	public void ensureChildren() {
	}

	public Layer firstLeaf() {
		Layer<?> cursor = this;
		while (cursor.getChildren().size() > 0) {
			cursor = cursor.getChildren().get(0);
		}
		return cursor;
	}

	public List<Layer<?>> getChildren() {
		return this.children;
	}

	public String getFilterName() {
		return getClass().getName();
	}

	public String getName() {
		return NestedName.get(this);
	}

	public <T extends Selection> Collection<T> getSelections() {
		return (Collection<T>) state.traversalState.selections.byLayer(this);
	}

	public <S1 extends Selection> Stream<S1> getSelections(Class<S1> clazz) {
		return state.traversalState.selections.get(clazz).stream();
	}

	public boolean hasReceivingLayer() {
		return inputsToLayer != null;
	}

	public void inputsFromLayer(Layer inputsFromLayer) {
		this.inputsFromLayer = inputsFromLayer;
	}

	public void inputsFromPreviousSiblingLayer() {
		Layer<?> fromLayer = parent.children.get(parent.children.size() - 2);
		this.inputsFromLayer = fromLayer;
		fromLayer.inputsToLayer = this;
	}

	protected boolean isComplete() {
		return state.complete;
	}

	protected boolean isSinglePass() {
		return true;
	}

	@Override
	public Iterator<S> iterator() {
		Collection<S> computeInputs = computeInputs();
		return computeInputs.iterator();
	}

	public String layerPath() {
		if (layerPath == null) {
			List<Integer> offsets = new ArrayList<>();
			Layer cursor = this;
			while (cursor.parent != null) {
				offsets.add(0, cursor.parent.children.indexOf(cursor));
				cursor = cursor.parent;
			}
			layerPath = offsets.isEmpty() ? "0"// root
					: offsets.stream().map(String::valueOf)
							.collect(Collectors.joining("."));
		}
		return layerPath;
	}

	protected void onAfterInputsProcessed() {
	}

	/**
	 * Called immediately after input traversal - subclasses should use this to
	 * flush any state which might emit a final selection, then call
	 * super.onAfterIteration()
	 */
	protected void onAfterIteration() {
		state.onAfterIteration();
	}

	protected void onAfterProcess(Selection selection) {
	}

	protected void onAfterTraversal() {
	}

	protected void onBeforeIteration() {
		state.iterationSubmitted = 0;
	}

	public void onBeforeTraversal(SelectionTraversal.State traversalState) {
		state = new State(traversalState);
	}

	void onSubmit(Selection selection) {
		state.iterationSubmitted++;
	}

	public void process(S selection) throws Exception {
		// if this layer processes no inputs, its children must
		Preconditions.checkState(children.size() > 0);
	}

	public Layer root() {
		Layer cursor = this;
		while (cursor.parent != null) {
			cursor = cursor.parent;
		}
		return cursor;
	}

	public void select(Selection selection) {
		state.select(selection);
	}

	protected <T extends Selection> T selection(Class<T> clazz) {
		return selections(clazz).iterator().next();
	}

	protected <T extends Selection> List<T> selections(Class<T> clazz) {
		return state.traversalState.selections.get(clazz, false);
	}

	protected <T extends Selection> List<T>
			selectionsWithSubtypes(Class<T> clazz) {
		return state.traversalState.selections.get(clazz, true);
	}

	/*
	 * Ensure a given input is only processed once
	 */
	public boolean submit(S selection) {
		boolean submitted = state.addToSubmitted(selection);
		if (submitted) {
			onSubmit(selection);
		}
		return submitted;
	}

	protected boolean testFilter(S selection) {
		return true;
	}

	public String toDebugString() {
		DepthFirstTraversal<Layer> debugTraversal = new DepthFirstTraversal<Layer>(
				this, Layer::getChildren);
		return debugTraversal.toTreeString();
	}

	@Override
	public String toString() {
		if (_toString == null) {
			_toString = Ax.format("%s :: %s", getName(),
					NestedName.get(inputType));
		}
		return _toString;
	}

	public void withParent(Layer parent) {
		parent.children.add(this);
		this.parent = parent;
	}

	/*
	 * Marker, copy layerstate.matches->layerstate.outputs
	 */
	public interface MatchesAreOutputs {
	}

	public class State {
		public SelectionTraversal.State traversalState;

		public int iterationSubmitted;

		public int iterationCount;

		public boolean complete;

		Set<S> submitted;

		List<S> firstPassSubmitted = new ArrayList<>();

		Set<String> emittedWarnings = new LinkedHashSet<>();

		public State(SelectionTraversal.State traversalState) {
			this.traversalState = traversalState;
		}

		void onAfterIteration() {
			complete = isSinglePass() || iterationSubmitted == 0;
			if (!complete && iterationCount == 0) {
				submitted = AlcinaCollections
						.newHashSet(firstPassSubmitted.size());
				submitted.addAll(firstPassSubmitted);
				firstPassSubmitted = null;
			}
			iterationCount++;
		}

		public boolean addToSubmitted(S selection) {
			if (submitted == null) {
				firstPassSubmitted.add(selection);
				return true;
			} else {
				return submitted.add(selection);
			}
		}

		/**
		 * Returns the traversal's traversalContext field, cast to T, requiring
		 * that it implements the interface T.
		 * 
		 * @param <T>
		 *            the type required, and returned
		 * @param clazz
		 *            the class used to query the layer tree
		 * @return an instance of T (the SelectionTraversal.traversalContext
		 *         field)
		 */
		public <T> T traversalContext(Class<T> clazz) {
			return traversalState.context(clazz);
		}

		public SelectionTraversal getTraversal() {
			return traversalState.getTraversal();
		}

		public void select(Selection selection) {
			traversalState.select(selection);
		}

		public void warnOnce(String template, Object... args) {
			String message = Ax.format(template, args);
			if (emittedWarnings.add(message)) {
				Ax.err(message);
			}
		}
	}
}
