package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class LiSet<H extends HasIdAndLocalId> extends AbstractSet<H> implements
		Cloneable, Serializable {
	static final transient long serialVersionUID = 1;

	private HasIdAndLocalId[] elementData;

	int size = 0;

	transient int modCount = 0;

	private Set<H> degenerate;

	static final transient int DEGENERATE_THRESHOLD = 30;

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
		if (size == DEGENERATE_THRESHOLD && degenerate == null) {
			degenerate = new LinkedHashSet<H>();
		}
		if (degenerate != null) {
			return degenerate.add(e);
		}
		if (isEmpty()) {
			elementData = new HasIdAndLocalId[1];
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
			size++;
			modCount++;
			HasIdAndLocalId[] newData = new HasIdAndLocalId[size];
			System.arraycopy(elementData, 0, newData, 0, idx);
			newData[idx] = e;
			System.arraycopy(elementData, idx, newData, idx + 1, size - idx - 1);
			elementData = newData;
			return true;
		}
	}

	public Object clone() {
		return new LiSet<H>(this);
	}

	@Override
	public boolean contains(Object o) {
		if (degenerate != null) {
			return degenerate.contains(o);
		}
		if (isEmpty()) {
			return false;
		}
		int idx = indexOf((HasIdAndLocalId) o);
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
		if (degenerate != null) {
			return degenerate.remove(o);
		}
		if (isEmpty()) {
			return false;
		}
		int idx = indexOf((HasIdAndLocalId) o);
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
		HasIdAndLocalId[] newData = new HasIdAndLocalId[size];
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

	private int compare(HasIdAndLocalId o1, HasIdAndLocalId o2) {
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

	private int indexOf(HasIdAndLocalId e) {
		int rangeMin = 0;
		int rangeMax = size;
		int arrayPos = 0;
		int res = 0;
		while (rangeMax > rangeMin) {
			arrayPos = (rangeMax - rangeMin) / 2 + rangeMin;
			HasIdAndLocalId f = elementData[arrayPos];
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
			HasIdAndLocalId f = elementData[rangeMax];
			if (e.equals(f)) {
				return rangeMax;
			}
		}
		if (res < 0) {
			if (arrayPos > 0) {
				HasIdAndLocalId d = elementData[arrayPos - 1];
				res = compare(e, d);
				if (res == -1) {
					return arrayPos - 1;
				}
			}
			return arrayPos;
		} else {
			if (arrayPos + 1 < size) {
				HasIdAndLocalId g = elementData[arrayPos + 1];
				res = compare(e, g);
				if (res == 1) {
					return arrayPos + 2;
				}
			}
			return arrayPos + 1;
		}
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
			LiSet.this.remove(elementData[--idx]);
			itrModCount++;
			nextCalled = false;
		}
	}
}
