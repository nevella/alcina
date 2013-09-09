package cc.alcina.framework.servlet.sync;

import java.util.Collection;

public interface SyncInterchangeModel {
	public <T> Collection<T> getCollectionFor(Class<T> clazz);
}
