package cc.alcina.framework.gwt.persistence.client;

public class PersistenceExceptionInterceptor {

	private static PersistenceExceptionInterceptor theInstance;
	public static void register(PersistenceExceptionInterceptor interceptor){
		theInstance=interceptor;
	}
	public static PersistenceExceptionInterceptor get() {
		if (theInstance == null) {
			theInstance = new PersistenceExceptionInterceptor();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}
	public boolean checkTerminateAfterPossiblePersistenceException(Throwable t){
		return false;
	}
}
