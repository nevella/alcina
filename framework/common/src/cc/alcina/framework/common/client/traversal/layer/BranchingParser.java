package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.traversal.layer.BranchToken.Group.Index;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState.ParserEnvironment;

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

	List<BranchToken> tokens;

	ParserState parserState;

	BranchingParser(LayerParser layerParser) {
		this.layerParser = layerParser;
		parserState = layerParser.parserState;
		peer = (BranchingParserPeer) layerParser.parserPeer;
	}

	class State {
		List<Branch> sentenceBranches = new ArrayList<>();

		List<Branch> activeBranches = new ArrayList<>();

		public Stream<Branch> testableBranches() {
			return Stream.concat(sentenceBranches.stream(),
					activeBranches.stream());
		}

		Optional<Branch> firstMatch;
	}

	/*
	 * A sequence of matches satisfying the structural (group) constraints
	 */
	class Branch {
		BranchToken token;

		Branch parent;

		Measure match;

		// debugging only
		List<Branch> children = new ArrayList<>();

		Index index;

		Branch(BranchToken token) {
			this.parent = null;
			this.index = new BranchToken.Group(token).index();
		}

		Branch(Branch parent, Index index) {
			this.parent = parent;
			this.index = index;
			parent.children.add(this);
		}

		List<Branch> potentialSuccessors;

		List<Branch> potentialLeafSuccessors;

		Branch childBranch(Index index) {
			return new Branch(this, index);
		}

		/* */
		void computeNext() {
			List<Branch> deepSuccessors = new ArrayList<>();
			computeNext0(deepSuccessors);
			potentialLeafSuccessors = deepSuccessors.stream()
					.filter(s -> s.isLeaf()).collect(Collectors.toList());
		}

		boolean isLeaf() {
			return index.isLeaf();
		}

		Stream<Branch> continuedBranches(Measure matchedLeaf) {
			return Stream.empty();
		}

		/*
		 * Note - this does not check for (invalid) directly recursive groups
		 */
		void computeNext0(List<Branch> deepSuccessors) {
			List<Index> validSuccessorIndicies = index.computeValidSuccessors();
			potentialSuccessors = validSuccessorIndicies.stream()
					.map(this::childBranch).collect(Collectors.toList());
			deepSuccessors.addAll(potentialSuccessors);
			potentialSuccessors.forEach(b -> b.computeNext0(deepSuccessors));
		}

		public boolean isNotContinued() {
			if (isLeaf()) {
				return match != null;
			}
			return potentialLeafSuccessors.stream()
					.allMatch(Branch::isNotContinued);
		}

		/*
		 * this isn't correct - but it's on the way. for every group seen in the
		 * ascent, the last branch must be complete
		 */
		public boolean isTreeComplete() {
			Branch cursor = this;
			do {
				if (!index.complete) {
					return false;
				}
				cursor = cursor.parent;
			} while (cursor != null);
			return true;
		}
	}

	// similar to LayerParser.linearParse -- but *dissimilar* enough to not try
	// and extend
	void parse(ParserEnvironment env) {
		this.env = env;
		this.state = new State();
		computeInvariants();
		while (parserState.location != null
				&& !env.afterTraversalBoundary.get()) {
			parserState.onBeforeTokenMatch();
			if (peer.filter == null || peer.filter.test(parserState.location)) {
				computeBranches();
			}
			if (parserState.bestMatch != null) {
				parserState.bestMatch.addToParent();
				parserState.matches.add(parserState.bestMatch);
				parserState.matchesByToken.add(parserState.bestMatch.token,
						parserState.bestMatch);
				parserState.location = env.successorFollowingMatch.get();
			} else {
				if (env.traverseUntilFound) {
					parserState.location = env.successorFollowingNoMatch.get();
				} else {
					parserState.location = env.boundary;
				}
			}
		}
	}

	void computeBranches() {
		List<Branch> testableBranches = state.testableBranches()
				.collect(Collectors.toList());
		testableBranches.forEach(Branch::computeNext);
		List<BranchToken> matchableTokens = testableBranches.stream()
				.map(b -> b.potentialLeafSuccessors).flatMap(Collection::stream)
				.map(b -> b.index.group.token).distinct()
				.collect(Collectors.toList());
		List<Branch> continuedBranches = new ArrayList<>();
		for (MatchingToken token : matchableTokens) {
			Measure measure = token.match(parserState);
			if (measure != null) {
				testableBranches.stream()
						.flatMap(b -> b.continuedBranches(measure))
						.forEach(continuedBranches::add);
			}
		}
		Boolean atTraversalBoundary = env.atTraversalBoundary.get();
		/*
		 * Any testable branches that are not continued and are tree complete?
		 * 
		 * todo - sort by start location
		 */
		state.firstMatch = testableBranches.stream()
				.filter(t -> (t.isNotContinued() || atTraversalBoundary)
						&& t.isTreeComplete())
				.findFirst();
		if (state.firstMatch.isPresent()) {
			// create the measure
		}
		state.activeBranches = continuedBranches;
	}

	void computeInvariants() {
		tokens = (List) layerParser.parserPeer.tokens;
		state.sentenceBranches = tokens.stream().filter(peer::isSentence)
				.map(Branch::new).collect(Collectors.toList());
	}
}
