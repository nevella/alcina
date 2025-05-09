package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class ContentDecoratorEvents {
	public static class ReferenceSelected
			extends ModelEvent<Object, ReferenceSelected.Handler> {
		@Override
		public void dispatch(ReferenceSelected.Handler handler) {
			handler.onReferenceSelected(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onReferenceSelected(ReferenceSelected event);
		}
	}

	public static class NodeDelta
			extends ModelEvent<Object, NodeDelta.Handler> {
		@Override
		public void dispatch(NodeDelta.Handler handler) {
			handler.onNodeDelta(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onNodeDelta(NodeDelta event);
		}
	}
}
