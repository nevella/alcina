package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.MapObjectLookupJvm;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.ServerTransformManagerSupport;

public class SubgraphTransformManager extends TransformManager {
    public static <T extends HasIdAndLocalId> T generateSynthetic(
            HiliLocatorMap locatorMap, Stream<DomainTransformEvent> stream) {
        try {
            SubgraphTransformManagerRecord tm = new SubgraphTransformManagerRecord();
            List<DomainTransformEvent> dtes = stream.map(dte -> {
                try {
                    // cheap hack to let local transforms work with
                    // DetachedEntityCache
                    DomainTransformEvent copy = ResourceUtilities
                            .fieldwiseClone(dte, true);
                    long objectLocalId = copy.getObjectLocalId();
                    if (objectLocalId != 0) {
                        if (locatorMap.containsKey(objectLocalId)) {
                            copy.setObjectId(
                                    locatorMap.getForLocalId(objectLocalId).id);
                            copy.setObjectLocalId(0);
                        } else {
                            copy.setObjectId(-objectLocalId);
                            copy.setObjectLocalId(0L);
                        }
                    }
                    return copy;
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            }).collect(Collectors.toList());
            for (DomainTransformEvent dte : dtes) {
                tm.consume(dte);
            }
            HasIdAndLocalId firstReferenced = tm.firstReferenced;
            long id = firstReferenced.getId();
            if (id < 0) {
                firstReferenced.setLocalId(-id);
                firstReferenced.setId(0);
            }
            return (T) firstReferenced;
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    protected DetachedCacheObjectStore store;

    SubgraphClassLookup classLookup = new SubgraphClassLookup();

    public SubgraphTransformManager() {
        super();
        createObjectLookup();
    }

    public DetachedEntityCache getDetachedEntityCache() {
        return store.getCache();
    }

    public DetachedCacheObjectStore getStore() {
        return this.store;
    }

    @Override
    protected ClassLookup classLookup() {
        return classLookup;
    }

    @Override
    protected void createObjectLookup() {
        store = new DetachedCacheObjectStore(
                new DetachedEntityCacheArrayBacked());
        setDomainObjects(store);
    }

    @Override
    protected void doCascadeDeletes(HasIdAndLocalId hili) {
        // rely on the client for this...
        // or the servletlayer aspect of tltm
    }

    @Override
    protected Object ensureEndpointInTransformGraph(Object object) {
        if (object instanceof HasIdAndLocalId) {
            return getObject((HasIdAndLocalId) object);
        }
        return object;
    }

    @Override
    protected HasIdAndLocalId getObjectForCreate(DomainTransformEvent event) {
        return null;
    }

    @Override
    protected ObjectLookup getObjectLookup() {
        return store;
    }

    @Override
    protected void removeAssociations(HasIdAndLocalId hili) {
        new ServerTransformManagerSupport().removeAssociations(hili);
    }

    @Override
    protected boolean updateAssociationsWithoutNoChangeCheck() {
        return true;
    }

    public static class SubgraphTransformManagerPreProcess
            extends SubgraphTransformManager {
        public SubgraphTransformManagerPreProcess(HiliLocatorMap locatorMap) {
            ((PreProcessBridgeLookup) getDomainObjects()).locatorMap = locatorMap;
        }

        @Override
        protected void createObjectLookup() {
            setDomainObjects(new PreProcessBridgeLookup());
        }

        @Override
        protected ObjectLookup getObjectLookup() {
            return getDomainObjects();
        }
    }

    static class PreProcessBridgeLookup extends MapObjectLookupJvm {
        private HiliLocatorMap locatorMap;

        public PreProcessBridgeLookup() {
        }

        @Override
        public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
                long id, long localId) {
            T t = super.getObject(c, id, localId);
            if (t == null) {
                if (id == 0) {
                    HiliLocator hiliLocator = locatorMap.getForLocalId(localId);
                    if (hiliLocator == null) {
                        return null;
                    }
                    id = hiliLocator.id;
                }
                if (id != 0) {
                    t = Domain.detachedVersion(c, id);
                    mapObject(t);
                }
            }
            return t;
        }
    }

    static class SubgraphClassLookup implements ClassLookup {
        @Override
        public String displayNameForObject(Object o) {
            return ObjectPersistenceHelper.get().displayNameForObject(o);
        }

        @Override
        public List<String> getAnnotatedPropertyNames(Class clazz) {
            return ObjectPersistenceHelper.get()
                    .getAnnotatedPropertyNames(clazz);
        }

        @Override
        public <A extends Annotation> A getAnnotationForClass(Class targetClass,
                Class<A> annotationClass) {
            return ObjectPersistenceHelper.get()
                    .getAnnotationForClass(targetClass, annotationClass);
        }

        @Override
        public Class getClassForName(String fqn) {
            return ObjectPersistenceHelper.get().getClassForName(fqn);
        }

        @Override
        public Class getPropertyType(Class clazz, String propertyName) {
            return ObjectPersistenceHelper.get().getPropertyType(clazz,
                    propertyName);
        }

        @Override
        public <T> T getTemplateInstance(Class<T> clazz) {
            return ObjectPersistenceHelper.get().getTemplateInstance(clazz);
        }

        @Override
        public List<PropertyInfoLite> getWritableProperties(Class clazz) {
            return ObjectPersistenceHelper.get().getWritableProperties(clazz);
        }

        @Override
        public <T> T newInstance(Class<T> clazz) {
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        }

        @Override
        public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
            try {
                HasIdAndLocalId newInstance = (HasIdAndLocalId) clazz
                        .newInstance();
                newInstance.setLocalId(localId);
                return (T) newInstance;
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        }
    }

    static class SubgraphTransformManagerRecord extends SubgraphTransformManager
            implements LazyObjectLoader {
        private HasIdAndLocalId firstReferenced = null;

        @Override
        public void consume(DomainTransformEvent event)
                throws DomainTransformException {
            super.consume(event);
            if (firstReferenced == null) {
                Iterator<HasIdAndLocalId> iterator = getDetachedEntityCache()
                        .allValues().iterator();
                firstReferenced = iterator.hasNext() ? iterator.next() : null;
            }
        }

        @Override
        public <T extends HasIdAndLocalId> void loadObject(
                Class<? extends T> clazz, long id, long localId) {
            store.mapObject(Domain.detachedVersion(clazz, id));
        }

        @Override
        protected void createObjectLookup() {
            store = new DetachedCacheObjectStore(new DetachedEntityCache());
            store.setLazyObjectLoader(this);
            setDomainObjects(store);
        }
    }
}
