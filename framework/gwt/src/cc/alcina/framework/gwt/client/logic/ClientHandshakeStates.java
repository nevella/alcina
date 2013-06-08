package cc.alcina.framework.gwt.client.logic;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

public class ClientHandshakeStates extends ExtensibleEnum {
	public ClientHandshakeStates(String key) {
		super(key);
	}



	public static final ClientHandshakeStates SERVICES_INITIALISED = new ClientHandshakeStates(
			"SERVICES_INITIALISED");

	public static final ClientHandshakeStates SAID_HELLO = new ClientHandshakeStates(
			"SAID_HELLO");

	public static final ClientHandshakeStates OBJECTS_LOADED = new ClientHandshakeStates(
			"OBJECTS_LOADED");

	public static final ClientHandshakeStates LAYOUT_INITIALISED = new ClientHandshakeStates(
			"LAYOUT_INITIALISED");
}
