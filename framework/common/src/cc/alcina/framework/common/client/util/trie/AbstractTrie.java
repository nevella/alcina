/*
 * Copyright 2005-2012 Roger Kapsi, Sam Berlin
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package cc.alcina.framework.common.client.util.trie;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;

/**
 * This class provides some basic {@link Trie} functionality and utility methods
 * for actual {@link Trie} implementations.
 */
abstract class AbstractTrie<K, V> extends AbstractMap<K, V>
		implements Serializable, Trie<K, V> {
	private static final long serialVersionUID = -6358111100045408883L;

	/**
	 * The {@link KeyAnalyzer} that's being used to build the PATRICIA
	 * {@link Trie}
	 */
	protected final KeyAnalyzer<? super K> keyAnalyzer;

	public AbstractTrie() {
		this(DefaultKeyAnalyzer.singleton());
	}

	/**
	 * Constructs a new {@link Trie} using the given {@link KeyAnalyzer}
	 */
	public AbstractTrie(KeyAnalyzer<? super K> keyAnalyzer) {
		this.keyAnalyzer = Tries.notNull(keyAnalyzer, "keyAnalyzer");
	}

	/**
	 * Returns the {@link KeyAnalyzer} that constructed the {@link Trie}.
	 */
	public KeyAnalyzer<? super K> getKeyAnalyzer() {
		return keyAnalyzer;
	}

	@Override
	public K selectKey(K key) {
		Map.Entry<K, V> entry = select(key);
		return entry != null ? entry.getKey() : null;
	}

	@Override
	public V selectValue(K key) {
		Map.Entry<K, V> entry = select(key);
		return entry != null ? entry.getValue() : null;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Trie[").append(size()).append("]={\n");
		for (Map.Entry<K, V> entry : entrySet()) {
			buffer.append("  ").append(entry).append("\n");
		}
		buffer.append("}\n");
		return buffer.toString();
	}

	private int bitIndex(K key) {
		int lengthInBits = lengthInBits(key);
		for (int i = 0; i < lengthInBits; i++) {
			if (isBitSet(key, i)) {
				return i;
			}
		}
		return KeyAnalyzer.NULL_BIT_KEY;
	}

	/**
	 * Utility method for calling {@link KeyAnalyzer#bitIndex(Object, Object)}
	 */
	final int bitIndex(K key, K otherKey) {
		if (key != null && otherKey != null) {
			return keyAnalyzer.bitIndex(key, otherKey);
		} else if (key != null && otherKey == null) {
			return bitIndex(key);
		} else if (key == null && otherKey != null) {
			return bitIndex(otherKey);
		}
		return KeyAnalyzer.NULL_BIT_KEY;
	}

	/**
	 * An utility method for calling {@link KeyAnalyzer#compare(Object, Object)}
	 */
	final boolean compareKeys(K key, K other) {
		if (key == null) {
			return (other == null);
		} else if (other == null) {
			return (key == null);
		}
		return keyAnalyzer.compare(key, other) == 0;
	}

	/**
	 * Returns whether or not the given bit on the key is set or false if the
	 * key is null.
	 * 
	 * @see KeyAnalyzer#isBitSet(Object, int)
	 */
	final boolean isBitSet(K key, int bitIndex) {
		if (key == null) { // root's might be null!
			return false;
		}
		return keyAnalyzer.isBitSet(key, bitIndex);
	}

	/**
	 * Returns the length of the given key in bits
	 * 
	 * @see KeyAnalyzer#lengthInBits(Object)
	 */
	final int lengthInBits(K key) {
		if (key == null) {
			return 0;
		}
		return keyAnalyzer.lengthInBits(key);
	}
}
