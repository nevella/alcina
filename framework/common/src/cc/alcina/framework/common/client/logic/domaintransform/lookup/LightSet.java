package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.gwt.core.shared.GwtIncompatible;

/**
 * 
 * @author nick@alcina.cc
 * 
 * @param <H>
 */
public class LightSet<H> extends AbstractSet<H> implements Cloneable,
		Serializable {
	static final transient long serialVersionUID = 1;

	static final transient int DEGENERATE_THRESHOLD = 30;

	static final transient int INITIAL_SIZE = 5;

	private transient Object[] elementData;

	transient int size = 0;

	transient int modCount = 0;

	private transient Set<H> degenerate;

	public LightSet() {
	}

	public LightSet(Collection<? extends H> c) {
		addAll(c);
	}

	@Override
	public boolean add(H e) {
		if (e == null) {
			throw new IllegalArgumentException();
		}
		if (degenerate != null) {
			return degenerate.add(e);
		}
		if (isEmpty()) {
			elementData = new Object[INITIAL_SIZE];
			elementData[0] = e;
			size++;
			modCount++;
			return false;
		}
		int idx = indexOf(e);
		if (idx != -1) {
			elementData[idx] = e;
			return false;
		} else {
			if (size == DEGENERATE_THRESHOLD) {
				LinkedHashSet degenerate = new LinkedHashSet<H>();
				degenerate.addAll(this);
				this.degenerate = degenerate;
				elementData = null;
				return degenerate.add(e);
			}
			size++;
			modCount++;
			if (size > elementData.length) {
				Object[] newData = new Object[size * 2];
				System.arraycopy(elementData, 0, newData, 0, elementData.length);
				elementData = newData;
			}
			elementData[size - 1] = e;
			return true;
		}
	}

	public Object clone() {
		return new LightSet<H>(this);
	}

	@Override
	public boolean contains(Object o) {
		if (o == null) {
			return false;
		}
		if (degenerate != null) {
			return degenerate.contains(o);
		}
		if (isEmpty()) {
			return false;
		}
		return indexOf(o) != -1;
	}

	@Override
	public Iterator<H> iterator() {
		if (degenerate != null) {
			return degenerate.iterator();
		}
		return new LightSetIterator();
	}

	@Override
	public boolean remove(Object o) {
		if (o == null) {
			return false;
		}
		if (degenerate != null) {
			return degenerate.remove(o);
		}
		if (isEmpty()) {
			return false;
		}
		int idx = indexOf(o);
		if (idx == -1) {
			return false;
		}
		size--;
		modCount++;
		if (size == 0) {
			elementData = new Object[INITIAL_SIZE];
			return true;
		}
		// A B C
		// A C
		System.arraycopy(elementData, idx + 1, elementData, idx, size - idx);
		elementData[size] = null;
		return true;
	}

	@Override
	public int size() {
		if (degenerate != null) {
			return degenerate.size();
		}
		return size;
	}

	private int indexOf(Object e) {
		for (int idx = 0; idx < size; idx++) {
			Object f = elementData[idx];
			if (f.equals(e)) {
				return idx;
			}
		}
		return -1;
	}

	@GwtIncompatible("java serialization")
	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();
		int arrayLength = s.readInt();
		if (arrayLength != 0) {
			// use add, to handle degenerate case
			for (int i = 0; i < arrayLength; i++) {
				add((H) s.readObject());
			}
		}
	}

	@GwtIncompatible("java serialization")
	private void writeObject(java.io.ObjectOutputStream s)
			throws java.io.IOException {
		// Write out element count, and any hidden stuff
		s.defaultWriteObject();
		// Write out array length
		s.writeInt(size());
		// Write out all elements in the proper order.
		for (Iterator<H> itr = iterator(); itr.hasNext();) {
			s.writeObject(itr.next());
		}
	}

	class LightSetIterator implements Iterator<H> {
		int idx = 0;

		int itrModCount = modCount;

		boolean nextCalled = false;

		@Override
		public boolean hasNext() {
			return idx < size;
		}

		@Override
		public H next() {
			if (modCount != itrModCount) {
				throw new ConcurrentModificationException();
			}
			if (idx == size) {
				throw new NoSuchElementException();
			}
			nextCalled = true;
			return (H) elementData[idx++];
		}

		@Override
		public void remove() {
			if (modCount != itrModCount) {
				throw new ConcurrentModificationException();
			}
			if (!nextCalled) {
				throw new IllegalStateException();
			}
			LightSet.this.remove(elementData[--idx]);
			itrModCount++;
			nextCalled = false;
		}
	}
}
