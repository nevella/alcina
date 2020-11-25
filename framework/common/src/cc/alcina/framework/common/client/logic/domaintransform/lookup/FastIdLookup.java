package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface FastIdLookup {
	public abstract Entity get(long id, boolean local);

	public abstract void put(Entity entity, boolean local);

	public abstract void putAll(Collection<Entity> values, boolean local);

	public abstract void remove(long id, boolean local);

	public abstract Collection<Entity> values();

	public abstract void changeMapping(Entity obj, long id, long localId);
}