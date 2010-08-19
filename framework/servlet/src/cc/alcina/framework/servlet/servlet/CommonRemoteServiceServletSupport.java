package cc.alcina.framework.servlet.servlet;

import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.entity.domaintransform.HiliLocatorMap;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformRequestPersistence.DomainTransformRequestPersistenceSupport;

public class CommonRemoteServiceServletSupport {
	public static CommonRemoteServiceServletSupport get() {
		if (theInstance == null) {
			// always called in app lifecycle startup - no need to worry about
			// simultaneous calls
			theInstance = new CommonRemoteServiceServletSupport();
		}
		return theInstance;
	}

	/**
	 * the instance used by the server layer when acting as a client to the ejb
	 * layer. Note - this must be set on webapp startup
	 */
	private ClientInstance serverAsClientInstance;

	private Map<Long, HiliLocatorMap> clientInstanceLocatorMap = new HashMap<Long, HiliLocatorMap>();

	private DomainTransformRequestPersistenceSupport requestPersistenceSupport = new DomainTransformRequestPersistenceSupport();

	private int transformRequestCounter = 1;

	private static CommonRemoteServiceServletSupport theInstance;

	private CommonRemoteServiceServletSupport() {
		super();
	}

	public void appShutdown() {
		theInstance = null;
	}

	synchronized int nextTransformRequestId() {
		return transformRequestCounter++;
	}

	public DomainTransformRequestPersistenceSupport getRequestPersistenceSupport() {
		return this.requestPersistenceSupport;
	}

	public ClientInstance getServerAsClientInstance() {
		return this.serverAsClientInstance;
	}

	public void setServerAsClientInstance(ClientInstance serverAsClientInstance) {
		this.serverAsClientInstance = serverAsClientInstance;
	}

	Map<Long, HiliLocatorMap> getClientInstanceLocatorMap() {
		return this.clientInstanceLocatorMap;
	}

	int getTransformRequestCounter() {
		return this.transformRequestCounter;
	}
}
