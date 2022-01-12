package cc.alcina.framework.common.client.logic.domaintransform.spi;

import java.util.Collection;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;

public interface ObjectStore {
	public <T extends Entity> T getObject(Class<? extends T> c, long id,
			long localId);

	default <T extends Entity> T getObject(EntityLocator locator) {
		return (T) getObject(locator.getClazz(), locator.getId(),
				locator.getLocalId());
	}

	public <T extends Entity> T getObject(T bean);

	 void changeMapping(Entity obj, long id, long localId);

	 boolean contains(Entity obj);

	default boolean contains(EntityLocator locator){
		return getObject(locator)!=null;
	}

	 void deregister(Entity entity);

	 <T> Collection<T> getCollection(Class<T> clazz);

	 Map<Class<? extends Entity>, Collection<Entity>>
			getCollectionMap();

	 void invalidate(Class<? extends Entity> clazz);

	 void mapObject(Entity obj);

	 void registerObjects(Collection objects);

	 void removeListeners();

	boolean contains(Class<? extends Entity> clazz, long id);
}
