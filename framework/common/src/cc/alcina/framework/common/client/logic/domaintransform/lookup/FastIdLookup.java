package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface FastIdLookup {
	public abstract HasIdAndLocalId get(long id, boolean local);
	

	public abstract void put(HasIdAndLocalId hili, boolean local);

	public abstract void remove(long id, boolean local);

	public abstract Collection<HasIdAndLocalId> values();
}