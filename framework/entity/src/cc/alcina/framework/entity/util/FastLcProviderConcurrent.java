package cc.alcina.framework.entity.util;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.FastLcProvider;
import cc.alcina.framework.entity.util.CachingConcurrentMap.CachingConcurrentLcMap;

@Reflected
@Registration(value = FastLcProvider.class, priority = Registration.Priority.PREFERRED_LIBRARY)
public class FastLcProviderConcurrent extends FastLcProvider {
	private CachingConcurrentLcMap map = new CachingConcurrentLcMap();

	public String lc(String string) {
		return string == null ? null : map.get(string);
	}
}
