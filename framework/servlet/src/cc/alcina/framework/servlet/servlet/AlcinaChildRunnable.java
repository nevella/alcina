package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;

public abstract class AlcinaChildRunnable implements Runnable {
	private int tLooseContextDepth;

	private PermissionsManager pm;

	private String threadName;

	public AlcinaChildRunnable(String name) {
		this.threadName = name;
		this.pm = PermissionsManager.get();
	}

	private boolean logExceptions = false;

	public AlcinaChildRunnable logExceptions() {
		logExceptions = true;
		return this;
	}

	protected abstract void run0() throws Exception;

	@Override
	public void run() {
		Thread.currentThread().setName(threadName);
		try {
			LooseContext.push();
			// different thread-local
			tLooseContextDepth = LooseContext.depth();
			this.pm.copyTo(PermissionsManager.get());
			run0();
		} catch (OutOfMemoryError e) {
			SEUtilities.threadDump();
			throw e;
		} catch (Throwable e) {
			if (logExceptions) {
				e.printStackTrace();
			}
			if (e instanceof RuntimeException) {
				throw ((RuntimeException) e);
			}
			throw new RuntimeException(e);
		} finally {
			LooseContext.confirmDepth(tLooseContextDepth);
			LooseContext.pop();
		}
	}
}