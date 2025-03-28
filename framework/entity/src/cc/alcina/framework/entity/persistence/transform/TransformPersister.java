package cc.alcina.framework.entity.persistence.transform;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DTRProtocolSerializer;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.AlcinaProcess;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.transform.TransformPersisterInPersistenceContext.DeliberatelyThrownWrapperException;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.TransformPersistenceToken.Pass;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvents;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

/**
 * <p>
 * Persists a {@link DomainTransformRequest} to a Postgres database via EJB
 * 
 * <p>
 * This process is connected to the {@link CommitToStorageTransformListener} and
 * {@link DomainTransformPersistenceEvents} processes
 */
public class TransformPersister implements AlcinaProcess {
	public static final String CONTEXT_TRANSFORM_LAYER_WRAPPER = ThreadlocalTransformManager.class
			.getName() + ".CONTEXT_TRANSFORM_LAYER_WRAPPER";

	public static final String CONTEXT_DO_NOT_PERSIST_DTES = TransformPersister.class
			.getName() + ".CONTEXT_DO_NOT_PERSIST_DTES";

	Logger logger = LoggerFactory.getLogger(getClass());

	public DomainTransformLayerWrapper
			transformExPersistenceContext(TransformPersistenceToken token) {
		try {
			LooseContext.pushWithTrue(
					DTRProtocolSerializer.CONTEXT_EXCEPTION_DEBUG);
			TransformPersisterInPersistenceContext.ThreadData.get()
					.observingFlushData(true);
			TransformPersisterPeer.get().setupCustomTransformContent();
			TransformPersisterToken persisterToken = new TransformPersisterToken();
			DomainTransformLayerWrapper wrapper = new DomainTransformLayerWrapper(
					token);
			boolean perform = true;
			while (perform) {
				perform = false;
				try {
					LooseContext.pushWithTrue(
							TransformManager.CONTEXT_DO_NOT_POPULATE_SOURCE);
					LooseContext.set(CONTEXT_TRANSFORM_LAYER_WRAPPER, wrapper);
					InFlightPersistence.get().register(token, true);
					if (Configuration.is("logAllTransforms")) {
						synchronized (TransformPersister.class) {
							Ax.err("\n\n%s================",
									Thread.currentThread().getName());
							Ax.out(token.getRequest());
						}
					}
					if (AppPersistenceBase.isInstanceReadOnly()) {
						wrapper = new ReadonlyInMemoryPersister()
								.commitInMemoryTransforms(persisterToken, token,
										wrapper);
					} else {
						wrapper = Registry.impl(CommonPersistenceProvider.class)
								.getCommonPersistence()
								.transformInPersistenceContext(persisterToken,
										token, wrapper);
					}
				} catch (RuntimeException ex) {
					String preparedStatementCausingIssue = TransformPersisterInPersistenceContext.ThreadData
							.get()
							.getAndClearLastFlushPreparedStatementCausingIssue();
					if (token.getTransformRetryPolicy().isRetry(token, wrapper,
							ex, preparedStatementCausingIssue)) {
						token.resetTransformsToCommitToStorage();
						wrapper.clearPersistentTransformData();
						DomainStore.writableStore().getPersistenceEvents()
								.fireDomainTransformPersistenceEvent(
										new DomainTransformPersistenceEvent(
												token, wrapper,
												DomainTransformPersistenceEventType.COMMIT_ERROR,
												true));
						token.setPass(Pass.RETRY_WITH_IGNORES);
					} else {
						InFlightPersistence.get().onThrowException(token, ex);
						DeliberatelyThrownWrapperException dtwe = null;
						if (ex instanceof DeliberatelyThrownWrapperException) {
							dtwe = (DeliberatelyThrownWrapperException) ex;
						} else if (ex
								.getCause() instanceof DeliberatelyThrownWrapperException) {
							dtwe = (DeliberatelyThrownWrapperException) ex
									.getCause();
						} else {
							// we used to throw - but we need to publish the
							// exception to inform queues, listeners that the
							// exception failed (an example issue is a JDBC
							// issue)
							//
							// that said, we used to not arrive here because of
							// flush() before the inPersistenceContext call
							// exited
							TransformPersisterInPersistenceContext
									.putExceptionInWrapper(token, ex, wrapper);
						}
					}
				} finally {
					InFlightPersistence.get().register(token, false);
					LooseContext.pop();
				}
				if (token.getPass() == Pass.DETERMINE_EXCEPTION_DETAIL) {
					token.getRequest().updateTransformCommitType(
							CommitType.TO_STORAGE, true);
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
		} finally {
			TransformPersisterInPersistenceContext.ThreadData.get()
					.observingFlushData(false);
			LooseContext.pop();
		}
	}

	@Registration(TransformPersisterPeer.class)
	public static class TransformPersisterPeer {
		public static TransformPersister.TransformPersisterPeer get() {
			return Registry
					.impl(TransformPersister.TransformPersisterPeer.class);
		}

		public void setupCustomTransformContent() {
		}
	}

	public static class TransformPersisterToken implements Serializable {
		static final transient long serialVersionUID = 1;

		long determineExceptionDetailPassStartTime = 0;

		int determinedExceptionCount;
	}
}
