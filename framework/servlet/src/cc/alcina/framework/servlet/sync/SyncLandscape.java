package cc.alcina.framework.servlet.sync;


public interface SyncLandscape<T> {
	public SyncConversionSpec getConversionSpec(String propertyName);

	public SyncObjectData toObjectData(T t);

	public void applyData(T t, SyncObjectData objectData);

	public T findApplicableObject(SyncObjectData data);

	public String getPreferredKey(SyncObjectData sourceData,
			SyncObjectData targetData);

	public boolean isOverrideLeftToRight(String key);
}
