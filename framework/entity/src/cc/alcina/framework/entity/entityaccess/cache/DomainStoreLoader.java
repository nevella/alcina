package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;

public interface DomainStoreLoader {
    void appShutdown();

    LazyObjectLoader getLazyObjectLoader();

    DomainStoreTransformSequencer getTransformSequencer();

    List<DomainTransformRequestPersistent> loadTransformRequests(
            Collection<Long> ids, Logger logger) throws Exception;

    void onTransformsPersisted();

    void warmup() throws Exception;
}
