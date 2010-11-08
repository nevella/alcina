package cc.alcina.framework.gwt.gears.client;

import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.PlaintextProtocolHandler0pt1;

public class DTESerializationPolicy {
	private String initialObjectPersistenceProtocol = PlaintextProtocolHandler0pt1.VERSION;

	private String transformPersistenceProtocol = PlaintextProtocolHandler0pt1.VERSION;

	public String getInitialObjectPersistenceProtocol() {
		return this.initialObjectPersistenceProtocol;
	}

	public String getTransformPersistenceProtocol() {
		return this.transformPersistenceProtocol;
	}
}
