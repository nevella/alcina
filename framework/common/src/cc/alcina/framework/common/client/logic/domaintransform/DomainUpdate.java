package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class DomainUpdate implements Serializable {
	public List<DomainTransformRequest> requests;

	public DomainTransformCommitPosition commitPosition;

	public static class DomainTransformCommitPosition implements Serializable {
		public Long commitTimestampMs;

		public Long commitRequestId;

		public DomainTransformCommitPosition() {
		}

		public DomainTransformCommitPosition(Long commitTimestampMs,
				Long commitRequestId) {
			this.commitTimestampMs = commitTimestampMs;
			this.commitRequestId = commitRequestId;
		}

		public boolean after(DomainTransformCommitPosition other) {
			if (other.commitTimestampMs == null) {
				return commitTimestampMs != null;
			}
			if (commitTimestampMs.longValue() < other.commitTimestampMs
					.longValue()) {
				return false;
			} else if (commitTimestampMs.longValue() == other.commitTimestampMs
					.longValue()) {
				return commitRequestId.longValue() > other.commitTimestampMs
						.longValue();
			} else {
				return true;
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DomainTransformCommitPosition) {
				DomainTransformCommitPosition o = (DomainTransformCommitPosition) obj;
				return CommonUtils.equals(commitTimestampMs,
						o.commitTimestampMs, commitRequestId,
						o.commitRequestId);
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return Ax.format("commit position: %s/%s", commitTimestampMs,
					commitRequestId);
		}
	}

	@RegistryLocation(registryPoint = DomainTransformCommitPositionProvider.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class DomainTransformCommitPositionProvider {
		public DomainTransformCommitPosition getPosition() {
			return null;
		}
	}
}
