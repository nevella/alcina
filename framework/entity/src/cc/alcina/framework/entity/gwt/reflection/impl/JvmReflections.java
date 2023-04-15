package cc.alcina.framework.entity.gwt.reflection.impl;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
import cc.alcina.framework.common.client.reflection.impl.ForName;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.ConcurrentMapCreatorJvm;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.DelegateMapCreatorConcurrentNoNulls;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.HashMapCreatorJvm;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.LinkedHashMapCreatorJvm;

public class JvmReflections {
	public static void init() {
		ForName.setImpl(new ForNameImpl());
		ClassReflectorProvider.setImpl(new ClassReflectorProviderImpl());
	}

	public static void setupBootstrapJvmServices() {
		Registry.Internals
				.setDelegateCreator(new DelegateMapCreatorConcurrentNoNulls());
		CollectionCreators.Bootstrap
				.setConcurrentClassMapCreator(new ConcurrentMapCreatorJvm());
		CollectionCreators.Bootstrap
				.setConcurrentStringMapCreator(new ConcurrentMapCreatorJvm());
		CollectionCreators.Bootstrap.setHashMapCreator(new HashMapCreatorJvm());
		CollectionCreators.Bootstrap
				.setLinkedMapCreator(new LinkedHashMapCreatorJvm());
	}
}
