package cc.alcina.framework.common.client.logic.domaintransform.spi;

import java.util.Collection;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;

/**
 * FIXME - TM - Probably switch to LocalDomainStore for almost all of this, and
 * have a client tm just be backed by a DetachedEntityCache (so can be removed)
 * 
 * @author nick@alcina.cc
 *
 */
public interface ObjectStore {
	// FIXME - dirndl 1x2 - in application code, tend to replace with
	// Domain.find
	public <T extends Entity> T getObject(Class<? extends T> c, long id,
			long localId);

	public <T extends Entity> T getObject(T bean);

	void changeMapping(Entity obj, long id, long localId);

	default boolean contains(Class<? extends Entity> clazz, long id) {
		return contains(new EntityLocator(clazz, id, 0));
	}

	default boolean contains(Entity obj) {
		return contains(obj.toLocator());
	}

	default boolean contains(EntityLocator locator) {
		return getObject(locator) != null;
	}

	void deregister(Entity entity);

	<T> Collection<T> getCollection(Class<T> clazz);

	Map<Class<? extends Entity>, Collection<Entity>> getCollectionMap();

	default <T extends Entity> T getObject(EntityLocator locator) {
		return (T) getObject(locator.getClazz(), locator.getId(),
				locator.getLocalId());
	}

	void invalidate(Class<? extends Entity> clazz);

	void mapObject(Entity obj);

	void registerObjects(Collection objects);

	void removeListeners();
}
