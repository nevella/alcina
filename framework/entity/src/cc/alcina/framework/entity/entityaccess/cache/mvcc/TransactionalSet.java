package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.AbstractSet;
import java.util.Iterator;

public class TransactionalSet<E> extends AbstractSet<E> {
	private TransactionalMap<E, Boolean> map;

	public TransactionalSet(Class<E> clazz) {
		this.map = new TransactionalMap<>(clazz, Boolean.class);
	}

	@Override
	public boolean add(E e) {
		return map.put(e, true) == null;
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
		return map.remove(o) != null;
	}

	@Override
	public int size() {
		return map.size();
	}
}
