package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class PersistenceExceptionInterceptor {
	public static PersistenceExceptionInterceptor get() {
		PersistenceExceptionInterceptor singleton = Registry
				.checkSingleton(PersistenceExceptionInterceptor.class);
		if (singleton == null) {
			singleton = new PersistenceExceptionInterceptor();
			Registry.registerSingleton(PersistenceExceptionInterceptor.class,
					singleton);
		}
		return singleton;
	}

	protected PersistenceExceptionInterceptor() {
	}

	public boolean
			checkTerminateAfterPossiblePersistenceException(Throwable t) {
		return false;
	}
}
