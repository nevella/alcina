package cc.alcina.framework.servlet;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.shared.UmbrellaException;

import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.AlcinaTopics;

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
 * <li>note that child (cascading) threads should not lock on any objects locked by td1  
 * </ul>
 * hwuh
 *
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class CascadingTransformSupport {
	private static ThreadLocal<CascadingTransformSupport> supports = new ThreadLocal() {
		protected synchronized CascadingTransformSupport initialValue() {
			return new CascadingTransformSupport();
		}
	};

	public static CascadingTransformSupport get() {
		return supports.get();
	}

	private List<Thread> waitFor = new ArrayList<Thread>();

	private Set<Throwable> throwables = new LinkedHashSet<Throwable>();

	private Thread launchingThread;

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

	public void runTransformingChild(Runnable runnable) {
		new CascadingTransformWorker(runnable).start();
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
				super.run();
			} catch (Throwable t) {
				t.printStackTrace();
				throwable = t;
			} finally {
				cascadingTransformSupport.releaseChildThread(this);
			}
		}
	}
}
