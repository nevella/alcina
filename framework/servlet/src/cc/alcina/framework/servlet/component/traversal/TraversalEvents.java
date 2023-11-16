package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class TraversalEvents {
	public static class SelectionSelected
			extends ModelEvent<Selection, SelectionSelected.Handler> {
		@Override
		public void dispatch(SelectionSelected.Handler handler) {
			handler.onSelectionSelected(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionSelected(SelectionSelected event);
		}
	}
}
