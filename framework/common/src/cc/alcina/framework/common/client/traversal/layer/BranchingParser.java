package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.traversal.layer.BranchToken.Group;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState.ParserEnvironment;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
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
 * 
 * 
 * 
 */
public class BranchingParser {
	State state;

	ParserEnvironment env;

	BranchingParserPeer peer;

	LayerParser layerParser;

	List<BranchToken> rootTokens;

	ParserState parserState;

	BranchingParser(LayerParser layerParser) {
		this.layerParser = layerParser;
		parserState = layerParser.parserState;
		peer = (BranchingParserPeer) layerParser.parserPeer;
	}

	List<Group> sentenceGroups;

	List<BranchToken> primitiveTokens;

	Logger logger = LoggerFactory.getLogger(getClass());

	class State {
		List<Branch> edgeBranches = new LinkedList<>();

		List<Branch> matchedSentenceBranches = new ArrayList<>();

		Branch bestMatch;

		void evaluateBranches() {
			logger.debug("Evaluating at location {}", parserState.location);
			matchedSentenceBranches.clear();
			List<Branch> sentenceBranches = sentenceGroups.stream()
					.map(group -> new Branch(group, parserState.location))
					.collect(Collectors.toList());
			sentenceBranches.forEach(edgeBranches::add);
			while (edgeBranches.size() > 0) {
				Branch branch = edgeBranches.remove(0);
				logger.debug("Entering branch {} at {}", branch,
						branch.location);
				branch.enter();
			}
			bestMatch = matchedSentenceBranches.stream()
					.sorted(new BranchOrdering()).findFirst().orElse(null);
			if (bestMatch != null) {
				parserState.bestMatch = bestMatch.match;
			}
		}

