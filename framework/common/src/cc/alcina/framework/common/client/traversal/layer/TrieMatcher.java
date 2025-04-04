package cc.alcina.framework.common.client.traversal.layer;

import java.util.Set;

import cc.alcina.framework.common.client.dom.Measure;
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
 * 
 * TODO - fix - or at least 'extension' - in the case of "must match whole
 * format section" - e.g. when an italic range starts with a space or ends with
 * a '.'
 */
public class TrieMatcher extends LookaheadMatcher<MatchCondition> {
	public TrieMatcher(ParserState parserState) {
		super(parserState);
	}

	@Override
	protected MatchResult matchText(LocationMatcher locationMatcher,
			CharSequence text) {
		return new MatchResultImpl(locationMatcher, text);
	}

	public TrieOptions options(BranchToken token, MatchCondition condition) {
		return new TrieOptions(token, condition);
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

	public interface InputMapper {
		String mapInput(CharSequence input);

		public static class ToLowerCase implements InputMapper {
			@Override
			public String mapInput(CharSequence input) {
				return input.toString().replace('\u00a0', ' ').toLowerCase();
			}
		}

		public static class ToString implements InputMapper {
			@Override
			public String mapInput(CharSequence input) {
				return input.toString().replace('\u00a0', ' ');
			}
		}
	}

	public static class MatchCondition {
		Trie<String, Set> trie;

		StartBoundaryTest startBoundaryTest = new WordCharacter();

		MatchTest matchTest = new TrieTest();

		public Disambiguator disambiguator = new Disambiguator.Simple();

		public MatchTest.Result lastMatch;

		public InputMapper inputMapper = new InputMapper.ToString();

		public boolean tryToExtend = false;

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

		public MatchTest.Result getLongestMatch(CharSequence sequence) {
			return matchTest.getLongestMatch(sequence, this);
		}
	}

	class MatchResultImpl implements MatchResult {
		int start = 0;

		int end;

		CharSequence text;

		MatchTest.Result longestMatch;

		// this could be better expressed - but the trie can match at the start
		// of the string, or after a non-word char ('(', space etc ). if not,
		// it'll be in state canStart=false
		public MatchResultImpl(LocationMatcher locationMatcher,
				CharSequence text) {
			int idx = 0;
			MatchCondition condition = locationMatcher.options.condition;
			int length = text.length();
			boolean canStart = true;
			for (; idx < length; idx++) {
				char c = text.charAt(idx);
				if (condition.startBoundaryTest.isNonStart(c)) {
					canStart = true;
					continue;
				}
				if (canStart) {
					longestMatch = condition
							.getLongestMatch(text.subSequence(idx, length));
					canStart = false;
				}
				if (longestMatch != null) {
					start = idx;
					end = idx + longestMatch.key.length();
					return;
				}
			}
			start = -1;
		}

		@Override
		public int end() {
			return end;
		}

		@Override
		public boolean found() {
			return start != -1;
		}

		@Override
		public void populateMeasureData(Measure match) {
			match.setData(longestMatch.value);
		}

		@Override
		public int start() {
			return start;
		}
	}

	public interface MatchTest {
		MatchTest.Result getLongestMatch(CharSequence sequence,
				MatchCondition matchCondition);

		public static class Result {
			public String key;

			public Object value;

			public Result(String key, Object value) {
				this.key = key;
				this.value = value;
			}
		}

		public static class TrieTest implements MatchTest {
			public static String closestCommonContainedInTrie(
					Trie<String, ?> trie, String key) {
				key = removeTrailingPunctuation(key);
				String match = trie.selectKey(key);
				if (match == null) {
					return null;
				}
				if (key.startsWith(match)) {
					return match;
				}
				int lastIndexOf = -1;
				/*
				 * the trimming logic here and at the start of the method ensure
				 * we go back looking for correct commonalities. We can't just
				 * use 'non-word' characters because that would include brackets
				 * - and (WA) etc terminators are crucial for legislation
				 * matching.
				 */
				while ((lastIndexOf = match.lastIndexOf(' ')) != -1) {
					match = match.substring(0, lastIndexOf);
					match = removeTrailingPunctuation(match);
					if (trie.containsKey(match) && key.startsWith(match)) {
						return match;
					}
				}
				return null;
			}

			static String removeTrailingPunctuation(String key) {
				if (key.length() == 0) {
					return key;
				}
				char c = key.charAt(key.length() - 1);
				switch (c) {
				case '.':
				case ',':
				case ';':
				case ':':
				case '-':
				case '\u2013':
				case '\u2014':
					return key.substring(0, key.length() - 1);
				default:
					return key;
				}
			}

			@Override
			public MatchTest.Result getLongestMatch(CharSequence sequence,
					MatchCondition condition) {
				// todo - this can be optimised by customising the trie - ditto
				// the lcase conversion
				//
				// ditto the end whitespace trimming. the reason we don't just
				// get longest match early on is performance - e.g. a 3-char
				// match may be huge (think 'the')
				String toMatch = condition.inputMapper.mapInput(sequence);
				String key = closestCommonContainedInTrie(condition.trie,
						toMatch);
				if (key != null) {
					Set value = condition.trie.get(key);
					if (value != null) {
						Object bestMatch = condition.disambiguator
								.getBestMatch(value);
						if (bestMatch != null) {
							// test expand to mark ", " as captured. todo -
							// abstract (actually - mark as complete segment
							// match although it isn't quite)
							if (condition.tryToExtend) {
								String sequenceTerminator = sequence
										.subSequence(key.length(),
												sequence.length())
										.toString();
								if (sequenceTerminator.matches("[., ]+")) {
									key = sequence.toString();
								}
							}
							return new MatchTest.Result(key, bestMatch);
						}
					}
				}
				return null;
			}
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

	public class TrieOptions extends Options {
		TrieOptions(BranchToken token, MatchCondition condition) {
			super(token, condition);
		}
	}
}
