package cc.alcina.framework.common.client.state;

import cc.alcina.framework.common.client.state.Machine.EventStateTuple;

/**
 * Events are pumped via setEvent() - which can be null (to signify 'arrived at
 * state x')
 * 
 * @author nick@alcina.cc
 * 
 */
public class MachineModel {
	private MachineState state;

	private MachineEvent event;

	private MachineTransitionHandler transitionHandler;

	private String log = "";

	private boolean debug = false;

	private Machine machine;

	private Object runningCallback;

	private boolean cancelled = false;

	public MachineState getState() {
		return this.state;
	}

	void setState(MachineState state) {
		setEventAndState(new EventStateTuple(state, null));
	}

	public MachineEvent getEvent() {
		return this.event;
	}

	void setEvent(MachineEvent event) {
		setEventAndState(new EventStateTuple(null, event));
	}

	 void log(String message) {
		log += message;
		if (debug) {
			System.out.println(message);
		}
	}

	public MachineTransitionHandler getTransitionHandler() {
		return this.transitionHandler;
	}

	void setTransitionHandler(MachineTransitionHandler transitionHandler) {
		this.transitionHandler = transitionHandler;
	}

	public Object getRunningCallback() {
		return this.runningCallback;
	}

	public void setRunningCallback(Object runningCallback) {
		this.runningCallback = runningCallback;
	}

	public String getLog() {
		return this.log;
	}

	public Machine getMachine() {
		return this.machine;
	}

	void setMachine(Machine machine) {
		this.machine = machine;
	}
	void setEventAndState(EventStateTuple tuple){
		this.state=tuple.state;
		this.event=tuple.event;
		machine.performTransition();
	}
	public boolean isUnset() {
		return state == null && event == null;
	}

	public boolean isDebug() {
		return this.debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isCancelled() {
		return this.cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
