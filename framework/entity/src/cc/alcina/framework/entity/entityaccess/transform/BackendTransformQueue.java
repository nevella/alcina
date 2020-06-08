package cc.alcina.framework.entity.entityaccess.transform;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.entityaccess.transform.TransformCommit.TransformPriorityStd;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;

class BackendTransformQueue {
	private AtEndOfEventSeriesTimer persistTimer;

	List<Runnable> tasks = new ArrayList<>();

	Object persistMonitor = new Object();

	Logger logger = LoggerFactory.getLogger(getClass());

	public void enqueue(Runnable runnable) {
		int size = 0;
		synchronized (this) {
			tasks.add(runnable);
			size = tasks.size();
			persistTimer.triggerEventOccurred();
		}
		if (size > ResourceUtilities.getInteger(BackendTransformQueue.class,
				"maxRunnables")) {
			persistQueue();
		}
	}

	private void persistQueue0() {
		synchronized (persistMonitor) {
			List<DomainTransformEvent> events = new ArrayList<>();
			List<Runnable> toCommit = null;
			synchronized (this) {
				toCommit = tasks;
				tasks = new ArrayList<>();
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
		}
	}

	void appShutdown() {
		persistTimer.cancel();
		persistQueue();
	}

	void persistQueue() {
		AlcinaChildRunnable.runInTransaction("backend-transform-persist",
				() -> persistQueue0(), true, false);
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
