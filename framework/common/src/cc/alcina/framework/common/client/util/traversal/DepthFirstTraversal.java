package cc.alcina.framework.common.client.util.traversal;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.google.common.base.Preconditions;

/**
 * <p>
 * One-off iterable/iterator tuple, models descent of a tree (often composed of
 * lists)
 *
 * <p>
 * There may be a fancy-schmancy way of doing this with fewer allocations for a
 * structure which doesn't modify during traversal - but this is easier for
 * handling the general case.
 *
 * <p>
 * This does *not* throw ConcurrentModificationExceptions if traversing
 * forwards, but will if traversing lastFirst
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class DepthFirstTraversal<T> implements Iterable<T>, Iterator<T> {
	boolean iteratorConsumed;

	private boolean lastFirst;

	private Function<T, List<T>> childrenSupplier;

	TraversalNode current;

	TraversalNode next;

	public DepthFirstTraversal(T root, Function<T, List<T>> childrenSupplier,
			boolean lastFirst) {
		this.lastFirst = lastFirst;
		this.childrenSupplier = childrenSupplier;
		next = new TraversalNode(root);
	}

	public void add(T t) {
		Preconditions.checkState(current != null);
		current.add(t, true);
	}

	@Override
	public boolean hasNext() {
		if (current != null && current.modifiedSinceLastNext) {
			prepareNext();
		}
		return next != null;
	}

	@Override
	public Iterator<T> iterator() {
		Preconditions.checkState(!iteratorConsumed);
		iteratorConsumed = true;
		return this;
	}

	@Override
	public T next() {
		Preconditions.checkState(next != null);
		current = next;
		T result = current.value;
		return result;
	}

	private void prepareNext() {
		next = current.next();
	}

	// so named because often T is named Node
	class TraversalNode {
		private T value;

		List<TraversalNode> children;

		TraversalNode parent;

		int childCursorPosition = -1;

		boolean modifiedSinceLastNext = true;

		TraversalNode(T value) {
			this.value = value;
		}

		public void add(T t, boolean duringIteration) {
			if (duringIteration && lastFirst) {
				throw new ConcurrentModificationException();
			}
			TraversalNode node = new TraversalNode(t);
			node.parent = this;
			if (children == null) {
				children = new ArrayList<>();
			}
			children.add(node);
			if (lastFirst) {
				childCursorPosition = children.size();
			}
			modifiedSinceLastNext = true;
		}

		public TraversalNode next() {
			modifiedSinceLastNext = false;
			// ensure children
			if (children == null) {
				if (childrenSupplier != null) {
					List<T> childValues = childrenSupplier.apply(value);
					if (childValues != null && childValues.size() > 0) {
						childValues.forEach(v -> add(v, false));
					}
				}
			}
			if (children != null) {
				// try in children
				childCursorPosition = lastFirst ? childCursorPosition - 1
						: childCursorPosition + 1;
				if (childCursorPosition >= 0
						&& childCursorPosition < children.size()) {
					return children.get(childCursorPosition);
				}
			}
			// children exhausted, try parent.next (child)
			if (parent != null) {
				return parent.next();
			}
			return null;
		}
	}
}