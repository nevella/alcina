package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.TextUtils;

/**
 * A measure token augmented for use by the layer parser
 * 
 * A BranchToken must either return non-null from getGroup() or override match()
 * 
 * Tokens must be unique to a given parser - so either use enums, singleton
 * instances or some other guarantee
 */
public interface BranchToken extends Token, BranchGroupMember {
	default Group getGroup() {
		return null;
	}

	/**
	 * This (primitive) token can begin a match
	 */
	default boolean isInitial() {
		return true;
	}

	default Measure match(ParserState state) {
		throw new UnsupportedOperationException(
				Ax.format("%s - %s - see BranchToken constraints",
						NestedName.get(this), toString()));
	}

	/*
	 * By default non-text end boundary locations are ignored for this token
	 * (when forwards traversing, that's after - when backwards traversing,
	 * that's !after)
	 */
	default MatchesBoundary matchesBoundary() {
		return MatchesBoundary.START;
	}

	default Measure measure(DomNode node) {
		return Measure.fromRange(node.asRange(), this);
	}

	default Measure measure(Location start, Location end) {
		return Measure.fromRange(new Range(start, end), this);
	}

	default Measure pointMeasure(LayerParser.ExtendedState state) {
		return measure(state.getLocation(), state.getLocation());
	}

	/*
	 * Either a logical group (a logical combination of leaf groups) or a leaf
	 * group - matching exactly one token
	 * 
	 * Note that group instances should not be reused for a given parser - token
	 * reuse is fine, since token.getGroup() is cloned
	 * 
	 */
	public static class Group implements BranchGroupMember {
		public static Group of(BranchGroupMember... members) {
			List<Group> memberGroups = Arrays.stream(members).map(m -> {
				if (m instanceof Group) {
					return (Group) m;
				} else {
					BranchToken token = (BranchToken) m;
					Group group = token.getGroup();
					if (group == null) {
						group = Group.primitive(token);
					} else {
						group = group.clone();
					}
					group.token = token;
					return group;
				}
			}).collect(Collectors.toList());
			Group result = new Group(memberGroups);
			if (members.length == 1 && members[0] instanceof BranchToken) {
				result.token = (BranchToken) members[0];
			}
			return result;
		}

		public static Group oneOf(BranchGroupMember... members) {
			return of(members).withOrderAny();
		}

		public static Group optional(BranchGroupMember... members) {
			return of(members).withMatchesZeroOrOne();
		}

		private static Group primitive(BranchToken token) {
			return new Group(token);
		}

		int min = 1;

		int max = 1;

		Order order = Order.SEQUENCE;

		List<Group> groups = new ArrayList<>();

		BranchToken token;

		Quantifier quantifier = Quantifier.GREEDY;

		/*
		 * initially unsupported (except for leaf tokens)
		 */
		boolean negated;

		// clone constructor
		private Group() {
		}

		private Group(BranchToken token) {
			this.token = token;
		}

		private Group(List<Group> groups) {
			this.groups = groups;
		}

		public Group clone() {
			Group result = new Group();
			result.min = min;
			result.max = max;
			result.order = order;
			result.token = token;
			result.quantifier = quantifier;
			result.negated = negated;
			result.groups = groups.stream().map(Group::clone)
					.collect(Collectors.toList());
			return result;
		}

		public int getTermCount() {
			if (groups.isEmpty()) {
				return 1;
			} else {
				return order == Order.SEQUENCE ? groups.size() : 1;
			}
		}

		public boolean isComplex() {
			return groups.size() > 0;
		}

		boolean isPrimitive() {
			return groups.size() == 0;
		}

