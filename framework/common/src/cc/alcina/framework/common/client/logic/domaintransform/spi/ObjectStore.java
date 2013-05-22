package cc.alcina.framework.common.client.logic.domaintransform.spi;

import java.util.Collection;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface ObjectStore extends ObjectLookup{

	public abstract <T> Collection<T> getCollection(Class<T> clazz);

	public abstract void removeListeners();

	public abstract void mapObject(HasIdAndLocalId obj);

	public abstract void registerObjects(Collection objects);

	public abstract void deregisterObjects(Collection<HasIdAndLocalId> objects);

	public abstract void deregisterObject(HasIdAndLocalId hili);

	public abstract void changeMapping(HasIdAndLocalId obj, long id, long localId);

	public abstract Map<Class<? extends HasIdAndLocalId>, Collection<HasIdAndLocalId>> getCollectionMap();
}
