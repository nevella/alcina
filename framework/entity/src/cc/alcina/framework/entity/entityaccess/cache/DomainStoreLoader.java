package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;

public interface DomainStoreLoader {
	void appShutdown();

	LazyObjectLoader getLazyObjectLoader();

	void warmup() throws Exception;
}
