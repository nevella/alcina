package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.servlet.ServletLayerTopics;

@AppSuggestorCommand(contexts = TraversalContext.class, name = "app")
public abstract class TraversalCommands<T, H extends NodeEvent.Handler>
		extends ModelEvent<T, H> implements AppSuggestorEvent {
	@AppSuggestorCommand(
		parent = TraversalCommands.class,
		name = "reload",
		description = "Reload the app -and- redeploy the console")
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
				Ax.sysLogHigh("Command - ReloadApp");
				ServletLayerTopics.topicRestartConsole.signal();
			}
		}
	}
}
