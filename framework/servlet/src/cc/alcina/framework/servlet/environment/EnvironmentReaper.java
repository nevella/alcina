package cc.alcina.framework.servlet.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Timer;

/**
 * <h2>Overview</h2>
 * <ul>
 * <li>Reap if localhost, enviroment created but no packets received, > 10 secs
 * <li>Reap if any, enviroment created but no packets received, > 60 secs *
 * <li>Reap if any, last packet received > 300 secs
 * </ul>
 * <p>
 * THis is different to environment *source* reaping - a larger topic
 * 
 */
class EnvironmentReaper {
	void start() {
		Timer.Provider.get().getTimer(this::reap)
				.scheduleRepeating(5 * TimeConstants.ONE_SECOND_MS);
	}

	void reap() {
		EnvironmentManager.get().environments.values()
				.forEach(this::conditionallyReap);
	}

	void conditionallyReap(Environment env) {
		boolean isLocalhost = env.access().getSession().provideIsLocalHost();
		long lastPacketReceived = env.access().getLastPacketsReceived()
				.getTime();
		boolean packetsReceived = lastPacketReceived > 0;
		long startTime = env.access().getSession().startTime;
		boolean reap = false;
		if (isLocalhost && !packetsReceived) {
			if (!TimeConstants.within(startTime,
					10 * TimeConstants.ONE_SECOND_MS)) {
				reap = true;
			}
		}
		if (!packetsReceived) {
			if (!TimeConstants.within(startTime,
					60 * TimeConstants.ONE_SECOND_MS)) {
				reap = true;
			}
		}
		long nonInteractionTimeout = env.access().getNonInteractionTimeout()
				.get();
		if (nonInteractionTimeout != 0 && !TimeConstants
				.within(lastPacketReceived, nonInteractionTimeout)) {
			reap = true;
		}
		if (reap) {
			env.access().end("reap/inactive");
		}
	}

	Logger logger = LoggerFactory.getLogger(getClass());
}
