package cc.alcina.framework.servlet.component.traversal.place;

import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView;

public class TraversalPlace extends BasePlace implements TraversalProcessPlace {
	Selection selection;

	public String treePath;

	public TraversalPlace withSelection(Selection selection) {
		this.selection = selection;
		return this;
	}

	public Selection provideSelection() {
		if (selection == null && treePath != null
				&& TraversalProcessView.Ui.get().getHistory() != null
				&& TraversalProcessView.Ui.get().getHistory().traversal
						.getRootSelection() != null) {
			selection = (Selection) TraversalProcessView.Ui.get()
					.getHistory().traversal.getRootSelection().processNode()
							.nodeForTreePath(treePath).map(Node::getValue)
							.orElse(null);
		}
		return selection;
	}

	public static class Tokenizer extends BasePlaceTokenizer<TraversalPlace> {
		@Override
		protected TraversalPlace getPlace0(String token) {
			TraversalPlace place = new TraversalPlace();
			if (parts.length > 1) {
				try {
					place.treePath = parts[1];
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
			}
			return place;
		}

		@Override
		protected void getToken0(TraversalPlace place) {
			if (place.selection != null) {
				place.treePath = place.selection.processNode().treePath();
			}
			if (Ax.notBlank(place.treePath)) {
				addTokenPart(place.treePath);
			}
		}
	}
}
