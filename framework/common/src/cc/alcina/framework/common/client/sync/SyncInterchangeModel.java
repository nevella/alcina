package cc.alcina.framework.common.client.sync;

import java.util.Collection;

public interface SyncInterchangeModel {
	public <T> Collection<T> getCollectionFor(Class<T> clazz);
}
