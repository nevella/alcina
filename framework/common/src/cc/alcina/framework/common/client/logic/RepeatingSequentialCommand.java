package cc.alcina.framework.common.client.logic;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

public class RepeatingSequentialCommand implements RepeatingCommand {
	protected List<RepeatingCommand> tasks = new ArrayList<RepeatingCommand>();

	public void cancel() {
		tasks.clear();
	}

	public void add(RepeatingCommand task) {
		if (tasks.isEmpty()) {
			Scheduler.get().scheduleIncremental(this);
		}
		tasks.add(task);
		if (isSynchronous()) {
			flushSynchronous();
		}
	}

	public void flushSynchronous() {
		if (flushing) {
			return;
		}
		try {
			flushing = true;
			while (!tasks.isEmpty()) {
				boolean result = tasks.get(0).execute();
				if (!result) {
					tasks.remove(0);
				}
			}
		} finally {
			flushing = false;
		}
	}

	private boolean flushing = false;

	private boolean synchronous = false;

	public static boolean DEBUG = false;

	@Override
	public boolean execute() {
		if (isSynchronous()) {
			flushSynchronous();
		}
		if (tasks.isEmpty()) {
			return false;
		}
		boolean ok=false;
		try {
			long t1 = System.currentTimeMillis();
			boolean result = tasks.get(0).execute();
			if (DEBUG) {
				long t2 = System.currentTimeMillis();
				if (t2 - t1 > 100) {
					ClientLayerLocator
							.get()
							.notifications()
							.log(CommonUtils.formatJ("Long task: %s - %sms",
									tasks.get(0).getClass().getName(), t2 - t1));
				}
			}
			if (!result) {
				tasks.remove(0);
			}
			ok=true;
		} finally{
			if(!ok){
				cancel();
			}
		}
		return !tasks.isEmpty();
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
		if (synchronous) {
			flushSynchronous();
		}
	}

	public boolean isSynchronous() {
		return synchronous;
	}
}
