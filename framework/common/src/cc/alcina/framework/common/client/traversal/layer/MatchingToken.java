package cc.alcina.framework.common.client.traversal.layer;

import java.util.Optional;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.InputState;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;

public interface MatchingToken extends Token {
	Measure match(InputState state);

	Selection select(InputState state, Measure measure);
	public static abstract class SingleMatch implements MatchingToken {
		private Optional<DomNode> match;

		@Override
		public Measure match(InputState state) {
			if (match == null) {
				DomNode document = state.getDocument().get().containingNode();
				this.match = getMatch(document);
			}
			if (match.isPresent() && state.contains(match.get().asLocation())) {
				return Measure.fromNode(match.get(), this);
			} else {
				return null;
			}
		}

		protected abstract Optional<DomNode> getMatch(DomNode document);
	}
}