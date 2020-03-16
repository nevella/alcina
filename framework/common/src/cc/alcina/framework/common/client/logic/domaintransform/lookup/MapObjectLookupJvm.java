package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;

import cc.alcina.framework.common.client.logic.domain.Entity;

public class MapObjectLookupJvm extends MapObjectLookup {
	@Override
	public void mapObject(Entity obj) {
		if ((obj.getId() == 0 && obj.getLocalId() == 0)) {
			return;
		}
		Class<? extends Entity> clazz = obj.getClass();
		FastIdLookup lookup = ensureLookup(clazz);
		lookup.put(obj, obj.getId() == 0);
	}

	@Override
	public void registerObjects(Collection objects) {
		for (Object o : objects) {
			mapObject((Entity) o);
		}
	}
}
