package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

public class HandshakeSignal extends ExtensibleEnum {
	public HandshakeSignal(String key) {
		super(key);
	}

	public static final HandshakeSignal LOGGED_OUT = new HandshakeSignal(
			"LOGGED_OUT");

	public static final HandshakeSignal LOGGED_IN = new HandshakeSignal(
			"LOGGED_IN");

	public static final HandshakeSignal OBJECTS_INVALIDATED = new HandshakeSignal(
			"OBJECTS_INVALIDATED");
}
