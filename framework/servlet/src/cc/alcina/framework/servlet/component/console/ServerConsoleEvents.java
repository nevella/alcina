package cc.alcina.framework.servlet.component.console;

import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class ServerConsoleEvents {
	public static class FilterElements
			extends ModelEvent<String, FilterElements.Handler> {
		@Override
		public void dispatch(FilterElements.Handler handler) {
			handler.onFilterElements(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onFilterElements(FilterElements event);
		}
	}
}
