package cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;

@Registration(DTRProtocolHandler.class)
public class PlaintextProtocolHandler0pt1 extends PlaintextProtocolHandler1pt0 {
	public static final String VERSION = "0.1 - plain text - old";

	public static final String DATA_TRANSFORM_EVENT_MARKER = "\nDataTransformEvent: ";

	public String getDataTransformEventMarker() {
		return DATA_TRANSFORM_EVENT_MARKER;
	}

	public String handlesVersion() {
		return VERSION;
	}
}
