package cc.alcina.framework.jvmclient.service;

import com.google.gwt.core.client.testing.StubScheduler;

public class SchedulerJvm extends StubScheduler{
	@Override
	public void scheduleIncremental(RepeatingCommand cmd) {
		while(cmd.execute());
	}
	@Override
	public void scheduleDeferred(ScheduledCommand cmd) {
		cmd.execute();
	}
}
