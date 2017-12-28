package cc.alcina.framework.servlet.servlet;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceQueue;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.cache.MemCacheRunner;

public class TransformCollector {
	public DomainUpdate waitForTransforms(long lastTransformRequestId,
			long clientInstanceId) {
		long maxDbId = 0;
		DomainTransformPersistenceQueue queue = Registry
				.impl(DomainTransformPersistenceQueue.class);
		long maxTime = System.currentTimeMillis()
				+ 61 * TimeConstants.ONE_SECOND_MS;
		while (System.currentTimeMillis() < maxTime) {
			//doesn't take account of out-of-order transform persistence. use published id instead
			throw new UnsupportedOperationException();
//			maxDbId = MemCacheRunner.get(() -> {
//				return queue.getMaxPublishedRequestId();
//			});
//			if (maxDbId > lastTransformRequestId) {
//				break;
//			}
//			synchronized (queue) {
//				try {
//					queue.wait(30 * TimeConstants.ONE_SECOND_MS);
//				} catch (Exception e) {
//					throw new WrappedRuntimeException(e);
//				}
//			}
		}
		List<DomainTransformRequestPersistent> requests = Registry
				.impl(CommonPersistenceProvider.class).getCommonPersistence()
				.getPersistentTransformRequests(lastTransformRequestId + 1,
						maxDbId, null, false, false);
		List<DomainTransformRequest> nonPersistentRequests = requests.stream()
				.map(rq -> {
					DomainTransformRequest res = new DomainTransformRequest();
					res.setClientInstance(rq.getClientInstance());
					res.setEvents(rq.getEvents().stream()
							.map(evt -> ((DomainTransformEventPersistent) evt)
									.toNonPersistentEvent(true))
							.collect(Collectors.toList()));
					return res;
				}).collect(Collectors.toList());
		DomainUpdate result = new DomainUpdate();
		result.maxDbPersistedRequestId = CommonUtils.last(requests).getId();
		result.requests = nonPersistentRequests;
		return result;
	}
}
