package cc.alcina.framework.entity.entityaccess.transform;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;

/**
 * see http://code.google.com/p/alcina/issues/detail?id=14 for proposed
 * improvements
 * 
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = TransformPersistenceQueue.class, implementationType = ImplementationType.SINGLETON)
public class NaiveTransformPersistenceQueue
		implements TransformPersistenceQueue {
	@Override
	public DomainTransformLayerWrapper
			submit(TransformPersistenceToken persistenceToken) {
		if (AppPersistenceBase.isTest()
				&& !TransformCommit.isCommitTestTransforms()) {
			DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper();
			wrapper.response = new DomainTransformResponse();
			wrapper.response.setResult(DomainTransformResponseResult.OK);
			return wrapper;
		}
		return new TransformPersister()
				.transformExPersistenceContext(persistenceToken);
	}
}
