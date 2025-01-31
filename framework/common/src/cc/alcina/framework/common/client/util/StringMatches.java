package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StringMatches {
	public static class PartialSubstring<T> {
		public class Match implements Comparable<Match> {
			T value;

			Match(T value, String src, String query) {
				this.value = value;
				this.src = src;
				this.query = query;
			}

			public T getValue() {
				return value;
			}

			String src;

			String query;

			public List<Region> regions = new ArrayList<>();

			boolean matches;

			boolean exact() {
				return query.length() == src.length();
			}

			public class Region {
				public IntPair srcRange = new IntPair();

				public IntPair queryRange = new IntPair();

				public String srcPart() {
					return src.substring(srcRange.i1, srcRange.i2);
				}

				public String queryPart() {
					return query.substring(queryRange.i1, queryRange.i2);
				}

				void extend() {
					for (;;) {
						if (queryRange.i2 == query.length()) {
							return;
						}
						if (srcRange.i2 == src.length()) {
							return;
						}
						String q = query.substring(queryRange.i2,
								queryRange.i2 + 1);
						String s = src.substring(srcRange.i2, srcRange.i2 + 1);
						if (q.equalsIgnoreCase(s)) {
							srcRange.i2++;
							queryRange.i2++;
						} else {
							// didn't match query char against src char, shift
							// src cursor right 1 char
							if (queryRange.isPoint()) {
								srcRange.i1++;
								srcRange.i2++;
							} else {
								// no further matches this region
								return;
							}
						}
					}
				}

				boolean isEmpty() {
					return srcRange.isPoint();
				}

				void startAfter(Region previous) {
					srcRange = new IntPair(previous.srcRange.i2,
							previous.srcRange.i2);
					queryRange = new IntPair(previous.queryRange.i2,
							previous.queryRange.i2);
				}
			}

			public boolean matches() {
				if (requireFirstLetterMatch) {
					if (src.length() > 0 && query.length() > 0
							&& !src.substring(0, 1)
									.equalsIgnoreCase(query.substring(0, 1))) {
						return false;
					}
				}
				regions.add(new Region());
				for (;;) {
					Region current = Ax.last(regions);
					current.extend();
					if (current.isEmpty()) {
						return false;
					} else {
						if (current.queryRange.i2 == query.length()) {
							longestRegion = regions.stream()
									.map(r -> r.srcPart().length())
									.max(Comparator.naturalOrder()).get();
							return true;
						}
					}
					Region next = new Region();
					next.startAfter(current);
					regions.add(next);
				}
			}

			@Override
			public int compareTo(Match o) {
				{
					int cmp = longestRegion - o.longestRegion;
					if (cmp != 0) {
						return -cmp;
					}
				}
				{
					return 0;
				}
			}

			int longestRegion;
		}

		boolean matchPartialsIfExactMatch = false;

		boolean requireFirstLetterMatch = true;

		public StringMatches.PartialSubstring withMatchPartialsIfExactMatch(
				boolean matchPartialsIfExactMatch) {
			this.matchPartialsIfExactMatch = matchPartialsIfExactMatch;
			return this;
		}

		public StringMatches.PartialSubstring
				withRequireFirstLetterMatch(boolean requireFirstLetterMatch) {
			this.requireFirstLetterMatch = requireFirstLetterMatch;
			return this;
		}

		public List<Match> match(List<T> values, Function<T, String> toString,
				String query) {
			List<PartialSubstring<T>.Match> result = values.stream()
					.map(v -> match(v, toString, query))
					.filter(Objects::nonNull).sorted()
					.collect(Collectors.toList());
			if (!matchPartialsIfExactMatch
					&& result.stream().anyMatch(r -> r.exact())) {
				result.removeIf(r -> !r.exact());
			}
			return result;
		}

		Match match(T v, Function<T, String> toString, String query) {
			String str = toString.apply(v);
			Match match = new Match(v, str, query);
			if (match.matches()) {
				return match;
			} else {
				return null;
			}
		}
	}
}
