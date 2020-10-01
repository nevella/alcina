package cc.alcina.framework.servlet.cluster.transform;

import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.ExternalTransformPersistenceListener;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.servlet.job.JobRegistry1;

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
			DomainTransformPersistenceEvent event) {
		TransformPersistenceToken persistenceToken = event
				.getTransformPersistenceToken();
		switch (event.getPersistenceEventType()) {
		case COMMIT_OK:
			event.getPersistedRequests().forEach(request -> {
				Future<RecordMetadata> f_recordMetadata = transformCommitLog
						.sendTransformPublishedMessage(request);
				try {
					RecordMetadata recordMetadata = f_recordMetadata.get();
					logger.info(
							"Published transform message: {} :: {} transforms",
							request.getId(), request.getEvents().size());
				} catch (Exception e) {
					logger.warn("Persist record issue: request {}",
							request.getId());
					e.printStackTrace();
				}
			});
			break;
		}
	}

	@Override
	public void startService() {
		try {
			transformCommitLog.consumer(commitLogHost,
					this::handleClusterTransformRequest,
					JobRegistry1.getLauncherName(), System.currentTimeMillis());
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
		logger.info("Received transform message: {}", request.id);
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
