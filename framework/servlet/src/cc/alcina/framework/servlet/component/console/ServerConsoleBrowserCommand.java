package cc.alcina.framework.servlet.component.console;

import com.google.gwt.dom.client.NativeEvent.Modifier;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentOptionalRegistration;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.servlet.environment.RemoteUi;

/**
 * FIXME - romcom - these are duplicated for many different apps - maybe add a
 * commanprovider which can cut down on duplication
 */
@AppSuggestorCommand(
	contexts = ServerConsoleBrowser.CommandContext.class,
	name = "app")
public abstract class ServerConsoleBrowserCommand<T, H extends NodeEvent.Handler>
		extends ModelEvent<T, H> implements AppSuggestorEvent {
	@AppSuggestorCommand(
		parent = ServerConsoleBrowserCommand.class,
		name = "property display: cycle",
		description = "Property display: cycle mode")
	@KeyBinding(key = "p")
	public static class PropertyDisplayCycle extends
			ServerConsoleBrowserCommand<Object, PropertyDisplayCycle.Handler> {
		@Override
		public void dispatch(PropertyDisplayCycle.Handler handler) {
			handler.onPropertyDisplayCycle(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPropertyDisplayCycle(PropertyDisplayCycle event);
		}
	}

	@AppSuggestorCommand(
		parent = ServerConsoleBrowserCommand.class,
		name = "reload",
		description = "Reload the app -and- redeploy the console",
		filter = AppSuggestorCommand.Filter.IsConsole.class)
	@KeyBinding(key = "R")
	public static class ReloadApp
			extends ServerConsoleBrowserCommand<Object, ReloadApp.Handler> {
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
				RemoteUi.get().reloadApp(
						"Reloading dev console + report browser view");
			}
		}
	}

	@AppSuggestorCommand(
		parent = ServerConsoleBrowserCommand.class,
		name = "Focus search bar",
		description = "Focus the app search bar")
	@KeyBinding(key = "/", context = ServerConsoleBrowser.CommandContext.class)
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
		parent = ServerConsoleBrowserCommand.class,
		name = "clear filter",
		description = "Clear the filter")
	@KeyBinding(key = "C")
	public static class ClearFilter
			extends ServerConsoleBrowserCommand<Object, ClearFilter.Handler> {
		@Override
		public void dispatch(ClearFilter.Handler handler) {
			handler.onClearFilter(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onClearFilter(ClearFilter event);
		}
	}

	@AppSuggestorCommand(
		parent = ServerConsoleBrowserCommand.class,
		name = "sequence column set: cycle",
		description = "ServerConsole column set: cycle mode")
	@KeyBinding(key = "c", modifiers = Modifier.SHIFT)
	public static class ColumnSetCycle
			extends ModelEvent<Object, ColumnSetCycle.Handler> {
		@Override
		public void dispatch(ColumnSetCycle.Handler handler) {
			handler.onColumnSetCycle(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onColumnSetCycle(ColumnSetCycle event);
		}
	}

	@AppSuggestorCommand(
		parent = ServerConsoleBrowserCommand.class,
		name = "keyboard shortcuts",
		description = "Show the keyboard shortcuts")
	@KeyBinding(key = "K")
	public static class ShowKeyboardShortcuts extends
			ServerConsoleBrowserCommand<Object, ShowKeyboardShortcuts.Handler> {
		@Override
		public void dispatch(ShowKeyboardShortcuts.Handler handler) {
			handler.onShowKeyboardShortcuts(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onShowKeyboardShortcuts(ShowKeyboardShortcuts event);
		}
	}

	@AppSuggestorCommand(
		parent = ServerConsoleBrowserCommand.class,
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
