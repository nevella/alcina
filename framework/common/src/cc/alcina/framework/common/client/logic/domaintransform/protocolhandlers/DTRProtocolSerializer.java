package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.List;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class DTRProtocolSerializer {
	public DTRProtocolHandler getHandler(String protocolVersion) {
		List<Class> lookup = Registry.get().lookup(DTRProtocolHandler.class);
		for (Class clazz : lookup) {
			DTRProtocolHandler handler = (DTRProtocolHandler) CommonLocator
					.get().classLookup().newInstance(clazz);
			if (handler.handlesVersion().equals(protocolVersion)) {
				return handler;
			}
		}
		return new PlaintextProtocolHandler();
	}

	public String serialize(DomainTransformRequest request,
			String protocolVersion) {
		return getHandler(protocolVersion).serialize(request.getItems());
	}

	public void deserialize(DomainTransformRequest request,
			String protocolVersion, String serializedEvents) {
		request.setItems(getHandler(protocolVersion).deserialize(
				serializedEvents));
	}
	public String serialize(DomainTransformEvent domainTransformEvent) {
		StringBuffer sb = new StringBuffer();
		getHandler(PlaintextProtocolHandler.VERSION).appendTo(
				domainTransformEvent, sb);
		return sb.toString();
	}
}
