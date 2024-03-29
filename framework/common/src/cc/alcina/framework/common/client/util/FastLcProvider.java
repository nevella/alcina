package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CachingMap.CachingLcMap;

@Reflected
@Registration(FastLcProvider.class)
public class FastLcProvider {
	private CachingLcMap map = new CachingLcMap();

	public String lc(String string) {
		return map.get(string);
	}
}
