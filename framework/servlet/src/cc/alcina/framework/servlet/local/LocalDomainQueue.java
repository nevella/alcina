package cc.alcina.framework.servlet.local;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ThrowingRunnable;

@Registration.Singleton
public class LocalDomainQueue {
	public static void checkNotOnDomainThread() {
		if (Thread.currentThread() == get().thread) {
			throw new IllegalStateException(
					Ax.format("Calling blocking code on domain thread: %s",
							Thread.currentThread()));
		}
	}

	public static void checkOnDomainThread() {
		if (Thread.currentThread() != get().thread) {
			throw new IllegalStateException(Ax.format(
					"Calling domain modification code on non-domain thread: %s",
					Thread.currentThread()));
		}
	}

	public static LocalDomainQueue get() {
		return Registry.impl(LocalDomainQueue.class);
	}

	// FIXME - jobs - move to txenvironment
	public static void run(ThrowingRunnable runnable) {
		get().execute0(runnable);
	}

	private BlockingQueue<RunnableEntry> queue = new LinkedBlockingQueue<>();

	private Thread thread;

	Logger logger = LoggerFactory.getLogger(getClass());

	public LocalDomainQueue() {
		QueueExecutorPoller executorPoller = new QueueExecutorPoller();
		thread = new Thread(executorPoller, "local-domain-access");
		thread.setDaemon(true);
		thread.start();
	}

	private void execute0(ThrowingRunnable runnable) {
		if (Thread.currentThread() == thread) {
			try {
				// reentrant call
				runnable.run();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		} else {
			RunnableEntry entry = new RunnableEntry(runnable);
			queue.add(entry);
			entry.await();
		}
	}

	class QueueExecutorPoller implements Runnable {
		boolean finished;

		@Override
		public void run() {
			while (!finished) {
				RunnableEntry entry = null;
				try {
					entry = queue.take();
					entry.runnable.run();
				} catch (Throwable t) {
					logger.warn("Local domain access issue", t);
					entry.throwable = t;
				} finally {
					if (entry != null) {
						entry.latch.countDown();
						entry = null;
					}
				}
			}
		}
	}

	// FIXME - localdomain - should *probably* be a completable future
	class RunnableEntry {
		ThrowingRunnable runnable;

		CountDownLatch latch = new CountDownLatch(1);

		Throwable throwable;

		RunnableEntry(ThrowingRunnable runnable) {
			this.runnable = runnable;
		}

		void await() {
			try {
				latch.await();
				if (throwable != null) {
					throw throwable;
				}
			} catch (Throwable e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}
}
