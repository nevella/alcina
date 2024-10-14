package cc.alcina.framework.entity.util;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * <p>
 * This class attempts to perform callable, backing off retries. The default
 * values give infinite retry, retries at max time ~ every 1s (after backoff),
 * log every 10s (after backoff)
 * 
 * <p>
 * Use for things like API calls, to avoid log spam
 * 
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class RetryWrapper<T> {
	public int maxRetryCount = Integer.MAX_VALUE;

	public int maxBackoffPower = 4;

	public int logEveryNFails = 10;

	public long initialDelayMs = 60;

	Callable<T> callable;

	public String taskDescription;

	long start;

	public RetryWrapper(Callable<T> callable) {
		this.callable = callable;
		if (taskDescription == null) {
			taskDescription = callable.toString();
		}
	}

	int retryCount = 0;

	public T call() throws Exception {
		start = System.currentTimeMillis();
		Exception lastException = null;
		while (true) {
			try {
				T result = callable.call();
				if (retryCount > 0) {
					Ax.out("Retried %s times, succeeded", retryCount);
				}
				return result;
			} catch (Exception e) {
				if (!isRetryableException(e)) {
					throw e;
				}
				lastException = e;
			}
			logException(lastException);
			retryCount++;
			if (retryCount >= maxRetryCount) {
				break;
			}
			int delayExponent = Math.min(retryCount, maxBackoffPower);
			Thread.sleep((long) Math.pow(2, delayExponent) * initialDelayMs);
		}
		throw new WrappedRuntimeException("Exceeded retry count",
				lastException);
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	protected boolean isRetryableException(Exception e) {
		return true;
	}

	protected void logException(Exception e) {
		if (retryCount % logEveryNFails == 0) {
			logger.warn("[retry {} :: {} ms] - {} - {}", retryCount + 1,
					System.currentTimeMillis() - start, taskDescription,
					CommonUtils.toSimpleExceptionMessage(e));
		}
	}
}