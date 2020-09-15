package cc.alcina.framework.servlet.cluster.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.ExternalTransformPersistenceListener;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.servlet.job.JobRegistry;

/**
 * Each server publishes the current max rq id to a node with its hostname
 * 
 * @author nick@alcina.cc
 * 
 */
public class ClusterTransformListener
		implements ExternalTransformPersistenceListener {
	private TransformCommitLog transformCommitLog;

	private DomainStore domainStore;

	private TransformCommitLogHost commitLogHost;

	Logger logger = LoggerFactory.getLogger(getClass());

	public ClusterTransformListener(TransformCommitLogHost commitLogHost,
			TransformCommitLog transformCommitLog, DomainStore domainStore) {
		this.commitLogHost = commitLogHost;
		this.transformCommitLog = transformCommitLog;
		this.domainStore = domainStore;
	}

	@Override
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent evt) {
		TransformPersistenceToken persistenceToken = evt
				.getTransformPersistenceToken();
		switch (evt.getPersistenceEventType()) {
		case COMMIT_OK:
			evt.getPersistedRequests()
					.forEach(transformCommitLog::sendTransformPublishedMessage);
			break;
		}
	}

	@Override
	public void startService() {
		try {
			transformCommitLog.consumer(commitLogHost,
					this::handleClusterTransformRequest,
					JobRegistry.getLauncherName(), System.currentTimeMillis());
			domainStore.getPersistenceEvents()
					.addDomainTransformPersistenceListener(this);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public void stopService() {
		domainStore.getPersistenceEvents()
				.removeDomainTransformPersistenceListener(this);
	}

	void handleClusterTransformRequest(ClusterTransformRequest request) {
		// basically a fall-back - either a huge transform, or a producer that
		// isn't serializing requests
		if (request.request == null && domainStore.isInitialised()) {
			// FIXME - mvcc.5 - allow multi-kafka-frame requests
			logger.warn("Loading request from db: {}", request.id);
			request.request = domainStore.loadTransformRequest(request.id);
		}
		if (request.request != null) {
			domainStore.getPersistenceEvents().getQueue()
					.cachePersistedRequest(request.request);
		}
		domainStore.getPersistenceEvents().getQueue()
				.transformRequestPublished(request.id);
	}
}
