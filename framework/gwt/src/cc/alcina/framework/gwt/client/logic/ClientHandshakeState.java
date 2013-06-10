package cc.alcina.framework.gwt.client.logic;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

public class ClientHandshakeState extends ExtensibleEnum {
	public ClientHandshakeState(String key) {
		super(key);
	}



	public static final ClientHandshakeState SERVICES_INITIALISED = new ClientHandshakeState(
			"SERVICES_INITIALISED");
	
	public static final ClientHandshakeState SYNCHRONOUS_SERVICES_INITIALISED = new ClientHandshakeState(
			"SYNCHRONOUS_SERVICES_INITIALISED");

	public static final ClientHandshakeState LOADER_UI_INITIALISED = new ClientHandshakeState(
			"LOADER_UI_INITIALISED");

	public static final ClientHandshakeState OBJECTS_LOADED = new ClientHandshakeState(
			"OBJECTS_LOADED");
	
	public static final ClientHandshakeState OBJECTS_REGISTERED = new ClientHandshakeState(
			"OBJECTS_REGISTERED");

	public static final ClientHandshakeState MAIN_LAYOUT_INITIALISED = new ClientHandshakeState(
			"MAIN_LAYOUT_INITIALISED");

	public static final ClientHandshakeState SETUP_AFTER_OBJECTS_LOADED = new ClientHandshakeState(
			"SETUP_AFTER_OBJECTS_LOADED");

	public static final ClientHandshakeState ASYNC_SERVICES_INITIALISED = new ClientHandshakeState(
			"ASYNC_SERVICES_INITIALISED");

}
