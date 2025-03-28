package cc.alcina.framework.entity.persistence.domain.segment;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.ConnResults;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.ConnResults.ConnResultsIterator;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.ConnResultsReuse;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.ValueContainer;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.entity.util.FsObjectCache;

public class DomainSegmentRemoteLoader implements DomainSegmentLoader {
	public static final Configuration.Key refresh = Configuration
			.key("refresh");

	public static final Configuration.Key clear = Configuration.key("clear");

	FsObjectCache<DomainSegment> cache;

	DomainSegment.Definition definition;

	Logger logger = LoggerFactory.getLogger(getClass());

	DomainSegment segment;

	public DomainSegmentRemoteLoader() {
		this.cache = new FsObjectCache<>(DataFolderProvider.get().getSubFolder(
				getClass().getSimpleName()), DomainSegment.class, null);
	}

	@Override
	public void init() {
		TransformCommit.setCommitTestTransforms(false);
		this.definition = Reflections
				.newInstance(Configuration.get("definitionClassName"));
		this.definition.configureLocal();
		logger.info("Definition :: {}", definition.asString());
		if (clear.is()) {
			logger.warn("Cache cleared :: {}", definition.asString());
			cache.clear();
		}
		load();
		if (clear.is()) {
			new LoadedWithClear().publish();
		}
		if (refresh.is() || segment.collections.isEmpty()) {
			refresh();
		}
	}

	public static class LoadedWithClear implements ProcessObservable {
	}

	void refresh() {
		DomainSegment localState = segment.toLocalState();
		DomainSegment remoteUpdates = Registry.impl(RemoteLoader.class)
				.load(definition, localState);
		logger.info(
				"State pre-refresh - local :: {} entities - remote delta :: {} ",
				localState.new Lookup().allValues().count(), remoteUpdates);
		segment.merge(remoteUpdates);
		persist(definition.name(), segment);
	}

	public void persist(String name, DomainSegment segment) {
		cache.persist(name, segment);
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
		return new ConnResultsReuseImpl();
	}

	class ConnResultsReuseImpl implements ConnResultsReuse {
		DomainSegment.Lookup lookup = segment.new Lookup();

		@Override
		public Iterator<ValueContainer[]> getIterator(ConnResults connResults,
				ConnResultsIterator itr) {
			Class clazz = connResults.clazz;
			if (clazz != null && Entity.class.isAssignableFrom(clazz)) {
				if (definition.providePassthroughClasses().contains(clazz)) {
				} else {
					return lookup.getValues(clazz).iterator();
				}
			} else {
				// join table classes
				int debug = 3;
			}
			return ConnResultsReuse.super.getIterator(connResults, itr);
		}
	}

	public DomainSegment getCachedSegment() {
		return segment;
	}
}
