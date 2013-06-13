package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.ExtensibleEnum;

public class HandshakeState extends ExtensibleEnum {
	public HandshakeState(String key) {
		super(key);
	}

	public HandshakeState(String key, String... tags) {
		super(key, tags);
	}

	public static final String TAG_POST_OBJECT_DATA_LOAD = "TAG_POST_OBJECT_DATA_LOAD";

	public static final HandshakeState SERVICES_INITIALISED = new HandshakeState(
			"SERVICES_INITIALISED");

	public static final HandshakeState SYNCHRONOUS_SERVICES_INITIALISED = new HandshakeState(
			"SYNCHRONOUS_SERVICES_INITIALISED");

	public static final HandshakeState LOADER_UI_INITIALISED = new HandshakeState(
			"LOADER_UI_INITIALISED");

	public static final HandshakeState OBJECT_DATA_LOADED = new HandshakeState(
			"OBJECT_DATA_LOADED", TAG_POST_OBJECT_DATA_LOAD);

	public static final HandshakeState OBJECT_DATA_LOAD_FAILED = new HandshakeState(
			"OBJECT_DATA_LOAD_FAILED", TAG_POST_OBJECT_DATA_LOAD);

	public static final HandshakeState OBJECTS_UNWRAPPED_AND_REGISTERED = new HandshakeState(
			"OBJECTS_UNWRAPPED_AND_REGISTERED", TAG_POST_OBJECT_DATA_LOAD);

	public static final HandshakeState MAIN_LAYOUT_INITIALISED = new HandshakeState(
			"MAIN_LAYOUT_INITIALISED", TAG_POST_OBJECT_DATA_LOAD);

	public static final HandshakeState SETUP_AFTER_OBJECTS_LOADED = new HandshakeState(
			"SETUP_AFTER_OBJECTS_LOADED", TAG_POST_OBJECT_DATA_LOAD);

	public static final HandshakeState ASYNC_SERVICES_INITIALISED = new HandshakeState(
			"ASYNC_SERVICES_INITIALISED");
}
