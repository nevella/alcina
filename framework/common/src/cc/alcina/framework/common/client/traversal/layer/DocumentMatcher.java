package cc.alcina.framework.common.client.traversal.layer;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.util.AlcinaCollections;

public class DocumentMatcher {
	private ParserState parserState;

	Map<Token, QueryMatcher> matchers = AlcinaCollections.newLinkedHashMap();

	public DocumentMatcher(ParserState parserState) {
		this.parserState = parserState;
	}

	public Measure match(Token token,
			Function<DomNode, Optional<DomNode>> matcher) {
		QueryMatcher queryMatcher = matchers
				.computeIfAbsent(token, QueryMatcher::new).withMatcher(matcher);
		return queryMatcher.match();
	}

	class QueryMatcher {
		Token token;

		Function<DomNode, Optional<DomNode>> matcher;

		Optional<DomNode> matchedNode;

		Location matchedLocation;

		QueryMatcher(Token token) {
			this.token = token;
		}

		Measure match() {
			if (matchedNode == null) {
				matchedNode = matcher
						.apply(parserState.input.start.getContainingNode().document
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

		QueryMatcher withMatcher(Function<DomNode, Optional<DomNode>> matcher) {
			Preconditions.checkState(
					this.matcher == null || this.matcher == matcher,
					"matcher must be invariant");
			this.matcher = matcher;
			return this;
		}
	}
}