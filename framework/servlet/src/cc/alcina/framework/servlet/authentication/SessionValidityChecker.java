package cc.alcina.framework.servlet.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.entity.util.TimedCacheMap;

/**
 * Abstracted session validity checker that caches results
 */
public abstract class SessionValidityChecker<A extends AuthenticationSession> {

	private TimedCacheMap<A, StateCheck> checks;

	protected Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Instantiate a new session validity checker with given cache duration
	 * @param cacheExpiry Milliseconds to persist cached check results
	 */
	public SessionValidityChecker(long cacheExpiry) {
		this.checks = new TimedCacheMap<>(cacheExpiry);
	}

	/**
	 * <p>Check session is valid</p>
	 * <p>Returns cached responses if present</p>
	 * @param session AuthenticationSession
	 * @return Session validity
	 */
	public boolean isValid(A session) {
		return checks.computeIfAbsent(session, StateCheck::new).checkValid();
	}

	/**
	 * <p>Check the session validity, calling external services if needed</p>
	 * <p>Results are cached temporarily</p>
	 * @param session AuthenticationSession
	 * @return Session validity
	 */
	protected abstract boolean checkValidity(A session);
	
	/**
	 * Validity state cached entry
	 */
	private class StateCheck {
		A session;

		boolean inflight;

		Boolean result;

		StateCheck(A session) {
			this.session = session;
		}

		/**
		 * Check validity, avoiding duplicated checks
		 */
		synchronized boolean checkValid() {
			try {
				// If there isn't already a result present, compute it
				if (result == null) {
					// If a check is already in progress, just wait on it
					// Otherwise, compute validity and then notify any waiters
					if (inflight) {
						wait();
					} else {
						logger.debug("Checking session validity: {sessionId={}}", session.getId());
						inflight = true;
						result = checkValidity(session);
						inflight = false;
						notifyAll();
					}
				}
				return result;
			} catch (InterruptedException e) {
				e.printStackTrace();
				// err on the side of leniency
				return true;
			}
		}
	}
}