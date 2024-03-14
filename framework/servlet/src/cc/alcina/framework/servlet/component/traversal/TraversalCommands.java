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
		name = "reload2",
		description = "Reload2 the app -and- redeploy the console",
		filter = AppSuggestorCommand.Filter.IsConsole.class)
	@KeyBinding(key = "2")
	public static class ReloadApp2
			extends TraversalCommands<Object, ReloadApp2.Handler> {
		@Override
		public void dispatch(ReloadApp2.Handler handler) {
			handler.onReloadApp2(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onReloadApp2(ReloadApp2 event);
		}

		@Registration({ TopLevelHandler.class, ReloadApp2.class })
		public static class HandlerImpl implements Handler, TopLevelHandler {
			@Override
			public void onReloadApp2(ReloadApp2 event) {
				/*
				 * FIXME - trav - per-environment registry/MessageManager
				 */
				StatusModule.get().showMessageTransitional(
						"Reloading dev console + not really");
				Environment.get().flush();
			}
		}
	}

	@AppSuggestorCommand(
		parent = TraversalCommands.class,
		name = "clear filter",
		description = "Clear the filter",
		filter = AppSuggestorCommand.Filter.IsConsole.class)
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
