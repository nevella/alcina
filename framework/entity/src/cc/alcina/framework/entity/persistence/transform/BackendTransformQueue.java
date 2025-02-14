package cc.alcina.framework.entity.persistence.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;

/**
 * FIXME - mvcc.5 - backup (zk) persistence of transforms? - use the
 * cli-id/dtr-uuid/event-id path. cluster leader to TCOB
 *
 * <h2>Collision avoidance</h2>
 * <p>
 * Of particular relevance to Job status persistence (which is commonly
 * performed on the queue event thread to avoid thundering persistence), this
 * class listens on all transform requests and potentially modifies or pauses
 * them to avoid concurrent db writes. The algorithm is as follows:
 * </p>
 * <h3>Terminology</h3>
 * <ul>
 * <li>BTQ - backendtransformqueue
 * <li>Thread A: non-BTQ, committing, modifying entity J
 * <li>Thread B: BTQ
 * </ul>
 * <h3>Process:</h3>
 * <ul>
 * <li>Have a BTQ.preCommitQueueModification lock (fair WriteLock). Acquired by
 * BTQ during commit, or thread A pre-commit.
 * <li>Synchronize access to the BTQ per-queue data on the queue instance.
 * Acquired by any thread which accesses the per-queue data structure (very fast
 * access/release)
 * <li>Before thread A commit, if the commit contains J transforms, check the
 * BTQ:
 * <ul>
 * <li>If there’s an inflight transform db commit on thread B, and that commit
 * contains transforms modifying J, wait for the BTQ.queueModification lock (so
 * let the commit complete but ensure that thread A commits before the next
 * thread B commit).
 * <li>If there’s not an inflight transform db commit on thread B, interpolate
 * (using collations) any transforms on the BTQ transform queue into our
 * transaction
 * </ul>
 * </ul>
 * </ul>
 */
/*
Implementation - in TransformInterpolator

Test sketch
 * @formatter:off
 - backend transforms
 	- start a job
 	- emit status message (pre end job)
 	- wait for backend persistence
 	- observe backend event thread 'persisted' message
 	- end job
 - backend pause
 	- start a job
 	- end job
 	- pause end job commit
 	- emit status message (post end job)
 	- wait for backend persistence
 	- observe backend event thread 'waiting' message
 - backend transforms > job commit
 	- start a job
 	- emit status message (pre end job)
 	- end job
 	- observe transforms moved to job commit
 	- observe no backend persistence
 - job pause
 	- start a job
 	- emit status message (pre end job)
 	- wait for backend persistence
 	- pause backend persistence commit
 	- end job
 	- observe job thread 'waiting' message


	FIXME (a different issue - queued/non-blocking entity creation support)

* what's the goal? basically to coalesce the transforms from independent, low priority runnables into one tx
* the BackendTransformQueue mechanism - currently it's "transform transfer" - but that's fragile and complicated
* instead - put the runnables in another tx (on the backing thread)
* for some tasks (AtRestEncryption), we'll need a fully isolated context - but probably want that *anyway*
* for the moment, anything non-blocking and complex (creation, deletion), do it on a committing (non-backend thread)

 * @formatter:on
 */
@Registration.Singleton
public class BackendTransformQueue {
	private static final String DEFAULT_QUEUE_NAME = "default-queue";

	public static BackendTransformQueue get() {
		return Registry.impl(BackendTransformQueue.class);
	}

	private BlockingQueue<Event> events = new LinkedBlockingQueue<>();

	Logger logger = LoggerFactory.getLogger(getClass());

	// concurrent to avoid locking during createBackendQueue
	private ConcurrentMap<String, Queue> queues = new ConcurrentHashMap<>();

	volatile boolean finished = false;

	private EventThread eventThread;

	AtomicLong idCounter = new AtomicLong(0);

	private Lock preCommitQueueModification = new ReentrantLock(true);

	private TransformInterpolator transformInterpolator = new TransformInterpolator();

	public BackendTransformQueue() {
	}

