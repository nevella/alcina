package cc.alcina.framework.gwt.client.impl;

import cc.alcina.framework.common.client.service.Common;
import cc.alcina.framework.gwt.client.logic.domaintransform.lookup.gwt.LongWrapperHash;

public class CommonScriptableExtensionsImpl
		implements Common._ScriptableExtensions {
	@Override
	public int entityHash(long lValue, int classHash) {
		return LongWrapperHash.fastHash(lValue) ^ classHash;
	}
}
