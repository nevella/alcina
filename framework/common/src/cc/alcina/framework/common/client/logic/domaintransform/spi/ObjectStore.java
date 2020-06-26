package cc.alcina.framework.common.client.logic.domaintransform.spi;

import java.util.Collection;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface ObjectStore extends ObjectLookup {
	public abstract void changeMapping(Entity obj, long id, long localId);

	public abstract boolean contains(Entity obj);

	public abstract void deregisterObject(Entity entity);

	public abstract void deregisterObjects(Collection<Entity> objects);

	public abstract <T> Collection<T> getCollection(Class<T> clazz);

	public abstract Map<Class<? extends Entity>, Collection<Entity>>
			getCollectionMap();

	public abstract void invalidate(Class<? extends Entity> clazz);

	public abstract void mapObject(Entity obj);

	public abstract void registerObjects(Collection objects);

	public abstract void removeListeners();

	boolean contains(Class<? extends Entity> clazz, long id);
}
