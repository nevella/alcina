package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class DomainUpdate implements Serializable {
	public List<DomainTransformRequest> requests;

	public DomainTransformCommitPosition commitPosition;

	@ClientInstantiable
	@Introspectable
	public static class DomainTransformCommitPosition
			implements Serializable, Comparable<DomainTransformCommitPosition> {
		private Timestamp commitTimestamp;

		private Long commitRequestId;

		public DomainTransformCommitPosition() {
		}

		public DomainTransformCommitPosition(long commitRequestId,
				Timestamp commitTimestamp) {
			this.commitTimestamp = commitTimestamp;
			this.commitRequestId = commitRequestId;
		}

		@Override
		public int compareTo(DomainTransformCommitPosition o) {
			if (commitTimestamp == null) {
				return o.commitTimestamp == null ? 0 : -1;
			}
			int i = commitTimestamp.compareTo(o.commitTimestamp);
			if (i != 0) {
				return i;
			}
			return CommonUtils.compareLongs(commitRequestId, o.commitRequestId);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DomainTransformCommitPosition) {
				DomainTransformCommitPosition o = (DomainTransformCommitPosition) obj;
				return CommonUtils.equals(commitTimestamp, o.commitTimestamp,
						commitRequestId, o.commitRequestId);
			} else {
				return false;
			}
		}

		public Long getCommitRequestId() {
			return commitRequestId;
		}

		public Timestamp getCommitTimestamp() {
			return commitTimestamp;
		}

		public void setCommitRequestId(Long commitRequestId) {
			this.commitRequestId = commitRequestId;
		}

		public void setCommitTimestamp(Timestamp commitTimestamp) {
			this.commitTimestamp = commitTimestamp;
		}

		@Override
		public String toString() {
			return Ax.format("commit position: %s/%s", commitTimestamp,
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
