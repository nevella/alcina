package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domain.Entity;

/**
 * 
 * 
 * 
 * <p>
 * Uses ordering (by id or localid) to speed 'find' ops. This is *useful* even
 * though 'index' is mostly used to improve performance in add() because we have
 * to check we're not adding an element that already exists in the set.
 * 
 * <p>
 * This class handles 'promoted' objects (local->global)
 * 
 * <p>
 * This class should be used as the main mvcc referenced entity class - it's
 * designed for contention - after some obscure issues with reads on other
 * threads immediately after writes, marking all fields as volatile (rather than
 * the entity fields themselves - a much bigger job) resolved these issues
 * 
 * <p>
 * This class is not designed for field-level serialization (the fields are
 * transient) - but all serialization frameworks (except JVM) use iterator-based
 * serialization for the general serializer case, so serialize without issues
 * 
 * @param <H>
 */
public class LiSet<H extends Entity> extends AbstractSet<H>
		implements Cloneable, Serializable {
	public static class DegenerateCreator {
		public Set copy(Set degenerate) {
			throw new UnsupportedOperationException();
		}

		public Set create() {
			return new LinkedHashSet<>();
		}
	}

	public interface NonDomainNotifier {
		public void notifyNonDomain(LiSet liSet, Entity e);
	}

	public static class TestComparator implements Comparator<Entity> {
		@Override
		public int compare(Entity o1, Entity o2) {
			return LiSet.compare(o1, o2);
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

	public static final transient String CONTEXT_NON_DOMAIN_NOTIFIER = LiSet.class
			.getName() + ".CONTEXT_NON_DOMAIN_NOTIFIER";

	static final transient int DEGENERATE_THRESHOLD = 30;

	public static transient DegenerateCreator degenerateCreator = new DegenerateCreator();

	public static <H extends Entity> LiSet<H> of(H h) {
		LiSet<H> result = new LiSet<>();
		result.add(h);
		return result;
	}

	private static int compare(Entity o1, Entity o2) {
		if (o1.getLocalId() != 0 || o2.getLocalId() != 0) {
			if (o2.getLocalId() == 0) {
				return -1;
			}
			if (o1.getLocalId() == 0) {
				return 1;
			}
			// localId is guaranteed < Integer.MAX_VALUE
			return (int) (o1.getLocalId() - o2.getLocalId());
		}
		if (o1.getId() < o2.getId()) {
			return -1;
		}
		if (o1.getId() > o2.getId()) {
			return 1;
		}
		return 0;
	}

	private transient volatile Entity[] elementData;

	transient volatile int size = 0;

	transient volatile int modCount = 0;

	private transient volatile Set<H> degenerate;

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
		if (e.domain().isZeroIds()) {
			// can't handle non-comparables
			if (!GWT.isClient()
					&& LooseContext.has(CONTEXT_NON_DOMAIN_NOTIFIER)) {
				NonDomainNotifier notifier = LooseContext
						.get(CONTEXT_NON_DOMAIN_NOTIFIER);
				notifier.notifyNonDomain(this, e);
			}
			toDegenerate();
			return degenerate.add(e);
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
				toDegenerate();
				return degenerate.add(e);
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
	public boolean addAll(Collection<? extends H> c) {
		if (c.size() >= DEGENERATE_THRESHOLD) {
			toDegenerate();
		}
		return super.addAll(c);
	}

	@Override
	public LiSet clone() {
		if (GWT.isClient()) {
			return new LiSet(this);
		}
		try {
			LiSet clone = (LiSet) super.clone();
			if (clone.elementData != null) {
				clone.elementData = Arrays.copyOf(clone.elementData,
						clone.elementData.length);
			}
			if (clone.degenerate != null) {
				clone.degenerate = degenerateCreator.copy(clone.degenerate);
			}
			return clone;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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

	protected void toDegenerate() {
		Set degenerate = degenerateCreator.create();
		degenerate.addAll(this);
		this.degenerate = degenerate;
		elementData = null;
		size = -1;
	}

	/*
	 * If elementData contains e, returns the index of e. If not, return index
	 * of least f gt e (or value of field 'size' if no f gt e)
	 * 
	 */
	private int indexOf(Entity e) {
		int rangeMin = 0;
		// open range - i.e. rangeMax is guaranteed gt target index (unless
		// target index==size)
		int rangeMax = size;
		int arrayPos = 0;
		int res = 0;
		if (size == 0) {
			return 0;
		}
		while (true) {
			arrayPos = (rangeMax - rangeMin) / 2 + rangeMin;
			Entity f = elementData[arrayPos];
			res = compare(e, f);
			if (res == 0) {
				return arrayPos;
			}
			if (rangeMax == rangeMin) {
				return arrayPos;
			} else {
				if (res < 0) {
					rangeMax = arrayPos;
				} else {
					rangeMin = arrayPos + 1;
				}
				if (rangeMin >= size) {
					// no elt f gt e
					return size;
				}
			}
		}
	}
}
