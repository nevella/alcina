package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PartialDtrUploadRequest implements Serializable {
	public boolean pleaseProvideCurrentStatus;

	public List<DeltaApplicationRecord> wrappers = new ArrayList<DeltaApplicationRecord>();

	public boolean hasTransforms() {
		return wrappers.size() > 0;
	}
}
