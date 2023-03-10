package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.util.NestedNameProvider;

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
 * @author nick@alcina.cc
 *
 */
public abstract class Layer<S extends Selection> implements
		SelectionTraversal.Generation, Selector<S, Selection>, Iterable<S> {
	public final Name name;

	List<Layer> children = new ArrayList<>();

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

	public Iterator<S> computeInputs() {
		return state.traversalState.selections.get(signature.input).iterator();
	}

	public List<Layer> getChildren() {
		return this.children;
	}

	@Override
	public Iterator<S> iterator() {
		return computeInputs();
	}

	public void onBeforeTraversal(SelectionTraversal.State traversalState) {
		state = new State(traversalState);
	}

	@Override
	public abstract void process(SelectionTraversal traversal, S selection)
			throws Exception;

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

	public void withParent(Layer parent) {
		parent.children.add(this);
		this.parent = parent;
	}

	protected boolean isComplete() {
		return state.complete;
	}

	protected void onAfterInputsProcessed() {
	}

	protected void onAfterIteration() {
		state.complete = state.iterationSubmitted == 0;
		state.iterationCount++;
	}

	protected void onAfterTraversal() {
	}

	protected void onBeforeIteration() {
		state.iterationSubmitted = 0;
	}

	protected <T extends Selection> T selection(Class<T> clazz) {
		return selections(clazz).iterator().next();
	}

	protected <T extends Selection> List<T> selections(Class<T> clazz) {
		return state.traversalState.selections.get(clazz);
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
				return NestedNameProvider.get(this.clazz);
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
