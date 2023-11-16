package cc.alcina.framework.servlet.component.traversal.place;

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
		if (selection == null && treePath != null) {
			selection = TraversalProcessView.Ui.get().getHistory().traversal
					.getRootSelection().processNode().nodeForTreePath(treePath)
					.typedValue();
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
				addTokenPart(place.selection.processNode().treePath());
			}
		}
	}
}
