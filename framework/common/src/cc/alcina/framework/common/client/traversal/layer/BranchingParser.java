package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.traversal.layer.BranchToken.Group;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState.ParserEnvironment;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Comparators;
import cc.alcina.framework.common.client.util.ConditionalLogger;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * <p>
 * A general brnaching matcher, matching a BNLF grammar composed of arbitrary
 * (not just textual) tokens, which matches sentences composed of tokens
 * (elements of the grammar).
 * 
 * <p>
 * A "sentence" ia a token which is a valid result (generally most tokens will
 * not be sentences).
 * 
 * <p>
 * A leaf token is a token not composed of other tokens
 * 
 * <h3>Debugging matches (at peer.confirmSentenceBranch())</h3>
 * <p>
 * Use branch.toResult().toStructuredString() to get a view of the matched
 * structure
 */
/*
 * The naive algorithm is:
 * 
 * Defina an object 'branch' which has a method "nextPossibleBranches" which
 * returns all potential valid branches/tokens given the current branch state. A
 * recursive partner to this method returns "nextPossibleLeafBranches"
 * 
 * For input I
 * 
 * Compute all leaf tokens (these can also be - say - string segments in
 * composite token definitions)
 * 
 * Using cursor L - a location in the DOM tree
 * 
 * 
 * Determine the set of possible next leaf tokens - run nextPossibleLeafBranches
 * against all current matching branches and all sentence-initiating branches
 * 
 * Find the next match all leaf tokens in I - their matching measures will have
 * initial locations {M[n,0]}. Note some tokens (say complex, context-free
 * regexes) should use a 'find all matches on first match attempt' strategy
 * 
 * Order the match measures. Only sentence-initiating branches can match
 * locations after L. Update branch state (emitting complete sentence, closing
 * branches, punting initiating branches if after L)
 * 
 * Emitting a sentence will close all branches intersecting the sentence measure
 * 
 * Determining the next L -- that's strategy-based, but the next value should at
 * least be > L
 * 
 * FIXME - parser - this doesn't need to be tied to SelectionTraversal/Layer -
 * refactor out for reuse? But there's a fair bit of support from LayerParser, a
 * look at factoring out suggested maybe more trouble than it's worth
 * 
 * 
 */
public class BranchingParser {
	int branchSizeLimit = Integer.MAX_VALUE;

	public static transient boolean debugLoggingEnabled;

	State state;

	ParserEnvironment env;

	LayerParserPeer peer;

	LayerParser layerParser;

	List<BranchToken> rootTokens;

	ParserState parserState;

	ConditionalLogger conditionalLogger;

	List<Group> sentenceGroups;

	List<BranchToken> primitiveTokens;

	List<BranchToken> primitiveInitialTokens;

	Logger logger;

	BranchingParser(LayerParser layerParser) {
		this.layerParser = layerParser;
		parserState = layerParser.parserState;
		peer = layerParser.parserPeer;
		branchSizeLimit = peer.getBranchSizeLimit();
		logger = LoggerFactory.getLogger(getClass());
		conditionalLogger = new ConditionalLogger(logger,
				this::isDebugLoggingEnabled);
	}

	void computeInvariants() {
		rootTokens = (List) layerParser.parserPeer.tokens;
		sentenceGroups = rootTokens.stream().map(Group::of)
				.collect(Collectors.toList());
		sentenceGroups.forEach(
				g -> conditionalLogger.debug("sentence group: {}", () -> g));
		primitiveTokens = sentenceGroups.stream()
				.flatMap(Group::primitiveTokens).distinct()
				.collect(Collectors.toList());
		primitiveInitialTokens = sentenceGroups.stream()
				.flatMap(Group::primitiveTokens).distinct()
				.filter(BranchToken::isInitial).collect(Collectors.toList());
	}

	boolean isDebugLoggingEnabled() {
		return debugLoggingEnabled;
	}