		Stream<BranchToken> primitiveTokens() {
			if (isPrimitive()) {
				return Stream.of(token);
			}
			Stream<BranchToken> result = Stream.empty();
			for (Group group : groups) {
				result = Stream.concat(result, group.primitiveTokens());
			}
			return result;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (groups.isEmpty()) {
				if (negated) {
					builder.append("!");
				}
				builder.append(token.toString());
				return builder.toString();
			}
			builder.append('(');
			if (token != null && groups.size() > 0
					&& (groups.size() > 1 || groups.get(0).token != token)) {
				builder.append('<');
				builder.append(token);
				builder.append('>');
			}
			String connector = order == Order.SEQUENCE ? "," : "|";
			for (int idx = 0; idx < groups.size(); idx++) {
				if (idx != 0) {
					builder.append(connector);
				}
				builder.append(groups.get(idx));
			}
			builder.append(')');
			String countIndicator = "";
			if (min == 1 && max == 1) {
			} else if (min == 0 && max == 1) {
				countIndicator = "?";
			} else if (min == 0 && max == Integer.MAX_VALUE) {
				countIndicator = "*";
			} else if (min == 1 && max == Integer.MAX_VALUE) {
				countIndicator = "+";
			} else {
				countIndicator = Ax.format("{%s,%s}", min == 0 ? "" : min,
						max == Integer.MAX_VALUE ? "" : max);
			}
			builder.append(countIndicator);
			String quantifierIndicator = "";
			switch (quantifier) {
			case GREEDY:
				break;
			case POSSESSIVE:
				quantifierIndicator = "+";
				break;
			case RELUCTANT:
				quantifierIndicator = "?";
				break;
			}
			builder.append(quantifierIndicator);
			return builder.toString();
		}

		public Group withMatchesOneToAny() {
			this.max = Integer.MAX_VALUE;
			return this;
		}

		public Group withMatchesZeroOrOne() {
			this.min = 0;
			return this;
		}

		public Group withMatchesZeroToAny() {
			this.min = 0;
			this.max = Integer.MAX_VALUE;
			return this;
		}

		public Group withMax(int max) {
			this.max = max;
			return this;
		}

		public Group withMin(int min) {
			this.min = min;
			return this;
		}

		// this negates the contained primitive group
		public Group withNegated() {
			Preconditions.checkState(
					groups.size() == 1 && groups.get(0).isPrimitive());
			groups.get(0).negated = true;
			return this;
		}

		public Group withOrderAny() {
			this.order = Order.ANY;
			return this;
		}
	}

	public enum MatchesBoundary {
		START, END, ANY
	}

	enum Order {
		SEQUENCE, ANY
	}

	enum Quantifier {
		POSSESSIVE, RELUCTANT, GREEDY
	}

	public enum Standard implements BranchToken {
		LINE_SEPARATOR {
			private Pattern PATTERN = Pattern.compile("\n",
					Pattern.CASE_INSENSITIVE);

			@Override
			public Measure match(ParserState state) {
				return state.patternMatcher().match(this, PATTERN);
			}
		},
		LINE {
			private Pattern PATTERN = Pattern.compile("[^\n]+",
					Pattern.CASE_INSENSITIVE);

			@Override
			public Measure match(ParserState state) {
				return state.patternMatcher().match(this, PATTERN);
			}
		},
		WHITESPACE {
			private Pattern PATTERN = Pattern.compile(
					TextUtils.NON_LINE_WS_PATTERN_STR,
					Pattern.CASE_INSENSITIVE);

			@Override
			public Measure match(ParserState state) {
				return state.patternMatcher().match(this, PATTERN);
			}
		},
		DIGITS {
			private Pattern PATTERN = Pattern.compile("\\d+");

			@Override
			public Measure match(ParserState state) {
				return state.patternMatcher().match(this, PATTERN);
			}
		},
		ANY_TEXT {
			private Pattern PATTERN = Pattern.compile(".+");

			@Override
			public Measure match(ParserState state) {
				return state.patternMatcher().match(this, PATTERN);
			}
		},
		/*
		 * Matches nothing, models anonymous groups in the branch parser result
		 */
		ANON {
			@Override
			public Measure match(ParserState state) {
				return null;
			}
		}, /*
			 * specialised matcher for non-dom primitive containers, which
			 * changes cursor advance behaviour
			 */
		ANON_NON_DOM {
			@Override
			public boolean isNonDomToken() {
				return true;
			}

			@Override
			public Measure match(ParserState state) {
				return null;
			}
		},
		WHITESPACE_NODE {
			@Override
			public Measure match(ParserState state) {
				Location location = state.getLocation();
				if (location.isAtNodeStart() && location.containingNode
						.isWhitespaceOrEmptyTextContent()) {
					return Measure.fromNode(location.containingNode, this);
				} else {
					return null;
				}
			}
		},
		OPTIONAL_WHITESPACE_NODES {
			public Group getGroup() {
				return Group.of(WHITESPACE_NODE).withMatchesZeroToAny();
			}
		};
	}
}
