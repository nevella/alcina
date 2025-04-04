package cc.alcina.framework.entity.gwt.headless;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;

import com.google.gwt.core.client.Scheduler;

import cc.alcina.framework.common.client.context.ContextFrame;
import cc.alcina.framework.common.client.context.ContextProvider;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IdCounter;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.entity.util.TimerJvm;

/*
 * Models a server-side version of the GWT scheduler/event pump, which itself
 * correlates somewhat to the DOM/JS event pump
 * 
 * It maintains several task queues, and has a more involved happens-after model
 * to handle DOM mutation being effectively async
 * 
 * Any call on any queue that is scheduled after a to-browser command emission
 * will wait until that command returns
 * 
 * FIXME - sel.trav - remove 'postmutations' - really, that should all be the
 * responsiblity of the blocking
 */
public class SchedulerFrame extends Scheduler implements ContextFrame {
	public static ContextProvider<Void, SchedulerFrame> contextProvider;

	public static class Task implements Comparable<Task> {
		static IdCounter counter = new IdCounter();

		ScheduledCommand scheduledCommand;

		RepeatingCommand repeatingCommand;

		long id = counter.nextId();

		@Override
		public String toString() {
			return Ax.format("%s - %s", id, command());
		}

		SchedulerFrame frame;

		Task(ScheduledCommand scheduledCommand) {
			this.scheduledCommand = scheduledCommand;
		}

		Task(RepeatingCommand repeatingCommand) {
			this.repeatingCommand = repeatingCommand;
		}

		long scheduledFor;

		long delayMs;

		Queue queue;

		Task withDelayMs(long delayMs) {
			this.delayMs = delayMs;
			scheduledFor = System.currentTimeMillis() + delayMs;
			return this;
		}

		@Override
		public int compareTo(Task o) {
			return CommonUtils.compareLongs(scheduledFor, o.scheduledFor);
		}

		public Command command() {
			return scheduledCommand != null ? scheduledCommand
					: repeatingCommand;
		}

		public boolean isFuture() {
			return scheduledFor != 0
					&& scheduledFor > System.currentTimeMillis();
		}

		public void executeCommand() {
			Command command = command();
			if (command instanceof RepeatingCommand) {
				RepeatingCommand repeatingCommand = (RepeatingCommand) command;
				boolean repeat = repeatingCommand.execute();
				if (repeat) {
					queue.add(repeatingCommand, (int) delayMs);
				}
			} else {
				((ScheduledCommand) command).execute();
			}
		}
	}

	class Queue {
		Collection<Task> tasks;

		String name;

		Queue(String name, boolean sorted) {
			this.name = name;
			if (sorted) {
				tasks = new TreeSet<>();
			} else {
				tasks = new ArrayList<>();
			}
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s tasks", name, tasks.size());
		}

		void add(ScheduledCommand cmd) {
			Task task = new Task(cmd);
			tasks.add(task);
			task.queue = this;
		}

		void add(RepeatingCommand cmd, int delayMs) {
			Task task = new Task(cmd).withDelayMs(delayMs);
			tasks.add(task);
			task.queue = this;
		}

		void add(ScheduledCommand cmd, int delayMs) {
			Task task = new Task(cmd).withDelayMs(delayMs);
			tasks.add(task);
			task.queue = this;
		}
	}

	Queue entry = new Queue("entry", false);

	Queue _finally = new Queue("_finally", false);

	/*
	 * Because all client-side execution is async, this queue is just treated as
	 * "after _finally" rather than "timed with delay 1ms" (the latter is the
	 * client implementation)
	 */
	Queue deferred = new Queue("deferred", false);

	Queue timed = new Queue("timed", true);

	/*
	 * The sync dispatch pump is not available until after the client has
	 * rendered its initial DOM, so any code requiring sync DOM access (e.g
	 * dropdown positioning) must be deferred until after that initial render
	 */
	Queue postClientStarted = new Queue("postClientStarted", false);

	public CommandExecutor commandExecutor;

	public interface CommandExecutor {
		void execute(SchedulerFrame.Task task);
	}

