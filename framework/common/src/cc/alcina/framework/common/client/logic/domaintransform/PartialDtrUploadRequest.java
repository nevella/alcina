package cc.alcina.framework.common.client.logic.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PartialDtrUploadRequest implements Serializable {
	public boolean pleaseProvideCurrentStatus;
	public List<DeltaApplicationRecord> wrappers=new ArrayList<DeltaApplicationRecord>();
	public List<List<DomainTransformEvent>> transformLists=new ArrayList<List<DomainTransformEvent>>();
	public boolean commitOnReceipt;
	public boolean hasTransforms() {
		return !transformLists.isEmpty()&&!transformLists.get(0).isEmpty();
	}
	
}
