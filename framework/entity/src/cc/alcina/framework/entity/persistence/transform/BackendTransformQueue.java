package cc.alcina.framework.entity.persistence.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.TransformCommit.TransformPriorityStd;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;

/*
 * FIXME - mvcc.4 - the threading is overly complex - or could be abstracted out maybe? With a task queue?
 * 
 * Actually we want an 'acceptor' thread that runs tasks as they come in (if not persisting), persists to disk, and commits after the delay
 * 
 *  It should run for the app lifetime (other cases of AtEndOfEventSeriesTimer may be misapplied too)
 * 
 * FIXME - mvcc.5 - backup (fs?) persistence of transforms? (get em via scp on fail) (why aren't the runnables run on the originating thread?)
 */
class BackendTransformQueue {
	private AtEndOfEventSeriesTimer persistTimer;

	List<Runnable> tasks = new ArrayList<>();

	Logger logger = LoggerFactory.getLogger(getClass());

	AtomicBoolean persisting = new AtomicBoolean(false);

	AtomicBoolean persistAtEndOfCurrentTask = new AtomicBoolean(false);

	private long lastPersistStarted;

	public void enqueue(Runnable runnable) {
		int size = 0;
		synchronized (this) {
			tasks.add(runnable);
			size = tasks.size();
			persistTimer.triggerEventOccurred();
		}
	}

	private void persistQueue0() {
		boolean doPersist = true;
		while (doPersist) {
			lastPersistStarted = System.currentTimeMillis();
			List<DomainTransformEvent> events = new ArrayList<>();
			List<Runnable> toCommit = null;
			synchronized (this) {
				toCommit = tasks;
				tasks = new ArrayList<>();
				if (toCommit.size() == 0) {
					synchronized (persisting) {
						persisting.set(false);
					}
				}
			}
			for (Runnable runnable : toCommit) {
				if (runnable instanceof AlcinaChildRunnable) {
					((AlcinaChildRunnable) runnable)
							.setRunningWithinTransaction(true);
				}
				ThreadlocalTransformManager.cast().resetTltm(null);
				try {
					LooseContext.push();
					runnable.run();
					events.addAll(TransformManager.get().getTransforms());
					TransformManager.get().clearTransforms();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					LooseContext.pop();
				}
			}
			ThreadlocalTransformManager.get().addTransforms(events, false);
			if (events.size() > 0) {
				logger.warn("(Backend queue)  - committing {} transforms",
						events.size());
			}
			try {
				LooseContext.push();
				TransformCommit.setPriority(TransformPriorityStd.Backend_admin);
				Transaction.commit();
			} finally {
				LooseContext.pop();
			}
			doPersist = false;
			synchronized (persisting) {
				if (persistAtEndOfCurrentTask.get()) {
					doPersist = true;
					persistAtEndOfCurrentTask.set(false);
				}
			}
		}
		persisting.set(false);
	}

	void appShutdown() {
		persistTimer.cancel();
		// force persist attempt, even if in-flight
		persisting.set(false);
		persistQueue();
	}

	void persistQueue() {
		boolean doPersist = false;
		synchronized (persisting) {
			if (!persisting.get()) {
				persisting.set(true);
				doPersist = true;
			} else {
				logger.info(
						"Not persisting - there's an inflight persistence job started {} ms ago",
						System.currentTimeMillis() - lastPersistStarted);
				persistAtEndOfCurrentTask.set(true);
			}
		}
		if (doPersist) {
			AlcinaChildRunnable.runInTransaction("backend-transform-persist",
					() -> persistQueue0(), true, false);
		}
	}

	void start() {
		int loopDelay = ResourceUtilities
				.getInteger(BackendTransformQueue.class, "loopDelay");
		persistTimer = new AtEndOfEventSeriesTimer<>(loopDelay, new Runnable() {
			@Override
			public void run() {
				persistQueue();
			}
		}).maxDelayFromFirstAction(loopDelay);
	}
}
