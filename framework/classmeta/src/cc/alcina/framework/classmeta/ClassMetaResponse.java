package cc.alcina.framework.classmeta;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.registry.ClassMetadataCache;

public class ClassMetaResponse {
	public ClassMetaRequest request;

	public ClassMetadataCache cache;

	@Override
	public String toString() {
		return Ax.format("Request: %s\nResponse: cache: %s", request, cache);
	}
}
