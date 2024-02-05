package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;

public class TraversalEvents {
	public static class SelectionSelected extends
			ModelEvent<TraversalPlace.SelectionPath, SelectionSelected.Handler> {
		@Override
		public void dispatch(SelectionSelected.Handler handler) {
			handler.onSelectionSelected(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionSelected(SelectionSelected event);
		}
	}
}
