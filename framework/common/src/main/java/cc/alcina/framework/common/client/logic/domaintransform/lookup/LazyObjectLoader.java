package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface LazyObjectLoader {
	default void load(Collection<? extends Entity> entities) {
		throw new UnsupportedOperationException();
	}

	public <T extends Entity> void loadObject(Class<? extends T> c, long id,
			long localId);
}
