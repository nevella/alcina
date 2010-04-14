package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;

public interface DTRProtocolHandler {
	public String handlesVersion();
	public String serialize(List<DomainTransformEvent> events);
	public List<DomainTransformEvent> deserialize(String serializedEvents);
}
