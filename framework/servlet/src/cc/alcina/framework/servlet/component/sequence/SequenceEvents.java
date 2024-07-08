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

	@KeyBinding(key = ".", context = SequenceBrowser.CommandContext.class)
	public static class NextHighlight
			extends ModelEvent<Object, NextHighlight.Handler> {
		@Override
		public void dispatch(NextHighlight.Handler handler) {
			handler.onNextHighlight(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onNextHighlight(NextHighlight event);
		}
	}

	@KeyBinding(key = ",", context = SequenceBrowser.CommandContext.class)
	public static class PreviousHighlight
			extends ModelEvent<Object, PreviousHighlight.Handler> {
		@Override
		public void dispatch(PreviousHighlight.Handler handler) {
			handler.onPreviousHighlight(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPreviousHighlight(PreviousHighlight event);
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
}
