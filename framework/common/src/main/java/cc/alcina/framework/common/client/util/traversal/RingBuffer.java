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

	private int resizeMinimumSize = 4096;

	public RingBuffer(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	public T acquire() {
		if (available == 0) {
			return supplier.get();
		} else {
			available--;
			T result = elements.get(availablePtr++);
			int size = elements.size();
			// resize (to at most half the size of the current list) if > half
			// the list is consumed
			if (size > resizeMinimumSize && size >> 1 < availablePtr) {
				List<T> subList = elements.subList(availablePtr,
						elements.size());
				elements = new ArrayList<>(subList);
				availablePtr = 0;
			}
			return result;
		}
	}

	public int getResizeMinimumSize() {
		return this.resizeMinimumSize;
	}

	public void release(T t) {
		elements.add(t);
		available++;
	}

	public void setResizeMinimumSize(int resizeMinimumSize) {
		this.resizeMinimumSize = resizeMinimumSize;
	}
}
