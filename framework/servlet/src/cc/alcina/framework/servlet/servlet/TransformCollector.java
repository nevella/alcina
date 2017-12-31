package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceQueue;

public class TransformCollector {
	public DomainUpdate waitForTransforms(DomainTransformCommitPosition client,
			long clientInstanceId) {
		long maxDbId = 0;
		long maxTime = System.currentTimeMillis()
				+ 61 * TimeConstants.ONE_SECOND_MS;
		DomainTransformPersistenceQueue queue = DomainTransformPersistenceQueue
				.get();
		while (System.currentTimeMillis() < maxTime) {
			DomainTransformCommitPosition server = queue
					.getTransformLogPosition();
			if (server.after(client)) {
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
		// FIXME - dem3 - for the mo, throw a reload exception if unmatched -
		// otherwise get
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
