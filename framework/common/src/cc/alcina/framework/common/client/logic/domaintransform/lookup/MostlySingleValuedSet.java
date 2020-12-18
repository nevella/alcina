package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 
 * @author nick@alcina.cc
 * 
 * @param <E>
 */
public class MostlySingleValuedSet<E> extends AbstractSet<E>
		implements Cloneable, Serializable {
	private int size = 0;

	private int modCount = 0;

	private Map<E, Boolean> map;

	private E soleValue;

	public MostlySingleValuedSet() {
	}

	public MostlySingleValuedSet(Collection<? extends E> c) {
		addAll(c);
	}

	@Override
	public boolean add(E value) {
		checkMustDegenerate();
		if (map != null) {
			return map.put(value, true) == null;
		}
		if (isEmpty()) {
			soleValue = value;
			modCount++;
			size++;
			return true;
		} else {
			if (!Objects.equals(value, soleValue)) {
				toDegenerateMap();
				return add(value);
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean contains(Object o) {
		if (map != null) {
			return map.containsKey(o);
		}
		if (isEmpty()) {
			return false;
		}
		return Objects.equals(soleValue, o);
	}

	@Override
	public Iterator<E> iterator() {
		if (map != null) {
			return map.keySet().iterator();
		}
		if (isEmpty()) {
			return Collections.EMPTY_SET.iterator();
		} else {
			return Collections.singleton(soleValue).iterator();
		}
	}

	@Override
	public boolean remove(Object o) {
		checkMustDegenerate();
		if (map != null) {
			return map.remove(o) != null;
		}
		if (isEmpty()) {
			return false;
		}
		if (!Objects.equals(o, soleValue)) {
			return false;
		}
		size--;
		modCount++;
		soleValue = null;
		return true;
	}

	@Override
	public int size() {
		if (map != null) {
			return map.size();
		}
		return size;
	}

	private void checkMustDegenerate() {
		if (map == null && mustDegenerate()) {
			toDegenerateMap();
		}
	}

	protected Map<E, Boolean> createDegenerateMap(E soleValue,
			boolean nonEmpty) {
		LinkedHashMap<E, Boolean> map = new LinkedHashMap<>();
		if (nonEmpty) {
			map.put(soleValue, true);
		}
		return map;
	}

	protected boolean mustDegenerate() {
		return false;
	}

	protected void toDegenerateMap() {
		map = createDegenerateMap(soleValue, size == 1);
		soleValue = null;
	}
}
