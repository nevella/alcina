package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class SuggestorEvents {
	public static class EditorAsk
			extends ModelEvent<Suggestor.Ask, EditorAsk.Handler> {
		@Override
		public void dispatch(EditorAsk.Handler handler) {
			handler.onEditorAsk(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onEditorAsk(EditorAsk event);
		}
	}

	public static class EditorExit
			extends ModelEvent<Object, EditorExit.Handler> {
		@Override
		public void dispatch(EditorExit.Handler handler) {
			handler.onEditorExit(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onEditorExit(EditorExit event);
		}
	}

	public static class SuggestionsVisible
			extends ModelEvent<Boolean, SuggestionsVisible.Handler> {
		@Override
		public void dispatch(SuggestionsVisible.Handler handler) {
			handler.onShowingSuggestions(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onShowingSuggestions(SuggestionsVisible event);
		}
	}
}