	void parse(ParserEnvironment env) {
		this.env = env;
		this.state = new State();
		computeInvariants();
		while (parserState.location != null && !env.afterTraversalBoundary.get()
				&& !parserState.finished) {
			state.onBeforeTokenMatch();
			if (peer.filter == null || peer.filter.test(parserState.location)) {
				if (layerParser.forwardsTraversalOrder
						&& layerParser.lookahead) {
					state.computeFirstMatchedLocation();
					Measure next = state.lookaheadMatches.minimalNext;
					if (next != null) {
						/*
						 * Move the location/cursor to this match iff it's an
						 * offset within the current text node. If there is such
						 * a match, post-branch evaluation movement changes - to
						 * be 'go to next char' rather than 'go to next
						 * location'
						 */
						if (next.start.getTreeIndex() == parserState.location
								.getTreeIndex()) {
							parserState.location = next.start;
						} else {
							state.lookaheadMatches.nextLocationAfterNoMatch = null;
						}
					}
				}
				state.evaluateBranches();
			}
			/*
			 * Location advancement is more complex with partial matches
			 */
			if (parserState.bestMatch != null) {
				parserState.matches.add(parserState.bestMatch);
				parserState.matchesByToken.add(parserState.bestMatch.token,
						parserState.bestMatch);
				parserState.sentenceBranches.add(state.bestMatch);
				parserState.topicSentenceMatched.signal();
				parserState.location = env.successorFollowingMatch
						.get(parserState.bestMatch);
				peer.onSentenceMatched(state.bestMatch,
						state.matchedSentenceBranches);
			} else {
				parserState.location = env.successorFollowingNoMatch
						.get(state.lookaheadMatches);
			}
		}
	}

	public class BeforeBranchEntry implements ProcessObservable {
		public Branch branch;

		public BeforeBranchEntry(Branch branch) {
			this.branch = branch;
		}

		public ParserState getParserState() {
			return parserState;
		}
	}

	public class BeforeTokenMatch implements ProcessObservable {
		public Branch branch;

		public Token token;

		public BeforeTokenMatch(Branch branch, Token token) {
			this.branch = branch;
			this.token = token;
		}

		public ParserState getParserState() {
			return parserState;
		}
	}

	/*
	 * A sequence of matches satisfying the structural (Group) constraints at a
	 * given Location
	 * 
	 */
	public class Branch {
		// immutable
		public Branch parent;

		Branch predecessor;

		public Location location;

		Group group;

		public Measure match;

		int indexInGroup;

		int repetitionIndex;
		// mutable

		int childBranchesSatisfied;

		int childBranchesReturned;

		boolean backtracking;

		Branch(Branch parent, Branch predecessor, Group group, int indexInGroup,
				int repetitionIndex) {
			this.parent = parent;
			this.predecessor = predecessor;
			this.location = predecessor.location;
			this.group = group;
			this.indexInGroup = indexInGroup;
			this.repetitionIndex = repetitionIndex;
		}

		// top-level (sentence matcher) constructor
		Branch(Group group, Location location) {
			this.group = group;
			this.location = location;
		}

		void emitBranch(Branch parent, Branch predecessor, Group group,
				int indexInGroup, int repetitionIndex) {
			Branch branch = new Branch(parent, predecessor, group, indexInGroup,
					repetitionIndex);
			if (predecessor.match != null) {
				branch.location = env.successorFollowingMatch
						.get(predecessor.match);
			}
			state.edgeBranches.add(branch);
		}

