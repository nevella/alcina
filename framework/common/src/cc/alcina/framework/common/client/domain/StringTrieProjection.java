package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.trie.StringKeyAnalyzer;

public class StringTrieProjection<E extends Entity>
		extends TrieProjection<String, E> {
	private int minSubstringLength;

	private int maxSubstringLength;

	public StringTrieProjection(Class<E> entityClass,
			Function<E, String> keyMapper, int minSubstringLength,
			int maxSubstringLength) {
		super(StringKeyAnalyzer.CHAR, entityClass, keyMapper);
		this.minSubstringLength = minSubstringLength;
		this.maxSubstringLength = maxSubstringLength;
	}

	@Override
	public Stream<E> getSubstringMatches(String prefix) {
		if (maxSubstringLength == 0 || prefix.length() <= maxSubstringLength) {
			return super.getSubstringMatches(prefix);
		}
		String trimmedPrefix = prefix.substring(0, maxSubstringLength);
		Predicate<String> keyFilter = k -> k.contains(prefix);
		return trie.prefixMap(trimmedPrefix).entrySet().stream()
				.filter(e -> keyFilter.test(e.getKey())).map(Entry::getValue)
				.flatMap(Collection::stream).distinct();
	}

	@Override
	protected List<String> extractSubKeys(String key) {
		if (minSubstringLength == 0) {
			return Collections.emptyList();
		}
		List<String> subKeys = new ArrayList<>();
		for (int idx = 0; idx < key.length() - minSubstringLength; idx++) {
			int to = Math.min(maxSubstringLength, key.length() - idx);
			subKeys.add(key.substring(idx, idx + to));
		}
		return subKeys;
	}
}
