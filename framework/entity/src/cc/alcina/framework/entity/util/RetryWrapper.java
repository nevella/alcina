package cc.alcina.framework.entity.util;

import java.util.concurrent.Callable;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class RetryWrapper<T> {

    static int ONE_SECOND = 1000;

    public static <O> O runWithMaxReties(int maxCount, Callable<O> callable)
            throws Exception{
        return new RetryWrapper<O>(maxCount, callable).call();
    }

    private int maxCount;

    private Callable<T> callable;

    public RetryWrapper(int maxCount, Callable<T> callable) {
        this.maxCount = maxCount;
        this.callable = callable;
    }

    public T call() throws Exception {
        int currentCount = 0;
        Exception lastException = null;
        while (currentCount < maxCount) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
            }
            currentCount++;
            Thread.sleep((long) Math.pow(2, currentCount) * ONE_SECOND);
        }
        throw new WrappedRuntimeException("Exceeded retry count", lastException);
    }
}