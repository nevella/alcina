package cc.alcina.framework.common.client.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CollectionCreators.MultiTrieCreator;
import cc.alcina.framework.common.client.util.trie.KeyAnalyzer;
import cc.alcina.framework.common.client.util.trie.MultiTrie;

public class TrieProjection<K, E extends Entity>
		implements DomainProjection<E> {
	private boolean enabled = true;

	MultiTrie<K, Set<E>> trie;

	private Class<E> entityClass;

	private Function<E, List<K>> keyMapper;

	public TrieProjection(KeyAnalyzer<? super K> keyAnalyzer,
			Class<E> entityClass, Function<E, List<K>> keyMapper) {
		this.entityClass = entityClass;
		this.keyMapper = keyMapper;
		trie = Registry.impl(MultiTrieCreator.class).create(keyAnalyzer,
				entityClass);
	}

	public void dumpDensity() {
		trie.dumpDensity();
	}

	@Override
	public Class<E> getListenedClass() {
		return entityClass;
	}

	public Stream<E> getSubstringMatches(K prefix) {
		SortedMap<K, Set<E>> prefixMap = trie.prefixMap(prefix);
		return prefixMap.entrySet().stream().map(Entry::getValue)
				.flatMap(Collection::stream).distinct();
	}

	@Override
	public void insert(E o) {
		List<K> keys = keyMapper.apply(o);
		for (K key : keys) {
			key = normalise(key);
			trie.add(key, o);
			List<K> subKeys = extractSubKeys(key);
			subKeys.forEach(k -> trie.add(k, o));
		}
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public void remove(E o) {
		List<K> keys = keyMapper.apply(o);
		for (K key : keys) {
			key = normalise(key);
			trie.remove(key, o);
			List<K> subKeys = extractSubKeys(key);
			subKeys.forEach(k -> trie.remove(k, o));
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	protected List<K> extractSubKeys(K key) {
		return Collections.emptyList();
	}

	protected K normalise(K key) {
		return key;
	}
}
