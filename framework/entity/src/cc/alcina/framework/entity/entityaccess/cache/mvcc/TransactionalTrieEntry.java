package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.trie.TrieEntry;

public class TransactionalTrieEntry<K, V> extends TrieEntry<K, V>
		implements MvccObject<TransactionalTrieEntry> {
	MvccObjectVersions<TransactionalTrieEntry> __mvccObjectVersions__;

	private Class<? extends Entity> entityClass;

	protected TransactionalTrieEntry(K key, V value, int bitIndex,
			Class<? extends Entity> entityClass) {
		super(key, value, bitIndex);
		this.entityClass = entityClass;
	}

	@Override
	public MvccObjectVersions<TransactionalTrieEntry> __getMvccVersions__() {
		return __mvccObjectVersions__;
	}

	@Override
	public void __setMvccVersions__(
			MvccObjectVersions<TransactionalTrieEntry> __mvccVersions__) {
		this.__mvccObjectVersions__ = __mvccVersions__;
	}

	@Override
	public boolean equals(Object o) {
		if (__mvccObjectVersions__ == null) {
			return super.equals(o);
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.equals(o);
		} else {
			return __instance__.equals(o);
		}
	}

	@Override
	public K getKey() {
		if (__mvccObjectVersions__ == null) {
			return super.getKey();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.getKey();
		} else {
			return __instance__.getKey();
		}
	}

	@Override
	public V getValue() {
		if (__mvccObjectVersions__ == null) {
			return super.getValue();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.getValue();
		} else {
			return __instance__.getValue();
		}
	}

	@Override
	public int hashCode() {
		if (__mvccObjectVersions__ == null) {
			return super.hashCode();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.hashCode();
		} else {
			return __instance__.hashCode();
		}
	}

	@Override
	public boolean isEmpty() {
		if (__mvccObjectVersions__ == null) {
			return super.isEmpty();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.isEmpty();
		} else {
			return __instance__.isEmpty();
		}
	}

	@Override
	public boolean isExternalNode() {
		if (__mvccObjectVersions__ == null) {
			return super.isExternalNode();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.isExternalNode();
		} else {
			return __instance__.isExternalNode();
		}
	}

	@Override
	public boolean isInternalNode() {
		if (__mvccObjectVersions__ == null) {
			return super.isInternalNode();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.isInternalNode();
		} else {
			return __instance__.isInternalNode();
		}
	}

	public Class<? extends Entity> provideEntityClass() {
		return this.entityClass;
	}

	@Override
	public V setKeyValue(K key, V value) {
		if (__mvccObjectVersions__ == null) {
			return super.setKeyValue(key, value);
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, true);
		if (__instance__ == this) {
			return super.setKeyValue(key, value);
		} else {
			return __instance__.setKeyValue(key, value);
		}
	}

	@Override
	public V setValue(V value) {
		if (__mvccObjectVersions__ == null) {
			return super.setValue(value);
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, true);
		if (__instance__ == this) {
			return super.setValue(value);
		} else {
			return __instance__.setValue(value);
		}
	}

	@Override
	public String toString() {
		if (__mvccObjectVersions__ == null) {
			return super.toString();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.toString();
		} else {
			return __instance__.toString();
		}
	}

	@Override
	protected int getBitIndex() {
		if (__mvccObjectVersions__ == null) {
			return super.getBitIndex();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.getBitIndex();
		} else {
			return __instance__.getBitIndex();
		}
	}

	@Override
	protected TrieEntry<K, V> getLeft() {
		if (__mvccObjectVersions__ == null) {
			return super.getLeft();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.getLeft();
		} else {
			return __instance__.getLeft();
		}
	}

	@Override
	protected TrieEntry<K, V> getParent() {
		if (__mvccObjectVersions__ == null) {
			return super.getParent();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.getParent();
		} else {
			return __instance__.getParent();
		}
	}

	@Override
	protected TrieEntry<K, V> getPredecessor() {
		if (__mvccObjectVersions__ == null) {
			return super.getPredecessor();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.getPredecessor();
		} else {
			return __instance__.getPredecessor();
		}
	}

	@Override
	protected TrieEntry<K, V> getRight() {
		if (__mvccObjectVersions__ == null) {
			return super.getRight();
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, false);
		if (__instance__ == this) {
			return super.getRight();
		} else {
			return __instance__.getRight();
		}
	}

	@Override
	protected void setBitIndex(int bitIndex) {
		if (__mvccObjectVersions__ == null) {
			super.setBitIndex(bitIndex);
			return;
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, true);
		if (__instance__ == this) {
			super.setBitIndex(bitIndex);
			return;
		} else {
			__instance__.setBitIndex(bitIndex);
			return;
		}
	}

	@Override
	protected void setKey(K key) {
		if (__mvccObjectVersions__ == null) {
			super.setKey(key);
			return;
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, true);
		if (__instance__ == this) {
			super.setKey(key);
			return;
		} else {
			__instance__.setKey(key);
			return;
		}
	}

	@Override
	protected void setLeft(TrieEntry<K, V> left) {
		if (__mvccObjectVersions__ == null) {
			super.setLeft(left);
			return;
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, true);
		if (__instance__ == this) {
			super.setLeft(left);
			return;
		} else {
			__instance__.setLeft(left);
			return;
		}
	}

	@Override
	protected void setParent(TrieEntry<K, V> parent) {
		if (__mvccObjectVersions__ == null) {
			super.setParent(parent);
			return;
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, true);
		if (__instance__ == this) {
			super.setParent(parent);
			return;
		} else {
			__instance__.setParent(parent);
			return;
		}
	}

	@Override
	protected void setPredecessor(TrieEntry<K, V> predecessor) {
		if (__mvccObjectVersions__ == null) {
			super.setPredecessor(predecessor);
			return;
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, true);
		if (__instance__ == this) {
			super.setPredecessor(predecessor);
			return;
		} else {
			__instance__.setPredecessor(predecessor);
			return;
		}
	}

	@Override
	protected void setRight(TrieEntry<K, V> right) {
		if (__mvccObjectVersions__ == null) {
			super.setRight(right);
			return;
		}
		TransactionalTrieEntry<K, V> __instance__ = Transactions
				.resolveTrie(this, true);
		if (__instance__ == this) {
			super.setRight(right);
			return;
		} else {
			__instance__.setRight(right);
			return;
		}
	}
}
