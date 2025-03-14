package cc.alcina.framework.servlet.component.gallery;

import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class GalleryEvents {
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

	@KeyBinding(key = ".", context = GalleryBrowser.CommandContext.class)
	public static class NextSelectable
			extends ModelEvent<Object, NextSelectable.Handler> {
		@Override
		public void dispatch(NextSelectable.Handler handler) {
			handler.onNextSelectable(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onNextSelectable(NextSelectable event);
		}
	}

	@KeyBinding(key = ",", context = GalleryBrowser.CommandContext.class)
	public static class PreviousSelectable
			extends ModelEvent<Object, PreviousSelectable.Handler> {
		@Override
		public void dispatch(PreviousSelectable.Handler handler) {
			handler.onPreviousSelectable(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPreviousSelectable(PreviousSelectable event);
		}
	}

	public static class LoadGallery
			extends ModelEvent<String, LoadGallery.Handler> {
		@Override
		public void dispatch(LoadGallery.Handler handler) {
			handler.onLoadGallery(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onLoadGallery(LoadGallery event);
		}
	}

	public static class HighlightModelChanged extends
			ModelEvent.DescendantEvent<Object, HighlightModelChanged.Handler, HighlightModelChanged.Emitter> {
		@Override
		public void dispatch(HighlightModelChanged.Handler handler) {
			handler.onHighlightModelChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onHighlightModelChanged(HighlightModelChanged event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}

	public static class SelectedIndexChanged extends
			ModelEvent.DescendantEvent<Object, SelectedIndexChanged.Handler, SelectedIndexChanged.Emitter> {
		@Override
		public void dispatch(SelectedIndexChanged.Handler handler) {
			handler.onSelectedIndexChanged(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectedIndexChanged(SelectedIndexChanged event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}
	}
}
