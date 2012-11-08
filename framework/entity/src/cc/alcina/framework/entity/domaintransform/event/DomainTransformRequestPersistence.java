package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;

public class DomainTransformRequestPersistence {
	public interface DomainTransformRequestPersistenceListener {
		public void onDomainTransformRequestPersistence(
				DomainTransformRequestPersistenceEvent evt);
	}

	public static enum DomainTransformRequestPersistenceEventType {
		PRE_COMMIT, COMMIT_OK, COMMIT_ERROR
	}

	public static class DomainTransformRequestPersistenceEvent {
		private final TransformPersistenceToken transformPersistenceToken;

		private final DomainTransformLayerWrapper domainTransformLayerWrapper;

		private final DomainTransformRequestPersistenceEventType persistenceEventType;

		public DomainTransformRequestPersistenceEvent(
				TransformPersistenceToken transformPersistenceToken,
				DomainTransformLayerWrapper domainTransformLayerWrapper) {
			this.transformPersistenceToken = transformPersistenceToken;
			this.domainTransformLayerWrapper = domainTransformLayerWrapper;
			persistenceEventType = domainTransformLayerWrapper == null ? DomainTransformRequestPersistenceEventType.PRE_COMMIT
					: domainTransformLayerWrapper.response.getResult() == DomainTransformResponseResult.OK ? DomainTransformRequestPersistenceEventType.COMMIT_OK
							: DomainTransformRequestPersistenceEventType.COMMIT_ERROR;
		}

		public TransformPersistenceToken getTransformPersistenceToken() {
			return this.transformPersistenceToken;
		}

		public DomainTransformLayerWrapper getDomainTransformLayerWrapper() {
			return this.domainTransformLayerWrapper;
		}

		public DomainTransformRequestPersistenceEventType getPersistenceEventType() {
			return this.persistenceEventType;
		}
	}

	public interface DomainTransformRequestPersistenceSource {
		public void addDomainTransformRequestPersistenceListener(
				DomainTransformRequestPersistenceListener listener);

		public void removeDomainTransformRequestPersistenceListener(
				DomainTransformRequestPersistenceListener listener);
	}

	public static class DomainTransformRequestPersistenceSupport implements
			DomainTransformRequestPersistenceSource {
		private List<DomainTransformRequestPersistenceListener> listenerList = new ArrayList<DomainTransformRequestPersistenceListener>();;

		public void addDomainTransformRequestPersistenceListener(
				DomainTransformRequestPersistenceListener listener) {
			listenerList.add(listener);
		}

		public void removeDomainTransformRequestPersistenceListener(
				DomainTransformRequestPersistenceListener listener) {
			listenerList.remove(listener);
		}

		public void fireDomainTransformRequestPersistenceEvent(
				DomainTransformRequestPersistenceEvent event) {
			for (DomainTransformRequestPersistenceListener listener : new ArrayList<DomainTransformRequestPersistenceListener>(
					listenerList)) {
				listener.onDomainTransformRequestPersistence(event);
			}
		}
	}
}
