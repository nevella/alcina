package cc.alcina.framework.entity.gwt.reflection.impl;

import com.google.gwt.core.shared.GWT;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
import cc.alcina.framework.common.client.reflection.impl.ForName;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.entity.gwt.headless.GWTBridgeHeadless;
import cc.alcina.framework.entity.persistence.mvcc.CollectionCreatorsMvcc.DegenerateCreatorMvcc;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.ConcurrentMapCreatorJvm;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.DelegateMapCreatorConcurrentNoNulls;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.HashMapCreatorJvm;
import cc.alcina.framework.entity.util.CollectionCreatorsJvm.LinkedHashMapCreatorJvm;
import elemental.json.impl.JsonUtil;

@SuppressWarnings("deprecation")
public class JvmReflections {
	public static void configureBootstrapJvmServices() {
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

	public static void init() {
		ForName.setImpl(new ForNameImpl());
		ClassReflectorProvider.setImpl(new ClassReflectorProviderImpl());
	}

	public static void initJvmServices() {
		/*
		 * This needs to be done servlet (app) layer, since
		 * AppLifecycleServletBase requires an Environment timer provider
		 * 
		 * Registry.register().singleton(Timer.Provider.class, new
		 * TimerJvm.Provider());
		 */
		LiSet.degenerateCreator = new DegenerateCreatorMvcc();
		GWT.setBridge(new GWTBridgeHeadless());
		JsonUtil.FAST_STRINGIFY = true;
	}
}
