package cc.alcina.framework.gwt.gears.client;

import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.OldPlaintextProtocolHandler;

public class DTESerializationPolicy {
	private String initialObjectPersistenceProtocol = OldPlaintextProtocolHandler.VERSION;

	private String transformPersistenceProtocol = OldPlaintextProtocolHandler.VERSION;

	public String getInitialObjectPersistenceProtocol() {
		return this.initialObjectPersistenceProtocol;
	}

	public String getTransformPersistenceProtocol() {
		return this.transformPersistenceProtocol;
	}
}