	public static Supplier<Scheduler> asSupplier() {
		return () -> contextProvider.contextFrame();
	}

	@Override
	public void scheduleDeferred(ScheduledCommand cmd) {
		deferred.add(cmd);
	}

	@Override
	public void scheduleEntry(RepeatingCommand cmd) {
		entry.add(cmd, 0);
	}

	@Override
	public void scheduleEntry(ScheduledCommand cmd) {
		entry.add(cmd);
	}

	@Override
	public void scheduleFinally(RepeatingCommand cmd) {
		_finally.add(cmd, 0);
	}

	@Override
	public void scheduleFinally(ScheduledCommand cmd) {
		_finally.add(cmd);
	}

	@Override
	public void scheduleFixedDelay(RepeatingCommand cmd, int delayMs) {
		timed.add(cmd, delayMs);
	}

	@Override
	public void scheduleFixedPeriod(RepeatingCommand cmd, int delayMs) {
		timed.add(cmd, delayMs);
	}

	@Override
	public void scheduleIncremental(RepeatingCommand cmd) {
		timed.add(cmd, 1);
	}

	public static void initialiseContextProvider(boolean multiple) {
		contextProvider = ContextProvider.createProvider(
				ctx -> new SchedulerFrame(), null, null, SchedulerFrame.class,
				multiple);
		Scheduler.supplier = asSupplier();
	}

	public void pump(boolean entry) {
		List<Queue> queues = entry ? List.of(this.entry)
				: List.of(_finally, deferred, timed);
		Task task = null;
		do {
			task = popNextTask(queues);
			if (task != null) {
				commandExecutor.execute(task);
			}
		} while (task != null);
	}

	Task popNextTask(List<Queue> queues) {
		for (Queue queue : queues) {
			if (queue.tasks.size() > 0) {
				Iterator<Task> itr = queue.tasks.iterator();
				Task next = itr.next();
				if (!next.isFuture()) {
					itr.remove();
					return next;
				}
			}
		}
		return null;
	}

	long getNextScheduledTaskTime() {
		Task next = Ax.first(timed.tasks);
		if (next != null) {
			return next.scheduledFor;
		} else {
			return 0;
		}
	}

	public Timer createTimer(Runnable runnable) {
		return new TimerImpl(runnable);
	}

	class TimerImpl implements Timer, Runnable {
		boolean cancelled = false;

		Runnable runnable;

		private long periodMillis;

		TimerImpl(Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public void run() {
			if (cancelled) {
				return;
			}
			runnable.run();
			if (cancelled) {
				return;
			}
			if (periodMillis != 0) {
				schedule(periodMillis);
			}
		}

		@Override
		public void cancel() {
			cancelled = true;
		}

		@Override
		public void scheduleRepeating(long periodMillis) {
			this.periodMillis = Math.max(1, periodMillis);
			/* run once */
			schedule(0);
			schedule(periodMillis);
		}

		@Override
		public void schedule(long delayMillis) {
			scheduleFixedDelay(new TimerCommand(), (int) delayMillis);
		}

		class TimerCommand implements RepeatingCommand {
			@Override
			public boolean execute() {
				run();
				return !cancelled && periodMillis != 0;
			}

			@Override
			public String toString() {
				return Ax.format("timercommand - timer %s",
						TimerImpl.this.hashCode());
			}
		}

		@Override
		public void scheduleDeferred() {
			SchedulerFrame.this.scheduleDeferred(this::run);
		}

		@Override
		public void scheduleDeferredIfOnUIThread() {
			throw new IllegalStateException("Only called on UI thread");
		}
	}

	public void scheduleNextEntry(Runnable entry) {
		long nextScheduledTaskTime = getNextScheduledTaskTime();
		if (nextScheduledTaskTime == 0) {
			return;
		}
		long now = System.currentTimeMillis();
		long delayMillis = Math.max(nextScheduledTaskTime - now, 0);
		timerProvider.getTimer(entry).schedule(delayMillis);
	}

	TimerJvm.Provider timerProvider = new TimerJvm.Provider();
}