		void enter() {
			ProcessObservers.publish(BeforeBranchEntry.class,
					() -> new BeforeBranchEntry(this));
			if (!isComplete()) {
				// try descent or token match
				if (group.isComplex()) {
					switch (group.order) {
					case ANY:
						group.groups.forEach(g -> {
							emitBranch(this, this, g, 0, 0);
						});
						break;
					case SEQUENCE:
						emitBranch(this, this, group.groups.get(indexInGroup),
								0, 0);
						break;
					}
				} else {
					ProcessObservers.publish(BeforeTokenMatch.class,
							() -> new BeforeTokenMatch(this, group.token));
					Measure match = parserState.match(location, group.token);
					boolean matchesLocation = false;
					if (match != null) {
						boolean testMeasureEnd = !layerParser.forwardsTraversalOrder
								&& !location.getContainingNode().isText();
						/*
						 * This is a little tricky. For the purposes of
						 * continuation of a branch (match), just check indexes
						 * are contiguous. Otherwise node containment will cause
						 * contiguous _text_ runs to not be continuous - and
						 * thus fail the sequence
						 */
						matchesLocation = Objects.equals(
								testMeasureEnd ? match.end.getIndex()
										: match.start.getIndex(),
								location.getIndex());
					}
					if (group.negated) {
						if (matchesLocation) {
							onTokenNotMatched();
						} else {
							Location end = match == null ? env.boundary
									: match.start;
							Location.Range range = new Location.Range(location,
									end);
							// ensure end is the deepest node
							range = range.toDeepestCommonNode();
							range = new Location.Range(location, range.end);
							Measure negatedMatch = Measure.fromRange(range,
									group.token);
							negatedMatch.setData(Measure.NEGATED_MATCH);
							onTokenMatched(negatedMatch);
						}
					} else {
						if (matchesLocation) {
							onTokenMatched(match);
						} else {
							onTokenNotMatched();
						}
					}
				}
			}
			// never true for a primitive branch
			if (isSatisfied()) {
				BranchToken matchToken = group.token;
				if (matchToken == null) {
					// state.location movement (advance) behaviour will change
					// depending on whether the group is a primitive child
					// container and the primitive child is non-dom
					matchToken = BranchToken.Standard.ANON;
					if (predecessor != null && predecessor.parent != null
							&& predecessor.parent.group == group
							&& predecessor.predecessor.group == group
							&& predecessor.match != null) {
						// it's a sole-child, check the match
						if (predecessor.match.token.isNonDomToken()) {
							matchToken = BranchToken.Standard.ANON_NON_DOM;
						}
					}
				}
				/*
				 * generate the containing mesaure by traversing to the start of
				 * the group repetition to determine the start + end measures
				 */
				Range start = null;
				Range end = null;
				Branch cursor = this;
				do {
					cursor = cursor.predecessor;
					if (cursor.match != null && end == null) {
						end = cursor.match;
						// root tokens will only have one child
						if (parent == null) {
							start = cursor.match;
							break;
						}
					}
					if ((cursor.group == group && cursor.indexInGroup == 0)
							|| cursor.predecessor == null) {
						if (end == null) {
							end = new Range(cursor.location, cursor.location);
						}
						if (cursor.predecessor == null && parent != null) {
							start = end;
						} else {
							start = new Range(cursor.location, cursor.location);
						}
					}
				} while (start == null);
				// only match on rep > 0 (zero will satisfy a zero-or-more
				// group, but won't have a match)
				if (repetitionIndex != 0) {
					Location.Range range = Location.Range
							.fromPossiblyReversedEndpoints(start, end);
					match = Measure.fromRange(range, matchToken);
				}
				if (parent == null) {
					if (peer.confirmSentenceBranch(this)) {
						conditionalLogger.debug(
								"Emitted sentence match : {} - '{}'",
								() -> new Object[] { match,
										toResult().root.match.toTextString() });
						state.matchedSentenceBranches.add(this);
					} else {
						conditionalLogger.debug(
								"Did not sentence match (lookahead/behind): {} - '{}'",
								() -> new Object[] { match,
										toResult().root.match.toTextString() });
					}
				} else {
					parent.onChildSatisfied(this);
				}
			} else {
				if (backtracking) {
					// backtrack to the last child of the previous entry in
					// the group
					Branch cursor = this;
					while (true) {
						cursor = cursor.predecessor;
						if (cursor == null) {
							// backtracking finished
							break;
						} else if (cursor == parent) {
							parent.onChildNotSatisfied(this);
							break;
						} else if (!cursor.backtracking) {
							cursor.backtracking = true;
							state.edgeBranches.add(cursor);
							break;
						}
					}
				}
			}
		}

		BranchToken getBranchToken() {
			return group.token;
		}

		public Measure getLastMeasure() {
			Branch cursor = this;
			while (cursor != null) {
				if (cursor.match != null) {
					return cursor.match;
				}
				cursor = cursor.predecessor;
			}
			return null;
		}

		/*
		 * A completed branch can not be extended (matched to tokens, which
		 * implies group descent if this branch is non-primitive) for a given
		 * backtracking state
		 * 
		 */
		boolean isComplete() {
			switch (group.quantifier) {
			case POSSESSIVE:
				return backtracking ? true : repetitionIndex == group.max;
			case GREEDY:
				return backtracking ? true : repetitionIndex == group.max;
			case RELUCTANT:
				return backtracking ? repetitionIndex == group.max
						: repetitionIndex == group.min;
			default:
				throw new UnsupportedOperationException();
			}
		}

