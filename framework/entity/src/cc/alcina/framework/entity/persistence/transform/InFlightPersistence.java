package cc.alcina.framework.entity.persistence.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

/*
 * Synchronisation - sync all entry operations (since registering a token will
 * change the watched tokens for all persisting threads)
 * 
 * This records all tokens active at any point in this token's db-tx-wrapped
 * persistencce - so any conflicts *should* be caused by some other thread
 */
@Registration.Singleton
public class InFlightPersistence {
	static InFlightPersistence get() {
		return Registry.impl(InFlightPersistence.class);
	}

	Map<TransformPersistenceToken, List<TokenThread>> tokenConcurrentTokens = new LinkedHashMap<>();

	class TokenThread {
		public TokenThread(TransformPersistenceToken token) {
			this.token = token;
			this.thread = Thread.currentThread();
			this.entryStackTrace = Arrays.stream(thread.getStackTrace())
					.map(Object::toString).collect(Collectors.joining("\n"));
		}

		TransformPersistenceToken token;

		Thread thread;

		String entryStackTrace;

		@Override
		public String toString() {
			FormatBuilder format = new FormatBuilder();
			format.line("Thread: %s", thread);
			format.line(
					"--------------------------------------------------------------------------------------------------");
			format.line("Entry stack trace:");
			format.line(entryStackTrace);
			format.line(
					"--------------------------------------------------------------------------------------------------");
			format.line("Request transforms:");
			// this can be truly enormous, so to allow the actual trace to be
			// read, log here
			format.line(Ax.trim(token.getRequest().toString(), 100000));
			format.line(
					"--------------------------------------------------------------------------------------------------");
			return format.toString();
		}
	}

	synchronized void register(TransformPersistenceToken token,
			boolean register) {
		if (!register) {
			tokenConcurrentTokens.remove(token);
		} else {
			tokenConcurrentTokens.put(token, new ArrayList<>());
			TokenThread tokenThread = new TokenThread(token);
			tokenConcurrentTokens.values()
					.forEach(list -> list.add(tokenThread));
		}
	}

	synchronized void onThrowException(TransformPersistenceToken token,
			Exception e) {
		logger.warn(
				"\nPersistence exception:\n=====================================================================\n",
				e);
		logger.warn(
				"\nLogging context thread data:\n=====================================================================\n");
		tokenConcurrentTokens.get(token)
				.forEach(tt -> logger.warn(tt.toString()));
		logger.warn(
				"\nLogging context thread data [finished]\n=====================================================================\n");
	}

	Logger logger = LoggerFactory.getLogger(getClass());
}