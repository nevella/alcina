package cc.alcina.framework.servlet.cluster.transform;

import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;

public class ClusterTransformRequest {
	public long id;

	public DomainTransformRequestPersistent request;

	public State state;

	public List<DomainTransformCommitPosition> positions;

	public enum State {
		PRE_COMMIT, COMMIT, ABORTED, SEQUENCED_COMMIT_REGISTERED;
	}
}
