package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;

import cc.alcina.framework.common.client.logic.domain.Entity;

public class MapObjectLookupJvm extends MapObjectLookup {
	@Override
	public void mapObject(Entity entity) {
		if ((entity.getId() == 0 && entity.getLocalId() == 0)) {
			return;
		}
		Class<? extends Entity> clazz = entity.entityClass();
		FastIdLookup lookup = ensureLookup(clazz);
		lookup.put(entity, entity.getId() == 0);
	}

	@Override
	public void registerObjects(Collection objects) {
		for (Object o : objects) {
			mapObject((Entity) o);
		}
	}
}
