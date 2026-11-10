package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;

public class JsUniqueSet<E> extends AbstractSet<E> {
	private static final Object PRESENT = new Object();

	public static <E> JsUniqueSet<E> create() {
		return new JsUniqueSet<>(null);
	}

	private Map<E, Object> map;

	public JsUniqueSet(Class keyClass) {
		this.map = (Map<E, Object>) JsUniqueMap.create();
	}

	@Override
	public boolean add(E e) {
		return map.put(e, PRESENT) == null;
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public boolean remove(Object o) {
		return map.remove(o) == PRESENT;
	}

	@Override
	public int size() {
		return map.size();
	}
}
