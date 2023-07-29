package cc.alcina.framework.servlet.cluster.transform;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.registry.ClassLoaderAwareRegistryProvider;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;
import cc.alcina.framework.entity.util.OffThreadLogger;
import cc.alcina.framework.servlet.cluster.transform.ClusterTransformRequest.State;

/**
 * Each server publishes the current max rq id to a node with its hostname
 *
 * 
 *
 *
 * @see https://github.com/nevella/alcina/issues/13,
 *      https://github.com/nevella/alcina/issues/20
 *
 */
public class ClusterTransformListener
		implements ExternalTransformPersistenceListener {
	private TransformCommitLog transformCommitLog;

	private DomainStore domainStore;

	private TransformCommitLogHost commitLogHost;

	Logger logger = OffThreadLogger.getLogger(getClass());

	private ConcurrentHashMap<Long, CountDownLatch> preCommitLatches = new ConcurrentHashMap<>();

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
		case PREPARE_COMMIT:
			break;
		case COMMIT_OK:
			publishRequests(requests, ClusterTransformRequest.State.COMMIT);
			break;
		case PRE_COMMIT:
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
				preCommitLatches.put(event.getMaxPersistedRequestId(), latch);
				publishRequests(requests, State.PRE_COMMIT);
				try {
					long start = System.currentTimeMillis();
					boolean countedDown = latch.await(
							5 * TimeConstants.ONE_MINUTE_MS,
							TimeUnit.MILLISECONDS);
					if (countedDown) {
						logger.info("Pre-commit await: request {} : {} ms",
								event.getMaxPersistedRequestId(),
								System.currentTimeMillis() - start);
					} else {
						logger.info("Pre-commit timeout: request {} : {} ms",
								event.getMaxPersistedRequestId(),
								System.currentTimeMillis() - start);
					}
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
			queue.onRequestDataReceived(request.request, true);
			logger.info("Post request data received: {} {}", request.id,
					request.state);
			CountDownLatch latch = preCommitLatches
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
