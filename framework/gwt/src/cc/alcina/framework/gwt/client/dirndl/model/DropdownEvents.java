package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.behaviour.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;

public class DropdownEvents {
	public static class DropdownButtonClicked
			extends ModelEvent<Object, DropdownButtonClicked.Handler> {
		@Override
		public void dispatch(DropdownButtonClicked.Handler handler) {
			handler.onDropdownButtonClicked(this);
		}

		@Override
		public Class<DropdownButtonClicked.Handler> getHandlerClass() {
			return DropdownButtonClicked.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onDropdownButtonClicked(DropdownButtonClicked event);
		}
	}

	public static class InsideDropdownClicked
			extends ModelEvent<Object, InsideDropdownClicked.Handler> {
		@Override
		public void dispatch(InsideDropdownClicked.Handler handler) {
			handler.onInsideDropdownClicked(this);
		}

		@Override
		public Class<InsideDropdownClicked.Handler> getHandlerClass() {
			return InsideDropdownClicked.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onInsideDropdownClicked(InsideDropdownClicked event);
		}
	}

	public static class OutsideDropdownClicked
			extends ModelEvent<Object, OutsideDropdownClicked.Handler> {
		@Override
		public void dispatch(OutsideDropdownClicked.Handler handler) {
			handler.onOutsideDropdownClicked(this);
		}

		@Override
		public Class<OutsideDropdownClicked.Handler> getHandlerClass() {
			return OutsideDropdownClicked.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onOutsideDropdownClicked(OutsideDropdownClicked event);
		}
	}
}
