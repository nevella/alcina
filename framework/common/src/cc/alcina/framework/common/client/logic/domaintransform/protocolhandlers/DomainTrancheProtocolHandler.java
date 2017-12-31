package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@RegistryLocation(registryPoint = DTRProtocolHandler.class)
@ClientInstantiable
public class DomainTrancheProtocolHandler implements DTRProtocolHandler {
	public static final String VERSION = "DomainTranche/1.0";

	public void appendTo(DomainTransformEvent domainTransformEvent,
			StringBuffer sb) {
		throw new UnsupportedOperationException();
	}

	public List<DomainTransformEvent> deserialize(String serializedEvents) {
		throw new UnsupportedOperationException();
	}

	public String deserialize(String serializedEvents,
			List<DomainTransformEvent> events, int maxCount) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StringBuffer finishSerialization(StringBuffer sb) {
		return sb;
	}

	public int getOffset() {
		throw new UnsupportedOperationException();
	}

	public String handlesVersion() {
		return VERSION;
	}

	public String serialize(List<DomainTransformEvent> events) {
		throw new UnsupportedOperationException();
	}
}
