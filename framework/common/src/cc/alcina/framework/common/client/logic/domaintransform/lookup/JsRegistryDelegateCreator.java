package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Map;

import cc.alcina.framework.common.client.util.MultikeyMapBase.DelegateMapCreator;

public class JsRegistryDelegateCreator extends DelegateMapCreator {
	@Override
	public Map createDelegateMap(int depthFromRoot) {
		return new JsUniqueMap<>(Class.class);
	}
}
