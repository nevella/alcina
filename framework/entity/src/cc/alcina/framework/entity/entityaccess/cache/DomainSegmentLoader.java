package cc.alcina.framework.entity.entityaccess.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.domain.IDomainSegmentLoader;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.ConnResults;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.ConnResults.ConnResultsIterator;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.ConnResultsReuse;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.LaterLookup.LaterItem;
import cc.alcina.framework.entity.util.SimpleAtomModel.AtomKey;

public abstract class DomainSegmentLoader
        implements IDomainSegmentLoader, ConnResultsReuse {
    List<LaterItem> toResolve = new ArrayList<>();

    Multiset<Class, Set<Long>> toLoadIds = new Multiset<>();

    List<DomainSegmentLoaderProperty> properties = new ArrayList<>();

    // clazz,property,id,id
    MultikeyMap<Long> queried = new UnsortedMultikeyMap<>(3);

    Map<ConnRsKey, List<Object[]>> savedRsResults = new LinkedHashMap<>();

    Logger logger = LoggerFactory.getLogger(getClass());

    protected Multiset<Class, Set<Long>> initialToLoadIds = new Multiset<>();

    protected transient DomainSegmentLoaderPhase phase;

    // class/id=>phase
    MultikeyMap<DomainSegmentLoaderPhase> loadedInPhase = new UnsortedMultikeyMap<>(
            2);

    public DomainSegmentLoader() {
    }

    public synchronized Collection<Long> filterForQueried(Class clazz,
            String property, Collection<Long> ids) {
        ids = new ArrayList<>(ids);
        ids.removeAll(queried.asMapEnsure(true, clazz, property).keySet());
        ids.forEach(id -> queried.put(clazz, property, id, id));
        return ids;
    }

    public abstract String getFilename();

    @Override
    public Iterator<Object[]> getIterator(ConnResults connResults,
            ConnResultsIterator itr) {
        ConnRsKey key = new ConnRsKey(connResults);
        synchronized (savedRsResults) {
            List<Object[]> savedResults = savedRsResults.get(key);
            if (savedResults == null) {
                savedResults = new ArrayList<>();
                connResults.cachedValues = savedResults;
                savedRsResults.put(key, savedResults);
                DomainStore.stores().writableStore().sqlLogger.debug("{}: {}",
                        connResults.clazz == null ? "(null)"
                                : connResults.clazz.getSimpleName(),
                        connResults.sqlFilter);
                return itr;
            } else {
                return savedResults.iterator();
            }
        }
    }

    public void initialise() throws Exception {
        if (isReload()) {
            try {
                loadSegmentData();
            } catch (Exception e) {
                e.printStackTrace();
                new File(getFilename()).delete();
            }
        }
        initialiseProperties();
        if (!isReload()) {
            initialiseSeedLookup0();
            toLoadIds.addAll(initialToLoadIds);
        }
    }

    public boolean isReload() {
        return new File(getFilename()).exists();
    }

    public void loadedInPhase(Class clazz, Collection<Long> ids) {
        ids.forEach(id -> {
            if (!loadedInPhase.containsKey(clazz, id)) {
                loadedInPhase.put(clazz, id, phase);
            }
        });
    }

    public synchronized void notifyLater(LaterItem item, Class type, long id) {
        if (properties.stream().anyMatch(property -> property.isIgnore(item))) {
            return;
        }
        toResolve.add(item);
        toLoadIds.add(type, id);
    }

    @Override
    public void onNext(ConnResults connResults, Object[] cached) {
        connResults.cachedValues.add(cached);
    }

    public int pendingCount() {
        return toLoadIds.allItems().size() + toResolve.size();
    }

    public void saveSegmentData() {
        if (isReload()) {
            return;
        }
        saveSegmentData0();
    }

    public void saveSegmentData0() {
        SavedSegmentDataHolder holder = new SavedSegmentDataHolder();
        holder.initialToLoadIds = initialToLoadIds;
        holder.savedRsResults = savedRsResults;
        KryoUtils.serializeToFile(holder, new File(getFilename()));
    }

    public boolean wasLoadedInPhase(Class clazz, long id,
            DomainSegmentLoaderPhase phase) {
        return loadedInPhase.get(clazz, id) == phase;
    }

    protected void addProperty(Class<? extends HasIdAndLocalId> source,
            String propertyName, Class<? extends HasIdAndLocalId> target,
            DomainSegmentPropertyType type) {
        addProperty(source, propertyName, target, type,
                DomainSegmentLoaderPhase.ALL_PHASES);
    }

    protected void addProperty(Class<? extends HasIdAndLocalId> source,
            String propertyName, Class<? extends HasIdAndLocalId> target,
            DomainSegmentPropertyType type, DomainSegmentLoaderPhase phase) {
        properties.add(new DomainSegmentLoaderProperty(source, propertyName,
                target, type, phase));
    }

    protected void clearCache() {
        new File(getFilename()).delete();
    }

    protected void initialiseProperties() {
        // TODO Auto-generated method stub
    }

    protected abstract void initialiseSeedLookup0() throws Exception;

    synchronized void ensureClass(Class clazz) {
        toLoadIds.addCollection(clazz, new ArrayList<>());
    }

    void loadSegmentData() {
        logger.info("Loading segment data...\n\t{}", getFilename());
        SavedSegmentDataHolder holder = KryoUtils.deserializeFromFile(
                new File(getFilename()), SavedSegmentDataHolder.class);
        savedRsResults = holder.savedRsResults;
        toLoadIds.addAll(holder.initialToLoadIds);
    }

    public enum DomainSegmentLoaderPhase {
        ALL_PHASES, ONE, TWO;
        public static List<DomainSegmentLoaderPhase> iterateOver() {
            return Arrays.asList(ONE, TWO);
        }

        public boolean isIgnoreForPhase(DomainSegmentLoaderPhase forPhase) {
            switch (this) {
            case ALL_PHASES:
                return false;
            default:
                return forPhase != this;
            }
        }
    }

    public static class DomainSegmentLoaderProperty {
        Class<? extends HasIdAndLocalId> clazz1;

        String propertyName1;

        Class<? extends HasIdAndLocalId> clazz2;

        DomainSegmentPropertyType type;

        private DomainSegmentLoaderPhase phase;

        public DomainSegmentLoaderProperty(
                Class<? extends HasIdAndLocalId> source, String propertyName,
                Class<? extends HasIdAndLocalId> target,
                DomainSegmentPropertyType type,
                DomainSegmentLoaderPhase phase) {
            this.clazz1 = source;
            this.propertyName1 = propertyName;
            this.clazz2 = target;
            this.type = type;
            this.phase = phase;
        }

        public boolean isIgnore(LaterItem item) {
            if (type == DomainSegmentPropertyType.IGNORE) {
                if (item.pdOperator.pd.getName().equals(propertyName1)
                        && item.source.getClass() == clazz1) {
                    return true;
                }
            }
            return false;
        }

        public boolean isIgnoreForPhase(DomainSegmentLoaderPhase forPhase) {
            if (this.phase == null) {
                return false;
            }
            return this.phase.isIgnoreForPhase(forPhase);
        }
    }

    public enum DomainSegmentPropertyType {
        TABLE_REF_CLAZZ_1_RSCOL_REFS_CLAZZ_2,
        STORE_REF_CLAZZ_1_PROP_EQ_CLAZZ_2_ID_LOAD_CLAZZ_2, IGNORE
    }

    public static class SavedSegmentDataHolder {
        public Map<ConnRsKey, List<Object[]>> savedRsResults = new LinkedHashMap<>();

        public Multiset<Class, Set<Long>> initialToLoadIds = new Multiset<>();
    }

    static class ConnRsKey extends AtomKey {
        String clazzName;

        String sqlFilter;

        public ConnRsKey() {
        }

        public ConnRsKey(ConnResults connResults) {
            clazzName = connResults.clazz == null ? "(null)"
                    : connResults.clazz.getName();
            sqlFilter = connResults.sqlFilter;
        }
    }
}