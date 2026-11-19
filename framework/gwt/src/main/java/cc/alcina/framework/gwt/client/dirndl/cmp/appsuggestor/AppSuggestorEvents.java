package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public interface AppSuggestorEvents {
	public static class Close extends ModelEvent<Object, Close.Handler> {
		@Override
		public void dispatch(Close.Handler handler) {
			handler.onClose(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onClose(Close event);
		}
	}
}
