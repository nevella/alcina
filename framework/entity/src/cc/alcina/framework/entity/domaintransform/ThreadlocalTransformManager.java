/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity.domaintransform;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import javax.persistence.ManyToMany;
import javax.persistence.Query;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.PropertyFilter;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaResult;
import cc.alcina.framework.common.client.csobjects.ObjectDeltaSpec;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;
import cc.alcina.framework.common.client.logic.domaintransform.ObjectRef;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.AssignmentPermission;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.entityaccess.WrappedObject;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerTransformPropogation;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.projection.EntityUtils;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class ThreadlocalTransformManager extends TransformManager
        implements PropertyAccessor, ObjectLookup, ClassLookup {
    public static final String CONTEXT_IGNORE_DOUBLE_DELETION = ThreadlocalTransformManager.class
            .getName() + ".CONTEXT_IGNORE_DOUBLE_DELETION";

    public static final String CONTEXT_TEST_PERMISSIONS = ThreadlocalTransformManager.class
            .getName() + ".CONTEXT_TEST_PERMISSIONS";

    public static final String CONTEXT_FLUSH_BEFORE_DELETE = ThreadlocalTransformManager.class
            .getName() + ".CONTEXT_FLUSH_BEFORE_DELETE";

    public static final String CONTEXT_ALLOW_MODIFICATION_OF_DETACHED_OBJECTS = ThreadlocalTransformManager.class
            .getName() + ".CONTEXT_ALLOW_MODIFICATION_OF_DETACHED_OBJECTS";

    private static final String TOPIC_RESET_THREAD_TRANSFORM_MANAGER = ThreadlocalTransformManager.class
            .getName() + ".TOPIC_RESET_THREAD_TRANSFORM_MANAGER";

    private static ThreadLocal threadLocalTLTMInstance = new ThreadLocal() {
        @Override
        protected synchronized Object initialValue() {
            ThreadlocalTransformManager tm = ThreadlocalTransformManager
                    .ttmInstance();
            tm.resetTltm(null);
            return tm;
        }
    };

    private static List<DomainTransformListener> threadLocalListeners = new ArrayList<DomainTransformListener>();;

    private static ThreadLocalSequentialIdGenerator tlIdGenerator = new ThreadLocalSequentialIdGenerator();

    public static void addThreadLocalDomainTransformListener(
            DomainTransformListener listener) {
        threadLocalListeners.add(listener);
    }

    public static ThreadlocalTransformManager cast() {
        return (ThreadlocalTransformManager) TransformManager.get();
    }

    /**
     * Convenience "override" of TransformManager.get()
     */
    public static ThreadlocalTransformManager get() {
        return ThreadlocalTransformManager.cast();
    }

    public static boolean is() {
        return TransformManager.get() instanceof ThreadlocalTransformManager;
    }

    public static boolean isInEntityManagerTransaction() {
        return get() instanceof ThreadlocalTransformManager
                && cast().getEntityManager() != null;
    }

    public static void threadTransformManagerWasReset() {
        GlobalTopicPublisher.get().publishTopic(
                TOPIC_RESET_THREAD_TRANSFORM_MANAGER, Thread.currentThread());
    }

    public static void threadTransformManagerWasResetListenerDelta(
            TopicListener<Thread> listener, boolean add) {
        GlobalTopicPublisher.get().listenerDelta(
                TOPIC_RESET_THREAD_TRANSFORM_MANAGER, listener, add);
    }

    public static ThreadlocalTransformManager ttmInstance() {
        ThreadlocalTransformManager tltm = new ThreadlocalTransformManager();
        return tltm;
    }

    private PersistenceLayerTransformExceptionPolicy exceptionPolicy;

    private boolean useObjectCreationId;

    Set<HasIdAndLocalId> modifiedObjects = new HashSet<HasIdAndLocalId>();

    List<DomainTransformEvent> modificationEvents = new ArrayList<DomainTransformEvent>();

    private ClientInstance clientInstance;

    protected Map<Long, HasIdAndLocalId> localIdToEntityMap;

    protected HiliLocatorMap userSessionHiliMap;

    private EntityManager entityManager;

    private boolean listenToFoundObjects;

    private IdentityHashMap<SourcesPropertyChangeEvents, SourcesPropertyChangeEvents> listeningTo = new IdentityHashMap<SourcesPropertyChangeEvents, SourcesPropertyChangeEvents>();

    private DetachedEntityCache detachedEntityCache;

    protected Set<HasIdAndLocalId> deleted;

    protected HasIdAndLocalId ignorePropertyChangesTo;

    DomainTransformEvent lastEvent = null;

    private boolean initialised = false;

    protected Set<HiliLocator> createdObjectLocators = new LinkedHashSet<>();

    private boolean transformsExplicitlyPermitted;

    private Set<DomainTransformEvent> explicitlyPermittedTransforms = new LinkedHashSet<>();

    private boolean useTlIdGenerator = false;

    private Set<DomainTransformEvent> flushAfterTransforms = new LinkedHashSet<>();

    private CachingMap<DomainStore, PostTransactionEntityResolver> postTransactionEntityResolvers = new CachingMap<DomainStore, PostTransactionEntityResolver>(
            PostTransactionEntityResolver::new);

    @Override
    public void addTransform(DomainTransformEvent evt) {
        if (transformsExplicitlyPermitted) {
            explicitlyPermittedTransforms.add(evt);
        }
        if (evt.getTransformType() == TransformType.DELETE_OBJECT
                && LooseContext.is(CONTEXT_FLUSH_BEFORE_DELETE)) {
            markFlushTransforms();
        }
        super.addTransform(evt);
    }

    @Override
    public IndividualPropertyAccessor cachedAccessor(Class clazz,
            String propertyName) {
        return new MethodIndividualPropertyAccessor(clazz, propertyName);
    }

    public boolean checkPropertyAccess(HasIdAndLocalId hili,
            String propertyName, boolean read) throws IntrospectionException {
        if (hili.provideWasPersisted()
                || LooseContext.is(CONTEXT_TEST_PERMISSIONS)) {
            PropertyDescriptor descriptor = SEUtilities
                    .getPropertyDescriptorByName(hili.getClass(), propertyName);
            if (descriptor == null) {
                throw new IntrospectionException(
                        String.format("Property not found - %s::%s",
                                hili.getClass().getName(), propertyName));
            }
            PropertyPermissions pp = SEUtilities
                    .getPropertyDescriptorByName(hili.getClass(), propertyName)
                    .getReadMethod().getAnnotation(PropertyPermissions.class);
            ObjectPermissions op = hili.getClass()
                    .getAnnotation(ObjectPermissions.class);
            return PermissionsManager.get().checkEffectivePropertyPermission(op,
                    pp, hili, read);
        }
        return true;
    }

    @Override
    /**
     * TODO - ignore collection mods to collection properties with
     * the @OneToMany annotation (inefficient and unnecessary)
     * ...hmmm...wait-a-sec - might be necessary for the level 2 cache
     */
    public void consume(DomainTransformEvent evt)
            throws DomainTransformException {
        super.consume(evt);
        if (getEntityManager() != null
                && evt.getTransformType() != TransformType.DELETE_OBJECT) {
            // for use in IVersionable/DomainStore
            maybeEnsureSource(evt);
        }
    }

    @Override
    public <T extends HasIdAndLocalId> T createDomainObject(
            Class<T> objectClass) {
        long localId = nextLocalIdCounter();
        T newInstance = newInstance(objectClass, 0, localId);
        // logic should probably be made clearer here - if id==0, we're not in
        // an
        // entitymanager context
        // so we want to record creates
        // nah - in fact, always record creates (even if in-em), but don't
        // process in consume() if obj exists
        // if (newInstance.getId() == 0) {
        registerDomainObject(newInstance);
        fireCreateObjectEvent(newInstance.getClass(), newInstance.getId(),
                newInstance.getLocalId());
        // }
        return newInstance;
    }

    /**
     * <<<<<<< HEAD Probably don't call this - rather call
     * deleteObject(hili,true) - this will always be a noop on the server
     * ======= Because TLTM never registers objects, delete(x,false) is always a
     * noop. A bit wonky, the whole thing - but this gets the job done. >>>>>>>
     * apdm-stable
     *
     * See registerDomainObject for explanation
     */
    @Override
    public DomainTransformEvent deleteObject(HasIdAndLocalId hili) {
        return deleteObject(hili, true);
    }

    @Override
    public DomainTransformEvent deleteObject(HasIdAndLocalId hili,
            boolean generateEventIfObjectNotFound) {
        if (deleted.contains(hili)) {
            if (!LooseContext.is(CONTEXT_IGNORE_DOUBLE_DELETION)) {
                RuntimeException ex = new RuntimeException(String.format(
                        "Double deletion - %s %s", new HiliLocator(hili),
                        CommonUtils.safeToString(hili)));
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
            return null;
        }
        hili = ensureNonProxy(hili);
        deleted.add(hili);
        DomainTransformEvent event = super.deleteObject(hili,
                generateEventIfObjectNotFound);
        if (event != null) {
            addTransform(event);
        }
        return event;
    }

    @Override
    public void deregisterDomainObject(Object o) {
        if (o instanceof SourcesPropertyChangeEvents) {
            listeningTo.remove(o);
            ((SourcesPropertyChangeEvents) o)
                    .removePropertyChangeListener(this);
        }
        super.deregisterDomainObject(o);
    }

    @Override
    public void deregisterDomainObjects(Collection<HasIdAndLocalId> hilis) {
        for (HasIdAndLocalId hili : hilis) {
            if (hili instanceof SourcesPropertyChangeEvents) {
                SourcesPropertyChangeEvents spce = (SourcesPropertyChangeEvents) hili;
                spce.removePropertyChangeListener(this);
                listeningTo.remove(spce);
            }
        }
        super.deregisterDomainObjects(hilis);
    }

    @Override
    public String displayNameForObject(Object o) {
        return ObjectPersistenceHelper.get().displayNameForObject(o);
    }

    @Override
    public <V extends HasIdAndLocalId> V find(Class<V> clazz, String key,
            Object value) {
        V first = null;
        if (getEntityManager() != null) {
            String eql = String.format(
                    value == null ? "from %s where %s is null"
                            : "from %s where %s = ?",
                    clazz.getSimpleName(), key);
            Query q = getEntityManager().createQuery(eql);
            if (value != null) {
                q.setParameter(1, value);
            }
            List<V> l = q.getResultList();
            first = CommonUtils.first(l);
            if (first != null) {
                return first;
            }
        }
        if (detachedEntityCache != null) {
            first = CommonUtils.first(
                    CollectionFilters.filter(detachedEntityCache.values(clazz),
                            new PropertyFilter<V>(key, value)));
            if (first != null) {
                return first;
            }
        }
        // maybe created in this 'transaction'
        return super.find(clazz, key, value);
    }

    public void flush() {
        flush(new ArrayList<>());
    }

    public void flush(List<DomainTransformEventPersistent> dtreps) {
        entityManager.flush();
    }

    @Override
    public List<String> getAnnotatedPropertyNames(Class clazz) {
        return ObjectPersistenceHelper.get().getAnnotatedPropertyNames(clazz);
    }

    @Override
    public <A extends Annotation> A getAnnotationForClass(Class targetClass,
            Class<A> annotationClass) {
        return (A) targetClass.getAnnotation(annotationClass);
    }

    @Override
    public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
            Class<A> annotationClass, String propertyName) {
        try {
            PropertyDescriptor[] pds = Introspector.getBeanInfo(targetClass)
                    .getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                if (pd.getName().equals(propertyName)) {
                    return pd.getReadMethod().getAnnotation(annotationClass);
                }
            }
            return null;
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public Class getClassForName(String fqn) {
        // Should never be called - but anyway...
        try {
            return Class.forName(fqn);
        } catch (ClassNotFoundException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public ClientInstance getClientInstance() {
        return this.clientInstance;
    }

    public DetachedEntityCache getDetachedEntityCache() {
        return this.detachedEntityCache;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public <H extends HasIdAndLocalId> long getLocalIdForClientInstance(
            H hili) {
        if (userSessionHiliMap != null) {
            return userSessionHiliMap.getLocalIdForClientInstance(hili);
        } else {
            return super.getLocalIdForClientInstance(hili);
        }
    }

    public List<DomainTransformEvent> getModificationEvents() {
        return this.modificationEvents;
    }

    @Override
    public <T extends HasIdAndLocalId> T getObject(Class<? extends T> c,
            long id, long localId) {
        if (!HasIdAndLocalId.class.isAssignableFrom(c)) {
            throw new WrappedRuntimeException(
                    "Attempting to obtain incompatible bean: " + c,
                    SuggestedAction.NOTIFY_WARNING);
        }
        if (id == 0) {
            if (localIdToEntityMap.containsKey(localId)) {
                return (T) localIdToEntityMap.get(localId);
            }
            if (userSessionHiliMap != null && localId != 0) {
                id = userSessionHiliMap.containsKey(localId)
                        ? userSessionHiliMap.getForLocalId(localId).id
                        : 0;
            }
            if (id == 0) {
                HiliLocator locator = postTransactionEntityResolvers
                        .get(DomainStore.writableStore()).resolve(localId);
                if (locator != null) {
                    id = locator.id;
                }
            }
        }
        if (id != 0 && getEntityManager() != null) {
            if (WrapperPersistable.class.isAssignableFrom(c)) {
                try {
                    WrappedObject wrapper = Registry
                            .impl(WrappedObjectProvider.class)
                            .getObjectWrapperForUser((Class) c, id,
                                    entityManager);
                    maybeListenToObjectWrapper(wrapper);
                    T wofu = (T) wrapper.getObject();
                    return (T) wofu;
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            }
            T t = getEntityManager().find(c, id);
            // this may be a performance hit - but worth it - otherwise all
            // sorts of potential problems
            // basically, transform events should (must) always have refs to
            // "real" objects, not wrappers
            t = ensureNonProxy(t);
            if (listenToFoundObjects
                    && t instanceof SourcesPropertyChangeEvents) {
                ((SourcesPropertyChangeEvents) t)
                        .addPropertyChangeListener(this);
            }
            if (localId != 0 && t != null) {
                localIdToEntityMap.put(localId, t);
            }
            return t;
        }
        return null;
    }

    @Override
    public <T extends HasIdAndLocalId> T getObject(T hili) {
        if (hili == null) {
            return null;
        }
        hili = ensureNonProxy(hili);
        return super.getObject(hili);
    }

    // TODO - permissions check
    public List<ObjectDeltaResult> getObjectDelta(List<ObjectDeltaSpec> specs)
            throws Exception {
        List<ObjectDeltaResult> result = new ArrayList<ObjectDeltaResult>();
        for (ObjectDeltaSpec itemSpec : specs) {
            ObjectRef ref = itemSpec.getObjectRef();
            String propertyName = itemSpec.getPropertyName();
            Association assoc = Reflections.propertyAccessor()
                    .getAnnotationForProperty(ref.getClassRef().getRefClass(),
                            Association.class, propertyName);
            ObjectDeltaResult itemResult = new ObjectDeltaResult();
            itemResult.setDeltaSpec(itemSpec);
            String eql = buildEqlForSpec(itemSpec, assoc.implementationClass());
            long t1 = System.currentTimeMillis();
            List results = getEntityManager().createQuery(eql).getResultList();
            EntityLayerObjects.get().getMetricLogger()
                    .debug("cache eql - total (ms):"
                            + (System.currentTimeMillis() - t1));
            try {
                itemResult.setTransforms(objectsToDtes(results,
                        assoc.implementationClass(), true));
            } catch (Exception e) {
                e.printStackTrace();
            }
            result.add(itemResult);
        }
        return result;
    }

    public PostTransactionEntityResolver getPostTransactionEntityResolver(
            DomainStore domainStore) {
        return this.postTransactionEntityResolvers.get(domainStore);
    }

    @Override
    public Class getPropertyType(Class clazz, String propertyName) {
        return ObjectPersistenceHelper.get().getPropertyType(clazz,
                propertyName);
    }

    @Override
    public Object getPropertyValue(Object bean, String propertyName) {
        try {
            PropertyDescriptor descriptor = SEUtilities
                    .getPropertyDescriptorByName(bean.getClass(), propertyName);
            if (descriptor == null) {
                throw new Exception(String.format("No property %s for class %s",
                        propertyName, bean.getClass().getName()));
            }
            return descriptor.getReadMethod().invoke(bean);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public TransformManager getT() {
        return (TransformManager) threadLocalTLTMInstance.get();
    }

    @Override
    public Enum getTargetEnumValue(DomainTransformEvent evt) {
        return ObjectPersistenceHelper.get().getTargetEnumValue(evt);
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
    public boolean isInCreationRequest(HasIdAndLocalId hili) {
        return createdObjectLocators.contains(new HiliLocator(hili));
    }

    public boolean isListeningTo(SourcesPropertyChangeEvents spce) {
        return listeningTo.containsKey(spce);
    }

    public boolean isListenToFoundObjects() {
        return listenToFoundObjects;
    }

    public boolean isTransformsExplicitlyPermitted() {
        return this.transformsExplicitlyPermitted;
    }

    /**
     * for complete database replay
     */
    public boolean isUseObjectCreationId() {
        return useObjectCreationId;
    }

    @Override
    public void listenTo(SourcesPropertyChangeEvents spce) {
        listeningTo.put(spce, spce);
        spce.removePropertyChangeListener(this);
        spce.addPropertyChangeListener(this);
    }

    public void markFlushTransforms() {
        flushAfterTransforms.add(CommonUtils.last(getTransforms().iterator()));
    }

    public void maybeListenToObjectWrapper(WrappedObject wrapper) {
        EntityLayerTransformPropogation transformPropogation = Registry
                .impl(EntityLayerTransformPropogation.class, void.class, true);
        if (transformPropogation != null
                && transformPropogation.listenToWrappedObject(wrapper)) {
            registerDomainObject((HasIdAndLocalId) wrapper);
        }
    }

    @Override
    public <T> T newInstance(Class<T> clazz) {
        return ObjectPersistenceHelper.get().newInstance(clazz);
    }

    /**
     * Can be called from the server layer(entityManager==null)
     */
    @Override
    public <T> T newInstance(Class<T> clazz, long objectId, long localId) {
        try {
            if (HasIdAndLocalId.class.isAssignableFrom(clazz)) {
                HasIdAndLocalId newInstance = (HasIdAndLocalId) clazz
                        .newInstance();
                localIdToEntityMap.put(localId, newInstance);
                if (entityManager != null) {
                    if (isUseObjectCreationId() && objectId != 0) {
                        newInstance.setId(objectId);
                        Object fromBefore = Registry
                                .impl(JPAImplementation.class)
                                .beforeSpecificSetId(entityManager,
                                        newInstance);
                        entityManager.persist(newInstance);
                        Registry.impl(JPAImplementation.class)
                                .afterSpecificSetId(fromBefore);
                    } else {
                        entityManager.persist(newInstance);
                    }
                } else {
                    newInstance.setLocalId(localId);
                }
                HiliLocator hiliLocator = new HiliLocator(
                        (Class<? extends HasIdAndLocalId>) clazz,
                        newInstance.getId(), localId);
                if (userSessionHiliMap != null) {
                    userSessionHiliMap.putToLookups(hiliLocator);
                }
                createdObjectLocators.add(hiliLocator);
                return (T) newInstance;
            }
            throw new Exception("only construct hilis here");
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public synchronized long nextLocalIdCounter() {
        return useTlIdGenerator ? tlIdGenerator.incrementAndGet()
                : localIdGenerator.incrementAndGet();
    }

    @Override
    public void performDeleteObject(HasIdAndLocalId hili) {
        HasIdAndLocalId object = getObject(hili);
        removeAssociations(hili);
        entityManager.remove(object);
    }

    public void persist(Object object) {
        entityManager.persist(object);
    }

    @Override
    public synchronized void propertyChange(PropertyChangeEvent evt) {
        if (isIgnorePropertyChangesForEvent(evt)) {
            return;
        }
        if (isIgnorePropertyChanges()
                || UNSPECIFIC_PROPERTY_CHANGE.equals(evt.getPropertyName())) {
            return;
        }
        DomainTransformEvent dte = createTransformFromPropertyChange(evt);
        convertToTargetObject(dte);
        if (lastEvent != null && lastEvent.equivalentTo(dte)) {
            // hibernate manipulations can cause a bunch of theses
            return;
        }
        lastEvent = dte;
        super.propertyChange(evt);
    }

    public boolean provideIsMarkedFlushTransform(DomainTransformEvent event) {
        return flushAfterTransforms.contains(event);
    }

    public HiliLocatorMap reconstituteHiliMap() {
        if (clientInstance != null) {
            CommonPersistenceLocal cp = Registry
                    .impl(CommonPersistenceProvider.class)
                    .getCommonPersistence();
            String message = "Reconstitute hili map - clientInstance: "
                    + clientInstance.getId();
            // System.out.println(message);
            // cp.log(message, LogMessageType.INFO.toString());
            String dteName = cp
                    .getImplementation(DomainTransformEventPersistent.class)
                    .getSimpleName();
            String dtrName = cp
                    .getImplementation(DomainTransformRequestPersistent.class)
                    .getSimpleName();
            MetricLogging.get().start(message);
            List<Long> dtrIds = getEntityManager().createQuery(String.format(
                    "select dtr.id from %s dtr where dtr.clientInstance.id = ?1",
                    dtrName)).setParameter(1, clientInstance.getId())
                    .getResultList();
            String eql = String.format(
                    "select dte.objectId, dte.objectLocalId, dte.objectClassRef.id "
                            + "from  %s dte  "
                            + " where dte.domainTransformRequestPersistent.id in %s "
                            + " and dte.objectLocalId!=0 and dte.transformType = ?1",
                    dteName, EntityUtils.longsToIdClause(dtrIds));
            List<Object[]> idTuples = getEntityManager().createQuery(eql)
                    .setParameter(1, TransformType.CREATE_OBJECT)
                    .getResultList();
            // force non-empty
            userSessionHiliMap.putToLookups(new HiliLocator(null, -1, 0));
            for (Object[] obj : idTuples) {
                ClassRef classRef = ClassRef.forId((long) obj[2]);
                userSessionHiliMap.putToLookups(new HiliLocator(
                        classRef.getRefClass(), (Long) obj[0], (Long) obj[1]));
            }
            MetricLogging.get().end(message);
        }
        return userSessionHiliMap;
    }

    @Override
    /**
     * NOTE - doesn't register children (unlike client)
     *
     * This is because of the two graph issue - db objects and current
     * Thread-memory objects.
     */
    public <T extends HasIdAndLocalId> T registerDomainObject(T hili) {
        if (hili instanceof SourcesPropertyChangeEvents) {
            listenTo((SourcesPropertyChangeEvents) hili);
        }
        return hili;
    }

    public void resetLocalIdCounterForCurrentThread() {
        resetLocalIdCounterForCurrentThread(new AtomicLong(0));
    }

    public void resetLocalIdCounterForCurrentThread(AtomicLong counter) {
        useTlIdGenerator = true;
        tlIdGenerator.reset(counter);
    }

    public void resetTltm(HiliLocatorMap locatorMap) {
        resetTltm(locatorMap, null, false);
    }

    public void resetTltm(HiliLocatorMap locatorMap,
            PersistenceLayerTransformExceptionPolicy exceptionPolicy,
            boolean keepExplicitlyPermittedAndFlushAfterTransforms) {
        setEntityManager(null);
        setDetachedEntityCache(null);
        this.exceptionPolicy = exceptionPolicy;
        if (this.userSessionHiliMap != null) {
            DomainStore.stores().stream()
                    .forEach(store -> this.postTransactionEntityResolvers
                            .get(store).merge(userSessionHiliMap));
        }
        this.userSessionHiliMap = locatorMap;
        localIdToEntityMap = new HashMap<Long, HasIdAndLocalId>();
        modifiedObjects = new HashSet<HasIdAndLocalId>();
        modificationEvents = new ArrayList<DomainTransformEvent>();
        transformListenerSupport.clear();
        deleted = new LinkedHashSet<HasIdAndLocalId>();
        createdObjectLocators.clear();
        if (!keepExplicitlyPermittedAndFlushAfterTransforms) {
            explicitlyPermittedTransforms.clear();
            flushAfterTransforms.clear();
        }
        this.lastEvent = null;
        for (SourcesPropertyChangeEvents spce : listeningTo.keySet()) {
            if (spce != null) {
                spce.removePropertyChangeListener(this);
            }
        }
        listeningTo = new IdentityHashMap<>();
        LinkedHashSet<DomainTransformEvent> pendingTransforms = getTransformsByCommitType(
                CommitType.TO_LOCAL_BEAN);
        if (!pendingTransforms.isEmpty() && !AppPersistenceBase.isTest()) {
            System.out.println(
                    "**WARNING ** TLTM - cleared (but still pending) transforms:\n "
                            + pendingTransforms);
            Thread.dumpStack();
            AlcinaTopics.notifyDevWarning(new UncomittedTransformsException());
        }
        clearTransforms();
        addDomainTransformListener(new ServerTransformListener());
        for (DomainTransformListener listener : threadLocalListeners) {
            addDomainTransformListener(listener);
        }
        if (initialised) {
            threadTransformManagerWasReset();
        } else {
            initialised = true;
        }
    }

    public <V extends HasIdAndLocalId> HiliLocator resolvePersistedLocal(
            DomainStore domainStore, V v) {
        return postTransactionEntityResolvers.get(domainStore).resolve(v);
    }

    public void setClientInstance(ClientInstance clientInstance) {
        this.clientInstance = clientInstance;
    }

    public void setDetachedEntityCache(
            DetachedEntityCache detachedEntityCache) {
        this.detachedEntityCache = detachedEntityCache;
    }

    public void setEntityManager(EntityManager entityManager) {
        // System.err.format("%s: %s\n", Thread.currentThread().getId(),
        // entityManager);
        // Thread.dumpStack();
        this.entityManager = entityManager;
    }

    public void setIgnorePropertyChangesTo(DomainTransformEvent event) {
        this.ignorePropertyChangesTo = null;
        if (event != null
                && event.getTransformType() != TransformType.CREATE_OBJECT) {
            this.ignorePropertyChangesTo = getObject(event, true);
        }
    }

    public void setListenToFoundObjects(boolean registerFoundObjects) {
        this.listenToFoundObjects = registerFoundObjects;
    }

    // dev-only
    public void setPostTransactionEntityResolver(
            PostTransactionEntityResolver postTransactionEntityResolver) {
        this.postTransactionEntityResolvers.put(DomainStore.writableStore(),
                postTransactionEntityResolver);
    }

    @Override
    public void setPropertyValue(Object bean, String propertyName,
            Object value) {
        if (!(bean instanceof HasIdAndLocalId)) {
            throw new WrappedRuntimeException(
                    "Attempting to serialize incompatible bean: " + bean,
                    SuggestedAction.NOTIFY_WARNING);
        }
        HasIdAndLocalId hili = (HasIdAndLocalId) bean;
        if (checkHasSufficientInfoForPropertyPersist(hili)) {
            try {
                SEUtilities.setPropertyValue(bean, propertyName, value);
                return;
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        } else {
        }
        throw new WrappedRuntimeException(
                "Attempting to alter property of non-persistent bean: " + bean,
                SuggestedAction.NOTIFY_WARNING);
    }

    public void setTransformsExplicitlyPermitted(
            boolean transformsExplicitlyPermitted) {
        this.transformsExplicitlyPermitted = transformsExplicitlyPermitted;
    }

    public void setUseObjectCreationId(boolean useObjectCreationId) {
        this.useObjectCreationId = useObjectCreationId;
    }

    public void setUserSessionHiliMap(HiliLocatorMap userSessionHiliMap) {
        this.userSessionHiliMap = userSessionHiliMap;
    }

    public boolean testPermissions(HasIdAndLocalId hili,
            DomainTransformEvent evt, String propertyName, Object change,
            boolean read) {
        if (!LooseContext.is(CONTEXT_TEST_PERMISSIONS)) {
            throw new RuntimeException("test property not set");
        }
        if (read) {
            try {
                checkPropertyReadAccessAndThrow(hili, propertyName, evt);
                return true;
            } catch (Exception e) {
                PermissionsException permissionsException = CommonUtils
                        .extractCauseOfClass(e, PermissionsException.class);
                if (permissionsException == null) {
                    throw new RuntimeException(e);
                } else {
                    return false;
                }
            }
        } else {
            return checkPermissions(hili, evt, propertyName, change, true);
        }
    }

    public void useGlobalLocalIdCounter() {
        useTlIdGenerator = false;
    }

    private String buildEqlForSpec(ObjectDeltaSpec itemSpec, Class assocClass)
            throws Exception {
        ObjectRef ref = itemSpec.getObjectRef();
        Class refClass = ref.getClassRef().getRefClass();
        List<String> projections = new ArrayList<String>();
        ClassLookup classLookup = Reflections.classLookup();
        String specProperty = null;
        projections.add(CommonUtils.formatJ("t.%s as %s", "id", "id"));
        List<PropertyInfoLite> pds = classLookup
                .getWritableProperties(assocClass);
        for (PropertyInfoLite pd : pds) {
            String propertyName = pd.getPropertyName();
            if (ignorePropertyForCaching(assocClass, pd.getPropertyType(),
                    propertyName)) {
                continue;
            }
            Class clazz = pd.getPropertyType();
            if (!HasIdAndLocalId.class.isAssignableFrom(clazz)) {
                projections.add(CommonUtils.formatJ("t.%s as %s", propertyName,
                        propertyName));
            } else {
                projections.add(CommonUtils.formatJ("t.%s.id as %s_id",
                        propertyName, propertyName));
                if (clazz == refClass) {
                    specProperty = propertyName;
                }
            }
        }
        String template = "select %s from %s t where t.%s.id=%s";
        return CommonUtils.formatJ(template, CommonUtils.join(projections, ","),
                assocClass.getSimpleName(), specProperty, ref.getId());
    }

    private boolean checkPermissions(HasIdAndLocalId hili,
            DomainTransformEvent evt, String propertyName, Object change,
            boolean muteLogging) {
        if (ResourceUtilities.getBoolean(ThreadlocalTransformManager.class,
                "ignoreTransformPermissions")) {
            return true;
        }
        if (explicitlyPermitted(evt)) {
            return true;
        }
        try {
            if (hili == null) {
                hili = (HasIdAndLocalId) evt.getObjectClass().newInstance();
            } else {
                hili = ensureNonProxy(hili);
                hili = resolveForPermissionsChecks(hili);
            }
            Class<? extends HasIdAndLocalId> objectClass = hili.getClass();
            ObjectPermissions op = objectClass
                    .getAnnotation(ObjectPermissions.class);
            op = op == null
                    ? PermissionsManager.get().getDefaultObjectPermissions()
                    : op;
            HasIdAndLocalId hiliChange = (HasIdAndLocalId) (change instanceof HasIdAndLocalId
                    ? change
                    : null);
            ObjectPermissions oph = null;
            AssignmentPermission aph = Reflections.propertyAccessor()
                    .getAnnotationForProperty(objectClass,
                            AssignmentPermission.class, propertyName);
            if (hiliChange != null) {
                oph = hiliChange.getClass()
                        .getAnnotation(ObjectPermissions.class);
                oph = oph == null
                        ? PermissionsManager.get().getDefaultObjectPermissions()
                        : oph;
            }
            switch (evt.getTransformType()) {
            case ADD_REF_TO_COLLECTION:
            case REMOVE_REF_FROM_COLLECTION:
                checkPropertyReadAccessAndThrow(hili, propertyName, evt);
                checkTargetReadAndAssignmentAccessAndThrow(hili, hiliChange,
                        oph, aph, evt);
                break;
            case CHANGE_PROPERTY_REF:
                checkTargetReadAndAssignmentAccessAndThrow(hili, hiliChange,
                        oph, aph, evt);
                // deliberate fall-through
            case NULL_PROPERTY_REF:
            case CHANGE_PROPERTY_SIMPLE_VALUE:
                return checkPropertyWriteAccessAndThrow(hili, propertyName,
                        evt);
            case CREATE_OBJECT:
                if (!PermissionsManager.get().isPermissible(hili,
                        op.create())) {
                    throw new DomainTransformException(new PermissionsException(
                            "Permission denied : create - object " + evt));
                }
                break;
            case DELETE_OBJECT:
                if (!PermissionsManager.get().isPermissible(hili,
                        op.delete())) {
                    throw new DomainTransformException(new PermissionsException(
                            "Permission denied : delete - object " + evt));
                }
                break;
            }
            // TODO:3.2, check r/w access for bean for add/remove ref
            // check r/w access for bean for all
        } catch (Exception e) {
            if (e instanceof DomainTransformException) {
                DomainTransformException dtex = (DomainTransformException) e;
                dtex.setEvent(evt);
                evt.setSource(hili);
                evt.setPropertyName(propertyName);
            }
            if (!muteLogging) {
                EntityLayerUtils.log(LogMessageType.TRANSFORM_EXCEPTION,
                        "Domain transform permissions exception", e);
            }
            throw new WrappedRuntimeException(e);
        }
        return true;
    }

    private void checkPropertyReadAccessAndThrow(HasIdAndLocalId hili,
            String propertyName, DomainTransformEvent evt)
            throws DomainTransformException, IntrospectionException {
        if (!checkPropertyAccess(hili, propertyName, true)) {
            throw new DomainTransformException(new PermissionsException(
                    "Permission denied : write - object/property " + evt));
        }
    }

    private boolean checkPropertyWriteAccessAndThrow(HasIdAndLocalId hili,
            String propertyName, DomainTransformEvent evt)
            throws DomainTransformException, IntrospectionException {
        if (!checkPropertyAccess(hili, propertyName, false)) {
            DomainProperty ann = getAnnotationForProperty(hili.getClass(),
                    DomainProperty.class, propertyName);
            if (ann != null && ann.silentFailOnIllegalWrites()) {
                return false;
            }
            throw new DomainTransformException(new PermissionsException(
                    "Permission denied : write - object/property " + evt));
        }
        return true;
    }

    private void checkTargetReadAndAssignmentAccessAndThrow(
            HasIdAndLocalId assigningTo, HasIdAndLocalId assigning,
            ObjectPermissions oph, AssignmentPermission aph,
            DomainTransformEvent evt) throws DomainTransformException {
        if (assigning == null) {
            return;
        }
        if (!PermissionsManager.get().isPermissible(assigning, oph.read())) {
            throw new DomainTransformException(new PermissionsException(
                    "Permission denied : read - target object " + evt));
        }
        if (aph != null && !PermissionsManager.get().isPermissible(assigning,
                assigningTo, new AnnotatedPermissible(aph.value()), false)) {
            throw new DomainTransformException(new PermissionsException(
                    "Permission denied : assign - target object " + evt));
        }
    }

    private boolean explicitlyPermitted(DomainTransformEvent evt) {
        return explicitlyPermittedTransforms.contains(evt);
    }

    protected boolean checkHasSufficientInfoForPropertyPersist(
            HasIdAndLocalId hili) {
        return hili.getId() != 0
                || (localIdToEntityMap.get(hili.getLocalId()) != null
                        && getEntityManager() == null)
                || (hili instanceof SourcesPropertyChangeEvents && listeningTo
                        .containsKey((SourcesPropertyChangeEvents) hili))
                || LooseContext
                        .is(CONTEXT_ALLOW_MODIFICATION_OF_DETACHED_OBJECTS);
    }

    @Override
    protected boolean checkPermissions(HasIdAndLocalId hili,
            DomainTransformEvent evt, String propertyName, Object change) {
        return checkPermissions(hili, evt, propertyName, change, false);
    }

    @Override
    protected void checkVersion(HasIdAndLocalId obj, DomainTransformEvent event)
            throws DomainTransformException {
        if (exceptionPolicy != null) {
            exceptionPolicy.checkVersion(obj, event);
        }
    }

    @Override
    protected void doCascadeDeletes(HasIdAndLocalId hili) {
        if (getEntityManager() == null) {
            new ServerTransformManagerSupport().removeParentAssociations(hili);
            new ServerTransformManagerSupport().doCascadeDeletes(hili);
        }
        // client-only for the moment.
    }

    @Override
    protected void doubleCheckAddition(Collection collection, Object tgt) {
        JPAImplementation jpaImplementation = Registry
                .impl(JPAImplementation.class);
        tgt = jpaImplementation.getInstantiatedObject(tgt);
        for (Iterator itr = collection.iterator(); itr.hasNext();) {
            Object next = itr.next();
            if (jpaImplementation.areEquivalentIgnoreInstantiationState(next,
                    tgt)) {
                return;
            }
        }
        collection.add(tgt);
    }

    @Override
    protected void doubleCheckRemoval(Collection collection, Object tgt) {
        JPAImplementation jpaImplementation = Registry
                .impl(JPAImplementation.class);
        tgt = jpaImplementation.getInstantiatedObject(tgt);
        for (Iterator itr = collection.iterator(); itr.hasNext();) {
            Object next = itr.next();
            if (jpaImplementation.areEquivalentIgnoreInstantiationState(next,
                    tgt)) {
                itr.remove();
                break;
            }
        }
    }

    protected <T extends HasIdAndLocalId> T ensureNonProxy(T hili) {
        if (hili != null && hili.getId() != 0 && getEntityManager() != null) {
            hili = Registry.impl(JPAImplementation.class)
                    .getInstantiatedObject(hili);
        }
        return hili;
    }

    protected boolean isIgnorePropertyChangesForEvent(PropertyChangeEvent evt) {
        return evt.getSource() == ignorePropertyChangesTo;
    }

    @Override
    protected boolean isZeroCreatedObjectLocalId(Class clazz) {
        return entityManager != null;
    }

    protected void maybeEnsureSource(DomainTransformEvent evt) {
        if (WrapperPersistable.class.isAssignableFrom(evt.getObjectClass())) {
            return;
        }
        if (evt.getSource() == null
                || !getEntityManager().contains(evt.getSource())) {
            getObject(evt);
        }
    }

    @Override
    protected void objectModified(HasIdAndLocalId hili,
            DomainTransformEvent evt, boolean targetObject) {
        boolean addToResults = false;
        if (evt.getTransformType() == TransformType.CREATE_OBJECT) {
            addToResults = true;
            evt.setGeneratedServerId(hili.getId());
        }
        // TODO - think about handling this as a postpersist entity listener?
        // that way we ensure correct version numbers
        if (hili instanceof HasVersionNumber
                && !modifiedObjects.contains(hili)) {
            addToResults = true;
            modifiedObjects.add(hili);
            HasVersionNumber hv = (HasVersionNumber) hili;
            if (targetObject) {
                evt.setValueVersionNumber(hv.getVersionNumber() + 1);
                if (evt.getValueId() == 0) {
                    evt.setValueId(hili.getId());
                }
            } else {
                evt.setObjectVersionNumber(hv.getVersionNumber() + 1);
                if (evt.getObjectId() == 0) {
                    evt.setObjectId(hili.getId());
                }
            }
        }
        if (addToResults) {
            modificationEvents.add(evt);
        }
    }

    protected void propertyChangeSuper(PropertyChangeEvent evt) {
        super.propertyChange(evt);
    }

    @Override
    protected void removeAssociations(HasIdAndLocalId hili) {
        new ServerTransformManagerSupport().removeAssociations(hili);
    }

    protected HasIdAndLocalId resolveForPermissionsChecks(
            HasIdAndLocalId hili) {
        return hili;
    }

    @Override
    // No need for property changes here - if in entitylayer
    // TODO - isn't this a huge hit?
    protected void updateAssociation(DomainTransformEvent evt,
            HasIdAndLocalId obj, Object tgt, boolean remove,
            boolean collectionPropertyChange) {
        if (getEntityManager() == null) {
            super.updateAssociation(evt, obj, tgt, remove, false);
        } else {
            ManyToMany manyToMany = Reflections.propertyAccessor()
                    .getAnnotationForProperty(evt.getObjectClass(),
                            ManyToMany.class, evt.getPropertyName());
            if (manyToMany != null && manyToMany.mappedBy().length() != 0) {
                super.updateAssociation(evt, obj, tgt, remove, false);
            }
        }
    }

    @Override
    protected boolean updateAssociationsWithoutNoChangeCheck() {
        return getEntityManager() == null;
    }

    public static class PostTransactionEntityResolver {
        private HiliLocatorMap locatorMap = new HiliLocatorMap();

        private boolean reconstituted = false;

        private long clientInstanceId;

        @SuppressWarnings("unused")
        private transient DomainStore domainStore;

        // for serializersr
        public PostTransactionEntityResolver() {
        }

        public PostTransactionEntityResolver(DomainStore domainStore) {
            this.domainStore = domainStore;
        }

        public void addMappings(
                DomainTransformRequestPersistent persistedRequest) {
            persistedRequest.allTransforms().stream()
                    .filter(dte -> dte
                            .getTransformType() == TransformType.CREATE_OBJECT)
                    .forEach(dte -> {
                        locatorMap.putToLookups(HiliLocator.objectLocator(dte));
                    });
            int debug = 3;
            // TODO Auto-generated method stub
        }

        public void merge(HiliLocatorMap locatorMap) {
            synchronized (locatorMap) {
                if (!PermissionsManager.get().isLoggedIn()) {
                    this.locatorMap = new HiliLocatorMap();
                } else {
                    long currentClientInstanceId = PermissionsManager.get()
                            .getClientInstance().getId();
                    if (currentClientInstanceId != clientInstanceId) {
                        this.locatorMap = new HiliLocatorMap();
                    }
                    this.locatorMap.merge(locatorMap);
                    clientInstanceId = currentClientInstanceId;
                }
                reconstituted = false;
            }
        }

        public HiliLocator resolve(HasIdAndLocalId v) {
            long localId = v.getLocalId();
            return resolve(localId);
        }

        protected HiliLocator resolve(long localId) {
            if (!AppPersistenceBase.isTest() && PermissionsManager.get()
                    .getClientInstance().getId() != clientInstanceId) {
                return null;
            }
            HiliLocator locator = locatorMap.getForLocalId(localId);
            if (locator != null) {
                return locator;
            }
            if (!reconstituted
                    && PermissionsManager.get().getClientInstance() != null) {
                locatorMap = CommonPersistenceProvider.get()
                        .getCommonPersistence()
                        .reconstituteHiliMap(PermissionsManager.get()
                                .getClientInstance().getId());
                reconstituted = true;
            }
            locator = locatorMap.getForLocalId(localId);
            return locator;
        }
    }

    public static class ThreadlocalTransformManagerFactory {
        public ThreadlocalTransformManager create() {
            return new ThreadlocalTransformManager();
        }
    }

    public static class UncomittedTransformsException extends Exception {
    }
}
