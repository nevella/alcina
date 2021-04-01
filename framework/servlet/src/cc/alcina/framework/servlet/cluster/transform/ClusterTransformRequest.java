package cc.alcina.framework.servlet.cluster.transform;

import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;

public class ClusterTransformRequest {
	public long id;

	public DomainTransformRequestPersistent request;

	public State state;

	public enum State {
		PRE_COMMIT, COMMIT, ABORTED;
	}
}
