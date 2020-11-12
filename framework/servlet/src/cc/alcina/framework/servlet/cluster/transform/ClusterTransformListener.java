package cc.alcina.framework.servlet.cluster.transform;

import java.util.List;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.transform.TransformPersisterInPersistenceContext;
import cc.alcina.framework.entity.registry.ClassLoaderAwareRegistryProvider;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;
import cc.alcina.framework.entity.transform.event.ExternalTransformPersistenceListener;
import cc.alcina.framework.servlet.cluster.transform.ClusterTransformRequest.State;
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

	private TopicListener<DomainTransformLayerWrapper> preFinalCommitlistener = new TopicListener<DomainTransformLayerWrapper>() {
		@Override
		public void topicPublished(String key,
				DomainTransformLayerWrapper message) {
			ClassLoader contextClassLoader = Thread.currentThread()
					.getContextClassLoader();
			try {
				Thread.currentThread()
						.setContextClassLoader(ClassLoaderAwareRegistryProvider
								.get().getServletLayerClassloader());
				publishRequests(message.persistentRequests, State.PRE_COMMIT);
			} finally {
				Thread.currentThread()
						.setContextClassLoader(contextClassLoader);
			}
		}
	};

	public ClusterTransformListener(TransformCommitLogHost commitLogHost,
			TransformCommitLog transformCommitLog, DomainStore domainStore) {
		this.commitLogHost = commitLogHost;
		this.transformCommitLog = transformCommitLog;
		this.domainStore = domainStore;
	}

	@Override
	/*
	 * So that cache propagation is not blocked by the local event queue
	 */
	public boolean isPreBarrierListener() {
		return true;
	}

	@Override
	public void onDomainTransformRequestPersistence(
			DomainTransformPersistenceEvent event) {
		TransformPersistenceToken persistenceToken = event
				.getTransformPersistenceToken();
		List<DomainTransformRequestPersistent> requests = event
				.getPersistedRequests();
		switch (event.getPersistenceEventType()) {
		case COMMIT_OK:
			publishRequests(requests, ClusterTransformRequest.State.COMMIT);
			break;
		case COMMIT_ERROR:
			publishRequests(requests, ClusterTransformRequest.State.ABORTED);
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
			if (domainStore == DomainStore.writableStore()) {
				TransformPersisterInPersistenceContext.topicPreFinalCommit
						.add(preFinalCommitlistener);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public void stopService() {
		if (domainStore == DomainStore.writableStore()) {
			TransformPersisterInPersistenceContext.topicPreFinalCommit
					.remove(preFinalCommitlistener);
		}
		domainStore.getPersistenceEvents()
				.removeDomainTransformPersistenceListener(this);
	}

	protected void publishRequests(
			List<DomainTransformRequestPersistent> requests, State state) {
		requests.forEach(request -> {
			List<Future<RecordMetadata>> recordMetadata = transformCommitLog
					.sendTransformPublishedMessages(request, state);
			if (state == State.PRE_COMMIT) {
				try {
					String packetSize = recordMetadata.size() == 1 ? ""
							: Ax.format("(%s packets) ", recordMetadata.size());
					logger.info(
							"Published transform message: {} {}{}:: {} transforms",
							request.getId(), state, packetSize,
							request.getEvents().size());
				} catch (Exception e) {
					logger.warn("Persist record issue: request {}",
							request.getId());
					e.printStackTrace();
				}
			}
		});
	}

	void handleClusterTransformRequest(ClusterTransformRequest request) {
		logger.info("Received transform message: {}", request.id);
		DomainTransformPersistenceQueue queue = domainStore
				.getPersistenceEvents().getQueue();
		switch (request.state) {
		case PRE_COMMIT:
			queue.cachePersistedRequest(request.request);
			break;
		case COMMIT:
			queue.transformRequestPublished(request.id);
			break;
		case ABORTED:
			queue.transformRequestAborted(request.id);
			break;
		}
	}
}
