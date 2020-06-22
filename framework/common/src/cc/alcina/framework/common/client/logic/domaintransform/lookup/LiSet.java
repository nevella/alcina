package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;

/**
 * 
 * @author nick@alcina.cc
 * 
 * @param <H>
 */
public class LiSet<H extends Entity> extends AbstractSet<H>
		implements Cloneable, Serializable {
	static final transient long serialVersionUID = 1;

	static final transient int DEGENERATE_THRESHOLD = 30;

	public static <H extends Entity> LiSet<H> of(H h) {
		LiSet<H> result = new LiSet<>();
		result.add(h);
		return result;
	}

	private transient Entity[] elementData;

	transient int size = 0;

	transient int modCount = 0;

	private transient Set<H> degenerate;

	public LiSet() {
	}

	public LiSet(Collection<? extends H> c) {
		addAll(c);
	}

	@Override
	public boolean add(H e) {
		if (e == null) {
			throw new IllegalArgumentException();
		}
		if (degenerate != null) {
			boolean added = degenerate.add(e);
			if (added) {
				modCount++;
			}
			return added;
		}
		if (e.domain().isNonDomain()) {
			// can't handle non-comparables
			return toDegenerate(e);
		}
		if (isEmpty()) {
			elementData = new Entity[1];
			elementData[0] = e;
			size++;
			modCount++;
			return false;
		}
		int idx = indexOf(e);
		if (idx < size && elementData[idx].equals(e)) {
			elementData[idx] = e;
			return false;
		} else {
			if (size == DEGENERATE_THRESHOLD) {
				return toDegenerate(e);
			}
			size++;
			modCount++;
			Entity[] newData = new Entity[size];
			System.arraycopy(elementData, 0, newData, 0, idx);
			newData[idx] = e;
			System.arraycopy(elementData, idx, newData, idx + 1,
					size - idx - 1);
			elementData = newData;
			return true;
		}
	}

	@Override
	public Object clone() {
		return new LiSet<H>(this);
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
		int idx = indexOf((Entity) o);
		if (idx == size) {
			return false;
		}
		return o.equals(elementData[idx]);
	}

	@Override
	public Iterator<H> iterator() {
		if (degenerate != null) {
			return degenerate.iterator();
		}
		return new LiSetIterator();
	}

	@Override
	public boolean remove(Object o) {
		if (o == null) {
			return false;
		}
		if (degenerate != null) {
			boolean removed = degenerate.remove(o);
			if (removed) {
				modCount++;
			}
			return removed;
		}
		if (isEmpty()) {
			return false;
		}
		int idx = indexOf((Entity) o);
		if (idx == size) {
			return false;
		}
		if (!o.equals(elementData[idx])) {
			return false;
		}
		size--;
		modCount++;
		if (size == 0) {
			elementData = null;
			return true;
		}
		Entity[] newData = new Entity[size];
		System.arraycopy(elementData, 0, newData, 0, idx);
		System.arraycopy(elementData, idx + 1, newData, idx, size - idx);
		elementData = newData;
		return true;
	}

	@Override
	public int size() {
		if (degenerate != null) {
			return degenerate.size();
		}
		return size;
	}

	private int compare(Entity o1, Entity o2) {
		if (o1.getId() < o2.getId()) {
			return -1;
		}
		if (o1.getId() > o2.getId()) {
			return 1;
		}
		if (o1.getLocalId() < o2.getLocalId()) {
			return -1;
		}
		if (o1.getLocalId() > o2.getLocalId()) {
			return 1;
		}
		return 0;
	}

	private int indexOf(Entity e) {
		int rangeMin = 0;
		int rangeMax = size;
		int arrayPos = 0;
		int res = 0;
		while (rangeMax > rangeMin) {
			arrayPos = (rangeMax - rangeMin) / 2 + rangeMin;
			Entity f = elementData[arrayPos];
			res = compare(e, f);
			if (res == 0) {
				return arrayPos;
			}
			if (res == -1) {
				rangeMax = arrayPos - 1;
			} else {
				rangeMin = arrayPos + 1;
			}
		}
		// we want the least elt f that is greater than e
		// if res>0 e>f -- but possibly e>g (elementData[arrayPos+1]) - limits
		// of binary search.
		// if res<0, e<f - but possibly e<d (elementData[arrayPos-1]) - limits
		// of binary search.
		if (rangeMax < size && rangeMax >= 0) {
			Entity f = elementData[rangeMax];
			if (e.equals(f)) {
				return rangeMax;
			}
		}
		if (res < 0) {
			if (arrayPos > 0) {
				Entity d = elementData[arrayPos - 1];
				res = compare(e, d);
				if (res == -1) {
					return arrayPos - 1;
				}
			}
			return arrayPos;
		} else {
			if (arrayPos + 1 < size) {
				Entity g = elementData[arrayPos + 1];
				res = compare(e, g);
				if (res == 1) {
					return arrayPos + 2;
				}
			}
			return arrayPos + 1;
		}
	}

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

	protected boolean toDegenerate(H e) {
		LinkedHashSet degenerate = new LinkedHashSet<H>();
		degenerate.addAll(this);
		this.degenerate = degenerate;
		elementData = null;
		size = -1;
		return degenerate.add(e);
	}

	class LiSetIterator implements Iterator<H> {
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
			if (idx >= size) {
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
			LiSet.this.remove(elementData[--idx]);
			itrModCount++;
			nextCalled = false;
		}
	}
}
