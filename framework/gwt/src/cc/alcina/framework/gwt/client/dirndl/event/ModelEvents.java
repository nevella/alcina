package cc.alcina.framework.gwt.client.dirndl.event;

import cc.alcina.framework.common.client.domain.search.ModelSearchResults;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent.DescendantEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent.NoHandlerRequired;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

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

	public static class ApplicationHelp
			extends ModelEvent<Object, ApplicationHelp.Handler> {
		@Override
		public void dispatch(ApplicationHelp.Handler handler) {
			handler.onApplicationHelp(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onApplicationHelp(ApplicationHelp event);
		}
	}

	public static class Apply extends ModelEvent<Object, Apply.Handler> {
		@Override
		public void dispatch(Apply.Handler handler) {
			handler.onApply(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onApply(Apply event);
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

	public static class BeforeClosed
			extends ModelEvent<Object, BeforeClosed.Handler>
			implements NoHandlerRequired {
		@Override
		public void dispatch(BeforeClosed.Handler handler) {
			handler.onBeforeClosed(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onBeforeClosed(BeforeClosed event);
		}
	}

	/**
	 * <p>
	 * Emitted by multiple-item and single-item selection sources, such as
	 * {@code Choices.Multiple}, {@code Choices.Single}), before the selection
	 * changes are dispatched. This allows code which changes the value of a
	 * selection (by, say creating a new model which fulfils a suggestion
	 * contract) to be called before the 'selected' event handlers (which expect
	 * a model) fire
	 *
	 */
	public static class BeforeSelectionChangedDispatch
			extends ModelEvent<Object, BeforeSelectionChangedDispatch.Handler>
			implements NoHandlerRequired {
		@Override
		public void dispatch(BeforeSelectionChangedDispatch.Handler handler) {
			handler.onBeforeSelectionChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onBeforeSelectionChanged(BeforeSelectionChangedDispatch event);
		}
	}

	/**
	 * Allow the selected object to react to selection (say by keyboard
	 * selection) prior to dispatch
	 * 
	 * Note, the receiver must check that the model is itself (i.e. the
	 * Choice.value)
	 */
	public static class BeforeSelectionChangedDispatchDescent extends
			ModelEvent.DescendantEvent<Object, BeforeSelectionChangedDispatchDescent.Handler, BeforeSelectionChangedDispatchDescent.Emitter> {
		@Override
		public void dispatch(
				BeforeSelectionChangedDispatchDescent.Handler handler) {
			handler.onBeforeSelectionChangedDispatchDescent(this);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		public interface Handler extends NodeEvent.Handler {
			void onBeforeSelectionChangedDispatchDescent(
					BeforeSelectionChangedDispatchDescent event);
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
			implements NoHandlerRequired, ValueChange {
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

	/**
	 * <p>
	 * Note - 'close' is an event modelling a request ('please close x') -
	 * 'Closed' is an event modelling an occurrence ('x was closed')
	 *
	 * <p>
	 * So clicking on a tiny little x in a UI will generally be modelled as
	 * {@code Click -> Close}, just prior to the model representation then
	 * being'closed' (removed from the UI) it could then emit a {@code Closed}
	 * event
	 *
	 */
	public static class Close extends ModelEvent<Object, Close.Handler>
			implements NoHandlerRequired {
		@Override
		public void dispatch(Close.Handler handler) {
			handler.onClose(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onClose(Close event);
		}
	}

	public static class Closed extends ModelEvent<Object, Closed.Handler>
			implements NoHandlerRequired {
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

	public static class Copy extends ModelEvent<Object, Copy.Handler> {
		@Override
		public void dispatch(Copy.Handler handler) {
			handler.onCopy(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onCopy(Copy event);
		}
	}

	public static class CopyToClipboard
			extends ModelEvent<String, CopyToClipboard.Handler> {
		@Override
		public void dispatch(CopyToClipboard.Handler handler) {
			handler.onCopyToClipboard(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onCopyToClipboard(CopyToClipboard event);
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

	public static class FilterContents extends
			DescendantEvent<Object, FilterContents.Handler, FilterContents.Emitter> {
		@Override
		public void dispatch(FilterContents.Handler handler) {
			handler.onFilterContents(this);
		}

		public String provideFilterValue() {
			ModelEvents.Input triggeringInput = getContext()
					.getPreviousEvent(ModelEvents.Input.class);
			return triggeringInput.getCurrentValue();
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		public interface Handler extends NodeEvent.Handler {
			void onFilterContents(FilterContents event);
		}
	}

	public interface FilterContentsElement {
		boolean matchesFilter(String filterString);
	}

	public interface FilterContentsFilterable extends FilterContents.Handler {
		boolean matchesFilter(String filterString);

		@Override
		default void onFilterContents(FilterContents event) {
			String filterValue = event.provideFilterValue();
			setVisible(matchesFilter(filterValue));
		}

		void setVisible(boolean visible);

		public static abstract class Abstract extends Model.All
				implements FilterContentsFilterable {
			private boolean visible = true;

			@Binding(
				to = "display",
				transform = Binding.DisplayBlankNone.class,
				type = Type.STYLE_ATTRIBUTE)
			public boolean isVisible() {
				return visible;
			}

			public void setVisible(boolean visible) {
				set("visible", this.visible, visible,
						() -> this.visible = visible);
			}
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

	public static class FormElementLabelClicked extends
			ModelEvent.DescendantEvent<Object, FormElementLabelClicked.Handler, FormElementLabelClicked.Emitter> {
		@Override
		public void dispatch(FormElementLabelClicked.Handler handler) {
			handler.onFormElementLabelClicked(this);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		public interface Handler extends NodeEvent.Handler {
			void onFormElementLabelClicked(FormElementLabelClicked event);
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

	public static class Invalidate
			extends ModelEvent<Object, Invalidate.Handler> {
		@Override
		public void dispatch(Invalidate.Handler handler) {
			handler.onInvalidate(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onInvalidate(Invalidate event);
		}
	}

	public static class LabelClicked
			extends ModelEvent<Object, LabelClicked.Handler> {
		@Override
		public void dispatch(LabelClicked.Handler handler) {
			handler.onLabelClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onLabelClicked(LabelClicked event);
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

	public static class Opened extends ModelEvent<Object, Opened.Handler>
			implements NoHandlerRequired {
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

	public static class Refresh extends ModelEvent<Object, Refresh.Handler> {
		@Override
		public void dispatch(Refresh.Handler handler) {
			handler.onRefresh(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onRefresh(Refresh event);
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

	public static class Run extends ModelEvent<Object, Run.Handler> {
		@Override
		public void dispatch(Run.Handler handler) {
			handler.onRun(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onRun(Run event);
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

	public static class Searching extends
			ModelEvent.DescendantEvent<Object, Searching.Handler, Searching.Emitter> {
		@Override
		public void dispatch(Searching.Handler handler) {
			handler.onSearching(this);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		public interface Handler extends NodeEvent.Handler {
			void onSearching(Searching event);
		}
	}

	public static class SearchResultsReturned extends
			ModelEvent<ModelSearchResults, SearchResultsReturned.Handler> {
		@Override
		public void dispatch(SearchResultsReturned.Handler handler) {
			handler.onSearchResultsReturned(this);
		}

		@Override
		public Class<SearchResultsReturned.Handler> getHandlerClass() {
			return SearchResultsReturned.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onSearchResultsReturned(SearchResultsReturned event);
		}
	}

	/**
	 * <p>
	 * Emitted by single-item selection sources, such as {@code Choices.Single}
	 *
	 * <p>
	 * Note that the most common use (by {@link Choices}) sets the model to be
	 * the new *choice/choies* - so most of the time {@link SelectionChanged} is
	 * a better event to listen for
	 *
	 *
	 *
	 */
	public static class Selected extends ModelEvent<Object, Selected.Handler>
			implements NoHandlerRequired {
		@Override
		public void dispatch(Selected.Handler handler) {
			handler.onSelected(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelected(Selected event);
		}
	}

	/**
	 * <p>
	 * Emitted by multiple-item and single-item selection sources, such as
	 * {@code Choices.Multiple}, {@code Choices.Single}) .
	 *
	 * <p>
	 * Not to be confused with the selectionchange DOM event
	 *
	 * <p>
	 * Note that the most common use (by {@link Choices}) sets the model to be
	 * the new *value/values*
	 *
	 *
	 *
	 */
	public static class SelectionChanged
			extends ModelEvent<Object, SelectionChanged.Handler>
			implements ValueChange {
		@Override
		public void dispatch(SelectionChanged.Handler handler) {
			handler.onSelectionChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionChanged(SelectionChanged event);
		}
	}

	/**
	 * When a suggestor/choices contains complex models, the model may itself
	 * handle a selection (click on it). But the container should still be
	 * cleaned up (e.g. overlay dismissed), so this event indiciates "don't
	 * perform the default selection *consequence* action, but do cleanup"
	 */
	public static class SelectionHandled
			extends ModelEvent<Object, SelectionHandled.Handler> {
		@Override
		public void dispatch(SelectionHandled.Handler handler) {
			handler.onSelectionHandled(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionHandled(SelectionHandled event);
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
	 * Allow - say - child components to handle global keyboard shortcut
	 * triggered events. The top-level component fires an event of this type,
	 * and they receive and optionally handle it
	 */
	public static class TopLevelMissedEvent extends
			DescendantEvent<ModelEvent, TopLevelMissedEvent.Handler, TopLevelMissedEvent.Emitter> {
		public static Topic<TopLevelMissedEvent> topicNotHandled() {
			return EventFrame.get().topicTopLevelMissedEvent;
		}

		boolean handled;

		@Override
		public void dispatch(TopLevelMissedEvent.Handler handler) {
			handler.onTopLevelMissedEvent(this);
		}

		public void handled() {
			TopLevelMissedEvent previous = (TopLevelMissedEvent) getContext()
					.getPrevious().getNodeEvent();
			previous.handled = true;
		}

		@Override
		protected void onDispatchComplete() {
			if (!handled) {
				topicNotHandled().publish(this);
			}
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		public interface Handler extends NodeEvent.Handler {
			void onTopLevelMissedEvent(TopLevelMissedEvent event);
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
