package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.PlaintextProtocolHandler;

public class DTESerializationPolicy {
	private String initialObjectPersistenceProtocol = PlaintextProtocolHandler.VERSION;

	private String transformPersistenceProtocol = PlaintextProtocolHandler.VERSION;

	public String getInitialObjectPersistenceProtocol() {
		return this.initialObjectPersistenceProtocol;
	}

	public String getTransformPersistenceProtocol() {
		return this.transformPersistenceProtocol;
	}
}
