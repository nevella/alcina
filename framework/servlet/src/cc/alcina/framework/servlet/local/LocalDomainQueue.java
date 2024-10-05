package cc.alcina.framework.servlet.local;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IdCounter;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ThrowingRunnable;

@Registration.Singleton
public class LocalDomainQueue {
	public static final String CONTEXT_IN_DOMAIN = LocalDomainQueue.class
			.getName() + ".CONTEXT_IN_DOMAIN";

	private static boolean paused;

	public static void checkNotInDomainContext() {
		if (inDomainContext()) {
			paused = true;
			throw new IllegalStateException(
					Ax.format("Calling blocking code in domain context: %s",
							Thread.currentThread()));
		}
	}

	public static void checkInDomainContext() {
		if (!inDomainContext()) {
			throw new IllegalStateException(Ax.format(
					"Calling domain modification code in non-domain context: %s",
					Thread.currentThread()));
		}
	}

	static boolean inDomainContext() {
		return LooseContext.is(CONTEXT_IN_DOMAIN);
	}

	public static LocalDomainQueue get() {
		return Registry.impl(LocalDomainQueue.class);
	}

	// FIXME - jobs - move to txenvironment
	public static void run(ThrowingRunnable runnable) {
		get().execute0(runnable);
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	QueueExecutor queueExecutor;

	public LocalDomainQueue() {
		this.queueExecutor = new QueueExecutor();
	}

	private void execute0(ThrowingRunnable runnable) {
		queueExecutor.queue(runnable);
	}

	/**
	 * TODO - make optionally non-reentrant; and non-blocking; and
	 * context-clearing
	 * 
	 * @author nick@alcina.cc
	 *
	 */
	class QueueExecutor {
		private BlockingQueue<RunnableEntry> queue = new LinkedBlockingQueue<>();

		void queue(ThrowingRunnable runnable) {
			if (paused) {
				synchronized (LocalDomainQueue.class) {
					try {
						LocalDomainQueue.class.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			if (inDomainContext()) {
				try {
					// reentrant call
					runnable.run();
				} catch (Exception e) {
					throw WrappedRuntimeException.wrap(e);
				}
			} else {
				RunnableEntry entry = new RunnableEntry(runnable);
				synchronized (this) {
					queue.add(entry);
					int size = queue.size();
				}
				pump(entry);
			}
		}

		void pump(RunnableEntry entry) {
			for (;;) {
				RunnableEntry head = queue.peek();
				if (head == entry) {
					entry.unblock();
					entry.execute();
					queue.poll();
					RunnableEntry next = queue.peek();
					/*
					 * There's a race (thread A peeks here, thread B peeks
					 * earlier) - but they'll just aee and unblock the *same*
					 * object
					 */
					if (next != null) {
						next.unblock();
					}
					return;
				} else {
					entry.await();
				}
			}
		}

		IdCounter counter = new IdCounter();

		// FIXME - localdomain - should *probably* be a completable future
		class RunnableEntry {
			ThrowingRunnable runnable;

			CountDownLatch latch = new CountDownLatch(1);

			Throwable throwable;

			long id;

			RunnableEntry(ThrowingRunnable runnable) {
				// currently unused (LDQ is reentrant)
				id = counter.nextId();
				this.runnable = runnable;
			}

			@Override
			public String toString() {
				return Ax.format("t: %s - id: %s",
						Thread.currentThread().getName(), id);
			}

			void unblock() {
				latch.countDown();
			}

			void execute() {
				try {
					LooseContext.pushWithTrue(CONTEXT_IN_DOMAIN);
					runnable.run();
				} catch (Throwable t) {
					logger.warn("Local domain access issue", t);
					this.throwable = t;
				} finally {
					LooseContext.pop();
				}
			}

			void await() {
				for (;;) {
					try {
						if (latch.await(1, TimeUnit.SECONDS)) {
							break;
						}
					} catch (Throwable e) {
						throw WrappedRuntimeException.wrap(e);
					}
				}
			}
		}
	}
}
