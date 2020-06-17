package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
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
	private int minSubstringLength;

	private int maxSubstringLength;

	private boolean usesSubstrings;

	private int minimumNonExactLength = 0;

	public StringTrieProjection(Class<E> entityClass,
			Function<E, Stream<String>> keyMapper) {
		super(StringKeyAnalyzer.CHAR, entityClass,
				keyMapper.andThen(stream -> stream.filter(Ax::notBlank)));
	}

	@Override
	public Stream<E> getSubstringMatches(String prefix) {
		String normalisedPrefix = normalise(prefix);
		if (usesSubstrings) {
			if (maxSubstringLength == 0
					|| normalisedPrefix.length() <= maxSubstringLength) {
				return super.getSubstringMatches(normalisedPrefix);
			}
			String trimmedPrefix = normalisedPrefix.substring(0,
					maxSubstringLength);
			Predicate<String> keyFilter = k -> k.contains(normalisedPrefix);
			return trie.prefixMap(trimmedPrefix).entrySet().stream()
					.filter(e -> keyFilter.test(e.getKey()))
					.map(Entry::getValue).flatMap(Collection::stream)
					.distinct();
		} else {
			if (normalisedPrefix.length() < minimumNonExactLength) {
				Set<E> set = trie.get(normalisedPrefix);
				return set == null ? Stream.empty() : set.stream();
			} else {
				return super.getSubstringMatches(normalisedPrefix);
			}
		}
	}

	public <STP extends StringTrieProjection> STP withInternalSubstrings(
			int minSubstringLength, int maxSubstringLength) {
		this.minSubstringLength = minSubstringLength;
		this.maxSubstringLength = maxSubstringLength;
		this.usesSubstrings = true;
		return (STP) this;
	}

	public <STP extends StringTrieProjection> STP
			withMinimumNonExactLength(int minimumNonExactLength) {
		this.minimumNonExactLength = minimumNonExactLength;
		return (STP) this;
	}

	@Override
	protected List<String> extractSubKeys(String key) {
		if (usesSubstrings) {
			List<String> subKeys = new ArrayList<>();
			for (int idx = 0; idx < key.length() - minSubstringLength; idx++) {
				int to = Math.min(maxSubstringLength, key.length() - idx);
				subKeys.add(key.substring(idx, idx + to));
			}
			return subKeys;
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	protected String normalise(String key) {
		return key == null ? null : key.toLowerCase();
	}
}
