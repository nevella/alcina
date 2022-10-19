package cc.alcina.framework.common.client.util.traversal;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.traversal.OneWayTraversal.TraversalNode;

/**
 * <p>
 * One-off iterable/iterator tuple, models depth-first descent of a
 * self-generating tree, such as a serialization process or render tree
 * traversal
 *
 * <p>
 * This version minimises allocations (using a ringbuffer of released nodes)
 *
 * <p>
 * This does *not* throw a ConcurrentModificationException if modifying the
 * children of the entered (current) node, but will in other cases
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

	RingBuffer<TraversalNode> buffer;

	private Supplier<T> supplier;

	public OneWayTraversal(T t, Supplier<T> supplier) {
		this.supplier = supplier;
		buffer = new RingBuffer<TraversalNode>(
				(Supplier<TraversalNode>) (Supplier<?>) this);
		// the root node is ex-buffer
		next = new TraversalNode();
		next.value = t;
	}

	public T add() {
		Preconditions.checkState(entered != null);
		return entered.add();
	}

	@Override
	public TraversalNode get() {
		TraversalNode node = new TraversalNode();
		node.value = supplier.get();
		return node;
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
		// the consumer can either handle result, or process via next.enter() -
		// dealer's choice
		next.enter();
		return result;
	}

	private void prepareNext() {
		next = entered.next();
	}

	// so named because often T is named Node
	class TraversalNode implements Traversable<TraversalNode> {
		T value;

		TraversalNode parent;

		TraversalNode nextSibling;

		TraversalNode lastChild;

		TraversalNode descentChild;

		TraversalNode() {
		}

		public T add() {
			if (entered != this) {
				throw new ConcurrentModificationException(
						"adding children to non-cursor node");
			}
			TraversalNode node = buffer.acquire();
			node.parent = this;
			if (descentChild == null) {
				descentChild = node;
			} else {
				lastChild.nextSibling = node;
			}
			lastChild = node;
			return node.value;
		}

		@Override
		/*
		 * This is a 'possible' - one way to push traversal to this framework is
		 * to populate children on enter() - but certainly this class shouldn't
		 * do nothing...possibly pull this doc up
		 */
		public Iterator<TraversalNode> children() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void enter() {
			value.enter();
		}

		@Override
		public void exit() {
			value.exit();
			release();
		}

		// next copying allows for release()
		public TraversalNode next() {
			if (descentChild != null) {
				TraversalNode next = descentChild;
				descentChild = null;
				return next;
			} else {
				if (nextSibling != null) {
					TraversalNode next = nextSibling;
					exit();
					return next;
				} else {
					TraversalNode parent = this.parent;
					exit();
					return parent == null ? null : parent.next();
				}
			}
		}

		@Override
		public void release() {
			value.release();
			nextSibling = null;
			parent = null;
			buffer.release(this);
		}
	}
}
