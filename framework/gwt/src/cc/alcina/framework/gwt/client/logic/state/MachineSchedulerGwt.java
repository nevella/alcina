package cc.alcina.framework.gwt.client.logic.state;

import cc.alcina.framework.common.client.state.MachineEvent;
import cc.alcina.framework.common.client.state.MachineModel;
import cc.alcina.framework.common.client.state.MachineSchedulerBase;
import cc.alcina.framework.common.client.state.MachineState;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class MachineSchedulerGwt extends MachineSchedulerBase {
	public void newEvent(final MachineEvent newEvent, final MachineModel model) {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				handleModelEvent(newEvent, model);
			}
		});
	}

	public void newState(final MachineState newState, final MachineModel model) {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				handleModelState(newState, model);
			}
		});
	}

	@Override
	public void scheduleDeferred(final Runnable runnable) {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				runnable.run();
			}
		});
	}
}
