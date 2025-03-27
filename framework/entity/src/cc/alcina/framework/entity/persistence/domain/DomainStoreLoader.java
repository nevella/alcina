package cc.alcina.framework.entity.persistence.domain;

import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;

public interface DomainStoreLoader {
	void appShutdown();

	boolean checkTransformRequestExists(long id);

	void close();

	LazyObjectLoader getLazyObjectLoader();

	DomainTransformPersistenceQueue.Sequencer getTransformSequencer();

	DomainTransformRequestPersistent loadTransformRequest(long id);

	void warmup() throws Exception;

	Set<Long> getVisiblePreWarmupCompletionPersistenceEvents(List<Long> dtrIds);
}
