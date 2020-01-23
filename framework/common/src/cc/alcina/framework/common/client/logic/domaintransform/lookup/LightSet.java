package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import com.google.gwt.core.shared.GwtIncompatible;

import cc.alcina.framework.common.client.domain.DomainCollections;

/**
 * 
 * @author nick@alcina.cc
 * 
 * @param <H>
 */
public class LightSet<H> extends AbstractSet<H>
		implements Cloneable, Serializable {
	static final transient long serialVersionUID = 1;

	private static final transient int DEGENERATE_THRESHOLD = 30;

	private static final transient int INITIAL_SIZE = 4;

	private transient Object[] elementData;

	/*
	 * used to reduce likelihood of page misses
	 */
	private transient int[] hashes;

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
		if (degenerate != null) {
			return degenerate.add(e);
		}
		if (isEmpty()) {
			elementData = new Object[INITIAL_SIZE];
			elementData[0] = e;
			hashes = new int[INITIAL_SIZE];
			hashes[0] = Objects.hashCode(e);
			size++;
			modCount++;
			return true;
		}
		int idx = indexOf(e);
		if (idx != -1) {
			elementData[idx] = e;
			/*
			 * hash won't have changed
			 */
			return false;
		} else {
			if (size == DEGENERATE_THRESHOLD) {
				Set degenerate = DomainCollections.get().createUnsortedSet();
				degenerate.addAll(this);
				this.degenerate = degenerate;
				elementData = null;
				hashes = null;
				return degenerate.add(e);
			}
			size++;
			modCount++;
			if (size > elementData.length) {
				Object[] newData = new Object[size * 2];
				System.arraycopy(elementData, 0, newData, 0,
						elementData.length);
				elementData = newData;
				int[] newHashes = new int[size * 2];
				System.arraycopy(hashes, 0, newHashes, 0, hashes.length);
				hashes = newHashes;
			}
			elementData[size - 1] = e;
			hashes[size - 1] = Objects.hashCode(e);
			return true;
		}
	}

	@Override
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
			hashes = new int[INITIAL_SIZE];
			return true;
		}
		// A B C
		// A C
		System.arraycopy(elementData, idx + 1, elementData, idx, size - idx);
		elementData[size] = null;
		System.arraycopy(hashes, idx + 1, hashes, idx, size - idx);
		hashes[size] = 0;
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
		int test = Objects.hashCode(e);
		for (int idx = 0; idx < size; idx++) {
			if (hashes[idx] == test) {
				Object f = elementData[idx];
				if (Objects.equals(e, f)) {
					return idx;
				}
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
