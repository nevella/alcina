package cc.alcina.framework.entity.persistence;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
	static final AtomicInteger poolNumber = new AtomicInteger(1);

	final ThreadGroup group;

	final AtomicInteger threadNumber = new AtomicInteger(1);

	final String namePrefix;

	public NamedThreadFactory(String prefix) {
		group = Thread.currentThread().getThreadGroup();
		namePrefix = "pool-" + prefix + "-" + poolNumber.getAndIncrement()
				+ "-thread-";
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r,
				namePrefix + threadNumber.getAndIncrement(), 0);
		if (t.isDaemon())
			t.setDaemon(false);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}