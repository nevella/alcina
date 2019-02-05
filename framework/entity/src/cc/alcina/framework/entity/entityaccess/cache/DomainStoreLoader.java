package cc.alcina.framework.entity.entityaccess.cache;

import org.slf4j.Logger;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;

public interface DomainStoreLoader {
    void appShutdown();

    LazyObjectLoader getLazyObjectLoader();

    DomainTransformRequestPersistent loadTransformRequest(Long id,
            Logger logger) throws Exception;

    void warmup() throws Exception;
}