		Location computeFirstMatchedLocation() {
			return primitiveTokens.stream().map(parserState::match)
					.filter(Objects::nonNull).sorted().findFirst()
					.map(m -> m.start).orElse(null);
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

	public static class StructuredResult
			implements Comparable<StructuredResult> {
		public StructuredResult parent;

		StructuredResult(Branch branch) {
			this.branch = branch;
		}

		public Branch branch;

		List<StructuredResult> children = new ArrayList<>();

		static StructuredResult ensure(Branch branch,
				Map<Branch, StructuredResult> lookup) {
			return lookup.computeIfAbsent(branch, StructuredResult::new);
		}

		public void ensureParent(Map<Branch, StructuredResult> lookup) {
			if (branch.parent != null) {
				if (parent == null) {
					parent = ensure(branch.parent, lookup);
				}
				parent.children.add(this);
			}
		}

		List<StructuredResult> getSortedChildren() {
			return children.stream().sorted().collect(Collectors.toList());
		}

		class Measures {
			Map<Group, Measure> groupMeasure = new LinkedHashMap<>();

			DepthFirstTraversal<StructuredResult> traversal;

			Stream<StructuredResult> stream;

			Measures() {
				Set<Group> visitedGroups = new LinkedHashSet<>();
				traversal = new DepthFirstTraversal<>(StructuredResult.this,
						StructuredResult::getSortedChildren);
				traversal.stream().filter(r -> r.branch.match != null).forEach(
						r -> groupMeasure.put(r.branch.group, r.branch.match));
				traversal = new DepthFirstTraversal<>(StructuredResult.this,
						StructuredResult::getSortedChildren);
				stream = traversal.stream()
						// the root will not have a matching measure
						.skip(1)
						//
						.filter(r -> visitedGroups.add(r.branch.group));
			}
		}

		public Stream<Measure> getTokenMeasures() {
			Measures measures = new Measures();
			return measures.stream
					.map(r -> measures.groupMeasure.get(r.branch.group));
		}

		public String toStructuredString() {
			Measures measures = new Measures();
			FormatBuilder builder = new FormatBuilder();
			measures.stream.forEach(r -> {
				builder.indent((r.depth() - 1) * 2);
				Measure measure = measures.groupMeasure.get(r.branch.group);
				builder.line("%s", measure);
			});
			return builder.toString();
		}

		int depth() {
			int depth = 0;
			StructuredResult cursor = this;
			while (cursor.parent != null) {
				cursor = cursor.parent;
				depth++;
			}
			return depth;
		}

		@Override
		public int compareTo(StructuredResult o) {
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
	}

	/*
	 * A sequence of matches satisfying the structural (Group) constraints at a
	 * given Location
	 * 
	 */
	public class Branch {
		// immutable
		public Branch parent;

		public StructuredResult toStructuredResult() {
			Branch cursor = this;
			Map<Branch, StructuredResult> lookup = AlcinaCollections
					.newHashMap();
			StructuredResult result = null;
			do {
				result = StructuredResult.ensure(cursor, lookup);
				result.ensureParent(lookup);
				cursor = cursor.predecessor;
			} while (cursor != null);
			return result;
		}

		Branch predecessor;

		Location location;

		Group group;

		public Measure match;

		int indexInGroup;

		int repetitionIndex;
		// mutable

		int childBranchesSatisfied;

		int childBranchesReturned;

		boolean backtracking;

		// top-level (sentence matcher) constructor
		Branch(Group group, Location location) {
			this.group = group;
			this.location = location;
		}

		BranchToken getMatchingToken() {
			return group.token;
		}

		Branch(Branch parent, Branch predecessor, Group group, int indexInGroup,
				int repetitionIndex) {
			this.parent = parent;
			this.predecessor = predecessor;
			this.location = predecessor.location;
			this.group = group;
			this.indexInGroup = indexInGroup;
			this.repetitionIndex = repetitionIndex;
		}

		void onTokenMatched(Measure match) {
			this.match = match;
			logger.debug("Matched: {}", match);
			if (match.toString().contains("DATE")) {
				int debug = 3;
			}
			parent.onChildSatisfied(this);
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

		@Override
		public String toString() {
			String leafMatch = match != null ? Ax.format(" %s", match) : "";
			String dir = backtracking ? "<" : ">";
			return Ax.format("%s %s [%s][%s]%s", dir, group, indexInGroup,
					repetitionIndex, leafMatch);
		}

		void enter() {
			if (backtracking) {
				int debug = 3;
			}
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
					Measure match = parserState.match(location, group.token);
					boolean matchesLocation = false;
					if (match != null) {
						boolean testMeasureEnd = !layerParser.forwardsTraversalOrder
								&& !location.containingNode.isText();
						matchesLocation = Objects.equals(
								testMeasureEnd ? match.end : match.start,
								location);
					}
					if (group.negated) {
						if (matchesLocation || match == null) {
							onTokenNotMatched();
						} else {
							Location.Range range = new Location.Range(location,
									match.start);
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
				BranchToken matchToken = group.token != null ? group.token
						: BranchToken.Standard.ANON;
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
				Location.Range range = Location.Range
						.fromPossiblyReversedEndpoints(start, end);
				match = Measure.fromRange(range, matchToken);
				if (parent == null) {
					logger.info("Emitted sentence match : {}", match);
					state.matchedSentenceBranches.add(this);
				} else {
					parent.onChildSatisfied(this);
				}
			}
		}

		void onTokenNotMatched() {
			if (backtracking) {
				return;
			}
			parent.onChildNotSatisfied(this);
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

		void emitBranch(Branch parent, Branch predecessor, Group group,
				int indexInGroup, int repetitionIndex) {
			Branch branch = new Branch(parent, predecessor, group, indexInGroup,
					repetitionIndex);
			if (predecessor.match != null) {
				branch.location = env.successorFollowingMatch
						.apply(predecessor.match);
			}
			state.edgeBranches.add(branch);
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
	}

	// similar to LayerParser.linearParse -- but *dissimilar* enough to not try
	// and extend
	void parse(ParserEnvironment env) {
		this.env = env;
		this.state = new State();
		computeInvariants();
		while (parserState.location != null && !env.afterTraversalBoundary.get()
				&& !parserState.finished) {
			parserState.onBeforeTokenMatch();
			if (peer.filter == null || peer.filter.test(parserState.location)) {
				/*
				 * Move the location/cursor to this match iff it's an offset
				 * within the current text node
				 * 
				 * 
				 */
				if (layerParser.forwardsTraversalOrder) {
					Location firstMatchedLocation = state
							.computeFirstMatchedLocation();
					if (firstMatchedLocation != null
							&& firstMatchedLocation.treeIndex == parserState.location.treeIndex) {
						parserState.location = firstMatchedLocation;
					}
				}
				state.evaluateBranches();
			}
			/*
			 * Location advancement is more complex with partial matches
			 */
			if (parserState.bestMatch != null) {
				parserState.bestMatch.addToParent();
				parserState.matches.add(parserState.bestMatch);
				parserState.matchesByToken.add(parserState.bestMatch.token,
						parserState.bestMatch);
				parserState.outputs.add(parserState.bestMatch);
				parserState.sentenceBranches.add(state.bestMatch);
				parserState.location = env.successorFollowingMatch
						.apply(parserState.bestMatch);
				peer.onTokenMatched();
			} else {
				// FIXME - st.b - always true
				if (env.traverseUntilFound) {
					parserState.location = env.successorFollowingNoMatch.get();
				} else {
					parserState.location = env.boundary;
				}
			}
		}
	}

	void computeInvariants() {
		rootTokens = (List) layerParser.parserPeer.tokens;
		sentenceGroups = rootTokens.stream().map(Group::of)
				.collect(Collectors.toList());
		sentenceGroups.forEach(g -> logger.info("sentence group: {}", g));
		primitiveTokens = sentenceGroups.stream()
				.flatMap(Group::primitiveTokens).distinct()
				.collect(Collectors.toList());
	}
}
