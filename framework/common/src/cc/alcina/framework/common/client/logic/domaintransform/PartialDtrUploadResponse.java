package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;

import cc.alcina.framework.common.client.util.CommonUtils;

public class PartialDtrUploadResponse implements Serializable {
	public boolean committed;

	public int transformsUploadedButNotCommitted;

	public int lastUploadedRequestId;

	public int lastUploadedRequestTransformUploadCount;

	@Override
	public String toString() {
		return CommonUtils.formatJ(
				"rq-wrapper-id:%s  -  lastUploadedRequestTransformUploadCount:%s  -  transformsUploadedButNotCommitted: %s - committed: %s",
				lastUploadedRequestId, lastUploadedRequestTransformUploadCount,
				transformsUploadedButNotCommitted, committed);
	}
}
