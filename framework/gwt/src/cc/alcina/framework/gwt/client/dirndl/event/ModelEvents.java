package cc.alcina.framework.gwt.client.dirndl.event;

/**
 * <p>
 * A collection of standard logical events. See naming notes in
 * {@code ModelEvents} - note that I have no idea when to use {@code Commit} vs
 * {@code Submit}, working on it...at the moment, rule of thumb is "Submit if
 * there's likely to be a form involved".
 */
public class ModelEvents {
	public static class Add extends ModelEvent<Object, Add.Handler> {
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

	public static class Back extends ModelEvent<Object, Back.Handler> {
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
			extends ModelEvent<Object, Cancelled.Handler> {
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

	public static class Change extends ModelEvent<Object, Change.Handler> {
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

	public static class Clear extends ModelEvent<Object, Clear.Handler> {
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

	public static class Close extends ModelEvent<Object, Close.Handler> {
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

	public static class Closed extends ModelEvent<Object, Closed.Handler> {
		@Override
		public void dispatch(Closed.Handler handler) {
			handler.onClosed(this);
		}

		@Override
		public Class<Closed.Handler> getHandlerClass() {
			return Closed.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onClosed(Closed event);
		}
	}

	public static class Commit extends ModelEvent<Object, Commit.Handler> {
		@Override
		public void dispatch(Commit.Handler handler) {
			handler.onCommit(this);
		}

		@Override
		public Class<Commit.Handler> getHandlerClass() {
			return Commit.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onCommit(Commit event);
		}
	}

	public static class Delete extends ModelEvent<Object, Delete.Handler> {
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

	public static class Download extends ModelEvent<Object, Download.Handler> {
		@Override
		public void dispatch(Download.Handler handler) {
			handler.onDownload(this);
		}

		@Override
		public Class<Download.Handler> getHandlerClass() {
			return Download.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onDownload(Download event);
		}
	}

	public static class Expand extends ModelEvent<Object, Expand.Handler> {
		@Override
		public void dispatch(Expand.Handler handler) {
			handler.onExpand(this);
		}

		@Override
		public Class<Expand.Handler> getHandlerClass() {
			return Expand.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onExpand(Expand event);
		}
	}

	public static class Filter extends ModelEvent<Object, Filter.Handler> {
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

	public static class Find extends ModelEvent<Object, Find.Handler> {
		@Override
		public void dispatch(Find.Handler handler) {
			handler.onFind(this);
		}

		@Override
		public Class<Find.Handler> getHandlerClass() {
			return Find.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onFind(Find event);
		}
	}

	public static class Forward extends ModelEvent<Object, Forward.Handler> {
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

	/**
	 * Model version of a DOM input event (input element value has changed, but
	 * has not been 'committed') - see
	 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/input_event
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static class Input extends ModelEvent<Object, Input.Handler> {
		@Override
		public void dispatch(Input.Handler handler) {
			handler.onInput(this);
		}

		@Override
		public Class<Input.Handler> getHandlerClass() {
			return Input.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onInput(Input event);
		}
	}

	public static class Insert extends ModelEvent<Object, Insert.Handler> {
		@Override
		public void dispatch(Insert.Handler handler) {
			handler.onInsert(this);
		}

		@Override
		public Class<Insert.Handler> getHandlerClass() {
			return Insert.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onInsert(Insert event);
		}
	}

	public static class Login extends ModelEvent<Object, Login.Handler> {
		@Override
		public void dispatch(Login.Handler handler) {
			handler.onLogin(this);
		}

		@Override
		public Class<Login.Handler> getHandlerClass() {
			return Login.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onLogin(Login event);
		}
	}

	public static class Logout extends ModelEvent<Object, Logout.Handler> {
		@Override
		public void dispatch(Logout.Handler handler) {
			handler.onLogout(this);
		}

		@Override
		public Class<Logout.Handler> getHandlerClass() {
			return Logout.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onLogout(Logout event);
		}
	}

	public static class Options extends ModelEvent<Object, Options.Handler> {
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

	public static class Remove extends ModelEvent<Object, Remove.Handler> {
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

	/**
	 * Emitted by single-item selection sources, such as {@code Choices.Single}
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static class Selected extends ModelEvent<Object, Selected.Handler> {
		@Override
		public void dispatch(Selected.Handler handler) {
			handler.onSelected(this);
		}

		@Override
		public Class<Selected.Handler> getHandlerClass() {
			return Selected.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelected(Selected event);
		}
	}

	/**
	 * Emitted by multiple-item and single-item selection sources, such as
	 * {@code Choices.Multiple}, {@code Choices.Single}) .
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static class SelectionChanged
			extends ModelEvent<Object, SelectionChanged.Handler> {
		@Override
		public void dispatch(SelectionChanged.Handler handler) {
			handler.onSelectionChanged(this);
		}

		@Override
		public Class<SelectionChanged.Handler> getHandlerClass() {
			return SelectionChanged.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionChanged(SelectionChanged event);
		}
	}

	public static class Show extends ModelEvent<Object, Show.Handler> {
		@Override
		public void dispatch(Show.Handler handler) {
			handler.onShow(this);
		}

		@Override
		public Class<Show.Handler> getHandlerClass() {
			return Show.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onShow(Show event);
		}
	}

	public static class Submit extends ModelEvent<Object, Submit.Handler> {
		@Override
		public void dispatch(Submit.Handler handler) {
			handler.onSubmit(this);
		}

		@Override
		public Class<Submit.Handler> getHandlerClass() {
			return Submit.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onSubmit(Submit event);
		}
	}

	public static class Toggle extends ModelEvent<Object, Toggle.Handler> {
		@Override
		public void dispatch(Toggle.Handler handler) {
			handler.onToggle(this);
		}

		@Override
		public Class<Toggle.Handler> getHandlerClass() {
			return Toggle.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onToggle(Toggle event);
		}
	}
}
