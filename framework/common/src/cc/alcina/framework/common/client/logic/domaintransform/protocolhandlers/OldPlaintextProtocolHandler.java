package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@RegistryLocation(registryPoint = DTRProtocolHandler.class, j2seOnly = false)
public class OldPlaintextProtocolHandler extends PlaintextProtocolHandler {
	public static final String VERSION = "0.1 - plain text - old";

	public static final String DATA_TRANSFORM_EVENT_MARKER = "\nDataTransformEvent: ";

	public String handlesVersion() {
		return VERSION;
	}
	public  String getDataTransformEventMarker() {
		return DATA_TRANSFORM_EVENT_MARKER;
	}
}

