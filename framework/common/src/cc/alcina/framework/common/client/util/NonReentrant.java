package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class NonReentrant {
	boolean inCall = false;

	public NonReentrant() {
	}

	public void enter(ThrowingRunnable runnable) {
		if (!inCall) {
			try {
				inCall = true;
				try {
					runnable.run();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			} finally {
				inCall = false;
			}
		}
	}
}