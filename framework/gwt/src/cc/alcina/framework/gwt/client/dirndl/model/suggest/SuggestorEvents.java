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

		@Override
		public Class<EditorAsk.Handler> getHandlerClass() {
			return EditorAsk.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onEditorAsk(EditorAsk event);
		}
	}
}