		/*
		 * A branch is satisfied if it is at index 0 and the repition count is
		 * >= group.min
		 */
		boolean isSatisfied() {
			if (indexInGroup != 0) {
				return false;
			}
			if (repetitionIndex < group.min) {
				return false;
			}
			switch (group.quantifier) {
			case POSSESSIVE:
				if (backtracking) {
					// possessive groups don't backtrack
					return false;
				} else {
					return repetitionIndex == group.max;
				}
			case GREEDY:
				return backtracking ? repetitionIndex != group.max
						: repetitionIndex == group.max;
			case RELUCTANT:
				return backtracking ? repetitionIndex != group.min
						: repetitionIndex == group.min;
			default:
				throw new UnsupportedOperationException();
			}
		}

		/*
		 * Test as to whether the branch should emit a backtracking
		 * branch/onchildnotsatisfied
		 */
		void onChildNotSatisfied(Branch child) {
			childBranchesReturned++;
			boolean notSatisfied = false;
			switch (group.order) {
			case ANY:
				notSatisfied = childBranchesSatisfied == 0
						&& childBranchesReturned == group.groups.size();
				break;
			case SEQUENCE:
				notSatisfied = true;
				break;
			}
			if (!notSatisfied) {
				return;
			}
			Branch cursor = this;
			while (cursor != null) {
				if (!cursor.backtracking) {
					cursor.backtracking = true;
					state.edgeBranches.add(cursor);
					return;
				}
				cursor = cursor.predecessor;
			}
		}

		void onChildSatisfied(Branch child) {
			childBranchesSatisfied++;
			childBranchesReturned++;
			int nextIndexInGroup = indexInGroup + 1;
			int nextRepetitionIndex = repetitionIndex;
			if (nextIndexInGroup == group.getTermCount()) {
				nextIndexInGroup = 0;
				nextRepetitionIndex++;
			}
			emitBranch(parent, child, group, nextIndexInGroup,
					nextRepetitionIndex);
		}

		void onTokenMatched(Measure match) {
			this.match = match;
			conditionalLogger.debug("Matched: {}{}", () -> new Object[] {
					group.negated ? "!" : "", match.toTokenTextString() });
			parent.onChildSatisfied(this);
		}

		void onTokenNotMatched() {
			if (backtracking) {
				return;
			}
			parent.onChildNotSatisfied(this);
		}

		/**
		 * Used by process observer debuggers
		 * 
		 * @return prior matches, determined by walking the predecessor chain
		 */
		public Stream<Measure> priorMatches() {
			List<Measure> matches = new ArrayList<>();
			Branch cursor = this;
			while (cursor != null) {
				matches.add(cursor.match);
				cursor = cursor.predecessor;
			}
			return matches.stream().filter(Objects::nonNull).distinct();
		}

		public Result toResult() {
			return new Result(this);
		}

		@Override
		public String toString() {
			String leafMatch = match != null ? Ax.format(" %s", match) : "";
			String dir = backtracking ? "<" : ">";
			return Ax.format("%s %s [%s][%s]%s", dir, group, repetitionIndex,
					indexInGroup, leafMatch);
		}
	}

	/*
	 * Order by start-low, end-high
	 */
	class BranchOrdering implements Comparator<Branch> {
		@Override
		public int compare(Branch o1, Branch o2) {
			Measure m1 = o1.match;
			Measure m2 = o2.match;
			{
				int cmp = m1.start.compareTo(m2.start);
				if (cmp != 0) {
					return cmp;
				}
			}
			{
				int cmp = m1.end.compareTo(m2.end);
				if (cmp != 0) {
					return -cmp;
				}
			}
			return 0;
		}
	}

	public static class Result implements Comparable<Result> {
		public Result parent;

		Map<BranchLocation, Entry> branchEntry = AlcinaCollections.newHashMap();

		Map<Group, Integer> groupIndicies = AlcinaCollections.newHashMap();

		Entry root;

		Branch branch;

		Result(Branch branch) {
			this.branch = branch;
			populate();
			stream().filter(r -> r.match == null).forEach(r -> {
				if (r.children.isEmpty()) {
					// zero-length anon group, ignore
				} else {
					Entry first = r.children.stream()
							.filter(r2 -> r2.match != null).findFirst()
							.orElse(null);
					if (first == null) {
						// TODO - repeated application of this process may work?
					} else {
						Entry last = r.children.stream()
								.filter(r2 -> r2.match != null)
								.reduce(Ax.last()).orElse(null);
						Range range = Range.fromPossiblyReversedEndpoints(
								first.match, last.match);
						r.match = Measure.fromRange(range,
								BranchToken.Standard.ANON);
					}
				}
			});
		}

