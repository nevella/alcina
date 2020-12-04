package cc.alcina.framework.entity.persistence.transform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.event.shared.UmbrellaException;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;

/**
 * <p>
 * The logic is:
 * </p>
 * <ul>
 * <li>listeners l1, l2
 * <li>transfroms t1, t2
 *
 * <li>thread td1 enqueues transforms t1
 * <li>t1 fires
 * <li>listener l1 launches a thread which will enqueue transforms t2 (different
 * thread, enqueued)
 * <li>listener l2 does whatever
 * <li>t1 finished firing
 * <li>only <b>now</b> does t2 fire
 * <li>l1 hears, l2
 * <li><b>now</b>, because of cascading support, td1 continues
 * <li>note that child (cascading) threads should not lock on any objects locked
 * by td1
 * </ul>
 * hwuh
 *
 * @author nick@alcina.cc
 * 
 *         2020 - Wherever possible, use jobs instead of this (it has no
 *         guarantees of successful completion)
 *
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class CascadingTransformSupport {
	private static ThreadLocal<CascadingTransformSupport> supports = new ThreadLocal() {
		@Override
		protected synchronized CascadingTransformSupport initialValue() {
			return new CascadingTransformSupport();
		}
	};

	private static ConcurrentHashMap<Thread, Object> cascadeThreads = new ConcurrentHashMap<>();

	private static Map<Long, CascadingTransformSupport> crossThread = new ConcurrentHashMap<>();

	private static Map<Thread, CascadingTransformSupport> crossThreadFiring = new ConcurrentHashMap<>();

	static Logger logger = LoggerFactory
			.getLogger(CascadingTransformSupport.class);

	public static void finishedFiring(DomainTransformPersistenceEvent event) {
		crossThread.remove(event.getMaxPersistedRequestId());
		crossThreadFiring.remove(Thread.currentThread());
		LooseContext.pop();
	}

	public static CascadingTransformSupport get() {
		CascadingTransformSupport adopted = crossThreadFiring
				.get(Thread.currentThread());
		if (adopted != null) {
			return adopted;
		} else {
			return supports.get();
		}
	}

	public static void register(DomainTransformPersistenceEvent event) {
		CascadingTransformSupport support = CascadingTransformSupport.get();
		crossThread.put(event.getMaxPersistedRequestId(), support);
		support.copyContext.putAll(LooseContext.getContext().properties);
	}

	public static void registerFiring(DomainTransformPersistenceEvent event) {
		LooseContext.push();
		CascadingTransformSupport blockingThreadSupport = crossThread
				.get(event.getMaxPersistedRequestId());
		if (blockingThreadSupport != null) {
			crossThreadFiring.put(Thread.currentThread(),
					blockingThreadSupport);
			blockingThreadSupport.copyContext
					.forEach((k, v) -> LooseContext.set(k, v));
		}
	}

	private List<Thread> waitFor = new ArrayList<Thread>();

	private Set<Throwable> throwables = new LinkedHashSet<Throwable>();

	private Thread launchingThread;

	Map<String, Object> copyContext = new LinkedHashMap<>();

	public void afterTransform() {
		launchingThread = null;
	}

	public void beforeTransform() {
		if (launchingThread != null) {
			Exception warn = new Exception(
					"Pushing transforms while still in an event publication cycle");
			warn.printStackTrace();
			AlcinaTopics.notifyDevWarning(warn);
		}
		launchingThread = Thread.currentThread();
	}

	public UmbrellaException getException() {
		if (throwables.isEmpty()) {
			return null;
		}
		UmbrellaException ex = new UmbrellaException(throwables);
		throwables.clear();
		return ex;
	}

	public boolean hasChildren() {
		return !waitFor.isEmpty();
	}

	public boolean isCascadeThread() {
		return cascadeThreads.containsKey(Thread.currentThread());
	}

	public void runTransformingChild(Runnable runnable) {
		Runnable wrappedRunnable = runnable;
		if (!(wrappedRunnable instanceof AlcinaChildRunnable)) {
			wrappedRunnable = new AlcinaChildRunnable("cascading-worker") {
				@Override
				protected void run0() throws Exception {
					runnable.run();
				}
			};
		}
		new CascadingTransformWorker(wrappedRunnable).start();
	}

	public void runTransformingChildIfInPublicationCycle(Runnable runnable) {
		if (launchingThread == null) {
			runnable.run();
		} else {
			runTransformingChild(runnable);
		}
	}

	synchronized void addChildThread(CascadingTransformWorker thread) {
		waitFor.add(thread);
	}

	void releaseChildThread(CascadingTransformWorker thread) {
		if (thread.getThrowable() != null) {
			throwables.add(thread.getThrowable());
		}
		synchronized (this) {
			waitFor.remove(thread);
			notifyAll();
		}
		// TODO - zeroex - notify exception via topic
	}

	static class CascadingTransformWorker extends Thread {
		private CascadingTransformSupport cascadingTransformSupport;

		private Throwable throwable;

		public CascadingTransformWorker(Runnable runnable) {
			super(runnable);
			cascadingTransformSupport = CascadingTransformSupport.get();
			cascadingTransformSupport.addChildThread(this);
		}

		public Throwable getThrowable() {
			return this.throwable;
		}

		@Override
		public final void run() {
			try {
				cascadeThreads.put(Thread.currentThread(),
						CascadingTransformWorker.class);
				super.run();
			} catch (Throwable t) {
				t.printStackTrace();
				throwable = t;
			} finally {
				cascadingTransformSupport.releaseChildThread(this);
				cascadeThreads.remove(Thread.currentThread());
			}
		}
	}
}
