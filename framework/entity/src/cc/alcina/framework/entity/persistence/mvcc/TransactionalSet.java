package cc.alcina.framework.entity.persistence.mvcc;

import java.util.AbstractSet;
import java.util.Iterator;

import cc.alcina.framework.common.client.logic.domain.Entity;

/*
 * 
 * 
 */
public class TransactionalSet<E extends Entity> extends AbstractSet<E>
		implements TransactionalCollection {
	private Class<E> entityClass;

	private TransactionalMap<E, Boolean> map;

	public TransactionalSet(Class<E> entityClass) {
		this.entityClass = entityClass;
		this.map = new TransactionalMap<>(entityClass, Boolean.class);
	}

	// for copying
	TransactionalSet() {
	}

	@Override
	public boolean add(E o) {
		return map.put(o, Boolean.TRUE) == null;
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	public Class<E> entityClass() {
		return entityClass;
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
