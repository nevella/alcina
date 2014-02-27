package cc.alcina.framework.servlet;

import java.util.ArrayList;
import java.util.List;

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
 * <li>
 * <b>now</b>, because of cascading support, td1 continues
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

	void addChildThread(Thread thread) {
		waitFor.add(thread);
	}

	void releaseChildThread(Thread thread) {
		waitFor.remove(thread);
		synchronized (this) {
			notifyAll();
		}
	}

	public boolean hasChildren() {
		return !waitFor.isEmpty();
	}

	public void runTransformingChild(Runnable runnable) {
		new CascadingTransformWorker(runnable).start();
	}

	static class CascadingTransformWorker extends Thread {
		private CascadingTransformSupport cascadingTransformSupport;

		public CascadingTransformWorker(Runnable runnable) {
			super(runnable);
			cascadingTransformSupport = CascadingTransformSupport.get();
			cascadingTransformSupport.addChildThread(this);
		}

		@Override
		public final void run() {
			try {
				super.run();
			} finally {
				cascadingTransformSupport.releaseChildThread(this);
			}
		}
	}

	private Thread launchingThread;

	public void beforeTransform() {
		if (launchingThread != null) {
			Exception warn = new Exception(
					"Pushing transforms while still in an event publication cycle");
			warn.printStackTrace();
			AlcinaTopics.notifyDevWarning(warn);
		}
		launchingThread = Thread.currentThread();
	}

	public void afterTransform() {
		launchingThread = null;
	}
}
