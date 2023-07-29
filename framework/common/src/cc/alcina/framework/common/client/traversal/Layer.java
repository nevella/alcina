package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedNameProvider;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * <p>
 * Models one layer of the traversal process tree.
 *
 * <p>
 * FIXME - traversal - e.g.
 *
 * <p>
 * FIXME - move 'tokens' to ParserLayer
 *
 *
 * 
 *
 */
public abstract class Layer<S extends Selection> implements
		SelectionTraversal.Generation, Selector<S, Selection>, Iterable<S> {
	public final Name name;

	List<Layer<?>> children = new ArrayList<>();

	Layer parent;

	protected State state;

	public final Signature signature;

	public Layer(Class<S> input, Class<? extends Selection>... outputs) {
		this(null, input, outputs);
	}

	public Layer(Name name, Class<S> input,
			Class<? extends Selection>... outputs) {
		this.name = name != null ? name : Name.fromClass(getClass());
		signature = new Signature(input, outputs);
	}

	public void addChild(Layer child) {
		child.parent = this;
		children.add(child);
	}

	public Collection<S> computeInputs() {
		return state.traversalState.selections.get(signature.input, false);
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
			if (layer.signature.input == clazz) {
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

	@Override
	public Iterator<S> iterator() {
		return computeInputs().iterator();
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

	@Override
	public void process(SelectionTraversal traversal, S selection)
			throws Exception {
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
				this, Layer::getChildren, false);
		return debugTraversal.toTreeString();
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", name, signature);
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

	protected void select(Selection selection) {
		state.traversalState.select(selection);
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

	public interface Name {
		public static Name fromClass(Class clazz) {
			return new NameFromClass(clazz);
		}

		String name();

		static final class NameFromClass implements Name {
			private final Class clazz;

			NameFromClass(Class clazz) {
				this.clazz = clazz;
			}

			@Override
			public String name() {
				return this.clazz.getName();
			}

			@Override
			public String toString() {
				return NestedNameProvider.get(this.clazz)
						.replaceFirst("(.+)Layer$", "$1");
			}
		}
	}

	public class Signature {
		public final Class<S> input;

		public final List<Class<? extends Selection>> outputs;

		public Signature(Class<S> input,
				Class<? extends Selection>... outputs) {
			this.input = input;
			this.outputs = List.of(outputs);
		}

		@Override
		public String toString() {
			return Ax.format("%s => %s", NestedNameProvider.get(input),
					outputs.stream().map(NestedNameProvider::get)
							.collect(Collectors.toList()));
		}
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
	}
}
