package cc.alcina.framework.gwt.client.logic.state;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

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

	public MachineState getState() {
		return this.state;
	}

	void setState(MachineState state) {
		this.state = state;
		log(CommonUtils.formatJ("state - %s\n",
				state == null ? null : state.name()));
	}

	public MachineEvent getEvent() {
		return this.event;
	}

	void setEvent(MachineEvent event) {
		this.event = event;
		log(CommonUtils.formatJ("event - %s\n",
				event == null ? null : event.name()));
		machine.performTransition();
	}

	private void log(String message) {
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

	public boolean isUnset() {
		return state == null && event == null;
	}

	public boolean isDebug() {
		return this.debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}
