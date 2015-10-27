package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.List;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CachingMap;

@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class DTRProtocolSerializer {
	public DTRProtocolHandler getHandler(String protocolVersion) {
		return (DTRProtocolHandler) Reflections.classLookup().newInstance(
				protocolHandlerLookup.get(protocolVersion));
	}

	static CachingMap<String, Class> protocolHandlerLookup = new CachingMap<>(
			protocolVersion -> {
				List<Class> lookup = Registry.get().lookup(
						DTRProtocolHandler.class);
				for (Class clazz : lookup) {
					DTRProtocolHandler handler = (DTRProtocolHandler) Reflections
							.classLookup().newInstance(clazz);
					if (handler.handlesVersion().equals(protocolVersion)) {
						return clazz;
					}
				}
				return PlaintextProtocolHandler.class;
			});

	public String serialize(DomainTransformRequest request,
			String protocolVersion) {
		return getHandler(protocolVersion).serialize(request.getEvents());
	}

	public void deserialize(DomainTransformRequest request,
			String protocolVersion, String serializedEvents) {
		request.setEvents(getHandler(protocolVersion).deserialize(
				serializedEvents));
	}

	public String serialize(DomainTransformEvent domainTransformEvent) {
		StringBuffer sb = new StringBuffer();
		getHandler(PlaintextProtocolHandler.VERSION).appendTo(
				domainTransformEvent, sb);
		return sb.toString();
	}
}
