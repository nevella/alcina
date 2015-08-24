package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.List;

public class DomainUpdate implements Serializable {
	public List<DomainTransformRequest> requests;
	public long maxDbPersistedRequestId;
}
