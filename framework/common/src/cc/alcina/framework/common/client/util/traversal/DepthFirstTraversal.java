package cc.alcina.framework.common.client.util.traversal;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Topic;

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
 * <p>
 * This does *not* generate a new iterator when iterator() is called multiple
 * times
 *
 *
 * @param <T>
 */
public class DepthFirstTraversal<T> implements Iterable<T>, Iterator<T> {
	boolean iteratorConsumed;

	private boolean lastFirst;

	private boolean writeOnce;

	private Function<T, List<T>> childrenSupplier;

	TraversalNode current;

	TraversalNode next;

	public Topic<T> topicBeforeNodeExit = Topic.create();

	public Topic<T> topicAtEndOfChildIterator = Topic.create();

	private T root;

	public DepthFirstTraversal(T root, Function<T, List<T>> childrenSupplier) {
		this(root, childrenSupplier, false);
	}

	public DepthFirstTraversal(T root, Function<T, List<T>> childrenSupplier,
			boolean lastFirst) {
		this.root = root;
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

	public T current() {
		return current.value;
	}

	@Override
	public Iterator<T> iterator() {
		Preconditions.checkState(!iteratorConsumed,
				"Stream/iterator has already been traversed");
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

	/*
	 * Will only work with a structure which is not modified after init
	 */
	public void setNext(T t) {
		current = null;
		next = new TraversalNode(root);
		iteratorConsumed = false;
		for (;;) {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			if (next.value == t) {
				if (current != null) {
					current.modifiedSinceLastNext = false;
				}
				break;
			}
			next();
		}
	}

	public Stream<T> stream() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
				iterator(), Spliterator.ORDERED), false);
	}

	public String toTreeString() {
		DepthFirstTraversal<T> toStringTraversal = new DepthFirstTraversal<>(
				root, childrenSupplier);
		// assumes non-generative
		toStringTraversal.writeOnce = true;
		FormatBuilder format = new FormatBuilder();
		for (T t : toStringTraversal) {
			format.indent(toStringTraversal.current.depth());
			format.line(t);
		}
		return format.toString();
	}

	// so named because often T is named Node
	class TraversalNode {
		private T value;

		List<TraversalNode> children;

		TraversalNode parent;

		int childCursorPosition = -1;

		boolean modifiedSinceLastNext = true;

		int indexInParent = -1;

		TraversalNode(T value) {
			this.value = value;
		}

		public void add(T t, boolean duringIteration) {
			if (duringIteration && (lastFirst || writeOnce)) {
				throw new ConcurrentModificationException();
			}
			TraversalNode node = new TraversalNode(t);
			node.parent = this;
			if (children == null) {
				children = new ArrayList<>();
			}
			node.indexInParent = children.size();
			children.add(node);
			if (lastFirst) {
				childCursorPosition = children.size();
			}
			modifiedSinceLastNext = true;
		}

		int depth() {
			TraversalNode cursor = this;
			int depth = 0;
			for (;;) {
				if (cursor.parent == null) {
					break;
				} else {
					cursor = cursor.parent;
					depth++;
				}
			}
			return depth;
		}

		public TraversalNode next() {
			current = this;
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
				} else {
					topicAtEndOfChildIterator.publish(value);
					/*
					 * retry to check if the child list was modified
					 */
					if (childCursorPosition >= 0
							&& childCursorPosition < children.size()) {
						return children.get(childCursorPosition);
					}
				}
			}
			// children exhausted, try parent.next (child)
			topicBeforeNodeExit.publish(value);
			if (parent != null) {
				return parent.next();
			}
			return null;
		}
	}

	public int getIndexInParent() {
		return current.indexInParent;
	}
}
