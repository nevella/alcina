package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

public class HandshakeSignal extends ExtensibleEnum {
	public static final HandshakeSignal LOGGED_OUT = new HandshakeSignal(
			"LOGGED_OUT");

	public static final HandshakeSignal LOGGED_IN = new HandshakeSignal(
			"LOGGED_IN");

	// for implementation purposes, identical to LOGGED_IN (check the default
	// handlers)
	public static final HandshakeSignal OBJECTS_INVALIDATED = new HandshakeSignal(
			"OBJECTS_INVALIDATED");

	public HandshakeSignal(String key) {
		super(key);
	}
}
