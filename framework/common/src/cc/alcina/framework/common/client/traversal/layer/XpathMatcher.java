package cc.alcina.framework.common.client.traversal.layer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.util.AlcinaCollections;

public class XpathMatcher {
	private ParserState parserState;

	Map<Token, QueryMatcher> matchers = AlcinaCollections.newLinkedHashMap();

	public XpathMatcher(ParserState parserState) {
		this.parserState = parserState;
	}

	public Measure match(Token token, String xpathQuery) {
		QueryMatcher matcher = matchers
				.computeIfAbsent(token, QueryMatcher::new)
				.withXpathQuery(xpathQuery);
		return matcher.match();
	}

	class QueryMatcher {
		Token token;

		String xpathQuery;

		Map<Location, Range> matches;

		QueryMatcher(Token token) {
			this.token = token;
		}

		Measure match() {
			if (matches == null) {
				matches = new LinkedHashMap<>();
				DomNode node = parserState.input.start.containingNode;
				List<DomNode> nodes = node.xpath(xpathQuery).nodes();
				nodes.forEach(n -> {
					Range range = n.asRange();
					matches.put(range.start, range);
				});
			}
			Measure match = null;
			Range matchedRange = matches.get(parserState.location);
			if (matchedRange != null) {
				match = Measure.fromRange(matchedRange, token);
			}
			return match;
		}

		QueryMatcher withXpathQuery(String xpathQuery) {
			Preconditions.checkState(
					this.xpathQuery == null || this.xpathQuery == xpathQuery,
					"xpathQuery must be invariant");
			this.xpathQuery = xpathQuery;
			return this;
		}
	}
}
