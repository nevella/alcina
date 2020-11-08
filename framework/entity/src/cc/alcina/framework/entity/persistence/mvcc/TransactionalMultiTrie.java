package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.trie.KeyAnalyzer;
import cc.alcina.framework.common.client.util.trie.MultiTrie;
import cc.alcina.framework.common.client.util.trie.TrieEntry;

public class TransactionalMultiTrie<K, E extends Entity, V extends Set<E>>
		extends MultiTrie<K, V> {
	private Class<E> entityClass;

	public TransactionalMultiTrie(KeyAnalyzer<? super K> keyAnalyzer,
			Class<E> entityClass) {
		super(keyAnalyzer);
		this.entityClass = entityClass;
		((TransactionalTrieEntry) root).entityClass = entityClass;
	}

	@Override
	protected V createNewSet() {
		return (V) new TransactionalSet<E>(entityClass);
	}

	@Override
	protected TrieEntry<K, V> createTrieEntry(K key, V value, int bitIndex) {
		return new TransactionalTrieEntry<K, V>(key, value, bitIndex,
				entityClass);
	}
}
