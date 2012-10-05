package cc.alcina.framework.gwt.client.logic.state;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.logic.IsCancellable;
import cc.alcina.framework.gwt.client.logic.state.MachineEvent.MachineEventWithEndpoints;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class Machine {
	protected MachineModel model;

	Map<EventStateTuple, MachineTransitionHandler> transitionHandlers = new LinkedHashMap<EventStateTuple, MachineTransitionHandler>();

	Multimap<EventStateTuple, List<MachineListener>> listeners = new Multimap<EventStateTuple, List<MachineListener>>();

	static class EventStateTuple {
		MachineEvent event;

		MachineState state;

		public EventStateTuple(MachineState state, MachineEvent event) {
			this.event = event;
			this.state = state;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof EventStateTuple) {
				EventStateTuple o = (EventStateTuple) obj;
				return o.event == event && o.state == state;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return (event == null ? 0 : event.hashCode())
					^ (state == null ? 0 : state.hashCode());
		}
	}

	protected void registerDefaultStatesEventsAndHandlers() {
		// TODO - cancel and error handlers
	}

	public void registerTransitionHandler(MachineState state,
			MachineEvent event, MachineTransitionHandler actionPermformer) {
		transitionHandlers.put(new EventStateTuple(state, event),
				actionPermformer);
	}

	public void addListener(MachineState state, MachineEvent event,
			MachineListener listener) {
		listeners.add(new EventStateTuple(state, event), listener);
	}

	public void removeListener(MachineState state, MachineEvent event,
			MachineListener listener) {
		listeners.remove(listener);
	}

	public void start(MachineState state, MachineEvent event) {
		model.setMachine(this);
		model.setState(state);
		model.setEvent(event);
	}

	public void cancel() {
		if (model.isUnset()) {
			return;
		} else {
			Object cb = model.getRunningCallback();
			if(cb instanceof IsCancellable){
				((IsCancellable) cb).setCancelled(true);
			}
			model.setEvent(MachineEvent.CANCEL);
			listeners.clear();
			transitionHandlers.clear();
		}
	}

	/**
	 * No handler/listener can call setstate/setevent directly, instead they
	 * have access to the various async methods/handlers on MachineModel(which
	 * will queue)
	 */
	void performTransition() {
		List<EventStateTuple> tuples = new ArrayList<Machine.EventStateTuple>();
		if (model.getState() == null || model.getEvent() == null) {
			tuples.add(new EventStateTuple(model.getState(), model.getEvent()));
		} else {
			tuples.add(new EventStateTuple(model.getState(), model.getEvent()));
			tuples.add(new EventStateTuple(null, model.getEvent()));
			tuples.add(new EventStateTuple(model.getState(), null));
		}
		for (EventStateTuple tuple : tuples) {
			for (MachineListener listener : listeners.getAndEnsure(tuple)) {
				listener.beforeAction(model);
			}
		}
		// ?? although we can have listeners based on arrows (which
		// shouldn't affect the FSM), transition should be state only
		if (model.getEvent() == null) {
			for (EventStateTuple tuple : tuples) {
				if (transitionHandlers.containsKey(tuple)) {
					transitionHandlers.get(tuple).performTransition(model);
					break;
				}
			}
		} else {
			// which should always be the case for non-null events...
			assert model.getEvent() instanceof MachineEventWithEndpoints;
			newState(((MachineEventWithEndpoints) model.getEvent()).getTo());
		}
		for (EventStateTuple tuple : tuples) {
			for (MachineListener listener : listeners.getAndEnsure(tuple)) {
				listener.afterAction(model);
			}
		}
	}

	public void newEvent(final MachineEvent newEvent) {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				model.setEvent(newEvent);
			}
		});
	}

	public void newState(final MachineState newState) {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				model.setState(newState);
				model.setEvent(null);
			}
		});
	}

	public void handleAsyncException(Throwable caught,
			MachineTransitionHandler handler) {
		// TODO - transition to state ERR if it exists
		throw new WrappedRuntimeException(caught);
	}
}
