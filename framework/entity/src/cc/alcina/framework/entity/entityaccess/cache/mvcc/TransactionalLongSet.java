package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.AbstractSet;
import java.util.Iterator;

public class TransactionalLongSet extends AbstractSet<Long> {
	private TransactionalMap<Long, Boolean> map;

	public TransactionalLongSet() {
		this.map = new TransactionalMap<>(Long.class, Boolean.class);
	}

	@Override
	public boolean add(Long e) {
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
	public Iterator<Long> iterator() {
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
