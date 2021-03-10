package cc.alcina.framework.servlet.cluster.transform;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;

public class ClusterTransformRequest {
	public long id;

	public DomainTransformRequestPersistent request;

	public State state;

	public List<DomainTransformCommitPosition> positions;

	public Object provideIds() {
		return id != 0 ? id
				: positions.stream().map(pos -> pos.commitRequestId)
						.collect(Collectors.toList());
	}

	public enum State {
		PRE_COMMIT, COMMIT, ABORTED, SEQUENCED_COMMIT_REGISTERED;
	}
}
