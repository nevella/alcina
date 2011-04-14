package cc.alcina.framework.common.client.logic;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.WrappedRuntimeException;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

public class RepeatingSequentialCommand implements RepeatingCommand {
	private List<RepeatingCommand> tasks = new ArrayList<RepeatingCommand>();

	public void cancel() {
		tasks.clear();
	}

	public void add(RepeatingCommand task) {
		if (tasks.isEmpty()) {
			Scheduler.get().scheduleIncremental(this);
		}
		tasks.add(task);
	}
	public void flushSynchronous(){
		while(!tasks.isEmpty()){
			boolean result = tasks.get(0).execute();
			if (!result) {
				tasks.remove(0);
			}
		}
	}

	@Override
	public boolean execute() {
		if (tasks.isEmpty()) {
			return false;
		}
		try {
			boolean result = tasks.get(0).execute();
			if (!result) {
				tasks.remove(0);
			}
		} catch (Exception e) {
			cancel();
			throw new WrappedRuntimeException(e);
		}
		return !tasks.isEmpty();
	}
}
