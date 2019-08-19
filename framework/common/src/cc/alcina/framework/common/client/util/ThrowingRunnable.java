package cc.alcina.framework.common.client.util;

import java.util.Collection;

import cc.alcina.framework.common.client.WrappedRuntimeException;

@FunctionalInterface
public interface ThrowingRunnable {
    public static void runAll(Collection<ThrowingRunnable> runnables) {
        try {
            for (ThrowingRunnable throwingRunnable : runnables) {
                throwingRunnable.run();
            }
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public void run() throws Exception;
}