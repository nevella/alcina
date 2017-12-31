package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface LazyObjectLoader {
	public <T extends HasIdAndLocalId> void loadObject(Class<? extends T> c,
			long id, long localId);
}
