package cc.alcina.framework.common.client.util.trie;

import java.util.Map;

/**
 * A {@link Trie} is a set of {@link TrieEntry} nodes
 */
public class TrieEntry<K, V> implements Map.Entry<K, V> {
	/** The index this entry is comparing. */
	private int bitIndex;

	/** The parent of this entry. */
	private TrieEntry<K, V> parent;

	/** The left child of this entry. */
	private TrieEntry<K, V> left;

	/** The right child of this entry. */
	private TrieEntry<K, V> right;

	/** The entry who uplinks to this entry. */
	private TrieEntry<K, V> predecessor;

	private K key;

	private V value;

	private transient int hashCode = 0;

	protected TrieEntry(K key, V value, int bitIndex) {
		this.key = key;
		this.value = value;
		this.bitIndex = bitIndex;
		this.parent = null;
		this.left = this;
		this.right = null;
		this.predecessor = this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof Map.Entry<?, ?>)) {
			return false;
		}
		Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
		if (Tries.areEqual(key, other.getKey())
				&& Tries.areEqual(value, other.getValue())) {
			return true;
		}
		return false;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = (key != null ? key.hashCode() : 0);
		}
		return hashCode;
	}

	/**
	 * Whether or not the entry is storing a key. Only the root can potentially
	 * be empty, all other nodes must have a key.
	 */
	public boolean isEmpty() {
		return key == null;
	}

	/**
	 * Either the left or right child is a loopback
	 */
	public boolean isExternalNode() {
		return !isInternalNode();
	}

	/**
	 * Neither the left nor right child is a loopback
	 */
	public boolean isInternalNode() {
		return left != this && right != this;
	}

	/**
	 * Replaces the current key and value with the provided key &amp; value
	 */
	public V setKeyValue(K key, V value) {
		this.key = key;
		this.hashCode = 0;
		return setValue(value);
	}

	@Override
	public V setValue(V value) {
		V previous = this.value;
		this.value = value;
		return previous;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		if (bitIndex == -1) {
			buffer.append("RootEntry(");
		} else {
			buffer.append("Entry(");
		}
		buffer.append("key=").append(getKey()).append(" [").append(bitIndex)
				.append("], ");
		buffer.append("value=").append(getValue()).append(", ");
		// buffer.append("bitIndex=").append(bitIndex).append(", ");
		if (parent != null) {
			if (parent.bitIndex == -1) {
				buffer.append("parent=").append("ROOT");
			} else {
				buffer.append("parent=").append(parent.getKey()).append(" [")
						.append(parent.bitIndex).append("]");
			}
		} else {
			buffer.append("parent=").append("null");
		}
		buffer.append(", ");
		if (left != null) {
			if (left.bitIndex == -1) {
				buffer.append("left=").append("ROOT");
			} else {
				buffer.append("left=").append(left.getKey()).append(" [")
						.append(left.bitIndex).append("]");
			}
		} else {
			buffer.append("left=").append("null");
		}
		buffer.append(", ");
		if (right != null) {
			if (right.bitIndex == -1) {
				buffer.append("right=").append("ROOT");
			} else {
				buffer.append("right=").append(right.getKey()).append(" [")
						.append(right.bitIndex).append("]");
			}
		} else {
			buffer.append("right=").append("null");
		}
		buffer.append(", ");
		if (predecessor != null) {
			if (predecessor.bitIndex == -1) {
				buffer.append("predecessor=").append("ROOT");
			} else {
				buffer.append("predecessor=").append(predecessor.getKey())
						.append(" [").append(predecessor.bitIndex).append("]");
			}
		}
		buffer.append(")");
		return buffer.toString();
	}

	protected int getBitIndex() {
		return bitIndex;
	}

	protected void setBitIndex(int bitIndex) {
		this.bitIndex = bitIndex;
	}

	protected TrieEntry<K, V> getParent() {
		return parent;
	}

	protected void setParent(TrieEntry<K, V> parent) {
		this.parent = parent;
	}

	protected TrieEntry<K, V> getLeft() {
		return left;
	}

	protected void setLeft(TrieEntry<K, V> left) {
		this.left = left;
	}

	protected TrieEntry<K, V> getRight() {
		return right;
	}

	protected void setRight(TrieEntry<K, V> right) {
		this.right = right;
	}

	protected TrieEntry<K, V> getPredecessor() {
		return predecessor;
	}

	protected void setPredecessor(TrieEntry<K, V> predecessor) {
		this.predecessor = predecessor;
	}

	protected void setKey(K key) {
		this.key = key;
	}
}