package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;

public interface DomainStoreLoader {
	void appShutdown();

	boolean checkTransformRequestExists(long id);

	LazyObjectLoader getLazyObjectLoader();

	DomainStoreTransformSequencer getTransformSequencer();

	DomainTransformRequestPersistent loadTransformRequest(long id);

	void onTransformsPersisted();

	void warmup() throws Exception;
}
