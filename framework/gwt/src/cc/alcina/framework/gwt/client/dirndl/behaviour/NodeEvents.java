package cc.alcina.framework.gwt.client.dirndl.behaviour;

import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;

/*
 * A collection of standard logical events
 */
public class NodeEvents {
	public static class Add extends TopicEvent<Object, Add.Handler> {
		@Override
		public void dispatch(Add.Handler handler) {
			handler.onAdd(this);
		}

		@Override
		public Class<Add.Handler> getHandlerClass() {
			return Add.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onAdd(Add event);
		}
	}

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

	public static class Change extends TopicEvent<Object, Change.Handler> {
		@Override
		public void dispatch(Change.Handler handler) {
			handler.onChange(this);
		}

		@Override
		public Class<Change.Handler> getHandlerClass() {
			return Change.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onChange(Change event);
		}
	}

	public static class Clear extends TopicEvent<Object, Clear.Handler> {
		@Override
		public void dispatch(Clear.Handler handler) {
			handler.onClear(this);
		}

		@Override
		public Class<Clear.Handler> getHandlerClass() {
			return Clear.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onClear(Clear event);
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

	public static class Delete extends TopicEvent<Object, Delete.Handler> {
		@Override
		public void dispatch(Delete.Handler handler) {
			handler.onDelete(this);
		}

		@Override
		public Class<Delete.Handler> getHandlerClass() {
			return Delete.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onDelete(Delete event);
		}
	}

	public static class Filter extends TopicEvent<Object, Filter.Handler> {
		@Override
		public void dispatch(Filter.Handler handler) {
			handler.onFilter(this);
		}

		@Override
		public Class<Filter.Handler> getHandlerClass() {
			return Filter.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onFilter(Filter event);
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

	public static class Options extends TopicEvent<Object, Options.Handler> {
		@Override
		public void dispatch(Options.Handler handler) {
			handler.onOptions(this);
		}

		@Override
		public Class<Options.Handler> getHandlerClass() {
			return Options.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onOptions(Options event);
		}
	}

	public static class Remove extends TopicEvent<Object, Remove.Handler> {
		@Override
		public void dispatch(Remove.Handler handler) {
			handler.onRemove(this);
		}

		@Override
		public Class<Remove.Handler> getHandlerClass() {
			return Remove.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onRemove(Remove event);
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
