package cc.alcina.framework.servlet.component.shared;

import java.util.List;

import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public interface ExecCommand<T> {
	default String name() {
		return getClass().getSimpleName();
	}

	default String description() {
		return NestedName.get(this);
	}

	void execCommand(ModelEvent event, List<T> filteredElements);

	public static class PerformCommand
			extends ModelEvent<Object, PerformCommand.Handler> {
		@Override
		public void dispatch(PerformCommand.Handler handler) {
			handler.onPerformCommand(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPerformCommand(PerformCommand event);
		}
	}
}