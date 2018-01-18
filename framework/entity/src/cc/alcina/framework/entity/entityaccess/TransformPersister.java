package cc.alcina.framework.entity.entityaccess;

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken.Pass;
import cc.alcina.framework.entity.entityaccess.TransformPersisterIn.DeliberatelyThrownWrapperException;

public class TransformPersister {
	private static final String TOPIC_PERSISTING_TRANSFORMS = TransformPersister.class
			.getName() + ".TOPIC_PERSISTING_TRANSFORMS";

	public static final String CONTEXT_TRANSFORM_LAYER_WRAPPER = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_TRANSFORM_LAYER_WRAPPER";

	public static final String CONTEXT_DO_NOT_PERSIST_DTES = TransformPersister.class
			.getName() + ".CONTEXT_DO_NOT_PERSIST_DTES";

	public static void persistingTransforms() {
		GlobalTopicPublisher.get().publishTopic(TOPIC_PERSISTING_TRANSFORMS,
				Thread.currentThread());
	}

	public static void persistingTransformsListenerDelta(
			TopicListener<Thread> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(TOPIC_PERSISTING_TRANSFORMS,
				listener, add);
	}

	public static class TransformPersisterToken implements Serializable {
		static final transient long serialVersionUID = 1;

		long determineExceptionDetailPassStartTime = 0;

		int determinedExceptionCount;
	}

	public DomainTransformLayerWrapper
			transformExPersistenceContext(TransformPersistenceToken token) {
		TransformPersisterToken persisterToken = new TransformPersisterToken();
		DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper();
		boolean perform = true;
		while (perform) {
			perform = false;
			try {
				LooseContext.pushWithTrue(
						TransformManager.CONTEXT_DO_NOT_POPULATE_SOURCE);
				LooseContext.set(CONTEXT_TRANSFORM_LAYER_WRAPPER, wrapper);
				wrapper = Registry.impl(CommonPersistenceProvider.class)
						.getCommonPersistence().transformInPersistenceContext(
								persisterToken, token, wrapper);
			} catch (RuntimeException ex) {
				DeliberatelyThrownWrapperException dtwe = null;
				if (ex instanceof DeliberatelyThrownWrapperException) {
					dtwe = (DeliberatelyThrownWrapperException) ex;
				} else if (ex
						.getCause() instanceof DeliberatelyThrownWrapperException) {
					dtwe = (DeliberatelyThrownWrapperException) ex.getCause();
				} else {
					throw ex;
				}
			} finally {
				LooseContext.pop();
			}
			if (token.getPass() == Pass.DETERMINE_EXCEPTION_DETAIL) {
				token.getRequest()
						.updateTransformCommitType(CommitType.TO_STORAGE, true);
				DomainTransformException firstException = token
						.getTransformExceptions().get(0);
				perform = !firstException.irresolvable();
			} else if (token.getPass() == Pass.RETRY_WITH_IGNORES) {
				token.setPass(Pass.TRY_COMMIT);
				perform = true;
			}
		}
		if (wrapper.response
				.getResult() == DomainTransformResponseResult.FAILURE) {
			Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence().expandExceptionInfo(wrapper);
		}
		return wrapper;
	}
}
