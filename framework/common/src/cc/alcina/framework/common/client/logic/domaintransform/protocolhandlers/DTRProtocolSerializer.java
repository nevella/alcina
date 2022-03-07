package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CachingMap;

@Registration(ClearStaticFieldsOnAppShutdown.class)
public class DTRProtocolSerializer {
	public static final String CONTEXT_EXCEPTION_DEBUG = DTRProtocolSerializer.class
			.getName() + ".CONTEXT_EXCEPTION_DEBUG";

	static CachingMap<String, Class> protocolHandlerLookup = new CachingMap<>(
			protocolVersion -> Registry.query(DTRProtocolHandler.class)
					.implementations()
					.filter(handler -> handler.handlesVersion()
							.equals(protocolVersion))
					.findFirst().map(DTRProtocolHandler::getClass)
					.orElse((Class) PlaintextProtocolHandler.class));

	public void deserialize(DomainTransformRequest request,
			String protocolVersion, String serializedEvents) {
		request.setEvents(
				getHandler(protocolVersion).deserialize(serializedEvents));
	}

	public DTRProtocolHandler getHandler(String protocolVersion) {
		return (DTRProtocolHandler) Reflections
				.newInstance(protocolHandlerLookup.get(protocolVersion));
	}

	public String serialize(DomainTransformEvent domainTransformEvent) {
		StringBuffer sb = new StringBuffer();
		getHandler(PlaintextProtocolHandler.VERSION)
				.appendTo(domainTransformEvent, sb);
		return sb.toString();
	}

	public String serialize(DomainTransformRequest request,
			String protocolVersion) {
		return getHandler(protocolVersion).serialize(request.getEvents());
	}
}
