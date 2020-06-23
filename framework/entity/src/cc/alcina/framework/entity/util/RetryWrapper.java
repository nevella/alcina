package cc.alcina.framework.entity.util;

import java.util.concurrent.Callable;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class RetryWrapper<T> {

    public static <O> O runWithMaxReties(int maxCount, Callable<O> callable) {
        return new RetryWrapper<O>(maxCount, callable).call();
    }

    private int maxCount;

    private Callable<T> callable;

    public RetryWrapper(int maxCount, Callable<T> callable) {
        this.maxCount = maxCount;
        this.callable = callable;
    }

    public T call() {
        int currentCount = 0;
        Exception lastException = null;
        while (currentCount < maxCount) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
            }
            currentCount++;
        }
        throw new WrappedRuntimeException("Exceeded retry count", lastException);
    }
}