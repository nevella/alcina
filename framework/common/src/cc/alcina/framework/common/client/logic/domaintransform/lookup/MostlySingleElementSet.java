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
public class MostlySingleElementSet<E> extends AbstractSet<E>
		implements Cloneable, Serializable {
	private int size = 0;


	private Map<E, Boolean> map;

	private E element;

	public MostlySingleElementSet() {
	}

	public MostlySingleElementSet(Collection<? extends E> c) {
		addAll(c);
	}

	@Override
	public boolean add(E value) {
		checkMustDegenerate();
		if (map != null) {
			return map.put(value, true) == null;
		}
		if (isEmpty()) {
			element = value;
			size++;
			return true;
		} else {
			if (!Objects.equals(value, element)) {
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
		return Objects.equals(element, o);
	}

	@Override
	public Iterator<E> iterator() {
		if (map != null) {
			return map.keySet().iterator();
		}
		if (isEmpty()) {
			return Collections.EMPTY_SET.iterator();
		} else {
			return Collections.singleton(element).iterator();
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
		if (!Objects.equals(o, element)) {
			return false;
		}
		size--;
		element = null;
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
		synchronized (this) {
			if (map == null) {
				map = createDegenerateMap(element, size == 1);
				element = null;
			}
		}
	}
}
