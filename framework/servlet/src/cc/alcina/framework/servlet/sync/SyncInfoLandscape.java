package cc.alcina.framework.servlet.sync;

import java.util.List;

public interface SyncInfoLandscape<T> {
	public List<SyncInfoConversionSpec> getConversionSpecs();
	public List<SyncInfoObjectData> toObjectData(T t);
	public void applyData(T t,  SyncInfoObjectData objectData);
	public T findApplicableObject(SyncInfoObjectData data);
	
}
