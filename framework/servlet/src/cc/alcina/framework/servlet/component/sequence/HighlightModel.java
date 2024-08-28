package cc.alcina.framework.servlet.component.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasFilterableText.Query;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multimap;

/**
 * Models highlights across a list of elements, and highlights within
 */
public class HighlightModel {
	class Match {
		Object sequenceElement;

		IntPair range;

		int idx;

		Match(Object sequenceElement, IntPair range, int idx) {
			this.sequenceElement = sequenceElement;
			this.range = range;
			this.idx = idx;
		}

		public int getIndexInSelectedElementMatches() {
			return idx - elementMatches.get(sequenceElement).get(0).idx;
		}
	}

	public int highlightIndex;

	List<?> filteredSequenceElements;

	String queryText;

	Function<Object, Object> toHasStringRepresentation;

	// the key is the sequence element
	Multimap<Object, List<Match>> elementMatches = new Multimap<>();

	List<Match> matches = new ArrayList<>();

	public HighlightModel(List<?> filteredSequenceElements,
			Function<Object, Object> toHasStringRepresentation,
			String queryText, int currentHighlightIndex) {
		this.filteredSequenceElements = filteredSequenceElements;
		this.toHasStringRepresentation = toHasStringRepresentation;
		this.queryText = queryText;
		this.highlightIndex = currentHighlightIndex;
	}

	public void computeMatches() {
		if (Ax.isBlank(queryText)) {
			return;
		}
		for (Object sequenceElement : filteredSequenceElements) {
			Object possiblyHasStringRepresentation = toHasStringRepresentation
					.apply(sequenceElement);
			if (possiblyHasStringRepresentation == null
					|| !(possiblyHasStringRepresentation instanceof HasStringRepresentation)) {
				continue;
			}
			String s = ((HasStringRepresentation) possiblyHasStringRepresentation)
					.provideStringRepresentation();
			if (s == null) {
				continue;
			}
			Query<?> query = Query.of(queryText).withCaseInsensitive(true);
			for (;;) {
				IntPair matchingRange = query.next(s);
				if (matchingRange == null) {
					break;
				}
				Match match = new Match(sequenceElement, matchingRange,
						matches.size());
				matches.add(match);
				elementMatches.add(sequenceElement, match);
			}
		}
	}

	public boolean hasMatches() {
		return matches.size() > 0;
	}

	public void move(int delta) {
		highlightIndex += delta;
		if (highlightIndex >= matches.size()) {
			highlightIndex = 0;
		}
		if (highlightIndex < 0) {
			highlightIndex = Math.max(matches.size() - 1, 0);
		}
	}

	public Object getHighlightedElement() {
		return matches.get(highlightIndex).sequenceElement;
	}

	public void goTo(int i) {
		highlightIndex = 0;
	}

	public Match getMatch(int highlightIdx) {
		return highlightIdx >= 0 && highlightIdx < matches.size()
				? matches.get(highlightIdx)
				: null;
	}
}