	private void commit() {
		TransformCollation committingCollation;
		List<DomainTransformEvent> pendingTransforms = new ArrayList<>();
		try {
			preCommitQueueModification.lock();
			queues.values()
					.forEach(queue -> queue.flushAndClear(pendingTransforms));
		} finally {
			preCommitQueueModification.unlock();
		}
		Collections.sort(pendingTransforms);
		committingCollation = new TransformCollation(pendingTransforms);
		committingCollation.filterNonpersistentTransforms();
		Multiset<String, Set<Long>> locators = committingCollation
				.getAllEvents().stream()
				.collect(AlcinaCollectors.toMultiset(
						t -> t.getObjectClass().getSimpleName(),
						t -> t.getObjectId()));
		if (locators.isEmpty()) {
			return;
		}
		logger.info(
				"(Backend queue)  - committing {} transforms - locators: {}",
				pendingTransforms.size(), locators);
		TransformManager.get().clearTransforms();
		Transaction.endAndBeginNew();
		ThreadlocalTransformManager.get()
				.addTransforms(committingCollation.getAllEvents(), false);
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

	long computeDelay() {
		long now = System.currentTimeMillis();
		// doesn't lock all queues, but worst case (of queue modification during
		// stream) is just a spurious commit() event which causes an commit noop
		return queues.values().stream().map(q -> q.computeDelay(now))
				.min(Comparator.naturalOrder()).orElse(Long.MAX_VALUE);
	}

	public void createBackendQueue(String queueName, long maxDelayMs) {
		Preconditions.checkState(!queues.containsKey(queueName));
		String normaliseQueueName = normaliseQueueName(queueName);
		queues.put(normaliseQueueName,
				new Queue(normaliseQueueName, maxDelayMs));
	}

	public int enqueue(List<DomainTransformEvent> transforms,
			String queueName) {
		long now = System.currentTimeMillis();
		events.add(new Event(transforms, normaliseQueueName(queueName), now));
		removeFromLocalEviction(transforms);
		return transforms.size();
	}

	public int enqueue(Runnable runnable, String queueName) {
		CollectingListener collectingListener = new CollectingListener();
		try {
			TransformManager.get()
					.addDomainTransformListener(collectingListener);
			runnable.run();
			List<DomainTransformEvent> transforms = collectingListener.transforms;
			collectingListener.transforms
					.forEach(TransformManager.get()::removeTransform);
			return enqueue(collectingListener.transforms, queueName);
		} finally {
			TransformManager.get()
					.removeDomainTransformListener(collectingListener);
		}
	}

	// for testing, forces a commit()
	public void flush() {
		Preconditions.checkState(Ax.isTest());
		events.add(new Event(null, null, 0L));
	}

	private String normaliseQueueName(String queueName) {
		return Ax.blankTo(queueName, DEFAULT_QUEUE_NAME);
	}

	void removeFromLocalEviction(List<DomainTransformEvent> transforms) {
		transforms.stream()
				.filter(DomainTransformEvent::provideIsCreationTransform)
				.map(DomainTransformEvent::getSource).distinct()
				.forEach(Transaction.current()::removeFromLocalEviction);
	}

	public void start() {
		int loopDelay = Configuration.getInt("loopDelay");
		createBackendQueue(DEFAULT_QUEUE_NAME, loopDelay);
		eventThread = new EventThread();
		eventThread.start();
		DomainStore.writableStore().getPersistenceEvents()
				.addDomainTransformPersistenceListener(transformInterpolator);
	}

	public void stop() {
		finished = true;
		if (eventThread != null) {
			eventThread.interrupt();
		}
	}

	private static class CollectingListener implements DomainTransformListener {
		List<DomainTransformEvent> transforms = new ArrayList<>();

		@Override
		public void domainTransform(DomainTransformEvent evt)
				throws DomainTransformException {
			transforms.add(evt);
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
						// only commit (flush) if the event queue is empty
						if (events.peek() == null) {
							commit();
							delay = computeDelay();
						} else {
							delay = 0;
						}
					}
					Event event = events.poll(delay, TimeUnit.MILLISECONDS);
					if (event != null) {
						if (event.queueName == null) {
							// flush, test only
							logger.info("Backend transform queue - flush");
							commit();
						} else {
							logger.debug(
									"Backend transform queue - adding event:\n{}",
									event);
							queues.get(event.queueName).add(event);
						}
					}
				} catch (InterruptedException interrupted) {
					// will exit
				} catch (Exception e) {
					logger.warn("Event thread issue", e);
				}
			}
		}
	}

	private class Queue {
		private long maxDelayMs;

		private String name;

		private long firstEventTime = -1;

		private List<DomainTransformEvent> events = new ArrayList<>();

		public Queue(String name, long maxDelayMs) {
			this.name = name;
			this.maxDelayMs = maxDelayMs;
		}

		public synchronized void add(Event event) {
			event.transforms.forEach(events::add);
			if (firstEventTime == -1) {
				firstEventTime = System.currentTimeMillis();
			}
		}

		private synchronized long computeDelay(long fromTime) {
			if (firstEventTime == -1) {
				return Long.MAX_VALUE;
			} else {
				return firstEventTime + maxDelayMs - fromTime;
			}
		}

		public synchronized void
				flushAndClear(List<DomainTransformEvent> pendingTransforms) {
			pendingTransforms.addAll(events);
			firstEventTime = -1;
			events.clear();
		}

		/*
		 * move transforms from this list into the event if the event contains
		 * the transform locator
		 */
		synchronized void possiblyMoveTransformsTo(
				DomainTransformPersistenceEvent nonQueueEvent) {
			AdjunctTransformCollation nonQueueEventCollation = nonQueueEvent
					.getPreProcessCollation();
			TransformCollation queueCollation = new TransformCollation(events);
			if (nonQueueEventCollation.conflictsWith(queueCollation)) {
				if (nonQueueEvent.getTransformPersistenceToken().getRequest()
						.getPriorRequestsWithoutResponse().size() > 0) {
					// transforms can't be easily modified in a backoff request
					// - but the entity shouldn't be modificable by the client
					// in this case
					logger.warn(
							"DEVEX-2 - Cannot move transforms from client/backoff request");
					return;
				}
				Set<DomainTransformEvent> removedFromBackendQueueTransforms = queueCollation
						.removeConflictingTransforms(nonQueueEventCollation);
				List<DomainTransformEvent> nonQueueTransforms = nonQueueEventCollation
						.getAllEvents();
				/*
				 * NO! See DomainTransformRequest.compareTo
				 */
				// Collections.sort(nonQueueTransforms);
				/*
				 * Instead, prepend the transforms.
				 */
				int idx = 0;
				Iterator<DomainTransformEvent> removedItr = removedFromBackendQueueTransforms
						.iterator();
				while (removedItr.hasNext()) {
					DomainTransformEvent removed = removedItr.next();
					nonQueueTransforms.add(idx++, removed);
				}
				nonQueueEventCollation.filterNonpersistentTransforms();
				nonQueueEvent.getTransformPersistenceToken()
						.updateRequestFromCollation();
				logger.info(
						"Transferred {} transforms from queue {} to non-backend commit",
						removedFromBackendQueueTransforms.size(), name);
			}
		}

		@Override
		public synchronized String toString() {
			return Ax.format("[%s] - %s transforms; next fire: %s ms", name,
					events.size(), computeDelay(System.currentTimeMillis()));
		}
	}

	/**
	 * A peristence listener, that transfers backend transforms to non-backend
	 * txs if desirable, and blocks non-backend txs if there is a conflicting
	 * backend tx committing
	 * 
	 * TODO - noted some strangeness (for instance timeouts are generated a
	 * looong time after ) - this should be debugged with more info
	 */
	private class TransformInterpolator
			implements DomainTransformPersistenceListener {
		private Map<String, PersistenceEventAffects> affects = new LinkedHashMap<>();

		AtomicInteger sequenceIdCounter = new AtomicInteger();

		// called from synchronized
		private void checkTimedoutAffects() {
			Iterator<Entry<String, PersistenceEventAffects>> itr = affects
					.entrySet().iterator();
			long now = System.currentTimeMillis();
			while (itr.hasNext()) {
				Entry<String, PersistenceEventAffects> next = itr.next();
				if (next.getValue().isTimedOut(now)) {
					/*
					 * this means the tx commit didn't return in 10 secs -
					 * remove from the affects list so as to not block further.
					 * 
					 * the problem is not here, rather in the
					 * transformpersistence ok/error not being reached
					 */
					logger.warn("Timed-out-waiting-for-backend-queue-exit : {}",
							next.getValue());
					itr.remove();
				} else {
					break;
				}
			}
		}

		private PersistenceEventAffects createAffects(
				DomainTransformPersistenceEvent event, boolean backend) {
			Optional<String> firstUuid = event.getFirstUuid();
			if (firstUuid.isEmpty()) {
				return null;
			}
			PersistenceEventAffects result = new PersistenceEventAffects(event,
					backend, sequenceIdCounter.incrementAndGet());
			affects.put(firstUuid.get(), result);
			return result;
		}

		@Override
		public synchronized void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent event) {
			switch (event.getPersistenceEventType()) {
			case PREPARE_COMMIT:
				boolean backend = Thread.currentThread() == eventThread;
				if (!backend) {
					try {
						preCommitQueueModification.lock();
						queues.values().forEach(
								q -> q.possiblyMoveTransformsTo(event));
					} finally {
						preCommitQueueModification.unlock();
					}
				}
				PersistenceEventAffects eventAffects = createAffects(event,
						backend);
				// notify other waiting threads
				waitForNonConflictingAffects(eventAffects);
				break;
			case COMMIT_ERROR:
			case COMMIT_OK:
				event.getFirstUuid().ifPresent(affects::remove);
				notifyAll();
				break;
			default:
				return;
			}
		}

		// the thread is already synchronized on the TransformInterpolator
		void waitForNonConflictingAffects(PersistenceEventAffects affects) {
			if (affects == null) {
				return;
			}
			boolean waited = false;
			while (true) {
				if (affects.isNonConflicting()) {
					if (waited) {
						logger.info(
								"Post-waiting to avoid transform conflicts: {}",
								affects);
					}
					return;
				}
				checkTimedoutAffects();
				waited = true;
				logger.info("Waiting to avoid transform conflicts: {}",
						affects);
				try {
					wait(100L);
				} catch (InterruptedException e) {
					throw WrappedRuntimeException.wrap(e);
				}
			}
		}

		class PersistenceEventAffects {
			private DomainTransformPersistenceEvent event;

			private boolean backend;

			int sequenceId;

			private Thread thread;

			private long creationTime;

			PersistenceEventAffects(DomainTransformPersistenceEvent event,
					boolean backend, int sequenceId) {
				this.thread = Thread.currentThread();
				this.event = event;
				this.backend = backend;
				this.sequenceId = sequenceId;
				this.creationTime = System.currentTimeMillis();
			}

			private boolean conflictsWith(PersistenceEventAffects other) {
				AdjunctTransformCollation collation = event
						.getPreProcessCollation();
				AdjunctTransformCollation otherCollation = other.event
						.getPreProcessCollation();
				return collation.conflictsWith(otherCollation);
			}

			private Stream<EntityCollation>
					getConflicting(PersistenceEventAffects other) {
				AdjunctTransformCollation collation = event
						.getPreProcessCollation();
				AdjunctTransformCollation otherCollation = other.event
						.getPreProcessCollation();
				return collation.getConflictingCollations(otherCollation);
			}

			Stream<EntityLocator> getConflictingLocators() {
				return canConflictWith().map(pea -> getConflicting(pea))
						.flatMap(s -> s).map(EntityCollation::getLocator)
						.distinct();
			}

			private Stream<PersistenceEventAffects> canConflictWith() {
				return affects.values().stream()
						.filter(pea -> pea.backend == !backend
								&& pea.sequenceId < sequenceId);
			}

			// deliberately *don't* check if two non-backend persistenceEvents
			// conflict - that's for another day
			///
			// only called with TransformInterpolator monitor
			//
			//// only wait (and check for conflicts) with sequence-prior affects
			// - avoids deadlock and provides fairness
			boolean isNonConflicting() {
				return canConflictWith()
						.noneMatch(pea -> pea.conflictsWith(this));
			}

			public boolean isTimedOut(long currentTimeMillis) {
				return currentTimeMillis - creationTime > 10
						* TimeConstants.ONE_SECOND_MS;
			}

			@Override
			public String toString() {
				FormatBuilder fb = new FormatBuilder().separator(" - ");
				fb.format("Thread: %s", thread.getName());
				fb.format("Backend: %s", backend);
				fb.format("SequenceId: %s", sequenceId);
				fb.format("Event: %s",
						event.getFirstUuid().orElse("<no uuid>"));
				fb.format("CreationTime: %s", creationTime);
				fb.format("Conflicts: %s",
						getConflictingLocators().collect(Collectors.toList()));
				return fb.toString();
			}
		}
	}
}
