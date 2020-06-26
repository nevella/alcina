package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;

public class PartialDtrUploadResponse implements Serializable {
	public List<Integer> uploadedRequestIds = new ArrayList<>();

	public int transformCount;

	@Override
	public String toString() {
		return Ax.format("Request ids: %s - Transforms: %s ",
				uploadedRequestIds, transformCount);
	}
}
