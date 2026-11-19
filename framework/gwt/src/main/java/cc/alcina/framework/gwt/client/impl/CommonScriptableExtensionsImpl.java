package cc.alcina.framework.gwt.client.impl;

import cc.alcina.framework.common.client.service.Common;
import cc.alcina.framework.gwtpeer.client.logic.domaintransform.lookup.LongWrapperHash;
import elemental.json.JsonValue;

@SuppressWarnings("deprecation")
public class CommonScriptableExtensionsImpl
		implements Common._ScriptableExtensions {
	@Override
	public int entityHash(long lValue, int classHash) {
		return LongWrapperHash.fastHash(lValue) ^ classHash;
	}

	public native boolean isNull(JsonValue value) /*-{
    return value === null;
	}-*/;
}
