package cc.alcina.framework.gwt.client.dirndl.event;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class ReflectedEvents {
	public static class Back
			extends ReflectedEvent<Object, Back.Handler, Back.Emitter>
			implements ReflectedEvent.NotStored {
		@Override
		public void dispatch(Back.Handler handler) {
			handler.onBack(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onBack(Back event);
		}

		public interface Binding extends Handler {
			@Override
			default void onBack(Back event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}

	public static class Forward
			extends ReflectedEvent<Object, Forward.Handler, Forward.Emitter>
			implements ReflectedEvent.NotStored {
		@Override
		public void dispatch(Forward.Handler handler) {
			handler.onForward(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onForward(Forward event);
		}

		public interface Binding extends Handler {
			@Override
			default void onForward(Forward event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}

	public static class ZoomIn
			extends ReflectedEvent<Object, ZoomIn.Handler, ZoomIn.Emitter>
			implements ReflectedEvent.NotStored {
		@Override
		public void dispatch(ZoomIn.Handler handler) {
			handler.onZoomIn(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onZoomIn(ZoomIn event);
		}

		public interface Binding extends Handler {
			@Override
			default void onZoomIn(ZoomIn event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}

	public static class ZoomOut
			extends ReflectedEvent<Object, ZoomOut.Handler, ZoomOut.Emitter>
			implements ReflectedEvent.NotStored {
		@Override
		public void dispatch(ZoomOut.Handler handler) {
			handler.onZoomOut(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onZoomOut(ZoomOut event);
		}

		public interface Binding extends Handler {
			@Override
			default void onZoomOut(ZoomOut event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}

		public interface Emitter extends ModelEvent.Emitter {
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
			ReflectedEvent<Object, BeforeSelectionChangedDispatchDescent.Handler, BeforeSelectionChangedDispatchDescent.Emitter> {
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

	public static class FormElementLabelClicked extends
			ReflectedEvent<Object, FormElementLabelClicked.Handler, FormElementLabelClicked.Emitter> {
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

	public static class Searching extends
			ReflectedEvent<Object, Searching.Handler, Searching.Emitter> {
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

	/**
	 * For large component structures, have the service root emit a PlaceChanged
	 * reflected event, rather than each subcomponent listening on the GWT event
	 * system. Bind handling etc is significantly easier
	 */
	public static class PlaceChanged extends
			ReflectedEvent<Place, PlaceChanged.Handler, PlaceChanged.Emitter> {
		@Override
		public void dispatch(PlaceChanged.Handler handler) {
			handler.onPlaceChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPlaceChanged(PlaceChanged event);
		}

		public interface Binding extends Handler, NodeEvent.TypeBinding {
			@Override
			default void onPlaceChanged(PlaceChanged event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}

	/**
	 * Application (browser-wide) events, essentially a recasting of events from
	 * other sources (window:scroll - history - etc)
	 */
	public interface Global {
		public static class WindowScroll extends
				ReflectedEvent<Integer, WindowScroll.Handler, WindowScroll.Emitter> {
			@Override
			public void dispatch(WindowScroll.Handler handler) {
				handler.onWindowScroll(this);
			}

			public interface Handler extends NodeEvent.Handler {
				void onWindowScroll(WindowScroll event);
			}

			public interface Binding extends Handler {
				@Override
				default void onWindowScroll(WindowScroll event) {
					((Model) this).bindings().onNodeEvent(event);
				}
			}

			public interface Emitter extends ModelEvent.Emitter {
			}
		}

		public static class HistoryChange extends
				ReflectedEvent<String, HistoryChange.Handler, HistoryChange.Emitter> {
			@Override
			public void dispatch(HistoryChange.Handler handler) {
				handler.onHistoryChange(this);
			}

			public interface Handler extends NodeEvent.Handler {
				void onHistoryChange(HistoryChange event);
			}

			public interface Binding extends Handler {
				@Override
				default void onHistoryChange(HistoryChange event) {
					((Model) this).bindings().onNodeEvent(event);
				}
			}

			public interface Emitter extends ModelEvent.Emitter {
			}
		}

		public interface Emitter extends WindowScroll.Emitter,
				HistoryChange.Emitter, LayoutEvents.Bind.Handler {
			public static class Support implements LayoutEvents.Bind.Handler {
				HandlerRegistration historyChangeHandlerRef;

				Node node;

				HandlerRegistration scrollHandlerRef;

				@Override
				public void onBind(Bind event) {
					this.node = event.getContext().node;
					if (event.isBound()) {
						historyChangeHandlerRef = History.addValueChangeHandler(
								change -> ((Model) node.getModel()).emitEvent(
										HistoryChange.class, change));
						scrollHandlerRef = Window.addWindowScrollHandler(
								evt -> ((Model) node.getModel()).emitEvent(
										WindowScroll.class,
										evt.getScrollTop()));
					} else {
						historyChangeHandlerRef.removeHandler();
						scrollHandlerRef.removeHandler();
					}
				}
			}

			@Override
			default void onBind(Bind event) {
				getGlobalEventsEmitterSupport().onBind(event);
			}

			Emitter.Support getGlobalEventsEmitterSupport();
		}
	}

	/**
	 * Instructs any editor (a model analagous to select, input etc) to focus
	 * itself
	 */
	public static class FocusEditor extends
			ReflectedEvent<Object, FocusEditor.Handler, FocusEditor.Emitter> {
		@Override
		public void dispatch(FocusEditor.Handler handler) {
			handler.onFocusEditor(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onFocusEditor(FocusEditor event);
		}

		public interface Binding extends Handler {
			@Override
			default void onFocusEditor(FocusEditor event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}

	/**
	 * Instructs any editor (a model analagous to select, input etc) to commit
	 * itself - i.e. to copy any pending (input) value to its value field
	 */
	public static class CommitEditor extends
			ReflectedEvent<Object, CommitEditor.Handler, CommitEditor.Emitter> {
		@Override
		public void dispatch(CommitEditor.Handler handler) {
			handler.onCommitEditor(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onCommitEditor(CommitEditor event);
		}

		public interface Binding extends Handler {
			@Override
			default void onCommitEditor(CommitEditor event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}

	/**
	 * Allow - say - child components to handle global keyboard shortcut
	 * triggered events. The top-level component fires an event of this type,
	 * and they receive and optionally handle it
	 */
	public static class TopLevelMissedEvent extends
			ReflectedEvent<ModelEvent, TopLevelMissedEvent.Handler, TopLevelMissedEvent.Emitter> {
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

	public static class Filter
			extends ReflectedEvent<Object, Filter.Handler, Filter.Emitter> {
		@Override
		public void dispatch(Filter.Handler handler) {
			handler.onFilter(this);
		}

		public String provideFilterValue() {
			ModelEvents.Input triggeringInput = getContext()
					.getPreviousEvent(ModelEvents.Input.class);
			return triggeringInput.getCurrentValue();
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		public interface Handler extends NodeEvent.Handler {
			void onFilter(Filter event);
		}
	}
}
