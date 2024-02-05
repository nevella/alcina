package cc.alcina.framework.common.client.traversal.layer;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.util.AlcinaCollections;

public class DocumentMatcher {
	private ParserState parserState;

	public DocumentMatcher(ParserState parserState) {
		this.parserState = parserState;
	}

	Map<Token, QueryMatcher> matchers = AlcinaCollections.newLinkedHashMap();

	class QueryMatcher {
		Token token;

		Function<DomNode, Optional<DomNode>> matcher;

		QueryMatcher(Token token) {
			this.token = token;
		}

		Optional<DomNode> matchedNode;

		Location matchedLocation;

		QueryMatcher withMatcher(Function<DomNode, Optional<DomNode>> matcher) {
			Preconditions.checkState(
					this.matcher == null || this.matcher == matcher,
					"matcher must be invariant");
			this.matcher = matcher;
			return this;
		}

		Measure match() {
			if (matchedNode == null) {
				matchedNode = matcher
						.apply(parserState.input.start.containingNode.document
								.getDocumentElementNode());
				if (matchedNode.isPresent()) {
					matchedLocation = matchedNode.get().asLocation();
				}
			}
			if (parserState.location.equals(matchedLocation)) {
				return Measure.fromNode(matchedNode.get(), token);
			} else {
				return null;
			}
		}
	}

	public Measure match(Token token,
			Function<DomNode, Optional<DomNode>> matcher) {
		QueryMatcher queryMatcher = matchers
				.computeIfAbsent(token, QueryMatcher::new).withMatcher(matcher);
		return queryMatcher.match();
	}
}