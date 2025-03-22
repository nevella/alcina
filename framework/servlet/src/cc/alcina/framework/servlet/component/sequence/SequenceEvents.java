package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class SequenceEvents {
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

	public static class HighlightElements
			extends ModelEvent<String, HighlightElements.Handler> {
		@Override
		public void dispatch(HighlightElements.Handler handler) {
			handler.onHighlightElements(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onHighlightElements(HighlightElements event);
		}
	}

	public static class SetSettingMaxElementRows
			extends ModelEvent<String, SetSettingMaxElementRows.Handler> {
		@Override
		public void dispatch(SetSettingMaxElementRows.Handler handler) {
			handler.onSetSettingMaxElementRows(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSetSettingMaxElementRows(SetSettingMaxElementRows event);
		}
	}

	@KeyBinding(key = ".", context = SequenceBrowser.CommandContext.class)
	public static class NextSelectable
			extends ModelEvent<Object, NextSelectable.Handler> {
		@Override
		public void dispatch(NextSelectable.Handler handler) {
			handler.onNextSelectable(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onNextSelectable(NextSelectable event);
		}
	}

	@KeyBinding(key = ",", context = SequenceBrowser.CommandContext.class)
	public static class PreviousSelectable
			extends ModelEvent<Object, PreviousSelectable.Handler> {
		@Override
		public void dispatch(PreviousSelectable.Handler handler) {
			handler.onPreviousSelectable(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPreviousSelectable(PreviousSelectable event);
		}
	}

	public static class LoadSequence
			extends ModelEvent<String, LoadSequence.Handler> {
		@Override
		public void dispatch(LoadSequence.Handler handler) {
			handler.onLoadSequence(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onLoadSequence(LoadSequence event);
		}
	}

	public static class HighlightModelChanged extends
			ModelEvent.DescendantEvent<Object, HighlightModelChanged.Handler, HighlightModelChanged.Emitter> {
		@Override
		public void dispatch(HighlightModelChanged.Handler handler) {
			handler.onHighlightModelChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onHighlightModelChanged(HighlightModelChanged event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}

	public static class SelectedIndexChanged extends
			ModelEvent.DescendantEvent<Object, SelectedIndexChanged.Handler, SelectedIndexChanged.Emitter> {
		@Override
		public void dispatch(SelectedIndexChanged.Handler handler) {
			handler.onSelectedIndexChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectedIndexChanged(SelectedIndexChanged event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}
}
