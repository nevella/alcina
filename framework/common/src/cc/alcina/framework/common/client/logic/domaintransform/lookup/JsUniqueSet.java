package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;

public class JsUniqueSet<E> extends AbstractSet<E> {
	private static final Object PRESENT = new Object();

	private Map<E, Object> map;

	public JsUniqueSet(Class keyClass) {
		this.map = (Map<E, Object>) JsUniqueMap.create(keyClass, false);
	}

	public boolean add(E e) {
		return map.put(e, PRESENT) == null;
	}

	public void clear() {
		map.clear();
	}

	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	public boolean remove(Object o) {
		return map.remove(o) == PRESENT;
	}

	public int size() {
		return map.size();
	}
}
