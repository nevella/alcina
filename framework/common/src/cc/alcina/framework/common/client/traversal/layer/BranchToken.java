package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.util.TextUtils;

public interface BranchToken extends MatchingToken {
	default Group getGroup() {
		return null;
	}

	@Override
	default Measure match(ParserState state) {
		throw new UnsupportedOperationException("Unimplemented method 'match'");
	}

	public static class Group {
		int min = 1;

		int max = 1;

		Order order = Order.SEQUENCE;

		List<Group> groups = new ArrayList<>();

		BranchToken token;

		Group(BranchToken token) {
			this.token = token;
		}

		Index index() {
			return new Index(this, 0);
		}

		/*
		 * Position in a group for matching. Invariant
		 */
		static class Index {
			/*
			 * the position in the parent group .groups array
			 */
			int position = -1;

			Group group;

			boolean complete;

			boolean continuable;

			Index(Group group, int position) {
				this.group = group;
				this.position = position;
			}

			List<Group.Index> computeValidSuccessors() {
				switch (group.order) {
				case SEQUENCE:
					Preconditions.checkState(position < group.groups.size());
					return List.of(atIndex(position++));
				case ANY:
					return IntStream.of(0, group.groups.size())
							.mapToObj(this::atIndex)
							.collect(Collectors.toList());
				default:
					throw new UnsupportedOperationException();
				}
			}

			Index atIndex(int newPosition) {
				return new Index(group, newPosition);
			}

			public boolean isLeaf() {
				return group.groups.isEmpty();
			}
		}
	}

	enum Order {
		SEQUENCE, ANY
	}

	public enum Standard implements BranchToken {
		LINE_SEPARATOR {
			private Pattern PATTERN = Pattern.compile("\n|\\z",
					Pattern.CASE_INSENSITIVE);

			@Override
			public Measure match(ParserState state) {
				return state.matcher().match(this, PATTERN);
			}
		},
		LINE {
			private Pattern PATTERN = Pattern.compile("[^\n]+",
					Pattern.CASE_INSENSITIVE);

			@Override
			public Measure match(ParserState state) {
				return state.matcher().match(this, PATTERN);
			}
		},
		WHITESPACE {
			private Pattern PATTERN = Pattern.compile(
					TextUtils.NON_LINE_WS_PATTERN_STR,
					Pattern.CASE_INSENSITIVE);

			@Override
			public Measure match(ParserState state) {
				return state.matcher().match(this, PATTERN);
			}
		};

		@Override
		public Selection select(ParserState state, Measure measure) {
			throw new UnsupportedOperationException();
		}
	}
}
