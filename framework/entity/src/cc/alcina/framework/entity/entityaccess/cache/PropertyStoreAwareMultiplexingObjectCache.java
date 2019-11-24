package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;

class PropertyStoreAwareMultiplexingObjectCache extends DetachedEntityCache {
    private volatile boolean committing;

    private DetachedEntityCacheTransactionalMap main = new DetachedEntityCacheTransactionalMap();

    private List<PropertyStoreCacheWrapper> psWrappers = new ArrayList<>();

    public PropertyStoreAwareMultiplexingObjectCache() {
        // main.setThrowOnExisting(true);
        //
        // double-put can happen due to incomplete transaction isolation on PG
        // warmup
    }

    public void addPropertyStore(DomainClassDescriptor descriptor) {
        psWrappers.add(new PropertyStoreCacheWrapper(
                (PropertyStoreItemDescriptor) descriptor));
    }

    @Override
    public Set<HasIdAndLocalId> allValues() {
        return main.allValues();
    }

    @Override
    public void clear() {
        main.clear();
    }

    @Override
    public DetachedEntityCache clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends HasIdAndLocalId> boolean contains(Class<T> clazz,
            long id) {
        return main.contains(clazz, id);
    }

    @Override
    public boolean contains(HasIdAndLocalId hili) {
        return main.contains(hili);
    }

    @Override
    public Map<Long, HasIdAndLocalId> createMap() {
        return main.createMap();
    }

    @Override
    public <T> List<T> fieldValues(Class<? extends HasIdAndLocalId> clazz,
            String propertyName) {
        return getSubCache(clazz).fieldValues(clazz, propertyName);
    }

    @Override
    public <T> T get(Class<T> clazz, Long id) {
        return getSubCache(clazz).get(clazz, id);
    }

    @Override
    public Map<Class, Map<Long, HasIdAndLocalId>> getDetached() {
        return main.getDetached();
    }

    @Override
    public Map<Long, HasIdAndLocalId> getMap(Class clazz) {
        return main.getMap(clazz);
    }

    @Override
    public <T> Collection<T> immutableRawValues(Class<T> clazz) {
        return main.immutableRawValues(clazz);
    }

    @Override
    public void invalidate(Class clazz) {
        main.invalidate(clazz);
    }

    @Override
    public void invalidate(Class[] classes) {
        main.invalidate(classes);
    }

    @Override
    public boolean isEmpty(Class clazz) {
        return main.isEmpty(clazz);
    }

    @Override
    public Set<Long> keys(Class clazz) {
        return getSubCache(clazz).keys(clazz);
    }

    @Override
    public List<Long> notContained(Collection<Long> ids, Class clazz) {
        return main.notContained(ids, clazz);
    }

    @Override
    public void put(HasIdAndLocalId hili) {
        getSubCache(hili.provideEntityClass()).put(hili);
    }

    @Override
    public void putAll(Class clazz,
            Collection<? extends HasIdAndLocalId> values) {
        main.putAll(clazz, values);
    }

    @Override
    public void putForSuperClass(Class clazz, HasIdAndLocalId hili) {
        main.putForSuperClass(clazz, hili);
    }

    @Override
    public void remove(HasIdAndLocalId hili) {
        getSubCache(hili.provideEntityClass()).remove(hili);
    }

    @Override
    public int size(Class clazz) {
        return main.size(clazz);
    }

    @Override
    public String sizes() {
        return main.sizes();
    }

    @Override
    public <T> Set<T> values(Class<T> clazz) {
        return main.values(clazz);
    }

    private <T> DomainStoreCache getSubCache(Class<T> clazz) {
        for (PropertyStoreCacheWrapper psWrapper : psWrappers) {
            if (psWrapper.getCachedClass() == clazz) {
                return psWrapper;
            }
        }
        return main;
    }

    @Override
    protected void ensureMaps(Class clazz) {
        main.ensureMaps(clazz);
    }

    synchronized void endCommit() {
        committing = false;
    }

    synchronized void startCommit() {
        for (PropertyStoreCacheWrapper psWrapper : psWrappers) {
            psWrapper.resetThreadCache();
        }
        committing = true;
    }

    class PropertyStoreCacheWrapper<V extends HasIdAndLocalId>
            implements DomainStoreCache {
        private PropertyStoreItemDescriptor<V> descriptor;

        Map<Long, V> commitLookup = new LinkedHashMap<>();

        PropertyStoreCacheWrapper(PropertyStoreItemDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public <T> List<T> fieldValues(Class<? extends HasIdAndLocalId> clazz,
                String propertyName) {
            return descriptor.propertyStore.fieldValues(propertyName);
        }

        @Override
        public <T> T get(Class<T> clazz, Long id) {
            if (committing) {
                if (!commitLookup.containsKey(id)) {
                    commitLookup.put(id, descriptor.getProxy(main, id, false));
                }
                return (T) commitLookup.get(id);
            } else {
                // does not cache, returns new instance each time.
                // FIXME - link to transaction?
                return (T) descriptor.getProxy(main, id, false);
            }
        }

        @Override
        public Set<Long> keys(Class clazz) {
            return descriptor.propertyStore.getIds();
        }

        @Override
        public void put(HasIdAndLocalId hili) {
            if (committing) {
                long id = hili.getId();
                commitLookup.put(id, descriptor.getProxy(main, id, true));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public void remove(HasIdAndLocalId hili) {
            if (committing) {
                commitLookup.remove(hili.getId());
                descriptor.remove(hili.getId());
            } else {
                throw new UnsupportedOperationException();
            }
        }

        public void resetThreadCache() {
            commitLookup.clear();
        }

        Class getCachedClass() {
            return descriptor.clazz;
        }
    }
}
