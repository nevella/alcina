package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.DomainDescriptor.PreProvideTask;
import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.MetricLogging;

public abstract class LazyLoadProvideTask<T extends HasIdAndLocalId>
        implements PreProvideTask<T> {
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

    public void evictDependents(EvictionToken evictionToken,
            Collection<? extends HasIdAndLocalId> hilis) {
        hilis.stream().forEach(
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
        List<T> requireLoad = requireLazyLoad(objects);
        if (!requireLoad.isEmpty()) {
            if (!beforeLazyLoad(requireLoad)) {
                return;
            }
            synchronized (getLockObject()) {
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

    @Override
    public void writeLockedCleanup() {
        if (evictionDisabled()) {
            return;
        }
        Iterator<Entry<Long, Long>> itr = idEvictionAge.entrySet().iterator();
        EvictionToken evictionToken = new EvictionToken(domainStore, this);
        while (idEvictionAge
                .size() > (minEvictionSize
                        + evictionToken.getTopLevelEvictedCount())
                && itr.hasNext()) {
            Entry<Long, Long> entry = itr.next();
            if ((System.currentTimeMillis()
                    - entry.getValue()) > minEvictionAge) {
                try {
                    Long key = entry.getKey();
                    if (!evictionToken.wasEvicted(key, this)) {
                        evict(evictionToken, key, true);
                    }
                } catch (Exception e) {
                    AlcinaTopics.notifyDevWarning(e);
                }
            }
        }
        evictionToken.removeEvicted();
    }

    private void registerLoaded(List<T> requireLoad) {
        for (T t : requireLoad) {
            idEvictionAge.put(t.getId(), System.currentTimeMillis());
        }
    }

    private synchronized List<T> requireLazyLoad(Collection<T> objects) {
        List<T> result = new ArrayList<T>();
        for (T t : objects) {
            Long evictionAge = idEvictionAge.get(t.getId());
            if (evictionAge == null) {
                result.add(t);
            }
        }
        return result;
    }

    protected abstract boolean beforeLazyLoad(List<T> toLoad);

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

    public static class EvictionToken {
        public DomainStore store;

        private LazyLoadProvideTask topLevelTask;

        Multiset<LazyLoadProvideTask, Set<Long>> evicted = new Multiset<>();

        public EvictionToken(DomainStore store,
                LazyLoadProvideTask lazyLoadProvideTask) {
            this.store = store;
            this.topLevelTask = lazyLoadProvideTask;
        }

        public <T extends HasIdAndLocalId> T getObject(Long key,
                Class<T> clazz) {
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

        public void removeFromDomainStore(Set<? extends HasIdAndLocalId> set) {
            if (!set.isEmpty()) {
                HasIdAndLocalId item0 = set.iterator().next();
                Class clazz = item0.getClass();
                int size1 = store.cache.getMap(clazz).size();
                for (HasIdAndLocalId hili : set) {
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
