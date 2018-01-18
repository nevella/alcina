package cc.alcina.framework.servlet.servlet;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.server.rpc.RPCRequest;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = CommonRemoteServiceServletSupport.class, implementationType = ImplementationType.SINGLETON)
public class CommonRemoteServiceServletSupport {
	public static CommonRemoteServiceServletSupport get() {
		return Registry.impl(CommonRemoteServiceServletSupport.class);
	}

	/**
	 * the instance used by the server layer when acting as a client to the ejb
	 * layer. Note - this must be set on webapp startup
	 */
	private ClientInstance serverAsClientInstance;

	private Map<Long, HiliLocatorMap> clientInstanceLocatorMap = new HashMap<Long, HiliLocatorMap>();

	private int transformRequestCounter = 1;

	private MetricTracker<RPCRequest> metricTracker = new MetricTracker<>();

	public void appShutdown() {
		metricTracker.stop();
	}

	public HiliLocatorMap
			getLocatorMapForClient(DomainTransformRequest request) {
		Long clientInstanceId = request.getClientInstance().getId();
		Map<Long, HiliLocatorMap> clientInstanceLocatorMap = getClientInstanceLocatorMap();
		synchronized (clientInstanceLocatorMap) {
			if (!clientInstanceLocatorMap.containsKey(clientInstanceId)) {
				clientInstanceLocatorMap.put(clientInstanceId,
						new HiliLocatorMap());
			}
		}
		HiliLocatorMap locatorMap = clientInstanceLocatorMap
				.get(clientInstanceId);
		return locatorMap;
	}

	public MetricTracker<RPCRequest> getMetricTracker() {
		return this.metricTracker;
	}

	public ClientInstance getServerAsClientInstance() {
		return this.serverAsClientInstance;
	}

	public void
			setServerAsClientInstance(ClientInstance serverAsClientInstance) {
		this.serverAsClientInstance = serverAsClientInstance;
	}

	Map<Long, HiliLocatorMap> getClientInstanceLocatorMap() {
		return this.clientInstanceLocatorMap;
	}

	int getTransformRequestCounter() {
		return this.transformRequestCounter;
	}

	synchronized int nextTransformRequestId() {
		return transformRequestCounter++;
	}
}
