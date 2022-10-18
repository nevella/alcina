package cc.alcina.framework.common.client.util.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/*
 * A low-level effort to reduce allocation in stack-based processes
 *
 * Freed elements are added at the end of #elements, and are marked as consumed
 * once released for reuse.
 *
 * The elements buffer is resized (halved) once a consumed threshold is reached.
 *
 * Not thread-safe.
 */
public class RingBuffer<T> {
	private List<T> elements = new ArrayList<>();

	// points to the next available element
	private int availablePtr = 0;

	// count of available(elements.size()-1-availablePtr)
	private int available = 0;

	private Supplier<T> supplier;

	public RingBuffer(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	public T acquire() {
		if (available == 0) {
			return supplier.get();
		} else {
			available--;
			return elements.get(availablePtr++);
		}
	}

	public void release(T t) {
		elements.add(t);
		available++;
	}

	public static class TraversableBuffer<TT extends Traversable<TT>>
			extends RingBuffer<TT> {
		public TraversableBuffer(Supplier<TT> supplier) {
			super(supplier);
		}

		@Override
		public void release(TT t) {
			t.release();
			super.release(t);
		}
	}
}
