package cc.alcina.framework.gwt.client.dirndl.event;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent.NoHandlerRequired;

/**
 * <p>
 * A collection of standard logical events. See naming notes in
 * {@code ModelEvents} - note that I have no idea when to use {@code Commit} vs
 * {@code Submit}, working on it...at the moment, rule of thumb is "Submit if
 * there's likely to be a form involved".
 *
 * <p>
 * Note that these 'events' are really english language imperative verb forms
 * (or infinitives without 'to') - that's the shortest...
 *
 * <p>
 * FIXME - there's possible confusion between 'Close' and 'Closed' -
 */
public class ModelEvents {
	public static class Add extends ModelEvent<Object, Add.Handler> {
		@Override
		public void dispatch(Add.Handler handler) {
			handler.onAdd(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onBack(Back event);
		}
	}

	public static class Cancel extends ModelEvent<Object, Cancel.Handler> {
		@Override
		public void dispatch(Cancel.Handler handler) {
			handler.onCancel(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onCancel(Cancel event);
		}
	}

	public static class Change extends ModelEvent<Object, Change.Handler>
			implements NoHandlerRequired {
		@Override
		public void dispatch(Change.Handler handler) {
			handler.onChange(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onClear(Clear event);
		}
	}

	public static class Close extends ModelEvent<Object, Close.Handler> {
		@Override
		public void dispatch(Close.Handler handler) {
			handler.onClose(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onClosed(Closed event);
		}
	}

	public static class Commit extends ModelEvent<Object, Commit.Handler> {
		@Override
		public void dispatch(Commit.Handler handler) {
			handler.onCommit(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onCommit(Commit event);
		}
	}

	public static class Create extends ModelEvent<Object, Create.Handler> {
		@Override
		public void dispatch(Create.Handler handler) {
			handler.onCreate(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onCreate(Create event);
		}
	}

	public static class Delete extends ModelEvent<Object, Delete.Handler> {
		@Override
		public void dispatch(Delete.Handler handler) {
			handler.onDelete(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onDownload(Download event);
		}
	}

	public static class Edit extends ModelEvent<Object, Edit.Handler> {
		@Override
		public void dispatch(Edit.Handler handler) {
			handler.onEdit(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onEdit(Edit event);
		}
	}

	public static class Expand extends ModelEvent<Object, Expand.Handler> {
		@Override
		public void dispatch(Expand.Handler handler) {
			handler.onExpand(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onFilter(Filter event);
		}
	}

	public static class Find extends ModelEvent<Object, Find.Handler> {
		@Override
		public void dispatch(Find.Handler handler) {
			handler.onFind(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onForward(Forward event);
		}
	}

	/**
	 * Model version of a DOM input event (input element value has changed, but
	 * has not been 'committed') - see
	 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/input_event
	 *
	 * 
	 *
	 */
	public static class Input extends ModelEvent<String, Input.Handler>
			implements NoHandlerRequired {
		@Override
		public void dispatch(Input.Handler handler) {
			handler.onInput(this);
		}

		public String getCurrentValue() {
			return getModel();
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

		
		public interface Handler extends NodeEvent.Handler {
			void onInsert(Insert event);
		}
	}

	public static class Login extends ModelEvent<Object, Login.Handler> {
		@Override
		public void dispatch(Login.Handler handler) {
			handler.onLogin(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onLogout(Logout event);
		}
	}

	public static class Next extends ModelEvent<Object, Next.Handler> {
		@Override
		public void dispatch(Next.Handler handler) {
			handler.onNext(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onNext(Next event);
		}
	}

	public static class Opened extends ModelEvent<Object, Opened.Handler> {
		@Override
		public void dispatch(Opened.Handler handler) {
			handler.onOpened(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onOpened(Opened event);
		}
	}

	public static class Options extends ModelEvent<Object, Options.Handler> {
		@Override
		public void dispatch(Options.Handler handler) {
			handler.onOptions(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onRemove(Remove event);
		}
	}

	public static class Search extends ModelEvent<Object, Search.Handler> {
		@Override
		public void dispatch(Search.Handler handler) {
			handler.onSearch(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onSearch(Search event);
		}
	}

	/**
	 * Emitted by single-item selection sources, such as {@code Choices.Single}
	 *
	 * 
	 *
	 */
	public static class Selected extends ModelEvent<Object, Selected.Handler> {
		@Override
		public void dispatch(Selected.Handler handler) {
			handler.onSelected(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onSelected(Selected event);
		}
	}

	/**
	 * Emitted by multiple-item and single-item selection sources, such as
	 * {@code Choices.Multiple}, {@code Choices.Single}) .
	 *
	 * Not to be confused with the selectionchange DOM event
	 *
	 * 
	 *
	 */
	public static class SelectionChanged
			extends ModelEvent<Object, SelectionChanged.Handler> {
		@Override
		public void dispatch(SelectionChanged.Handler handler) {
			handler.onSelectionChanged(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onShow(Show event);
		}
	}

	public static class Submit extends ModelEvent<Object, Submit.Handler> {
		@Override
		public void dispatch(Submit.Handler handler) {
			handler.onSubmit(this);
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

		
		public interface Handler extends NodeEvent.Handler {
			void onToggle(Toggle event);
		}
	}

	/**
	 * The nested model knows (with its cleverness) that it's the source of the
	 * transform. But of course the ancestor decides whether it should refresh
	 * the transform
	 *
	 * FIXME - dirndl 1x1e - for top-level properties of a transformed object,
	 * should be replaced by a listener controlled and defined by the transform.
	 * Use only for deeper properties of an object that have essentially a
	 * computed representation (a direct representation will be handled by the
	 * property's propertychangelistener)
	 *
	 */
	public static class TransformSourceModified
			extends ModelEvent<Object, TransformSourceModified.Handler> {
		@Override
		public void dispatch(TransformSourceModified.Handler handler) {
			handler.onTransformSourceModified(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onTransformSourceModified(TransformSourceModified event);
		}
	}

	public static class View extends ModelEvent<Object, View.Handler> {
		@Override
		public void dispatch(View.Handler handler) {
			handler.onView(this);
		}

		
		public interface Handler extends NodeEvent.Handler {
			void onView(View event);
		}
	}
}
