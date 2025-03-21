package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentOptionalRegistration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.servlet.ServletLayerTopics;
import cc.alcina.framework.servlet.environment.RemoteUi;

@Feature.Ref(Feature_TraversalBrowser_AppSuggestorImplementation.Shortcuts.class)
@AppSuggestorCommand(
	contexts = TraversalBrowser.CommandContext.class,
	name = "app")
public abstract class TraversalCommand<T, H extends NodeEvent.Handler>
		extends ModelEvent<T, H> implements AppSuggestorEvent {
	@AppSuggestorCommand(
		parent = TraversalCommand.class,
		name = "reload",
		description = "Reload the app -and- redeploy the console",
		filter = AppSuggestorCommand.Filter.IsConsole.class)
	@KeyBinding(key = "R")
	public static class ReloadApp
			extends TraversalCommand<Object, ReloadApp.Handler> {
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
						.reloadApp("Reloading dev console + traversal view");
			}
		}
	}

	@AppSuggestorCommand(
		parent = TraversalCommand.class,
		name = "Focus search bar",
		description = "Focus the app search bar")
	@KeyBinding(key = "/", context = TraversalBrowser.CommandContext.class)
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
		parent = TraversalCommand.class,
		name = "clear filter",
		description = "Clear the filter")
	@KeyBinding(key = "C")
	public static class ClearFilter
			extends TraversalCommand<Object, ClearFilter.Handler> {
		@Override
		public void dispatch(ClearFilter.Handler handler) {
			handler.onClearFilter(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onClearFilter(ClearFilter event);
		}
	}

	@AppSuggestorCommand(
		parent = TraversalCommand.class,
		name = "keyboard shortcuts",
		description = "Show the keyboard shortcuts")
	@KeyBinding(key = "K")
	public static class ShowKeyboardShortcuts
			extends TraversalCommand<Object, ShowKeyboardShortcuts.Handler> {
		@Override
		public void dispatch(ShowKeyboardShortcuts.Handler handler) {
			handler.onShowKeyboardShortcuts(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onShowKeyboardShortcuts(ShowKeyboardShortcuts event);
		}
	}

	@AppSuggestorCommand(
		parent = TraversalCommand.class,
		name = "exec commands",
		description = "Show the exec commands list")
	@KeyBinding(key = "E")
	public static class ShowExecCommands
			extends TraversalCommand<Object, ShowExecCommands.Handler> {
		@Override
		public void dispatch(ShowExecCommands.Handler handler) {
			handler.onShowExecCommands(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onShowExecCommands(ShowExecCommands event);
		}
	}
}
