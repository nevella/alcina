package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface LazyObjectLoader {
	public <T extends Entity> void loadObject(Class<? extends T> c, long id,
			long localId);
}
