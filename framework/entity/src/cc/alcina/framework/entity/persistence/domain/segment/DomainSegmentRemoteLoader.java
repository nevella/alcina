package cc.alcina.framework.entity.persistence.domain.segment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.ConnResultsReuse;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.entity.util.FsObjectCache;

public class DomainSegmentRemoteLoader implements DomainSegmentLoader {
	FsObjectCache<DomainSegment> cache;

	DomainSegment.Definition definition;

	Logger logger = LoggerFactory.getLogger(getClass());

	DomainSegment segment;

	@Override
	public void init() {
		this.cache = new FsObjectCache<>(DataFolderProvider.get().getSubFolder(
				getClass().getSimpleName()), DomainSegment.class, null);
		this.definition = Reflections
				.newInstance(Configuration.get("definitionClassName"));
		this.definition.configureLocal();
		logger.info("Definition :: {}", definition.asString());
		load();
		if (Configuration.is("refresh")) {
			refresh();
		}
	}

	void refresh() {
		DomainSegment localState = segment.toLocalState();
		DomainSegment remoteUpdates = Registry.impl(RemoteLoader.class)
				.load(definition, localState);
		localState.merge(remoteUpdates);
		cache.persist(definition.name(), remoteUpdates);
	}

	public interface RemoteLoader {
		DomainSegment load(DomainSegment.Definition definition,
				DomainSegment localState);
	}

	void load() {
		segment = cache.optional(definition.name()).orElse(new DomainSegment());
	}

	@Override
	public ConnResultsReuse getConnResultsReuse() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getConnResultsReuse'");
	}
}
