package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Map;

import cc.alcina.framework.common.client.util.CollectionCreators.DelegateMapCreator;

public class JsRegistryDelegateCreator implements DelegateMapCreator {
	@Override
	public Map createDelegateMap(int depthFromRoot, int depth) {
		return JsUniqueMap.create(Class.class, false);
	}
}
