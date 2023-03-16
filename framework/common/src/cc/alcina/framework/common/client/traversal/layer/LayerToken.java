package cc.alcina.framework.common.client.traversal.layer;

import java.util.Optional;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.InputState;

public interface LayerToken {
	Slice match(InputState state);

	Selection select(InputState state, Slice slice);

	public static abstract class SingleMatch implements LayerToken {
		private Optional<DomNode> match;

		@Override
		public Slice match(InputState state) {
			if (match == null) {
				DomNode document = state.getDocument().get().containingNode();
				this.match = getMatch(document);
			}
			if (match.isPresent() && state.contains(match.get().asLocation())) {
				return Slice.fromNode(match.get(), this);
			} else {
				return null;
			}
		}

		protected abstract Optional<DomNode> getMatch(DomNode document);
	}
}