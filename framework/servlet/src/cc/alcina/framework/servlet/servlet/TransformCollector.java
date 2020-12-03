package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;

public class TransformCollector {
	public DomainUpdate waitForTransforms(DomainTransformCommitPosition client,
			long clientInstanceId) {
		long maxDbId = 0;
		long maxTime = System.currentTimeMillis()
				+ 61 * TimeConstants.ONE_SECOND_MS;
		DomainTransformPersistenceQueue queue = DomainStore.stores()
				.writableStore().getPersistenceEvents().getQueue();
		while (System.currentTimeMillis() < maxTime) {
			DomainTransformCommitPosition server = queue
					.getTransformCommitPosition();
			if (server.compareTo(client) > 0) {
				break;
			}
			synchronized (queue) {
				try {
					queue.wait(30 * TimeConstants.ONE_SECOND_MS);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
		// This was working...just need to adjust for the more relaxed commit
		// ordering in DomainTransformPersistenceQueue
		throw new UnsupportedOperationException();
		// List<DomainTransformRequestPersistent> requests = Registry
		// .impl(CommonPersistenceProvider.class).getCommonPersistence()
		// .getPersistentTransformRequests(position + 1,
		// maxDbId, null, false, false);
		// List<DomainTransformRequest> nonPersistentRequests =
		// requests.stream()
		// .map(rq -> {
		// DomainTransformRequest res = new DomainTransformRequest();
		// res.setClientInstance(rq.getClientInstance());
		// res.setEvents(rq.getEvents().stream()
		// .map(evt -> ((DomainTransformEventPersistent) evt)
		// .toNonPersistentEvent(true))
		// .collect(Collectors.toList()));
		// return res;
		// }).collect(Collectors.toList());
		// DomainUpdate result = new DomainUpdate();
		// result.maxDbPersistedRequestId = CommonUtils.last(requests).getId();
		// result.requests = nonPersistentRequests;
		// return result;
	}
}
