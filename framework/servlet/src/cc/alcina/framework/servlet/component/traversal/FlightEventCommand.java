package cc.alcina.framework.servlet.component.traversal;

import com.google.gwt.dom.client.NativeEvent.Modifier;

import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorEvent;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

/**
 * TODO - feature
 */
@AppSuggestorCommand(
	contexts = FlightEventCommand.CommandContext.class,
	name = "flight")
public abstract class FlightEventCommand<T, H extends NodeEvent.Handler>
		extends ModelEvent<T, H> implements AppSuggestorEvent {
	@AppSuggestorCommand(
		parent = FlightEventCommand.class,
		name = "persist flight events",
		description = "Persist current flight event selection to data folder")
	@KeyBinding(key = "l")
	public static class PersistFlightEvents
			extends FlightEventCommand<Object, PersistFlightEvents.Handler> {
		@Override
		public void dispatch(PersistFlightEvents.Handler handler) {
			handler.onPersistFlightEvents(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPersistFlightEvents(PersistFlightEvents event);
		}
	}

	@AppSuggestorCommand(
		parent = FlightEventCommand.class,
		name = "mark flight events",
		description = "Mark the start of the current flight event selection")
	@KeyBinding(key = "l", modifiers = Modifier.SHIFT)
	public static class MarkFlightEvents
			extends FlightEventCommand<Object, MarkFlightEvents.Handler> {
		@Override
		public void dispatch(MarkFlightEvents.Handler handler) {
			handler.onMarkFlightEvents(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onMarkFlightEvents(MarkFlightEvents event);
		}
	}

	public interface CommandContext extends
			cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext {
	}
}
