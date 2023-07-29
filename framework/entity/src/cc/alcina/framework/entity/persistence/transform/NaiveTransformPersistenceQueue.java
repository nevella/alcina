package cc.alcina.framework.entity.persistence.transform;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;

/**
 * see http://code.google.com/p/alcina/issues/detail?id=14 for proposed
 * improvements
 *
 * 
 */
@Registration.Singleton(TransformPersistenceQueue.class)
public class NaiveTransformPersistenceQueue
		implements TransformPersistenceQueue {
	@Override
	public DomainTransformLayerWrapper
			submit(TransformPersistenceToken persistenceToken) {
		if (AppPersistenceBase.isTest()
				&& !TransformCommit.isCommitTestTransforms()) {
			DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper(
					null);
			wrapper.response = new DomainTransformResponse();
			wrapper.response.setResult(DomainTransformResponseResult.OK);
			return wrapper;
		}
		return new TransformPersister()
				.transformExPersistenceContext(persistenceToken);
	}
}
