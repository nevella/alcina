package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.DomainDescriptor.PreProvideTask;
import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.MetricLogging;

/*
 * With mvcc and lazy properties, some of the motivation for this has left. Left in the codebase for reference rather than the expectation it'll ever be used
 */
public abstract class LazyLoadProvideTask<T extends Entity>
		implements PreProvideTask<T> {
	public static final String CONTEXT_LAZY_LOAD_DISABLED = LazyLoadProvideTask.class
			.getName() + ".CONTEXT_LAZY_LOAD_DISABLED";

	final static Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass());

	private long minEvictionAge;

	private int minEvictionSize;

	protected Class<T> clazz;

	private LinkedHashMap<Long, Long> idEvictionAge = new LinkedHashMap<>();

	protected DomainStore domainStore;

	public LazyLoadProvideTask() {
	}

	public LazyLoadProvideTask(long minEvictionAge, int minEvictionSize,
			Class<T> clazz) {
		this.minEvictionAge = minEvictionAge;
		this.minEvictionSize = minEvictionSize;
		this.clazz = clazz;
	}

	public void addToEvictionQueue(Entity entity) {
		idEvictionAge.put(entity.getId(), System.currentTimeMillis());
	}

	public void evictDependents(EvictionToken evictionToken,
			Collection<? extends Entity> entities) {
		entities.stream().forEach(
				hili -> this.evict(evictionToken, hili.getId(), false));
	}

	@Override
	public Class<T> forClazz() {
		return this.clazz;
	}

	public void metric(String key, boolean end) {
		if (end) {
			MetricLogging.get().end(key, domainStore.metricLogger);
		} else {
			MetricLogging.get().start(key);
		}
	}

	@Override
	public void registerStore(IDomainStore iDomainStore) {
		this.domainStore = (DomainStore) iDomainStore;
	}

	@Override
	public void run(Class clazz, Collection<T> objects, boolean topLevel)
			throws Exception {
		if (clazz != this.clazz) {
			return;
		}
		if (LooseContext.is(CONTEXT_LAZY_LOAD_DISABLED)) {
			return;
		}
		List<T> requireLoad = requireLazyLoad(objects);
		if (!requireLoad.isEmpty()) {
			if (!checkShouldLazyLoad(requireLoad)) {
				return;
			}
			Object lockObject = getLockObject();
			if (lockObject == null) {
				// transactional population only, no need to evict or lock
				lazyLoad(requireLoad);
			} else {
				synchronized (lockObject) {
					// reget, just in case of interim eviction
					// requireLoad = requireLazyLoad(objects);
					// now eviction is happening in write-lock, and this only
					// happens in read-lock, no need
					lazyLoad(requireLoad);
					registerLoaded(requireLoad);
					if (topLevel) {
						loadDependents(requireLoad);
					}
				}
			}
		}
	}

	@Override
	public Stream<T> wrap(Stream<T> stream) {
		return wrapAll(stream);
	}

	private void registerLoaded(List<T> requireLoad) {
		for (T t : requireLoad) {
			idEvictionAge.put(t.getId(), System.currentTimeMillis());
		}
	}

	protected abstract boolean checkShouldLazyLoad(List<T> toLoad);

	protected void evict(EvictionToken evictionToken, Long key, boolean top) {
	}

	protected boolean evictionDisabled() {
		return true;
	}

	protected DetachedEntityCache getDomainCache() {
		return domainStore.cache;
	}

	protected Object getLockObject() {
		return this;
	}

	protected abstract void lazyLoad(Collection<T> objects) throws Exception;

	protected void lllog(String template, Object... args) {
		logger.debug(template.replace("%s", "{}"), args);
	}

	protected abstract void loadDependents(List<T> requireLoad)
			throws Exception;

	protected List<T> loadTable(Class clazz, String sqlFilter, ClassIdLock lock)
			throws Exception {
		Preconditions.checkState(
				domainStore.loader instanceof DomainStoreLoaderDatabase);
		return ((DomainStoreLoaderDatabase) domainStore.loader).loadTable(clazz,
				sqlFilter, lock);
	}

	protected void log(String template, Object... args) {
		this.domainStore.sqlLogger.debug(template.replace("%s", "{}"), args);
	}

	protected synchronized List<T> requireLazyLoad(Collection<T> objects) {
		List<T> result = new ArrayList<T>();
		for (T t : objects) {
			Long evictionAge = idEvictionAge.get(t.getId());
			if (evictionAge == null) {
				result.add(t);
			}
		}
		return result;
	}

	protected Stream<T> wrapAll(Stream<T> stream) {
		List<T> list = stream.collect(Collectors.toList());
		Preconditions.checkArgument(list.size() < 100000,
				"Max length of lazyload task is 100000");
		try {
			lazyLoad(list);
			return list.stream();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static class EvictionToken {
		public DomainStore store;

		private LazyLoadProvideTask topLevelTask;

		Multiset<LazyLoadProvideTask, Set<Long>> evicted = new Multiset<>();

		public EvictionToken(DomainStore store,
				LazyLoadProvideTask lazyLoadProvideTask) {
			this.store = store;
			this.topLevelTask = lazyLoadProvideTask;
		}

		public <T extends Entity> T getObject(Long key, Class<T> clazz) {
			return store.cache.get(clazz, key);
		}

		public int getTopLevelEvictedCount() {
			return evicted.getAndEnsure(topLevelTask).size();
		}

		public void removeEvicted() {
			FormatBuilder fb = new FormatBuilder();
			fb.line("Eviction stats:");
			evicted.entrySet().forEach(e -> {
				LazyLoadProvideTask task = e.getKey();
				LinkedHashMap<Long, Long> idEvictionAge = task.idEvictionAge;
				int s1 = idEvictionAge.size();
				idEvictionAge.keySet().removeAll(e.getValue());
				int s2 = idEvictionAge.size();
				fb.line("\t%s: %s => %s (%s)", task.clazz, s1, s2, s1 - s2);
			});
			topLevelTask.lllog(fb.toString());
		}

		public void removeFromDomainStore(Set<? extends Entity> set) {
			if (!set.isEmpty()) {
				Entity item0 = set.iterator().next();
				Class clazz = item0.getClass();
				int size1 = store.cache.getMap(clazz).size();
				for (Entity hili : set) {
					store.cache.remove(hili);
				}
				int size2 = store.cache.getMap(clazz).size();
				topLevelTask.lllog(
						"remove from domain store: %s :: %s => %s (%s)",
						clazz.getSimpleName(), size1, size2, size1 - size2);
			}
		}

		public void setEvicted(Long key, LazyLoadProvideTask task) {
			evicted.add(task, key);
		}

		public boolean wasEvicted(Long key, LazyLoadProvideTask task) {
			return evicted.contains(task, key);
		}
	}
}
