package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Multiset;

public class TransactionalMultiset<K, V>
		extends Multiset<K, Set<V>> {
	@SuppressWarnings("unused")
	private Class<K> keyClass;

	private Class<V> valueClass;

	public TransactionalMultiset(Class<K> keyClass, Class<V> valueClass) {
		this.keyClass = keyClass;
		this.valueClass = valueClass;
		map = new TransactionalMap(keyClass, Set.class);
	}

	@Override
	protected Set<V> createSet() {
		Class<? extends Entity> entityClass = (Class<? extends Entity>) valueClass;
		return new TransactionalSet(entityClass);
	}

	@Override
	protected void createTopMap() {
		// do it in *our* init (not the superclasses')
	}
}