package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class DropdownEvents {
	public static class DropdownButtonClicked
			extends ModelEvent<Object, DropdownButtonClicked.Handler> {
		@Override
		public void dispatch(DropdownButtonClicked.Handler handler) {
			handler.onDropdownButtonClicked(this);
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

		public interface Handler extends NodeEvent.Handler {
			void onOutsideDropdownClicked(OutsideDropdownClicked event);
		}
	}

	public static class ArrowClicked
			extends ModelEvent<Object, ArrowClicked.Handler> {
		@Override
		public void dispatch(ArrowClicked.Handler handler) {
			handler.onArrowClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onArrowClicked(ArrowClicked event);
		}
	}

	public static class ComboLabelSelected
			extends ModelEvent<Object, ComboLabelSelected.Handler> {
		@Override
		public void dispatch(ComboLabelSelected.Handler handler) {
			handler.onComboLabelSelected(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onComboLabelSelected(ComboLabelSelected event);
		}
	}
}
