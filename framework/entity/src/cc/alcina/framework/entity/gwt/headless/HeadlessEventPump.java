package cc.alcina.framework.entity.gwt.headless;

import com.google.gwt.core.client.Scheduler;

import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration(Scheduler.class)
public class HeadlessEventPump extends Scheduler {
	@Override
	public void scheduleDeferred(ScheduledCommand cmd) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'scheduleDeferred'");
	}

	@Override
	public void scheduleEntry(RepeatingCommand cmd) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'scheduleEntry'");
	}

	@Override
	public void scheduleEntry(ScheduledCommand cmd) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'scheduleEntry'");
	}

	@Override
	public void scheduleFinally(RepeatingCommand cmd) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'scheduleFinally'");
	}

	@Override
	public void scheduleFinally(ScheduledCommand cmd) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'scheduleFinally'");
	}

	@Override
	public void scheduleFixedDelay(RepeatingCommand cmd, int delayMs) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'scheduleFixedDelay'");
	}

	@Override
	public void scheduleFixedPeriod(RepeatingCommand cmd, int delayMs) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'scheduleFixedPeriod'");
	}

	@Override
	public void scheduleIncremental(RepeatingCommand cmd) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'scheduleIncremental'");
	}
}
