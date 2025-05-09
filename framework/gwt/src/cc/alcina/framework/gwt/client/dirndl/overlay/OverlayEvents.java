package cc.alcina.framework.gwt.client.dirndl.overlay;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class OverlayEvents {
	public static class PositionedDescendants extends
			ModelEvent.DescendantEvent<Object, PositionedDescendants.Handler, PositionedDescendants.Emitter> {
		public interface Handler extends NodeEvent.Handler {
			void onPositionedDescendants(PositionedDescendants event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		@Override
		public void dispatch(PositionedDescendants.Handler handler) {
			handler.onPositionedDescendants(this);
		}
	}

	public static class RefreshPositioning
			extends ModelEvent<Object, RefreshPositioning.Handler> {
		@Override
		public void dispatch(RefreshPositioning.Handler handler) {
			handler.onRefreshPositioning(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onRefreshPositioning(RefreshPositioning event);
		}
	}
}