		@Override
		public int compareTo(Result o) {
			Branch b1 = branch;
			Branch b2 = o.branch;
			Preconditions.checkArgument(b1.group == b2.group);
			{
				int cmp = b1.repetitionIndex - b2.repetitionIndex;
				if (cmp != 0) {
					return cmp;
				}
			}
			return b1.indexInGroup - b2.indexInGroup;
		}

		int depth() {
			int depth = 0;
			Result cursor = this;
			while (cursor.parent != null) {
				cursor = cursor.parent;
				depth++;
			}
			return depth;
		}

		Entry ensureEntry(Branch cursor) {
			Entry entry = ensureEntry0(cursor, 0);
			if (cursor.match != null) {
				// if cursor is at repetitionIndex zero, it will be a leaf, so
				// the match applies to it,
				// otherwise to the repetition index predecessor
				if (cursor.repetitionIndex == 0) {
					entry.match = cursor.match;
				} else {
					Entry matchRecipient = ensureEntry0(cursor, -1);
					Preconditions.checkState(matchRecipient.match == null);
					matchRecipient.match = cursor.match;
				}
			}
			if (cursor.parent != null && entry.parent == null) {
				entry.parent = ensureEntry0(cursor.parent, 0);
				entry.parent.children.add(0, entry);
			}
			return entry;
		}

		Entry ensureEntry0(Branch cursor, int repetitionIndexOffset) {
			BranchLocation branchLocation = new BranchLocation(cursor,
					repetitionIndexOffset);
			return branchEntry.computeIfAbsent(branchLocation, Entry::new);
		}

		public Measure measure(Token token) {
			List<Measure> list = measures(token).collect(Collectors.toList());
			Preconditions.checkState(list.size() < 2);
			return Ax.first(list);
		}

		public boolean has(Token token) {
			return measures(token).count() > 0;
		}

		public Stream<Measure> measures() {
			return stream().filter(e -> !e.isNegated() && e.match != null)
					// the root result will occur twice, so dedupe
					.map(e -> e.match).distinct();
		}

		public Stream<Measure> measures(Token token) {
			return measures().filter(m -> m.token == token);
		}

		void populate() {
			Branch cursor = branch;
			while (true) {
				Entry entry = ensureEntry(cursor);
				cursor = cursor.predecessor;
				if (cursor == null) {
					root = entry;
					break;
				}
			}
		}

		public Measure rootMeasure() {
			return measures().findFirst().get();
		}

		public Stream<Entry> stream() {
			return new DepthFirstTraversal<>(root, Entry::children).stream();
		}

		public String toStructuredString() {
			FormatBuilder builder = new FormatBuilder();
			stream().filter(r -> r.match != null).forEach(r -> {
				builder.indent((r.depth() - 1) * 2);
				Measure measure = r.match;
				String negated = r.isNegated() ? "!" : "";
				builder.line("%s%s", negated, measure);
			});
			return builder.toString();
		}

		public class BranchLocation {
			List<LevelLocation> locations = new ArrayList<>();

			int hash = 0;

			BranchLocation(Branch branch, int repetitionIndexOffset) {
				Branch cursor = branch;
				while (cursor != null) {
					locations.add(
							new LevelLocation(cursor, repetitionIndexOffset));
					repetitionIndexOffset = 0;
					cursor = cursor.parent;
				}
				Collections.reverse(locations);
			}

			public int depth() {
				return locations.size() - 1;
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof BranchLocation) {
					BranchLocation o = (BranchLocation) obj;
					return locations.equals(o.locations);
				} else {
					return false;
				}
			}

			Group group() {
				return Ax.last(locations).group;
			}

			int groupIndex(Group group) {
				return groupIndicies.computeIfAbsent(group,
						g -> groupIndicies.size());
			}

			@Override
			public int hashCode() {
				if (hash == 0) {
					hash = locations.hashCode();
					if (hash == 0) {
						hash = -1;
					}
				}
				return hash;
			}

			@Override
			public String toString() {
				return locations.toString();
			}

			class LevelLocation {
				Group group;

				int indexInGroup;

				int repetitionIndex;

				LevelLocation(Branch branch, int repetitionIndexOffset) {
					group = branch.group;
					indexInGroup = branch.indexInGroup;
					repetitionIndex = branch.repetitionIndex
							+ repetitionIndexOffset;
				}

