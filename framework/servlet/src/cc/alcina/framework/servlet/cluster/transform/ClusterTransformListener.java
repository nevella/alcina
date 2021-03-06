package cc.alcina.framework.servlet.cluster.transform;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.registry.ClassLoaderAwareRegistryProvider;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;
import cc.alcina.framework.entity.transform.event.ExternalTransformPersistenceListener;
import cc.alcina.framework.entity.util.OffThreadLogger;
import cc.alcina.framework.servlet.cluster.transform.ClusterTransformRequest.State;

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

	Logger logger = OffThreadLogger.getLogger(getClass());

	private ConcurrentHashMap<Long, CountDownLatch> preFlushLatches = new ConcurrentHashMap<>();

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
		if (event.isFiringFromQueue()) {
			return;
		}
		TransformPersistenceToken persistenceToken = event
				.getTransformPersistenceToken();
		List<DomainTransformRequestPersistent> requests = event
				.getPersistedRequests();
		switch (event.getPersistenceEventType()) {
		case PRE_COMMIT:
			break;
		case COMMIT_OK:
			publishRequests(requests, ClusterTransformRequest.State.COMMIT);
			break;
		case PRE_FLUSH:
			if (requests.isEmpty()) {
				return;
			}
			ClassLoader contextClassLoader = Thread.currentThread()
					.getContextClassLoader();
			try {
				Thread.currentThread()
						.setContextClassLoader(ClassLoaderAwareRegistryProvider
								.get().getServletLayerClassloader());
				CountDownLatch latch = new CountDownLatch(1);
				preFlushLatches.put(event.getMaxPersistedRequestId(), latch);
				publishRequests(requests, State.PRE_COMMIT);
				try {
					long start = System.currentTimeMillis();
					latch.await();
					logger.info("Pre-commit await: request {} : {} ms",
							event.getMaxPersistedRequestId(),
							System.currentTimeMillis() - start);
				} catch (InterruptedException e) {
					throw new WrappedRuntimeException(e);
				}
			} finally {
				Thread.currentThread()
						.setContextClassLoader(contextClassLoader);
			}
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
					EntityLayerUtils.getLocalHostName(),
					System.currentTimeMillis());
			domainStore.getPersistenceEvents()
					.addDomainTransformPersistenceListener(this);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public void stopService() {
		if (domainStore == DomainStore.writableStore()) {
			domainStore.getPersistenceEvents()
					.removeDomainTransformPersistenceListener(this);
		}
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
			} else {
				logger.info("Published transform message: {} {}",
						request.getId(), state);
			}
		});
	}

	void handleClusterTransformRequest(ClusterTransformRequest request) {
		logger.info("Received transform message: {} {}", request.id,
				request.state);
		DomainTransformPersistenceQueue queue = domainStore
				.getPersistenceEvents().getQueue();
		switch (request.state) {
		case PRE_COMMIT:
			queue.onRequestDataReceived(request.request);
			logger.info("Post request data received: {} {}", request.id,
					request.state);
			CountDownLatch latch = preFlushLatches
					.remove(request.request.getId());
			if (latch != null) {
				latch.countDown();
				logger.info("Released latch: {} {}", request.id, request.state);
			}
			break;
		case COMMIT:
			try {
				queue.onTransformRequestCommitted(request.id, false);
			} catch (Throwable t) {
				logger.warn(
						"DEVEX::0 - Exception in handleClusterTransformRequest::commit - {}",
						t);
				t.printStackTrace();
			}
			break;
		case ABORTED:
			queue.onTransformRequestAborted(request.id);
			break;
		}
	}
}
