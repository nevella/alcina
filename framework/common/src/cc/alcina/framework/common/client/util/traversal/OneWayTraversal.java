package cc.alcina.framework.common.client.util.traversal;

import java.util.Iterator;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.traversal.OneWayTraversal.TraversalNode;

/**
 * <p>
 * One-off iterable/iterator tuple, models descent of a tree
 *
 * <p>
 * This version minimises allocations (using a ringbuffer of released nodes)
 *
 * <p>
 * This does *not* throw ConcurrentModificationExceptions if traversing
 * forwards, but will if traversing lastFirst
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class OneWayTraversal<T extends Traversable>
		implements Iterable<T>, Iterator<T>, Supplier<TraversalNode> {
	TraversalNode next;

	TraversalNode entered;

	private boolean iteratorConsumed;

	RingBuffer.TraversableBuffer<TraversalNode> buffer;

	private Supplier<T> supplier;

	public OneWayTraversal(T root, Supplier<T> supplier) {
		this.supplier = supplier;
		this.supplier = supplier;
		buffer = new RingBuffer.TraversableBuffer<TraversalNode>(
				(Supplier<TraversalNode>) (Supplier<?>) this);
		// TODO - this should be buffer
		next = new TraversalNode(null, root);
	}

	public void add(T t) {
		Preconditions.checkState(next != null);
		next.add(t);
	}

	@Override
	public TraversalNode get() {
		return null;
	}

	@Override
	public boolean hasNext() {
		if (next == entered) {
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
		entered = next;
		T result = next.value;
		// although result has an enter() method, let the iterator call that.
		// exit() will be called during iteration though
		return result;
	}

	private void prepareNext() {
		next = entered.next();
	}

	// so named because often T is named Node
	class TraversalNode implements Traversable<TraversalNode> {
		T value;

		TraversalNode parent;

		TraversalNode cursorSibling;

		TraversalNode lastChild;

		TraversalNode descentChild;

		TraversalNode() {
		}

		public void add(T t) {
			TraversalNode node = new TraversalNode(this, t);
			if (descentChild == null) {
				descentChild = node;
			} else {
				node.cursorSibling = lastChild;
			}
			lastChild = node;
		}

		@Override
		public Iterator<TraversalNode> children() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void enter() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void exit() {
			throw new UnsupportedOperationException();
		}

		// next copying allows for release()
		public TraversalNode next() {
			if (descentChild != null) {
				TraversalNode next = descentChild;
				descentChild = null;
				return next;
			} else {
				if (cursorSibling != null) {
					TraversalNode next = cursorSibling;
					value.exit();
					release();
					return next;
				} else {
					TraversalNode parent = this.parent;
					value.exit();
					release();
					return parent == null ? null : parent.next();
				}
			}
		}

		@Override
		public void release() {
			value.release();
			cursorSibling = null;
			parent = null;
		}

		TraversalNode init(TraversalNode parent, T value) {
			this.parent = parent;
			this.value = value;
			return this;
		}
	}
}