				@Override
				public boolean equals(Object obj) {
					if (obj instanceof LevelLocation) {
						LevelLocation o = (LevelLocation) obj;
						return Ax.equals(group, o.group, indexInGroup,
								o.indexInGroup, repetitionIndex,
								o.repetitionIndex);
					} else {
						return false;
					}
				}

				@Override
				public int hashCode() {
					return Objects.hash(group, indexInGroup, repetitionIndex);
				}

				@Override
				public String toString() {
					return Ax.format("[%s,%s,%s]", groupIndex(group),
							repetitionIndex, indexInGroup);
				}
			}
		}

		public class Entry {
			public BranchLocation branchLocation;

			public Entry parent;

			public List<Entry> children = new LinkedList<>();

			public Measure match;

			Entry(BranchLocation branchLocation) {
				this.branchLocation = branchLocation;
			}

			List<Entry> children() {
				return children;
			}

			public int depth() {
				return branchLocation.depth();
			}

			public boolean isNegated() {
				return branchLocation.group().negated;
			}

			@Override
			public String toString() {
				return Ax.format("%s - %s children - %s", branchLocation,
						children.size(), match);
			}
		}
	}

	class State {
		List<Branch> edgeBranches = new LinkedList<>();

		List<Branch> matchedSentenceBranches = new ArrayList<>();

		Branch bestMatch;

		Branch evaluatingBranch;

		LookaheadMatches lookaheadMatches;

		void computeFirstMatchedLocation() {
			lookaheadMatches = new LookaheadMatches();
		}

		void evaluateBranches() {
			conditionalLogger.debug("Evaluating at location {}",
					() -> parserState.location);
			conditionalLogger.debug("Text at location: {}",
					() -> Ax.trim(parserState.inputContent().toString(), 400));
			matchedSentenceBranches.clear();
			sentenceGroups.stream()
					.map(g -> new Branch(g, parserState.location))
					.forEach(edgeBranches::add);
			while (edgeBranches.size() > 0
					&& edgeBranches.size() < branchSizeLimit) {
				Branch branch = edgeBranches.remove(0);
				conditionalLogger.debug("Entering branch {} at {}",
						() -> new Object[] { branch, branch.location });
				evaluatingBranch = branch;
				branch.enter();
				evaluatingBranch = null;
			}
			if (edgeBranches.size() == branchSizeLimit) {
				// FIXME - traversal - emit warning observable
				Ax.err("Exiting eval branches - max branches exceeded - %s",
						branchSizeLimit);
			}
			bestMatch = matchedSentenceBranches.stream()
					.sorted(new BranchOrdering()).findFirst().orElse(null);
			if (bestMatch != null) {
				parserState.bestMatch = bestMatch.match;
			}
		}

		void onBeforeTokenMatch() {
			parserState.onBeforeTokenMatch();
			lookaheadMatches = null;
		}

		class LookaheadMatches {
			List<Measure> matches;

			Measure minimalNext;

			// if this is null, use standard next location test
			Location nextLocationAfterNoMatch;

			LookaheadMatches() {
				matches = primitiveInitialTokens.stream()
						.map(parserState::match).filter(Objects::nonNull)
						.sorted().collect(Collectors.toList());
				minimalNext = Ax.first(matches);
				if (minimalNext != null) {
					Location after = nextLocationAfterNoMatch();
					if (after.equals(parserState.location)
							|| after.getTreeIndex() != parserState.location
									.getTreeIndex()) {
						after = null;
					} else {
						if (after.isAtNodeEnd() && after.isTextNode()) {
							after = after.relativeLocation(
									RelativeDirection.NEXT_LOCATION,
									TextTraversal.EXIT_NODE);
						}
					}
					nextLocationAfterNoMatch = after;
				}
			}

			Location nextLocationAfterNoMatch() {
				// return min :(end of(measures which start at location)) -
				// (start of (measures which start after location))
				Location endOfStartingAtCurrentLocation = matches.stream()
						.filter(m -> m.start.equals(minimalNext.start))
						.map(m -> m.end).findFirst().get();
				Optional<Location> startOfStartingAfterCurrentLocation = matches
						.stream()
						.filter(m -> m.start.isAfter(minimalNext.start))
						.map(m -> m.start).findFirst();
				return startOfStartingAfterCurrentLocation
						.map(s -> Comparators.min(
								endOfStartingAtCurrentLocation,
								startOfStartingAfterCurrentLocation.get()))
						.orElse(endOfStartingAtCurrentLocation);
			}
		}
	}
}
