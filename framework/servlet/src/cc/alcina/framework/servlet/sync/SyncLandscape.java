package cc.alcina.framework.servlet.sync;

import java.util.List;

public interface SyncLandscape<T> {
	public SyncConversionSpec getConversionSpec(String propertyName);

	public SyncObjectData toObjectData(T t);

	public void applyData(T t, SyncObjectData objectData);

	public T findApplicableObject(SyncObjectData data);
}
