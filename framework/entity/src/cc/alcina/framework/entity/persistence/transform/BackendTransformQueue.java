package cc.alcina.framework.entity.persistence.transform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;

/**
 * FIXME - mvcc.5 - backup (zk) persistence of transforms? - use the
 * cli-id/dtr-uuid/event-id path. cluster leader to TCOB
 */
@Registration.Singleton
public class BackendTransformQueue {
	private static final String DEFAULT_QUEUE_NAME = "default-queue";

	public static BackendTransformQueue get() {
		return Registry.impl(BackendTransformQueue.class);
	}

	private BlockingQueue<Event> events = new LinkedBlockingQueue<>();

	Logger logger = LoggerFactory.getLogger(getClass());

	private Map<String, Long> queueMaxDelay = new ConcurrentHashMap<>();

	private Map<String, Long> queueFirstEvent = new LinkedHashMap<>();

	volatile boolean finished = false;

	private EventThread eventThread;

	private List<DomainTransformEvent> pendingTransforms = new ArrayList<>();

	AtomicLong idCounter = new AtomicLong(0);

	public void enqueue(List<DomainTransformEvent> transforms,
			String queueName) {
		long now = System.currentTimeMillis();
		events.add(new Event(transforms, normaliseQueueName(queueName), now));
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
			queueMaxDelay.put(normaliseQueueName(queueName), delayMs);
		}
	}

	public void start() {
		int loopDelay = ResourceUtilities
				.getInteger(BackendTransformQueue.class, "loopDelay");
		setBackendTransformQueueMaxDelay(DEFAULT_QUEUE_NAME, loopDelay);
		eventThread = new EventThread();
		eventThread.start();
	}

	public void stop() {
		finished = true;
		if (eventThread != null) {
			eventThread.interrupt();
		}
	}

	private void commit() {
		Multiset<String, Set<Long>> locators = pendingTransforms.stream()
				.collect(AlcinaCollectors.toMultiset(
						t -> t.getObjectClass().getSimpleName(),
						t -> t.getObjectId()));
		logger.info(
				"(Backend queue)  - committing {} transforms - locators: {}",
				pendingTransforms.size(), locators);
		Transaction.endAndBeginNew();
		TransformCollation transformCollation = new TransformCollation(
				pendingTransforms);
		pendingTransforms.clear();
		transformCollation.filterNonpersistentPropertyTransforms();
		ThreadlocalTransformManager.get()
				.addTransforms(transformCollation.getAllEvents(), false);
		queueFirstEvent.clear();
		try {
			LooseContext.pushWithTrue(
					AdjunctTransformCollation.CONTEXT_TM_TRANSFORMS_ARE_EX_THREAD);
			LooseContext
					.setTrue(TransformCommit.CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK);
			Transaction.commit();
		} finally {
			LooseContext.pop();
		}
		Transaction.end();
	}

	private String normaliseQueueName(String queueName) {
		return Ax.blankTo(queueName, DEFAULT_QUEUE_NAME);
	}

	long computeDelay() {
		long now = System.currentTimeMillis();
		return queueFirstEvent.entrySet().stream().map(e -> {
			String queueName = e.getKey();
			long firstEventTime = e.getValue();
			long commitDelay = firstEventTime + queueMaxDelay.get(queueName)
					- now;
			return commitDelay;
		}).min(Comparator.naturalOrder()).orElse(Long.MAX_VALUE);
	}

	private static class CollectingListener implements DomainTransformListener {
		List<DomainTransformEvent> transforms = new ArrayList<>();

		@Override
		public void domainTransform(DomainTransformEvent evt)
				throws DomainTransformException {
			transforms.add(evt);
		}
	}

	private class EventThread extends Thread {
		public EventThread() {
			super("BackendTransformQueue-events");
		}

		@Override
		public void run() {
			while (!finished) {
				try {
					long delay = computeDelay();
					if (delay <= 0) {
						// only commit (flush) if the queue is empty
						if (events.peek() == null) {
							commit();
							delay = computeDelay();
						} else {
							delay = 0;
						}
					}
					Event event = events.poll(delay, TimeUnit.MILLISECONDS);
					if (event != null) {
						logger.debug(
								"Backend transform queue - adding event:\n{}",
								event);
						event.transforms.forEach(pendingTransforms::add);
						queueFirstEvent.putIfAbsent(event.queueName,
								event.time);
					}
				} catch (InterruptedException interrupted) {
					// will exit
				} catch (Exception e) {
					logger.warn("Event thread issue", e);
				}
			}
		}
	}

	class Event {
		List<DomainTransformEvent> transforms;

		String queueName;

		long time;

		long id;

		Event(List<DomainTransformEvent> transforms, String queueName,
				long time) {
			this.id = idCounter.incrementAndGet();
			this.transforms = transforms;
			this.queueName = queueName;
			this.time = time;
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s :: %s :: %s transforms", id, queueName,
					time, transforms.size());
		}
	}
}
