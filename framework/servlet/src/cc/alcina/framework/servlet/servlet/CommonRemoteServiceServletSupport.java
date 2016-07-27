package cc.alcina.framework.servlet.servlet;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.server.rpc.RPCRequest;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = CommonRemoteServiceServletSupport.class, implementationType = ImplementationType.SINGLETON)
public class CommonRemoteServiceServletSupport {
	/**
	 * the instance used by the server layer when acting as a client to the ejb
	 * layer. Note - this must be set on webapp startup
	 */
	private ClientInstance serverAsClientInstance;

	private Map<Long, HiliLocatorMap> clientInstanceLocatorMap = new HashMap<Long, HiliLocatorMap>();

	private int transformRequestCounter = 1;

	synchronized int nextTransformRequestId() {
		return transformRequestCounter++;
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

	private MetricTracker<RPCRequest> metricTracker = new MetricTracker<>();

	public MetricTracker<RPCRequest> getMetricTracker() {
		return this.metricTracker;
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

	int getTransformRequestCounter() {
		return this.transformRequestCounter;
	}

	public void appShutdown() {
		metricTracker.stop();
	}
}
