package cc.alcina.framework.classmeta.rdb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.persistence.WrappedObject.WrappedObjectHelper;
import cc.alcina.framework.entity.util.ShellWrapper;

@RegistryLocation(registryPoint = RdbProxies.class, implementationType = ImplementationType.SINGLETON)
public class RdbProxies {
	public static RdbProxies get() {
		return Registry.impl(RdbProxies.class);
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	RdbEndpointSchema schema;

	List<Endpoint> endpoints = new ArrayList<>();

	public RdbProxies() {
		ResourceUtilities.set("JacksonJsonObjectSerializer.maxLength",
				"50000000");
	}

	public Endpoint endpointByName(String name) {
		return endpoints.stream().filter(e -> e.descriptor.name.equals(name))
				.findFirst().orElse(null);
	}

	public synchronized void replaceEndpoint(Endpoint endpoint) {
		endpoints.remove(endpoint);
		// restart with a delay to make sure we don't answer old calls
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
					RdbProxies.this.start(endpoint.descriptor);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	public void start() {
		String modelXml = null;
		try {
			modelXml = ResourceUtilities
					.readRelativeResource("../schema/rdb/rdbEndpointSchema.xml");
		} catch (Exception e) {
			if (!CommonUtils.hasCauseOfClass(e, NullPointerException.class)) {
				e.printStackTrace();
			}
		}
		if (modelXml == null) {
			return;
		} else {
			schema = WrappedObjectHelper.xmlDeserialize(RdbEndpointSchema.class,
					modelXml);
		}
		schema.endpointDescriptors.forEach(this::start);
		EntityLayerLogging.setLevel("cc.alcina.framework.classmeta.rdb",
				Level.INFO);
		if (Boolean.getBoolean("testRdbProxies") || "ee".isEmpty()) {
			try {
				Thread.sleep(1000);
				new ShellWrapper().runBashScript(
						"/usr/bin/java -jar /g/alcina/lib/framework/dev/eclipse_remote_control_client.jar execute_command hija.app0z.jade.io DEBUG");
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	private synchronized void start(RdbEndpointDescriptor descriptor) {
		Endpoint endpoint = descriptor.jdwpAttach
				? new DebuggeeEndpoint(descriptor)
				: new DebuggerEndpoint(descriptor);
		endpoints.add(endpoint);
		endpoint.launch();
	}

	public static class RdbEndpointDescriptor {
		public boolean jdwpAttach;

		public int jdwpPort;

		public String jdwpHost;

		public String name;

		public TransportType transportType;

		public String transportUrl;

		public String transportEndpointName;

		public int transportDelay;

		public long transportNotificationBundleWait;

		@Override
		public String toString() {
			return name;
		}
	}

	@XmlRootElement
	public static class RdbEndpointSchema {
		public List<RdbEndpointDescriptor> endpointDescriptors = new ArrayList<>();
	}

	public enum TransportType {
		shared_vm, http_initiator, http_acceptor;
	}
}
