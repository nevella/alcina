package cc.alcina.framework.servlet.component.gallery;

import com.google.gwt.dom.client.NativeEvent.Modifier;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentOptionalRegistration;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.servlet.environment.RemoteUi;

@AppSuggestorCommand(
	contexts = GalleryBrowser.CommandContext.class,
	name = "app")
public abstract class GalleryBrowserCommand<T, H extends NodeEvent.Handler>
		extends ModelEvent<T, H> implements AppSuggestorEvent {
	@AppSuggestorCommand(
		parent = GalleryBrowserCommand.class,
		name = "reload",
		description = "Reload the app -and- redeploy the console",
		filter = AppSuggestorCommand.Filter.IsConsole.class)
	@KeyBinding(key = "R")
	public static class ReloadApp
			extends GalleryBrowserCommand<Object, ReloadApp.Handler> {
		@Override
		public void dispatch(ReloadApp.Handler handler) {
			handler.onReloadApp(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onReloadApp(ReloadApp event);
		}

		@EnvironmentOptionalRegistration(@Registration({ TopLevelHandler.class,
				ReloadApp.class }))
		public static class HandlerImpl implements Handler, TopLevelHandler {
			@Override
			public void onReloadApp(ReloadApp event) {
				RemoteUi.get()
						.reloadApp("Reloading dev console + gallery view");
			}
		}
	}

	@AppSuggestorCommand(
		parent = GalleryBrowserCommand.class,
		name = "Focus search bar",
		description = "Focus the app search bar")
	@KeyBinding(key = "/", context = GalleryBrowser.CommandContext.class)
	public static class FocusSearch
			extends ModelEvent<Object, FocusSearch.Handler> {
		@Override
		public void dispatch(FocusSearch.Handler handler) {
			handler.onFocusSearch(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onFocusSearch(FocusSearch event);
		}
	}

	@AppSuggestorCommand(
		parent = GalleryBrowserCommand.class,
		name = "clear filter",
		description = "Clear the filter")
	@KeyBinding(key = "C")
	public static class ClearFilter
			extends GalleryBrowserCommand<Object, ClearFilter.Handler> {
		@Override
		public void dispatch(ClearFilter.Handler handler) {
			handler.onClearFilter(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onClearFilter(ClearFilter event);
		}
	}

	@AppSuggestorCommand(
		parent = GalleryBrowserCommand.class,
		name = "keyboard shortcuts",
		description = "Show the keyboard shortcuts")
	@KeyBinding(key = "K")
	public static class ShowKeyboardShortcuts extends
			GalleryBrowserCommand<Object, ShowKeyboardShortcuts.Handler> {
		@Override
		public void dispatch(ShowKeyboardShortcuts.Handler handler) {
			handler.onShowKeyboardShortcuts(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onShowKeyboardShortcuts(ShowKeyboardShortcuts event);
		}
	}

	@AppSuggestorCommand(
		parent = GalleryBrowserCommand.class,
		name = "help",
		description = "Show the application help panel")
	@KeyBinding(key = "?", modifiers = Modifier.SHIFT)
	public static class ToggleHelp
			extends ModelEvent<Object, ToggleHelp.Handler> {
		@Override
		public void dispatch(ToggleHelp.Handler handler) {
			handler.onToggleHelp(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onToggleHelp(ToggleHelp event);
		}
	}
}
