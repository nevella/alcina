package cc.alcina.framework.servlet.job2;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

class CurrentThreadExecutorService extends AbstractExecutorService {
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return true;
	}

	@Override
	public void execute(Runnable runnable) {
		runnable.run();
	}

	@Override
	public boolean isShutdown() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isTerminated() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void shutdown() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException();
	}
}
