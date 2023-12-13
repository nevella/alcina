package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.reflection.Reflections;
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

	public void inputsFromLayer(Layer inputsFromLayer) {
		this.inputsFromLayer = inputsFromLayer;
	}

	public void inputsFromPreviousSiblingLayer() {
		Layer<?> fromLayer = parent.children.get(parent.children.size() - 2);
		this.inputsFromLayer = fromLayer;
		fromLayer.inputsToLayer = this;
	}

	public Layer() {
		List<Class> bounds = Reflections.at(getClass())
				.getGenericBounds().bounds;
		if (bounds.size() > 0) {
			inputType = bounds.get(0);
		}
	}

	public void addChild(Layer child) {
		child.ensureChildren();
		child.parent = this;
		children.add(child);
		if (child instanceof InputsFromPreviousSibling) {
			child.inputsFromPreviousSiblingLayer();
		}
	}

	/**
	 * Normally child layers are populated during the constructor, this allows a
	 * builder pattern to specify attributes of the layer before child
	 * population
	 */
	public void ensureChildren() {
	}

	public Collection<S> computeInputs() {
		if (inputsFromLayer != null) {
			return (Collection<S>) state.traversalState.selections
					.byLayer(inputsFromLayer);
		} else {
			return state.traversalState.selections.get(inputType, false);
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

	public Layer findHandlingLayer(Class<? extends Selection> clazz) {
		Stack<Layer> layers = new Stack<>();
		layers.push(this);
		while (layers.size() > 0) {
			Layer<?> layer = layers.pop();
			if (layer.inputType == clazz) {
				return layer;
			}
			layer.getChildren().forEach(layers::push);
		}
		return null;
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

	public <S1 extends Selection> Stream<S1> getSelections(Class<S1> clazz) {
		return state.traversalState.getSelections(clazz).stream();
	}

	@Override
	public Iterator<S> iterator() {
		Collection<S> computeInputs = computeInputs();
		return computeInputs.iterator();
	}

	public String layerPath() {
		List<Integer> offsets = new ArrayList<>();
		Layer cursor = this;
		while (cursor.parent != null) {
			offsets.add(0, cursor.parent.children.indexOf(cursor));
			cursor = cursor.parent;
		}
		return offsets.isEmpty() ? "0"// root
				: offsets.stream().map(String::valueOf)
						.collect(Collectors.joining("."));
	}

	public void onBeforeTraversal(SelectionTraversal.State traversalState) {
		state = new State(traversalState);
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

	/*
	 * Ensure a given input is only processed once
	 */
	public boolean submit(S selection) {
		boolean submitted = state.submitted.add(selection);
		if (submitted) {
			onSubmit(selection);
		}
		return submitted;
	}

	public String toDebugString() {
		DepthFirstTraversal<Layer> debugTraversal = new DepthFirstTraversal<Layer>(
				this, Layer::getChildren);
		return debugTraversal.toTreeString();
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", getName(), NestedName.get(inputType));
	}

	public void withParent(Layer parent) {
		parent.children.add(this);
		this.parent = parent;
	}

	protected boolean isComplete() {
		return state.complete;
	}

	protected boolean isSinglePass() {
		return true;
	}

	protected void onAfterInputsProcessed() {
	}

	/**
	 * Called immediately after input traversal - subclasses should use this to
	 * flush any state which might emit a final selection, then call
	 * super.onAfterIteration()
	 */
	protected void onAfterIteration() {
		state.complete = isSinglePass() || state.iterationSubmitted == 0;
		state.iterationCount++;
	}

	protected void onAfterTraversal() {
	}

	protected void onBeforeIteration() {
		state.iterationSubmitted = 0;
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

	protected boolean testFilter(S selection) {
		return true;
	}

	void onSubmit(Selection selection) {
		state.iterationSubmitted++;
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

		Set<S> submitted = new LinkedHashSet<>();

		public State(SelectionTraversal.State traversalState) {
			this.traversalState = traversalState;
		}

		void select(Selection selection) {
			traversalState.select(selection);
		}
	}

	public String getName() {
		return NestedName.get(this);
	}

	public boolean hasReceivingLayer() {
		return inputsToLayer != null;
	}

	public String getFilterName() {
		return getClass().getName();
	}
}
