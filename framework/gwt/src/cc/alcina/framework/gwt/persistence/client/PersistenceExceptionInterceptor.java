package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class PersistenceExceptionInterceptor {
	public static PersistenceExceptionInterceptor get() {
		return Registry.impl(PersistenceExceptionInterceptor.class);
	}

	public boolean
			checkTerminateAfterPossiblePersistenceException(Throwable t) {
		return false;
	}
}
