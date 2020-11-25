package cc.alcina.framework.entity.persistence.cache;

import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;

public interface DomainStoreLoader {
	void appShutdown();

	boolean checkTransformRequestExists(long id);

	LazyObjectLoader getLazyObjectLoader();

	DomainStoreTransformSequencer getTransformSequencer();

	DomainTransformRequestPersistent loadTransformRequest(long id);

	void onTransformRequestsPersisted(List<Long> ids);

	void warmup() throws Exception;
}
