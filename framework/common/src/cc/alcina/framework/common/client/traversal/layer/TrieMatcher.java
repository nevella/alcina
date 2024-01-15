package cc.alcina.framework.common.client.traversal.layer;

import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.TrieMatcher.MatchCondition;
import cc.alcina.framework.common.client.traversal.layer.TrieMatcher.MatchTest.TrieTest;
import cc.alcina.framework.common.client.traversal.layer.TrieMatcher.StartBoundaryTest.WordCharacter;
import cc.alcina.framework.common.client.util.trie.Trie;

/**
 * <p>
 * This class matches a string against a MultiTrie-like data structure.
 * 
 * <p>
 * The algorithm is:
 * 
 * <pre>
 * - traverse to a start character (non-ws, punctuation for example)
 * - find the longest match in the trie
 * - check not ambiguous
 * - repeat until matched
 * </pre>
 */
public class TrieMatcher extends LookaheadMatcher<MatchCondition> {
	public TrieMatcher(ParserState parserState) {
		super(parserState);
	}

	public static class MatchCondition {
		Trie<String, Set> trie;

		public MatchCondition(Trie<String, Set> trie) {
			this.trie = trie;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof MatchCondition) {
				MatchCondition o = (MatchCondition) obj;
				return trie == o.trie;
			} else {
				return false;
			}
		}

		StartBoundaryTest startBoundaryTest = new WordCharacter();

		MatchTest matchTest = new TrieTest();

		public Disambiguator disambiguator = new Disambiguator.Simple();

		public MatchTest.Result getLongestMatch(CharSequence sequence) {
			return matchTest.getLongestMatch(sequence, this);
		}

		public MatchTest.Result lastMatch;

		public InputMapper inputMapper = new InputMapper.ToString();
	}

	public interface InputMapper {
		String mapInput(CharSequence input);

		public static class ToString implements InputMapper {
			@Override
			public String mapInput(CharSequence input) {
				return input.toString().replace('\u00a0', ' ');
			}
		}

		public static class ToLowerCase implements InputMapper {
			@Override
			public String mapInput(CharSequence input) {
				return input.toString().replace('\u00a0', ' ').toLowerCase();
			}
		}
	}

	public interface MatchTest {
		public static class Result {
			public String key;

			public Object value;

			public Result(String key, Object value) {
				this.key = key;
				this.value = value;
			}
		}

		MatchTest.Result getLongestMatch(CharSequence sequence,
				MatchCondition matchCondition);

		public static class TrieTest implements MatchTest {
			@Override
			public MatchTest.Result getLongestMatch(CharSequence sequence,
					MatchCondition condition) {
				// todo - this can be optimised by customising the trie - ditto
				// the lcase conversion
				String toMatch = condition.inputMapper.mapInput(sequence);
				SortedMap<String, Set> longestPrefixMap = null;
				int len = 2;
				for (; len < sequence.length(); len++) {
					SortedMap<String, Set> prefixMap = condition.trie
							.prefixMap(toMatch.subSequence(0, len).toString());
					if (prefixMap != null && prefixMap.size() > 0) {
						longestPrefixMap = prefixMap;
					} else {
						break;
					}
				}
				int f_len = len - 1;
				if (longestPrefixMap != null) {
					Entry<String, Set> entry = longestPrefixMap.entrySet()
							.stream().filter(e -> e.getKey().length() == f_len)
							.findFirst().orElse(null);
					if (entry != null) {
						Object bestMatch = condition.disambiguator
								.getBestMatch(entry.getValue());
						if (bestMatch != null) {
							String key = entry.getKey();
							// test expand to mark ", " as captured. todo -
							// abstract (actually - mark as complete segment
							// match although it isn't quite)
							String sequenceTerminator = sequence.subSequence(
									key.length(), sequence.length()).toString();
							if (sequenceTerminator.matches("[, ]+")) {
								key = sequence.toString();
							}
							return new MatchTest.Result(key, bestMatch);
						}
					}
				}
				return null;
			}
		}
	}

	public interface Disambiguator<V> {
		V getBestMatch(Set<V> set);

		public static class Simple<V> implements Disambiguator<V> {
			@Override
			public V getBestMatch(Set<V> set) {
				return set.size() == 1 ? set.iterator().next() : null;
			}
		}
	}

	public class TrieOptions extends Options {
		TrieOptions(BranchToken token, MatchCondition condition) {
			super(token, condition);
		}
	}

	public interface StartBoundaryTest {
		boolean isNonStart(char c);

		public static class WordCharacter implements StartBoundaryTest {
			@Override
			public boolean isNonStart(char c) {
				if (c >= 'a' && c <= 'z') {
					return false;
				}
				if (c >= 'A' && c <= 'Z') {
					return false;
				}
				if (c >= '0' && c <= '9') {
					return false;
				}
				return true;
			}
		}
	}

	class MatchResultImpl implements MatchResult {
		int start = 0;

		int end;

		CharSequence text;

		MatchTest.Result longestMatch;

		@Override
		public void populateMeasureData(Measure match) {
			match.setData(longestMatch.value);
		}

		public MatchResultImpl(LocationMatcher locationMatcher,
				CharSequence text) {
			int idx = 0;
			MatchCondition condition = locationMatcher.options.condition;
			int length = text.length();
			for (; idx < length; idx++) {
				char c = text.charAt(idx);
				if (condition.startBoundaryTest.isNonStart(c)) {
					continue;
				}
				longestMatch = condition
						.getLongestMatch(text.subSequence(idx, length));
				if (longestMatch != null) {
					start = idx;
					end = idx + longestMatch.key.length();
					return;
				}
			}
			start = -1;
		}

		@Override
		public boolean found() {
			return start != -1;
		}

		@Override
		public int start() {
			return start;
		}

		@Override
		public int end() {
			return end;
		}
	}

	@Override
	protected MatchResult matchText(LocationMatcher locationMatcher,
			CharSequence text) {
		return new MatchResultImpl(locationMatcher, text);
	}

	public TrieOptions options(BranchToken token, MatchCondition condition) {
		return new TrieOptions(token, condition);
	}
}
