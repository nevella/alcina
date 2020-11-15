package cc.alcina.framework.entity.persistence.transform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;

/*
 * FIXME - mvcc.4 - the threading is overly complex - or could be abstracted out maybe? With a task queue?
 * 
 * Actually we want an 'acceptor' thread that runs tasks as they come in (if not persisting), persists to disk, and commits after the delay
 * 
 *  It should run for the app lifetime (other cases of AtEndOfEventSeriesTimer may be misapplied too)
 * 
 * FIXME - mvcc.5 - backup (fs?) persistence of transforms? (get em via scp on fail) (why aren't the runnables run on the originating thread?)
 * 
 * synchronization - all access to internal collections synchronized on this
 */
@RegistryLocation(registryPoint = BackendTransformQueue.class, implementationType = ImplementationType.SINGLETON)
public class BackendTransformQueue {
	public static BackendTransformQueue get() {
		return Registry.impl(BackendTransformQueue.class);
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	private Map<String, Long> queueMaxDelay = new LinkedHashMap<>();

	private Map<String, Long> queueFirstEvent = new LinkedHashMap<>();

	private List<DomainTransformEvent> transforms = new ArrayList<>();

	private TimerTask scheduledTask = null;

	private boolean persisting = false;

	private boolean zeroDelayTaskScheduled = false;

	private Timer timer = new Timer("Backend-transform-queue");

	public void enqueue(List<DomainTransformEvent> transforms,
			String queueName) {
		synchronized (this) {
			this.transforms.addAll(transforms);
			long now = System.currentTimeMillis();
			queueFirstEvent.putIfAbsent(queueName, now);
			maybeEnqueueTask();
		}
	}

	public void enqueue(Runnable runnable, String queueName) {
		CollectingListener collectingListener = new CollectingListener();
		try {
			TransformManager.get()
					.addDomainTransformListener(collectingListener);
			runnable.run();
			collectingListener.transforms
					.forEach(TransformManager.get()::removeTransform);
			enqueue(collectingListener.transforms, queueName);
		} finally {
			TransformManager.get()
					.removeDomainTransformListener(collectingListener);
		}
	}

	public void setBackendTransformQueueMaxDelay(String queueName,
			long delayMs) {
		synchronized (this) {
			queueMaxDelay.put(queueName, delayMs);
		}
	}

	public void start() {
		int loopDelay = ResourceUtilities
				.getInteger(BackendTransformQueue.class, "loopDelay");
		queueMaxDelay.put(null, (long) loopDelay);
	}

	public void stop() {
		timer.cancel();
	}

	private void persistQueue0() {
		while (true) {
			synchronized (this) {
				logger.info("(Backend queue)  - committing {} transforms",
						transforms.size());
				Transaction.endAndBeginNew();
				ThreadlocalTransformManager.get().addTransforms(transforms,
						false);
				transforms.clear();
				persisting = true;
				zeroDelayTaskScheduled = false;
				queueFirstEvent.clear();
			}
			try {
				LooseContext.push();
				LooseContext.setTrue(
						AdjunctTransformCollation.CONTEXT_TM_TRANSFORMS_ARE_EX_THREAD);
				Transaction.commit();
			} finally {
				LooseContext.pop();
			}
			synchronized (this) {
				if (getCommitDelay().orElse(Long.MAX_VALUE) == 0L) {
				} else {
					persisting = false;
					break;
				}
			}
		}
	}

	protected Optional<Long> getCommitDelay() {
		long now = System.currentTimeMillis();
		Optional<Long> commitDelay = queueFirstEvent.entrySet().stream()
				.map(e -> {
					String queueName = e.getKey();
					Long maxDelay = queueMaxDelay.get(queueName);
					Long firstEventTime = e.getValue();
					long queueDelay = firstEventTime + maxDelay - now;
					return Math.max(queueDelay, 0L);
				}).collect(Collectors.minBy(Comparator.naturalOrder()));
		return commitDelay;
	}

	/**
	 * return true if a zero-delay persistence task should run
	 */
	protected boolean maybeEnqueueTask() {
		Optional<Long> commitDelay = getCommitDelay();
		if (persisting || (zeroDelayTaskScheduled
				&& commitDelay.get().longValue() == 0)) {
			// no need to change the current task
		} else {
			if (scheduledTask != null) {
				scheduledTask.cancel();
			}
			// commitDelay will be < Long.MAX_VALUE
			scheduledTask = new TimerTask() {
				@Override
				public void run() {
					AlcinaChildRunnable.runInTransaction(
							"backend-transform-persist", () -> persistQueue0(),
							true, false);
				}
			};
			timer.schedule(scheduledTask, commitDelay.get());
			zeroDelayTaskScheduled = commitDelay.get() == 0;
		}
		return false;
	}

	private static class CollectingListener implements DomainTransformListener {
		List<DomainTransformEvent> transforms = new ArrayList<>();

		@Override
		public void domainTransform(DomainTransformEvent evt)
				throws DomainTransformException {
			transforms.add(evt);
		}
	}
}
