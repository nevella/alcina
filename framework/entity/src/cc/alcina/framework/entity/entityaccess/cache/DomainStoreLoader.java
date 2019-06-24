package cc.alcina.framework.entity.entityaccess.cache;

import org.slf4j.Logger;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;

public interface DomainStoreLoader {
    void appShutdown();

    DomainStoreTransformSequencer getTransformSequencer();

    LazyObjectLoader getLazyObjectLoader();

    DomainTransformRequestPersistent loadTransformRequest(Long id,
            Logger logger) throws Exception;

    void onTransformsPersisted();

    void warmup() throws Exception;
}
