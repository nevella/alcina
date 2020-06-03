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

/*
 * Notes on filter cost estimation
 * 
 * 
 * 
 * frac returned:
track per-length key count
tract distinct per-length key count
if (contact,3) == 20 && count(contact)==2

if(per-length == distinct per-length) - fraction = 1/size
if(per-length == 2xdistinct per-length) - fraction = 2/size

so roughly per-length/disintct per-length / size for length n

per outgoing entity cost:1
per incoming: 0

// TODO  But...until we have
			// framework level option to evaulate without lookup, it's always
			// going to make sense to put trie projections first
 */
public class StringTrieProjection<E extends Entity>
		extends TrieProjection<String, E> {
	private static boolean fastStringTrieProjections;

	public static boolean isFastStringTrieProjections() {
		return fastStringTrieProjections;
	}

	public static void
			setFastStringTrieProjections(boolean fastStringTrieProjections) {
		StringTrieProjection.fastStringTrieProjections = fastStringTrieProjections;
	}

	private int minSubstringLength;

	private int maxSubstringLength;

	public StringTrieProjection(Class<E> entityClass,
			Function<E, List<String>> keyMapper, int minSubstringLength,
			int maxSubstringLength) {
		super(StringKeyAnalyzer.CHAR, entityClass, keyMapper);
		this.minSubstringLength = minSubstringLength;
		this.maxSubstringLength = maxSubstringLength;
		if (fastStringTrieProjections) {
			this.minSubstringLength = Math.max(this.minSubstringLength, 4);
			this.maxSubstringLength = this.minSubstringLength;
		}
	}

	@Override
	public Stream<E> getSubstringMatches(String prefix) {
		String normalisedPrefix = normalise(prefix);
		if (maxSubstringLength == 0
				|| normalisedPrefix.length() <= maxSubstringLength) {
			return super.getSubstringMatches(normalisedPrefix);
		}
		String trimmedPrefix = normalisedPrefix.substring(0,
				maxSubstringLength);
		Predicate<String> keyFilter = k -> k.contains(normalisedPrefix);
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
			if (fastStringTrieProjections) {
				break;
			}
		}
		return subKeys;
	}

	@Override
	protected String normalise(String key) {
		return key == null ? null : key.toLowerCase();
	}
}
