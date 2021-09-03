package cc.alcina.framework.gwt.client.dirndl.behaviour;

import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;

public class NodeEvents {
	public static class Back extends TopicEvent<Object, Back.Handler> {
		@Override
		public void dispatch(Back.Handler handler) {
			handler.onBack(this);
		}

		@Override
		public Class<Back.Handler> getHandlerClass() {
			return Back.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onBack(Back event);
		}
	}

	public static class Cancelled
			extends TopicEvent<Object, Cancelled.Handler> {
		@Override
		public void dispatch(Cancelled.Handler handler) {
			handler.onCancelled(this);
		}

		@Override
		public Class<Cancelled.Handler> getHandlerClass() {
			return Cancelled.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onCancelled(Cancelled event);
		}
	}

	public static class Close extends TopicEvent<Object, Close.Handler> {
		@Override
		public void dispatch(Close.Handler handler) {
			handler.onClose(this);
		}

		@Override
		public Class<Close.Handler> getHandlerClass() {
			return Close.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onClose(Close event);
		}
	}

	public static class Forward extends TopicEvent<Object, Forward.Handler> {
		@Override
		public void dispatch(Forward.Handler handler) {
			handler.onForward(this);
		}

		@Override
		public Class<Forward.Handler> getHandlerClass() {
			return Forward.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onForward(Forward event);
		}
	}

	public static class Submitted
			extends TopicEvent<Object, Submitted.Handler> {
		@Override
		public void dispatch(Submitted.Handler handler) {
			handler.onSubmitted(this);
		}

		@Override
		public Class<Submitted.Handler> getHandlerClass() {
			return Submitted.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onSubmitted(Submitted event);
		}
	}
}
