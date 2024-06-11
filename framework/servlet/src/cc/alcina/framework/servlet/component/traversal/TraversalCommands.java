package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.servlet.ServletLayerTopics;
import cc.alcina.framework.servlet.dom.Environment;

@Feature.Ref(Feature_TraversalProcessView_AppSuggestorImplementation.Shortcuts.class)
@AppSuggestorCommand(contexts = TraversalViewContext.class, name = "app")
public abstract class TraversalCommands<T, H extends NodeEvent.Handler>
		extends ModelEvent<T, H> implements AppSuggestorEvent {
	@AppSuggestorCommand(
		parent = TraversalCommands.class,
		name = "reload",
		description = "Reload the app -and- redeploy the console",
		filter = AppSuggestorCommand.Filter.IsConsole.class)
	@KeyBinding(key = "R")
	public static class ReloadApp
			extends TraversalCommands<Object, ReloadApp.Handler> {
		@Override
		public void dispatch(ReloadApp.Handler handler) {
			handler.onReloadApp(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onReloadApp(ReloadApp event);
		}

		@Registration({ TopLevelHandler.class, ReloadApp.class })
		public static class HandlerImpl implements Handler, TopLevelHandler {
			@Override
			public void onReloadApp(ReloadApp event) {
				/*
				 * FIXME - trav - per-environment registry/MessageManager
				 */
				StatusModule.get().showMessageTransitional(
						"Reloading dev console + traversal view");
				Environment.get().flush();
				ServletLayerTopics.topicRestartConsole.signal();
			}
		}
	}

	@AppSuggestorCommand(
		parent = TraversalCommands.class,
		name = "Focus search bar",
		description = "Focus the app search bar")
	@KeyBinding(key = "/", context = TraversalViewContext.class)
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
		parent = TraversalCommands.class,
		name = "clear filter",
		description = "Clear the filter")
	@KeyBinding(key = "C")
	public static class ClearFilter
			extends TraversalCommands<Object, ClearFilter.Handler> {
		@Override
		public void dispatch(ClearFilter.Handler handler) {
			handler.onClearFilter(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onClearFilter(ClearFilter event);
		}
	}
}
