package cc.alcina.framework.entity.transform.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

public interface PersistenceLayerTransformRetryPolicy {
	public boolean isRetry(TransformPersistenceToken token,
			DomainTransformLayerWrapper wrapper, RuntimeException ex,
			String preparedStatementCausingIssue);

	public static class NoRetry
			implements PersistenceLayerTransformRetryPolicy {
		@Override
		public boolean isRetry(TransformPersistenceToken token,
				DomainTransformLayerWrapper wrapper, RuntimeException ex,
				String preparedStatementCausingIssue) {
			return false;
		}
	}

	public static class JobPersistenceBackoff
			implements PersistenceLayerTransformRetryPolicy {
		static Logger logger = LoggerFactory
				.getLogger(JobPersistenceBackoff.class);

		int initialDelayMs;

		int retries;

		double delayMs;

		double retryMultiplier;

		public JobPersistenceBackoff(int initialDelayMs, int retries,
				double delayMs, double retryMultiplier) {
			this.initialDelayMs = initialDelayMs;
			this.retries = retries;
			this.delayMs = delayMs;
			this.retryMultiplier = retryMultiplier;
		}

		@Override
		public boolean isRetry(TransformPersistenceToken token,
				DomainTransformLayerWrapper wrapper, RuntimeException ex,
				String preparedStatementCausingIssue) {
			if (retries-- > 0) {
				boolean jobPersistenceConflict = preparedStatementCausingIssue != null
						&& preparedStatementCausingIssue
								.contains("update job set ");
				if (jobPersistenceConflict) {
					Ax.simpleExceptionOut(ex);
					logger.warn(
							"Exception in commitWithBackoff [job persistence] [{} transforms], retrying [delay {}ms]",
							token.getRequest().getEvents().size(), delayMs);
					try {
						Thread.sleep((long) delayMs);
					} catch (Exception e) {
						e.printStackTrace();
					}
					delayMs *= (0.5 + Math.random()) * retryMultiplier;
					return true;
				}
			}
			return false;
		}
	}
}
