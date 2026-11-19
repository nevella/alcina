package cc.alcina.framework.gwt.client.dirndl.event;

import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class PageEvents {
	public static class ClearScrollDown
			extends ModelEvent<Object, ClearScrollDown.Handler> {
		@Override
		public void dispatch(ClearScrollDown.Handler handler) {
			handler.onClearScrollDown(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onClearScrollDown(ClearScrollDown event);
		}
	}

	public static class FixedElementsChanged
			extends ModelEvent<Object, FixedElementsChanged.Handler> {
		@Override
		public void dispatch(FixedElementsChanged.Handler handler) {
			handler.onFixedElementsChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onFixedElementsChanged(FixedElementsChanged event);
		}
	}

	public static class ShowingFullHeightPanel
			extends ModelEvent<Boolean, ShowingFullHeightPanel.Handler> {
		@Override
		public void dispatch(ShowingFullHeightPanel.Handler handler) {
			handler.onShowingFullHeightPanel(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onShowingFullHeightPanel(ShowingFullHeightPanel event);
		}

		public interface Binding extends Handler {
			@Override
			default void
					onShowingFullHeightPanel(ShowingFullHeightPanel event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}

	public static class ShowingPanel
			extends ModelEvent<Boolean, ShowingPanel.Handler> {
		@Override
		public void dispatch(ShowingPanel.Handler handler) {
			handler.onShowingPanel(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onShowingPanel(ShowingPanel event);
		}

		public interface Binding extends Handler {
			@Override
			default void onShowingPanel(ShowingPanel event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}

	public static class PageTitleChange
			extends ModelEvent<String, PageTitleChange.Handler> {
		@Override
		public void dispatch(PageTitleChange.Handler handler) {
			handler.onPageTitleChange(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPageTitleChange(PageTitleChange event);
		}
	}

	public static class WindowTitleChange
			extends ModelEvent<Object, WindowTitleChange.Handler> {
		@Override
		public void dispatch(WindowTitleChange.Handler handler) {
			handler.onWindowTitleChange(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onWindowTitleChange(WindowTitleChange event);
		}

		public interface Binding extends Handler {
			@Override
			default void onWindowTitleChange(WindowTitleChange event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}

	public static class SectionPathChange
			extends ModelEvent<String, SectionPathChange.Handler> {
		public String getSectionPath() {
			return getModel();
		}

		@Override
		public void dispatch(SectionPathChange.Handler handler) {
			handler.onSectionPathChange(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSectionPathChange(SectionPathChange event);
		}
	}
}
