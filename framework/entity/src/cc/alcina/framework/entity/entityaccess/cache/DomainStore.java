package cc.alcina.framework.entity.entityaccess.cache;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.UmbrellaException;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;
import cc.alcina.framework.common.client.domain.ComplexFilter;
import cc.alcina.framework.common.client.domain.ComplexFilter.ComplexFilterContext;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.Domain.DomainHandler;
import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.domain.DomainDescriptor;
import cc.alcina.framework.common.client.domain.DomainDescriptor.PreProvideTask;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.DomainListener;
import cc.alcina.framework.common.client.domain.DomainLookup;
import cc.alcina.framework.common.client.domain.DomainQuery;
import cc.alcina.framework.common.client.domain.DomainStoreLookupDescriptor;
import cc.alcina.framework.common.client.domain.FilterCost;
import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.domain.MemoryStat;
import cc.alcina.framework.common.client.domain.MemoryStat.ObjectMemory;
import cc.alcina.framework.common.client.domain.MemoryStat.StatType;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvents;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceListener;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.TransformPersister;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreProperty.DomainStorePropertyLoadType;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreThreads.DomainStoreHealth;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreThreads.DomainStoreInstrumentation;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Mvcc;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transactions;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjections;

/**
 * <h3>Locking notes:</h3>
 * <p>
 * main lock (post-process) - normal lock sublock - basically go from read
 * (possibly write::main) to write (subgraph) - so we know we'll have a main
 * lock
 * </p>
 * <p>
 * TODO - the multithreaded warmup is still a little dodgy, thread-safety wise -
 * probably a formal/synchronized datastore approach would be best -
 * ConcurrentLinkedQueue??
 * </p>
 *
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class DomainStore implements IDomainStore {
	private static final String TOPIC_UPDATE_EXCEPTION = DomainStore.class
			.getName() + ".TOPIC_UPDATE_EXCEPTION";

	private static final String TOPIC_MAPPING_EVENT = DomainStore.class
			.getName() + ".TOPIC_MAPPING_EVENT";

	private static final String TOPIC_NON_LOCKED_ACCESS = DomainStore.class
			.getName() + ".TOPIC_NON_LOCKED_ACCESS";

	public static final String CONTEXT_DEBUG_QUERY_METRICS = DomainStore.class
			.getName() + ".CONTEXT_DEBUG_QUERY_METRICS";

	public static final String CONTEXT_WILL_PROJECT_AFTER_READ_LOCK = DomainStore.class
			.getName() + ".CONTEXT_WILL_PROJECT_AFTER_READ_LOCK";

	public static final String CONTEXT_KEEP_LOAD_TABLE_DETACHED_FROM_GRAPH = DomainStore.class
			.getName() + ".CONTEXT_KEEP_LOAD_TABLE_DETACHED_FROM_GRAPH";

	public static final String CONTEXT_DO_NOT_RESOLVE_LOAD_TABLE_REFS = DomainStore.class
			.getName() + ".CONTEXT_DO_NOT_RESOLVE_LOAD_TABLE_REFS";

	public static final String CONTEXT_WRITEABLE_PROJECTOR = DomainStore.class
			.getName() + ".CONTEXT_WRITEABLE_PROJECTOR";

	public static final String CONTEXT_POPULATE_LAZY_PROPERTY_VALUES = DomainStore.class
			.getName() + ".CONTEXT_POPULATE_LAZY_PROPERTY_VALUES";

	// while debugging, prevent reentrant locks
	public static final String CONTEXT_NO_LOCKS = DomainStore.class.getName()
			+ ".CONTEXT_NO_LOCKS";

	public static final Logger LOGGER_WRAPPED_OBJECT_REF_INTEGRITY = AlcinaLogUtils
			.getTaggedLogger(DomainStore.class, "wrapped_object_ref_integrity");
	static {
		ThreadlocalTransformManager.addThreadLocalDomainTransformListener(
				new TransformManager.AssociationPropogationTransformListener(
						CommitType.TO_LOCAL_BEAN));
		ThreadlocalTransformManager.addThreadLocalDomainTransformListener(
				new IndexingTransformListener());
	}

	private static DomainStores domainStores;

	public static Builder builder() {
		return new Builder();
	}

	// FIXME - mvcc.jade - remove
	public static void checkInLockedSection() {
		// mvcc.end - remove
		// if (stores().hasInitialisedDatabaseStore()
		// && !writableStore().threads.isCurrentThreadHoldingLock()) {
		// topicNonLoggedAccess().publish(null);
		// }
	}

	// FIXME - this is over-called, probably should be changed to strict
	// start/finish semantics (and made non-static)
	public static PerThreadTransaction ensureActiveTransaction() {
		return null;
		// return stores().writableStore().transactions().ensureTransaction();
	}

	public static DomainStores stores() {
		synchronized (DomainStores.class) {
			if (domainStores == null) {
				domainStores = new DomainStores();
				Registry.registerSingleton(DomainStore.class, domainStores);
			}
		}
		return domainStores;
	}

	public static TopicSupport<Entity> topicMappingEvent() {
		return new TopicSupport<>(TOPIC_MAPPING_EVENT);
	}

	public static TopicSupport<Void> topicNonLoggedAccess() {
		return new TopicSupport<>(TOPIC_NON_LOCKED_ACCESS);
	}

	public static TopicSupport<DomainStoreUpdateException>
			topicUpdateException() {
		return new TopicSupport<>(TOPIC_UPDATE_EXCEPTION);
	}

	public static DomainStore writableStore() {
		return stores().writableStore();
	}

	static Timestamp testSensitiveTimestamp(Date transactionCommitTime) {
		return transactionCommitTime == null
				? new Timestamp(System.currentTimeMillis())
				: new Timestamp(transactionCommitTime.getTime());
	}

	private DomainTransformPersistenceEvents persistenceEvents;

	SubgraphTransformManagerPostProcess transformManager;

	DomainStoreDescriptor domainDescriptor;

	Logger sqlLogger = AlcinaLogUtils.getTaggedLogger(getClass(), "sql");

	Logger metricLogger = AlcinaLogUtils.getMetricLogger(getClass());

	Logger logger = LoggerFactory.getLogger(getClass());

	Mvcc mvcc;

	private ThreadLocal<PerThreadTransaction> transactions = new ThreadLocal() {
	};

	private TopicListener<Thread> resetListener = new TopicListener<Thread>() {
		@Override
		public void topicPublished(String key, Thread message) {
			transactions().transactionFinished();
		}
	};

	private TopicListener<Thread> persistingListener = new TopicListener<Thread>() {
		@Override
		public void topicPublished(String key, Thread message) {
			transactions().transactionCommitting();
		}
	};

	DetachedEntityCache cache;

	private DomainStorePersistenceListener persistenceListener;

	boolean initialised = false;

	private DomainStoreTransactions transactional = new DomainStoreTransactions();

	private Field modificationCheckerField;

	boolean initialising;

	private boolean debug;

	Multimap<Class, List<BaseProjectionHasEquivalenceHash>> cachingProjections = new Multimap<Class, List<BaseProjectionHasEquivalenceHash>>();

	DomainStoreThreads threads;

	long timzoneOffset = -1;

	Calendar startupCal = Calendar.getInstance();

	TimeZone startupTz = (TimeZone) startupCal.getTimeZone().clone();

	boolean publishMappingEvents;

	DomainTransformPersistenceEvent postProcessEvent;

	DomainStoreLoader loader;

	private UnsortedMultikeyMap<DomainStoreProperty> domainStoreProperties = new UnsortedMultikeyMap<>(
			2);

	private LazyObjectLoader lazyObjectLoader;

	private boolean writable;

	private DomainStoreDomainHandler handler;

	public String name;

	AtomicLong applyTxToGraphCounter = new AtomicLong(0);

	public DomainStore(DomainStoreDescriptor descriptor) {
		this();
		this.domainDescriptor = descriptor;
	}

	private DomainStore() {
		ThreadlocalTransformManager.topicTransformManagerWasReset()
				.add(resetListener);
		TransformPersister.persistingTransformsListenerDelta(persistingListener,
				true);
		persistenceListener = new DomainStorePersistenceListener();
		threads = new DomainStoreThreads(this);
		threads.maxLockQueueLength = ResourceUtilities
				.getInteger(DomainStore.class, "maxLockQueueLength", 120);
		threads.maxLockQueueTimeForNoDisablement = ResourceUtilities
				.getLong(DomainStore.class, "maxLockQueueTimeForNoDisablement");
		publishMappingEvents = ResourceUtilities.is(DomainStore.class,
				"publishMappingEvents");
		this.persistenceEvents = new DomainTransformPersistenceEvents(this);
		// this is where we multiplex...move to app?
		// FIXME - jade.ceres.2
		//
		// yeah - store handler should call stores() store for class() (a proxy)
		this.handler = new DomainStoreDomainHandler();
	}

	public void appShutdown() {
		threads.appShutdown();
		loader.appShutdown();
		persistenceEvents.getQueue().appShutdown();
	}

	public void dumpLocks() {
		threads.dumpLocks();
	}

	public void enableAndAddValues(DomainListener listener) {
		listener.setEnabled(true);
		addValues(listener);
	}

	public void externalReadLock(boolean lock) {
		if (lock) {
			threads.lock(false);
		} else {
			threads.unlock(false);
		}
	}

	public DetachedEntityCache getCache() {
		checkInLockedSection();
		return this.cache;
	}

	public DomainStoreDescriptor getDomainDescriptor() {
		return this.domainDescriptor;
	}

	public DomainStoreHealth getHealth() {
		return threads.health;
	}

	public String getLockDumpString(String lockDumpCause, boolean full) {
		return threads.getLockDumpString(lockDumpCause, full);
	}

	public MemoryStat getMemoryStats(StatType type) {
		MemoryStat top = new MemoryStat(this);
		top.setObjectMemory(Registry.impl(ObjectMemory.class));
		top.type = type;
		/*
		 * Add cache stats first (notionally it's the owner of the entities)
		 */
		cache.addMemoryStats(top);
		getDomainDescriptor().addMemoryStats(top);
		return top;
	}

	public Mvcc getMvcc() {
		return this.mvcc;
	}

	public DomainTransformPersistenceEvents getPersistenceEvents() {
		return this.persistenceEvents;
	}

	public DomainStorePersistenceListener getPersistenceListener() {
		return this.persistenceListener;
	}

	public DomainStoreTransformSequencer getTransformSequencer() {
		return loader.getTransformSequencer();
	}

	public DomainStoreInstrumentation instrumentation() {
		return threads.instrumentation;
	}

	public boolean isCached(Class clazz) {
		return domainDescriptor.perClass.containsKey(clazz);
	}

	public <T extends Entity> boolean isCached(Class<T> clazz, long id) {
		return cache.contains(clazz, id);
	}

	public boolean isCachedTransactional(Class clazz) {
		return isCached(clazz)
				&& domainDescriptor.perClass.get(clazz).isTransactional();
	}

	public boolean isCurrentThreadHoldingLock() {
		return threads.isCurrentThreadHoldingLock();
	}

	public boolean isDebug() {
		return this.debug;
	}

	public boolean isInitialised() {
		return initialised;
	}

	public List<DomainTransformRequestPersistent> loadTransformRequests(
			Collection<Long> ids, Logger logger) throws Exception {
		return loader.loadTransformRequests(ids, logger);
	}

	public <T extends Entity> void onLocalObjectCreated(T newInstance) {
		cache.put(newInstance);
	}

	public void onTransformsPersisted() {
		loader.onTransformsPersisted();
	}

	public void readLockExpectLongRunning(boolean lock) {
		threads.readLockExpectLongRunning(lock);
	}

	public void remove(Entity entity) {
		cache.remove(entity);
	}

	public void runWithWriteLock(Runnable runnable) {
		try {
			threads.lock(true);
			runnable.run();
		} finally {
			threads.unlock(true);
		}
	}

	/**
	 * Normally should be true, expect in warmup (where we know threads will be
	 * non-colliding)
	 */
	public void
			setCheckModificationWriteLock(boolean checkModificationWriteLock) {
		threads.checkModificationWriteLock = checkModificationWriteLock;
	}

	public void setConnectionUrl(String newUrl) {
		((DomainStoreLoaderDatabase) loader).setConnectionUrl(newUrl);
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	@Override
	public String toString() {
		return Ax.format("Domain store: %s %s", name,
				domainDescriptor.getClass().getSimpleName());
	}

	public DomainStoreTransactions transactions() {
		return transactional;
	}

	public void warmup() throws Exception {
		MetricLogging.get().start("domainStore.warmup");
		initialised = false;
		initialising = true;
		transformManager = new SubgraphTransformManagerPostProcess();
		lazyObjectLoader = loader.getLazyObjectLoader();
		cache = transformManager.getDetachedEntityCache();
		transformManager.getStore().setLazyObjectLoader(lazyObjectLoader);
		modificationCheckerField = BaseSourcesPropertyChangeEvents.class
				.getDeclaredField("propertyChangeSupport");
		modificationCheckerField.setAccessible(true);
		setCheckModificationWriteLock(false);
		domainDescriptor.registerStore(this);
		domainDescriptor.perClass.values().stream()
				.forEach(this::prepareClassDescriptor);
		mvcc = new Mvcc(this, domainDescriptor, cache);
		MetricLogging.get().start("mvcc");
		mvcc.init();
		MetricLogging.get().end("mvcc");
		Transaction.beginDomainPreparing();
		Transaction.current().setBaseTransaction(true, this);
		domainDescriptor.perClass.values().stream()
				.forEach(DomainClassDescriptor::initialise);
		loader.warmup();
		// loader responsible for this
		// Transaction.current().toCommitted();
		Transaction.end();
		initialising = false;
		initialised = true;
		threads.startLongLockHolderCheck();
		if (ResourceUtilities.is("checkAccessWithoutLock")) {
			threads.setupLockedAccessCheck();
		}
		MetricLogging.get().end("domainStore.warmup");
	}

	// FIXME.mvcc - useentityclass
	private void addToLazyPropertyEvictionQueue(Entity entity) {
		LazyPropertyLoadTask task = (LazyPropertyLoadTask) domainDescriptor
				.getPreProvideTasks(entity.provideEntityClass()).stream()
				.filter(ppt -> ppt instanceof LazyPropertyLoadTask).findFirst()
				.get();
		task.addToEvictionQueue(entity);
	}

	private <E extends Entity> void applyFilter(final Class clazz,
			DomainFilter filter, DomainFilter nextFilter, QueryToken<E> token) {
		ComplexFilter complexFilter = getComplexFilterFor(clazz, filter,
				nextFilter);
		if (complexFilter != null) {
			ComplexFilterContextImpl<E> filterContext = new ComplexFilterContextImpl<E>(
					clazz, token);
			token.stream = complexFilter.evaluate(filterContext, filter,
					nextFilter);
			token.idx += complexFilter.topLevelFiltersConsumed() - 1;
			if (isDebug()) {
				token.lastFilterString = String.format("Complex - %s - %s %s",
						complexFilter, filter, nextFilter);
			}
			return;
		}
		if (isDebug()) {
			token.lastFilterString = filter.toString();
		}
		DomainLookup lookup = getLookupFor(clazz, filter.propertyPath);
		if (lookup != null) {
			switch (filter.filterOperator) {
			case EQ:
			case IN:
				// TODO - mvcc.2 - if we have estimates of size, we might be
				// able to optimise here
				Set<E> lookupValues = lookup
						.getKeyMayBeCollection(filter.propertyValue);
				token.appendEvaluatedValueFilter(lookupValues);
				return;
			// all others non-optimised
			default:
				break;
			}
		}
		token.appendFilter(filter.asCollectionFilter());
	}

	private void doEvictions() {
		domainDescriptor.preProvideTasks
				.forEach(PreProvideTask::writeLockedCleanup);
	}

	private DomainTransformEvent
			filterForDomainStoreProperty(DomainTransformEvent dte) {
		switch (dte.getTransformType()) {
		case CREATE_OBJECT:
		case DELETE_OBJECT:
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION:
			return dte;
		}
		DomainStoreProperty ann = domainStoreProperties
				.get(dte.getObjectClass(), dte.getPropertyName());
		if (ann == null) {
			return dte;
		}
		if (!ann.translateObjectWritesToIdWrites()) {
			return null;
		}
		if (ann.toIdProperty().isEmpty()) {
			return dte;
		} else {
			DomainTransformEvent translated = ResourceUtilities
					.fieldwiseClone(dte, true, false);
			translated.setPropertyName(ann.toIdProperty());
			translated.setNewValue(translated.getValueId());
			TransformManager.get().convertToTargetObject(translated);
			translated.setTransformType(
					TransformType.CHANGE_PROPERTY_SIMPLE_VALUE);
			return translated;
		}
	}

	private List<DomainTransformEvent>
			filterInterestedTransforms(Collection<DomainTransformEvent> dtes) {
		return dtes.stream().filter(new InSubgraphFilter())
				.filter(domainDescriptor::customFilterPostProcess)
				.map(dte -> filterForDomainStoreProperty(dte))
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	private ComplexFilter getComplexFilterFor(Class clazz,
			DomainFilter... filters) {
		return domainDescriptor.complexFilters.stream()
				.filter(cf -> cf.handles(clazz, filters)).findFirst()
				.orElse(null);
	}

	private DomainLookup getLookupFor(Class clazz, String propertyName) {
		for (DomainStoreLookupDescriptor descriptor : domainDescriptor.perClass
				.get(clazz).lookupDescriptors) {
			if (descriptor.handles(clazz, propertyName)) {
				return descriptor.getLookup();
			}
		}
		return null;
	}

	private boolean isWillProjectLater() {
		return LooseContext.is(CONTEXT_WILL_PROJECT_AFTER_READ_LOCK);
	}

	private void prepareClassDescriptor(DomainClassDescriptor classDescriptor) {
		try {
			Class clazz = classDescriptor.clazz;
			List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>(
					Arrays.asList(Introspector.getBeanInfo(clazz)
							.getPropertyDescriptors()));
			for (PropertyDescriptor pd : pds) {
				if (pd.getReadMethod() == null || pd.getWriteMethod() == null) {
					continue;
				}
				Method rm = pd.getReadMethod();
				if ((rm.getAnnotation(Transient.class) != null
						&& rm.getAnnotation(DomainStoreDbColumn.class) == null)
						|| rm.getAnnotation(
								DomainStoreProperty.class) != null) {
					DomainStoreProperty propertyAnnotation = rm
							.getAnnotation(DomainStoreProperty.class);
					if (propertyAnnotation != null) {
						Field field = getField(clazz, pd.getName());
						field.setAccessible(true);
						domainStoreProperties.put(clazz, field.getName(),
								propertyAnnotation);
					}
					continue;
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	void addValues(DomainListener listener) {
		for (Object o : cache.values(listener.getListenedClass())) {
			listener.insert((Entity) o);
		}
	}

	<T extends Entity> T findRaw(Class<T> clazz, long id) {
		checkInLockedSection();
		T t = cache.get(clazz, id);
		if (t == null) {
			if (domainDescriptor.perClass.get(clazz).lazy && id != 0) {
				lazyObjectLoader.loadObject(clazz, id, 0);
			}
		}
		if (t != null) {
			for (PreProvideTask task : domainDescriptor
					.getPreProvideTasks(clazz)) {
				try {
					task.run(clazz, Collections.singletonList(t), true);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
		return t;
	}

	<T extends Entity> T findRaw(T t) {
		return (T) findRaw(t.provideEntityClass(), t.getId());
	}

	String getCanonicalPropertyPath(Class clazz, String propertyPath) {
		return domainDescriptor.perClass.get(clazz)
				.getCanonicalPropertyPath(propertyPath);
	}

	Field getField(Class clazz, String name) throws Exception {
		List<Field> fields = new GraphProjection().getFieldsForClass(clazz);
		Optional<Field> field = fields.stream()
				.filter(f -> f.getName().equals(name)).findFirst();
		if (!field.isPresent()) {
			throw new RuntimeException(
					String.format("Field not available - %s.%s",
							clazz.getSimpleName(), name));
		}
		return field.get();
	}

	void index(Entity obj, boolean add) {
		Class<? extends Entity> clazz = obj.provideEntityClass();
		if (obj instanceof DomainProxy) {
			clazz = (Class<? extends Entity>) clazz.getSuperclass();
		}
		DomainClassDescriptor<?> itemDescriptor = domainDescriptor.perClass
				.get(clazz);
		if (itemDescriptor != null) {
			itemDescriptor.index(obj, add);
			for (Entity dependentObject : itemDescriptor
					.getDependentObjectsWithDerivedProjections(obj)) {
				index(dependentObject, add);
			}
		}
	}

	<V extends Entity> boolean isRawValue(V v) {
		V existing = (V) cache.get(v.provideEntityClass(), v.getId());
		return existing == v;
	}

	// we only have one thread allowed here - but they won't start blocking the
	// reader thread
	// TODO - optimise!
	synchronized void
			postProcess(DomainTransformPersistenceEvent persistenceEvent) {
		DomainModificationMetadataProvider metadataProvider = persistenceEvent
				.getMetadataProvider();
		if (metadataProvider == null) {
			metadataProvider = new SourceMetadataProvider(this);
		}
		DteToLocatorMapper objectLocator = new DteToLocatorMapper();
		DteToLocatorMapper valueLocator = new DteToLocatorMapper(true);
		{
			List<DomainTransformEvent> dtes = (List) persistenceEvent
					.getDomainTransformLayerWrapper().persistentEvents;
			metadataProvider.registerTransforms(dtes);
			// preload outside of the writer lock - this uses a db conn, and
			// there
			// have been some odd networking issues...
			// note that since method is synchronized, nothing will be
			// evicted
			// from hereonin
			/*
			 * mvcc.2 - check that eviction thing
			 */
			List<DomainTransformEvent> filtered = filterInterestedTransforms(
					dtes);
			Multimap<EntityLocator, List<DomainTransformEvent>> perObjectTransforms = CollectionFilters
					.multimap(filtered, objectLocator);
			for (DomainTransformEvent dte : filtered) {
				DomainTransformEvent first = CommonUtils
						.first(perObjectTransforms
								.get(EntityLocator.objectLocator(dte)));
				DomainTransformEvent last = CommonUtils.last(perObjectTransforms
						.get(EntityLocator.objectLocator(dte)));
				if (dte.getTransformType() != TransformType.CREATE_OBJECT
						&& first == dte) {
					Entity obj = transformManager.getObject(dte, true);
				}
			}
		}
		Set<Throwable> causes = new LinkedHashSet<Throwable>();
		StringBuilder warnBuilder = new StringBuilder();
		long postProcessStart = 0;
		DomainStoreHealth health = getHealth();
		try {
			LooseContext.pushWithTrue(
					TransformManager.CONTEXT_DO_NOT_POPULATE_SOURCE);
			postProcessStart = System.currentTimeMillis();
			MetricLogging.get().start("post-process");
			Map<Long, Entity> createdLocalsSnapshot = persistenceEvent
					.isLocalToVm()
					&& !persistenceEvent.getTransformPersistenceToken()
							.isAsyncClient() ? cache.getCreatedLocalsSnapshot()
									: Collections.emptyMap();
			transformManager.setLocalReplacementCreationObjectResolver(
					localId -> createdLocalsSnapshot.get(localId));
			Transaction.ensureEnded();
			Transaction.beginDomainPreparing();
			Date transactionCommitTime = persistenceEvent
					.getDomainTransformLayerWrapper().persistentRequests.get(0)
							.getTransactionCommitTime();
			Transaction.current().toDomainCommitting(
					testSensitiveTimestamp(transactionCommitTime), this,
					applyTxToGraphCounter.getAndIncrement());
			threads.postProcessWriterThread = Thread.currentThread();
			postProcessEvent = persistenceEvent;
			health.domainStorePostProcessStartTime = System.currentTimeMillis();
			transformManager.startCommit();
			List<DomainTransformEvent> dtes = (List) persistenceEvent
					.getDomainTransformLayerWrapper().persistentEvents;
			List<DomainTransformEvent> filtered = filterInterestedTransforms(
					dtes);
			Multimap<EntityLocator, List<DomainTransformEvent>> perObjectTransforms = CollectionFilters
					.multimap(filtered, objectLocator);
			Set<EntityLocator> createdAndDeleted = perObjectTransforms
					.entrySet().stream().filter(e -> {
						DomainTransformEvent first = e.getValue().get(0);
						DomainTransformEvent last = CommonUtils
								.last(e.getValue());
						return last
								.getTransformType() == TransformType.DELETE_OBJECT
								&& first.getTransformType() == TransformType.CREATE_OBJECT;
					}).map(e -> e.getKey()).collect(Collectors.toSet());
			if (createdAndDeleted.size() > 0) {
				filtered.removeIf(dte -> createdAndDeleted
						.contains(objectLocator.getKey(dte))
						|| createdAndDeleted
								.contains(valueLocator.getKey(dte)));
			}
			Set<Entity> indexAtEnd = new LinkedHashSet<>();
			metadataProvider.registerTransforms(filtered);
			Set<Long> uncommittedToLocalGraphLids = new LinkedHashSet<Long>();
			for (DomainTransformEvent dte : filtered) {
				dte.setNewValue(null);// force a lookup from the subgraph
			}
			TransformManager.get().setIgnorePropertyChanges(true);
			for (DomainTransformEvent dte : filtered) {
				// remove from indicies before first change - and only if
				// preexisting object
				DomainTransformEvent first = CommonUtils
						.first(perObjectTransforms
								.get(EntityLocator.objectLocator(dte)));
				DomainTransformEvent last = CommonUtils.last(perObjectTransforms
						.get(EntityLocator.objectLocator(dte)));
				if (last.getTransformType() == TransformType.DELETE_OBJECT
						&& first.getTransformType() != TransformType.CREATE_OBJECT) {
					// this a check against deletion during cache warmup.
					// shouldn't happen anyway (trans. isolation)
					// TODO - check if necessary
					// (note) also a check against trying to handle deletion of
					// lazy objects
					Entity domainStoreObj = transformManager.getObject(dte,
							true);
					if (domainStoreObj == null) {
						continue;
					}
				}
				if (dte.getTransformType() != TransformType.CREATE_OBJECT
						&& first == dte) {
					Entity obj = transformManager.getObject(dte, true);
					if (obj != null) {
						index(obj, false);
					} else {
						logger.warn("Null domain store object for index - {}",
								EntityLocator.objectLocator(dte));
					}
				}
				try {
					transformManager.apply(dte);
				} catch (DomainTransformException dtex) {
					if (dtex.getType() == DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND
							&& dte.getTransformType() == TransformType.DELETE_OBJECT) {
						warnBuilder.append(String.format("%s\n%s\n\n", dte,
								dtex.getType(), dtex.getMessage()));
					} else if (dtex
							.getType() == DomainTransformExceptionType.TARGET_ENTITY_NOT_FOUND
							&& dte.getTransformType() == TransformType.REMOVE_REF_FROM_COLLECTION) {
						warnBuilder.append(String.format("%s\n%s\n\n", dte,
								dtex.getType(), dtex.getMessage()));
					} else {
						causes.add(dtex);
					}
				}
				if (dte.getTransformType() != TransformType.DELETE_OBJECT
						&& last == dte) {
					Entity domainStoreObject = transformManager.getObject(dte,
							true);
					if (domainStoreObject != null) {
						metadataProvider.updateMetadata(dte, domainStoreObject);
						index(domainStoreObject, true);
					} else {
						logger.warn("Null domain store object for index - {}",
								EntityLocator.objectLocator(dte));
					}
				}
				if (dte.getPropertyName() != null) {
					DomainStoreProperty ann = domainStoreProperties
							.get(dte.getObjectClass(), dte.getPropertyName());
					if (ann != null && ann
							.loadType() == DomainStorePropertyLoadType.LAZY) {
						addToLazyPropertyEvictionQueue(
								transformManager.getObject(dte, true));
					}
				}
			}
			indexAtEnd.forEach(domainStoreObject -> {
				List<DomainTransformEvent> list = perObjectTransforms
						.get(new EntityLocator(domainStoreObject));
				DomainTransformEvent last = list == null ? null
						: CommonUtils.last(list);
				if (last != null && last
						.getTransformType() == TransformType.DELETE_OBJECT) {
				} else {
					index(domainStoreObject, true);
				}
			});
			doEvictions();
			Transaction.current().toDomainCommitted();
		} catch (Exception e) {
			Transaction.current().toDomainAborted();
			causes.add(e);
		} finally {
			transformManager.endCommit();
			TransformManager.get().setIgnorePropertyChanges(false);
			health.domainStorePostProcessStartTime = 0;
			threads.postProcessWriterThread = null;
			postProcessEvent = null;
			long postProcessTime = System.currentTimeMillis()
					- postProcessStart;
			health.domainStoreMaxPostProcessTime = Math
					.max(health.domainStoreMaxPostProcessTime, postProcessTime);
			MetricLogging.get().end("post-process", metricLogger);
			Transaction.endAndBeginNew();
			try {
				if (warnBuilder.length() > 0) {
					Exception warn = new Exception(warnBuilder.toString());
					System.out.println(warn);
					warn.printStackTrace();
					AlcinaTopics.notifyDevWarning(warn);
				}
				if (!causes.isEmpty()) {
					UmbrellaException umby = new UmbrellaException(causes);
					causes.iterator().next().printStackTrace();
					DomainStoreUpdateException updateException = new DomainStoreUpdateException(
							umby);
					topicUpdateException().publish(updateException);
					if (updateException.ignoreForDomainStoreExceptionCount) {
						updateException.printStackTrace();
					} else {
						health.domainStoreExceptionCount.incrementAndGet();
						throw new DomainStoreException(umby);
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			LooseContext.pop();
		}
	}

	<T extends Entity> Stream<T> query(Class<T> clazz,
			DomainStoreQuery<T> query) {
		try {
			threads.lock(false);
			boolean debugMetrics = isDebug()
					&& LooseContext.is(CONTEXT_DEBUG_QUERY_METRICS);
			StringBuilder debugMetricBuilder = new StringBuilder();
			int filterSize = query.getFilters().size();
			QueryToken token = new QueryToken(query);
			token.planner().optimiseFilters();
			for (; token.idx < filterSize; token.idx++) {
				int idx = token.idx;
				long start = System.nanoTime();
				DomainFilter cacheFilter = query.getFilters().get(idx);
				DomainFilter nextFilter = idx == filterSize - 1 ? null
						: query.getFilters().get(idx + 1);
				applyFilter(clazz, cacheFilter, nextFilter, token);
				if (debugMetrics) {
					double ms = (double) (System.nanoTime() - start)
							/ 1000000.0;
					String filters = token.lastFilterString;
					debugMetricBuilder.append(String.format("\t%.3f ms - %s\n",
							ms, CommonUtils.trimToWsChars(filters, 100, true)));
				}
				if (token.isEmpty()) {
					break;
				}
			}
			if (debugMetrics && !token.hasIdQuery()) {
				metricLogger.debug("Query metrics:\n========\n{}\n{}", query,
						debugMetricBuilder.toString());
			}
			// FIXME - mvcc.2 - remove isRaw() etc (always raw)
			if (query.isRaw() || isWillProjectLater()) {
				Stream stream = token.ensureStream();
				List<PreProvideTask<T>> preProvideTasks = domainDescriptor
						.getPreProvideTasks(clazz);
				for (PreProvideTask<T> preProvideTask : preProvideTasks) {
					stream = preProvideTask.wrap(stream);
				}
				return stream;
			} else {
				throw new UnsupportedOperationException();
			}
		} finally {
			threads.unlock(false);
		}
	}

	public static class Builder {
		private DomainStoreDescriptor descriptor;

		private ThreadPoolExecutor warmupExecutor;

		private RetargetableDataSource dataSource;

		private DomainLoaderType loaderType;

		private boolean readOnly = false;

		private String name = "store.default";

		public DomainStore register() {
			DomainStore domainStore = new DomainStore();
			domainStore.domainDescriptor = descriptor;
			domainStore.writable = !readOnly;
			domainStore.name = name;
			Preconditions.checkNotNull(loaderType);
			switch (loaderType) {
			case Database:
				domainStore.loader = new DomainStoreLoaderDatabase(domainStore,
						dataSource, warmupExecutor);
				break;
			default:
				throw new UnsupportedOperationException();
			}
			stores().register(domainStore);
			return domainStore;
		}

		public Builder withDomainDescriptor(DomainStoreDescriptor descriptor) {
			this.descriptor = descriptor;
			return this;
		}

		public Builder withLoaderDatabase(ThreadPoolExecutor warmupExecutor,
				RetargetableDataSource dataSource) {
			this.warmupExecutor = warmupExecutor;
			this.dataSource = dataSource;
			loaderType = DomainLoaderType.Database;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		enum DomainLoaderType {
			Database, Remote;
		}
	}

	public static class DomainStoreException extends RuntimeException {
		public DomainStoreException(Exception e) {
			super(e);
		}

		public DomainStoreException(String message) {
			super(message);
		}
	}

	@RegistryLocation(registryPoint = DomainStores.class, implementationType = ImplementationType.SINGLETON)
	public static class DomainStores implements RegistrableService {
		// not concurrent, handle in methods
		private Map<DomainDescriptor, DomainStore> descriptorMap = new LinkedHashMap<>();

		private Map<Class, DomainStore> classMap = new LinkedHashMap<>();

		private DomainStore writableStore;

		DomainStoresDomainHandler storesHandler = new DomainStoresDomainHandler();

		private DomainStores() {
			Domain.registerHandler(storesHandler);
		}

		@Override
		public void appShutdown() {
			descriptorMap.values().forEach(DomainStore::appShutdown);
		}

		public synchronized boolean hasInitialisedDatabaseStore() {
			return writableStore0() != null && writableStore0().initialised;
		}

		public synchronized boolean
				isInitialised(DomainDescriptor domainDescriptor) {
			return descriptorMap.containsKey(domainDescriptor)
					&& descriptorMap.get(domainDescriptor).initialised;
		}

		public <V extends Entity> DomainStoreQuery<V> query(Class<V> clazz) {
			return new DomainStoreQuery(clazz, storeFor(clazz));
		}

		public synchronized void register(DomainStore store) {
			descriptorMap.put(store.domainDescriptor, store);
			store.domainDescriptor.getHandledClasses().forEach(clazz -> {
				Preconditions.checkState(!classMap.containsKey(clazz));
				classMap.put(clazz, store);
			});
		}

		public DomainStore storeFor(Class clazz) {
			return classMap.get(clazz);
		}

		public synchronized DomainStore
				storeFor(DomainDescriptor domainDescriptor) {
			return descriptorMap.get(domainDescriptor);
		}

		public synchronized DomainStore
				storeFor(String domainDescriptorClassName) {
			DomainDescriptor domainDescriptor = descriptorMap.keySet().stream()
					.filter(dd -> dd.getClass().getName()
							.equals(domainDescriptorClassName))
					.findFirst().get();
			return storeFor(domainDescriptor);
		}

		public Stream<DomainStore> stream() {
			return descriptorMap.values().stream();
		}

		public DomainStore writableStore() {
			DomainStore store = writableStore0();
			Preconditions.checkNotNull(store);
			return store;
		}

		private DomainStore writableStore0() {
			if (writableStore == null) {
				writableStore = descriptorMap.values().stream()
						.filter(d -> d.writable).findFirst().orElse(null);
			}
			return writableStore;
		}

		class DomainStoresDomainHandler implements DomainHandler {
			@Override
			public <V extends Entity> void async(Class<V> clazz, long objectId,
					boolean create, Consumer<V> resultConsumer) {
				storeHandler(clazz).async(clazz, objectId, create,
						resultConsumer);
			}

			@Override
			public <V extends Entity> V byProperty(Class<V> clazz,
					String propertyName, Object value) {
				return storeHandler(clazz).byProperty(clazz, propertyName,
						value);
			}

			@Override
			public void commitPoint() {
				// Noop
			}

			@Override
			public <V extends Entity> V detachedVersion(V v) {
				return v == null ? null
						: storeHandler(v.provideEntityClass())
								.detachedVersion(v);
			}

			@Override
			public <V extends Entity> V find(Class clazz, long id) {
				return storeHandler(clazz).find(clazz, id);
			}

			@Override
			public <V extends Entity> V find(V v) {
				return v == null ? null
						: storeHandler(v.provideEntityClass()).find(v);
			}

			@Override
			public <V extends Entity> List<Long> ids(Class<V> clazz) {
				return storeHandler(clazz).ids(clazz);
			}

			@Override
			public <V extends Entity> boolean isDomainVersion(V v) {
				return v == null ? null
						: storeHandler(v.provideEntityClass())
								.isDomainVersion(v);
			}

			@Override
			public <V extends Entity> DomainQuery<V> query(Class<V> clazz) {
				return storeHandler(clazz).query(clazz);
			}

			@Override
			public <V extends Entity> V resolveTransactional(
					DomainListener listener, V value, Object[] path) {
				return value;
				// DomainStore domainStore = (DomainStore) listener
				// .getDomainStore();
				// if (domainStore == null || domainStore.handler == null) {
				// // localdomain
				// return value;
				// } else {
				// return domainStore.handler.resolveTransactional(listener,
				// value, path);
				// }
			}

			@Override
			public <V extends Entity> long size(Class<V> clazz) {
				return storeHandler(clazz).size(clazz);
			}

			@Override
			public <V extends Entity> Stream<V> stream(Class<V> clazz) {
				return storeHandler(clazz).stream(clazz);
			}

			@Override
			public <V extends Entity> V transactionalFind(Class clazz,
					long id) {
				return storeHandler(clazz).transactionalFind(clazz, id);
			}

			@Override
			public <V extends Entity> V transactionalVersion(V v) {
				return v == null ? null
						: storeHandler(v.provideEntityClass())
								.transactionalVersion(v);
			}

			@Override
			public <V extends Entity> Collection<V> values(Class<V> clazz) {
				return storeHandler(clazz).values(clazz);
			}

			@Override
			public <V extends Entity> V writeable(V v) {
				return v == null ? null
						: storeHandler(v.provideEntityClass()).writeable(v);
			}

			DomainHandler storeHandler(Class clazz) {
				return classMap.get(clazz).handler;
			}
		}
	}

	/*
	 * Will be removed (mvcc)
	 */
	public class DomainStoreTransactions {
		public volatile int transactionCount;

		Set<Long> activeTransactionThreadIds = new LinkedHashSet<Long>();

		public void ensureReferredPropertyIsTransactional(Entity entity,
				String propertyName) {
			// target, even if new object, will still be equals() to old, so no
			// property change will be fired, which is the desired behaviour
			Entity target = (Entity) Reflections.propertyAccessor()
					.getPropertyValue(entity, propertyName);
			if (target != null) {
				target = ensureTransactional(target);
				Reflections.propertyAccessor().setPropertyValue(entity,
						propertyName, target);
			}
		}

		public PerThreadTransaction ensureTransaction() {
			PerThreadTransaction transaction = transactions.get();
			if (transaction == null) {
				Set<DomainTransformEvent> localTransforms = TransformManager
						.get()
						.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
				int pendingTransformCount = localTransforms.size();
				if (pendingTransformCount != 0
						&& !AppPersistenceBase.isTest()) {
					for (DomainTransformEvent dte : localTransforms) {
						if (domainDescriptor.perClass.keySet()
								.contains(dte.getObjectClass())) {
							throw new DomainStoreException(String.format(
									"Starting a domain store transaction with an existing transform of a graphed object - %s."
											+ " In certain cases that might work -- "
											+ "but better practice to not do so. All transforms: \n%s",
									dte, localTransforms));
						}
					}
				}
				transaction = Registry.impl(PerThreadTransaction.class);
				transaction.store = DomainStore.this;
				transactions.set(transaction);
				synchronized (this) {
					activeTransactionThreadIds
							.add(Thread.currentThread().getId());
					transactionCount = activeTransactionThreadIds.size();
				}
				transaction.start();
			}
			return transaction;
		}

		public <V extends Entity> V ensureTransactional(V value) {
			if (value == null) {
				return null;
			}
			if (!transactionActiveInCurrentThread()) {
				return value;
			}
			if (value.getId() == 0) {
				return value;
			}
			return transactions.get().ensureTransactional(value);
		}

		public <T> T find(Class<T> clazz, long id) {
			checkInLockedSection();
			T t = cache.get(clazz, id);
			if (transactionActiveInCurrentThread() && t != null) {
				return (T) transactions.get().ensureTransactional((Entity) t);
			} else {
				return t;
			}
		}

		/*
		 * FIXME - mvcc - make this the TransactionalMap.values
		 */
		public Set immutableRawValues(Class clazz) {
			return cache.values(clazz);
		}

		public <T> Map<Long, T> lookup(Class<T> clazz) {
			checkInLockedSection();
			return (Map<Long, T>) cache.getMap(clazz);
		}

		public void requireActiveTransaction() {
			if (!transactionActiveInCurrentThread()) {
				throw new RuntimeException(
						"requires transaction in current thread");
			}
		}

		public <V extends Entity> V resolveTransactional(
				DomainListener listener, V value, Object[] path) {
			PerThreadTransaction perThreadTransaction = transactions.get();
			if (perThreadTransaction == null || (value != null
					&& !isCached(value.provideEntityClass()))) {
				return value;
			}
			return perThreadTransaction.getListenerValue(listener, value, path);
		}

		public boolean transactionActiveInCurrentThread() {
			return transactionsActive() && transactions.get() != null;
		}

		public void transactionCommitting() {
			PerThreadTransaction transaction = transactions.get();
			if (transaction != null) {
				transaction.committing();
			}
		}

		public void transactionFinished() {
			PerThreadTransaction transaction = transactions.get();
			if (transaction != null) {
				transaction.end();
				for (BaseProjectionHasEquivalenceHash listener : cachingProjections
						.allItems()) {
					listener.onTransactionEnd();
				}
				transactions.remove();
				synchronized (this) {
					activeTransactionThreadIds
							.remove(Thread.currentThread().getId());
					transactionCount = activeTransactionThreadIds.size();
				}
			}
		}

		public boolean transactionsActive() {
			return transactionCount != 0;
		}
	}

	public static class DomainStoreUpdateException extends Exception {
		public UmbrellaException umby;

		public boolean ignoreForDomainStoreExceptionCount;

		public DomainStoreUpdateException(UmbrellaException umby) {
			super("Domain store update exception - ignoreable", umby);
			this.umby = umby;
		}
	}
	/*
	 * Note - not synchronized - per-thread access only
	 */

	public static class RawValueReplacer<I> extends DomainReader<I, I> {
		private DomainStore domainStore;

		public RawValueReplacer(DomainStore domainStore) {
			this.domainStore = domainStore;
		}

		@Override
		protected I read0(I input) throws Exception {
			if (input == null) {
				return null;
			}
			checkInLockedSection();
			List<Field> fields = new GraphProjection()
					.getFieldsForClass(input.getClass());
			for (Field field : fields) {
				if (Entity.class.isAssignableFrom(field.getType())) {
					Entity value = (Entity) field.get(input);
					if (value != null) {
						I raw = (I) domainStore.cache.get(field.getType(),
								value.getId());
						field.set(input, raw);
					}
				}
			}
			return input;
		}
	}

	private class ComplexFilterContextImpl<E extends Entity>
			implements ComplexFilterContext<E> {
		private final Class clazz;

		private final QueryToken<E> token;

		private ComplexFilterContextImpl(Class clazz, QueryToken<E> token) {
			this.clazz = clazz;
			this.token = token;
		}

		@Override
		public Stream<E> getEntitiesForIds(Set<Long> ids) {
			return (Stream<E>) (Stream<?>) ids.stream()
					.map(id -> cache.get(this.clazz, id));
		}

		@Override
		public Stream<E> getIncomingStream() {
			return this.token.stream;
		}

		@Override
		public <E2 extends Entity> ComplexFilterContext<E2>
				getOtherEntityFilterContext(Class<E2> otherEntityClass) {
			return new ComplexFilterContextImpl(otherEntityClass, token);
		}

		@Override
		public <E2 extends Entity> DomainLookup<String, E2>
				getStringLookup(Class<E2> clazz, String propertyPath) {
			return getLookupFor(clazz, propertyPath);
		}
	}

	class DetachedCacheObjectStorePsAware extends DetachedCacheObjectStore {
		public DetachedCacheObjectStorePsAware() {
			super(new PropertyStoreAwareMultiplexingObjectCache());
		}

		@Override
		public void mapObject(Entity obj) {
			if (publishMappingEvents) {
				topicMappingEvent().publish(obj);
			}
			super.mapObject(obj);
		}
	}

	class DomainQueryPlanner<E extends Entity> {
		private QueryToken<E> token;

		public DomainQueryPlanner(QueryToken<E> queryToken) {
			this.token = queryToken;
		}

		public void optimiseFilters() {
			DomainQuery<E> query = token.query;
			List<DomainFilter> filters = query.getFilters();
			List<FilterCostTuple> filterCosts = new ArrayList<>();
			Class<E> clazz = query.getEntityClass();
			int entityCount = cache.size(clazz);
			for (int idx = 0; idx < filters.size();) {
				DomainFilter filter = query.getFilters().get(idx);
				DomainFilter nextFilter = idx == filters.size() - 1 ? null
						: query.getFilters().get(idx + 1);
				ComplexFilter complexFilter = getComplexFilterFor(clazz, filter,
						nextFilter);
				List<DomainFilter> consumedFilters = Collections
						.singletonList(filter);
				FilterCost filterCost = null;
				if (complexFilter != null) {
					filterCost = complexFilter.estimateFilterCost(entityCount,
							filter, nextFilter);
					int filtersConsumed = complexFilter
							.topLevelFiltersConsumed();
					switch (filtersConsumed) {
					case 1:
						break;
					case 2:
						consumedFilters = Arrays.asList(filter, nextFilter);
						break;
					default:
						throw new UnsupportedOperationException();
					}
					idx += filtersConsumed;
				} else {
					DomainLookup lookup = getLookupFor(clazz,
							filter.propertyPath);
					if (lookup != null) {
						switch (filter.filterOperator) {
						case EQ:
						case IN:
							filterCost = lookup.estimateFilterCost(entityCount,
									filter);
							break;
						// all others non-optimised
						default:
							break;
						}
					}
					if (filterCost == null) {
						filterCost = FilterCost.evaluatorProjectionCost();
					}
					idx++;
				}
				filterCosts
						.add(new FilterCostTuple(consumedFilters, filterCost));
			}
			Collections.sort(filterCosts,
					Comparator.comparing(fc -> fc.filterCost.naiveOrdering()));
			filters.clear();
			filterCosts.stream().map(fct -> fct.filters)
					.flatMap(Collection::stream).forEach(filters::add);
		}

		class FilterCostTuple {
			List<DomainFilter> filters;

			FilterCost filterCost;

			public FilterCostTuple(List<DomainFilter> filters,
					FilterCost filterCost) {
				this.filters = filters;
				this.filterCost = filterCost;
			}
		}
	}

	class DomainStoreDomainHandler implements DomainHandler {
		@Override
		public <V extends Entity> void async(Class<V> clazz, long objectId,
				boolean create, Consumer<V> resultConsumer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends Entity> V byProperty(Class<V> clazz,
				String propertyName, Object value) {
			return Domain.query(clazz).raw().filter(propertyName, value).find();
		}

		@Override
		public void commitPoint() {
			// do nothing, assume explicit commit in servlet layer
		}

		@Override
		public <V extends Entity> V detachedVersion(V v) {
			return (V) Domain.query(v.provideEntityClass())
					.filterById(v.getId()).find();
		}

		@Override
		public <V extends Entity> V find(Class clazz, long id) {
			return (V) findRaw(clazz, id);
		}

		@Override
		public <V extends Entity> V find(V v) {
			checkInLockedSection();
			if (!v.provideWasPersisted()) {
				if (v.getLocalId() == 0) {
					return null;
				}
				EntityLocator locator = ThreadlocalTransformManager.get()
						.resolvePersistedLocal(DomainStore.this, v);
				if (locator == null) {
					return null;
				} else {
					return (V) cache.get(v.provideEntityClass(), locator.id);
				}
			} else {
				return (V) cache.get(v.provideEntityClass(), v.getId());
			}
		}

		@Override
		@DomainStoreUnsafe
		public <V extends Entity> List<Long> ids(Class<V> clazz) {
			try {
				threads.lock(false);
				return new ArrayList<Long>(cache.keys(clazz));
			} finally {
				threads.unlock(false);
			}
		}

		@Override
		public <V extends Entity> boolean isDomainVersion(V v) {
			return isRawValue(v);
		}

		@Override
		public <V extends Entity> List<V> listByProperty(Class<V> clazz,
				String propertyName, Object value) {
			return Domain.query(clazz).raw().filter(propertyName, value).list();
		}

		@Override
		public <V extends Entity> DomainQuery<V> query(Class<V> clazz) {
			return new DomainStoreQuery<>(clazz, DomainStore.this);
		}

		@Override
		public <V extends Entity> V resolve(V v) {
			return Transactions.resolve(v, false, false);
		}

		@Override
		public <V extends Entity> V resolveTransactional(
				DomainListener listener, V value, Object[] path) {
			return transactions().resolveTransactional(listener, value, path);
		}

		@Override
		public <V extends Entity> long size(Class<V> clazz) {
			return cache.size(clazz);
		}

		@Override
		public <V extends Entity> Stream<V> stream(Class<V> clazz) {
			return Domain.query(clazz).stream();
		}

		@Override
		public <V extends Entity> V transactionalFind(Class clazz, long id) {
			return (V) transactions().find(clazz, id);
		}

		@Override
		public <V extends Entity> V transactionalVersion(V v) {
			return (V) transactions().ensureTransactional(v);
		}

		@Override
		public <V extends Entity> Collection<V> values(Class<V> clazz) {
			checkInLockedSection();
			return cache.immutableRawValues(clazz);
		}

		@Override
		public <V extends Entity> V writeable(V v) {
			V out = v;
			if (out == null) {
				return null;
			}
			if (ThreadlocalTransformManager.is() && ThreadlocalTransformManager
					.get().isListeningTo((SourcesPropertyChangeEvents) out)) {
				return out;
			}
			if (out.provideWasPersisted()) {
				out = project(out);
			} else {
				out = Domain.detachedToDomain(out);
			}
			if (ThreadlocalTransformManager.is()) {
				ThreadlocalTransformManager.get()
						.listenTo((SourcesPropertyChangeEvents) out);
			}
			return out;
		}

		private <V extends Entity> V project(V v) {
			if (LooseContext.has(CONTEXT_WRITEABLE_PROJECTOR)) {
				Function<V, V> projector = LooseContext
						.get(CONTEXT_WRITEABLE_PROJECTOR);
				return projector.apply(v);
			} else {
				return GraphProjections.defaultProjections().project(v);
			}
		}

		<T extends Entity> List<T> list(Class<T> clazz) {
			return Domain.query(clazz).raw().list();
		}
	}

	class DomainStorePersistenceListener
			implements DomainTransformPersistenceListener {
		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent evt) {
			switch (evt.getPersistenceEventType()) {
			case PRE_COMMIT:
				break;
			case COMMIT_ERROR:
				break;
			case COMMIT_OK:
				postProcess(evt);
				break;
			}
		}
	}

	static class IndexingTransformListener implements DomainTransformListener {
		@Override
		public void domainTransform(DomainTransformEvent event)
				throws DomainTransformException {
			if (event.getCommitType() != CommitType.TO_LOCAL_BEAN) {
				return;
			}
			DomainStore store = DomainStore.stores()
					.storeFor(event.getObjectClass());
			Entity object = TransformManager.get().getObject(event);
			if (event.getTransformType() != TransformType.CREATE_OBJECT) {
				switch (event.getTransformType()) {
				case CHANGE_PROPERTY_REF:
				case CHANGE_PROPERTY_SIMPLE_VALUE:
				case NULL_PROPERTY_REF: {
					TransformManager.get().setIgnorePropertyChanges(true);
					/*
					 * undo last property change
					 */
					Reflections.propertyAccessor().setPropertyValue(object,
							event.getPropertyName(), event.getOldValue());
					store.index(object, false);
					/*
					 * redo
					 */
					Reflections.propertyAccessor().setPropertyValue(object,
							event.getPropertyName(), event.getNewValue());
					TransformManager.get().setIgnorePropertyChanges(false);
					break;
				}
				default:
					store.index(object, false);
				}
			}
			if (event.getTransformType() != TransformType.DELETE_OBJECT) {
				store.index(object, true);
			}
		}
	}

	class InSubgraphFilter implements CollectionFilter<DomainTransformEvent> {
		@Override
		public boolean allow(DomainTransformEvent o) {
			if (!domainDescriptor.applyPostTransform(o.getObjectClass(), o)) {
				return false;
			}
			switch (o.getTransformType()) {
			case ADD_REF_TO_COLLECTION:
			case REMOVE_REF_FROM_COLLECTION:
			case CHANGE_PROPERTY_REF:
				return GraphProjection.isEnumOrEnumSubclass(o.getValueClass())
						|| domainDescriptor
								.applyPostTransform(o.getValueClass(), o);
			}
			return true;
		}
	}

	@FunctionalInterface
	interface LocalReplacementCreationObjectResolver
			extends Function<Long, Entity> {
	}

	class QueryToken<E extends Entity> {
		boolean empty = false;

		int idx = 0;

		public String lastFilterString;

		Stream<E> stream;

		DomainQuery<E> query;

		DomainQueryPlanner planner;

		public QueryToken(DomainStoreQuery<E> query) {
			this.query = query;
		}

		public void appendEvaluatedValueFilter(Set<E> values) {
			if (values == null || values.size() == 0) {
				empty = true;
				stream = Stream.empty();
			} else {
				if (stream == null) {
					stream = values.stream();
				} else {
					stream = stream.filter(values::contains);
				}
			}
		}

		public void appendFilter(Predicate predicate) {
			stream = ensureStream().filter(predicate);
		}

		public Stream<E> ensureStream() {
			if (stream == null) {
				if (query.getSourceStream().isPresent()) {
					stream = query.getSourceStream().get();
				} else {
					stream = streamFromCacheValues();
				}
				if (LooseContext.has(DomainQuery.CONTEXT_DEBUG_CONSUMER)) {
					stream.peek(LooseContext
							.get(DomainQuery.CONTEXT_DEBUG_CONSUMER));
				}
			}
			return stream;
		}

		public boolean hasIdQuery() {
			return query.getFilters().stream()
					.anyMatch(filter -> filter.propertyPath.equals("id"));
		}

		public boolean isEmpty() {
			return empty;
		}

		public DomainQueryPlanner planner() {
			if (planner == null) {
				planner = new DomainQueryPlanner(this);
			}
			return planner;
		}

		private Stream<E> streamFromCacheValues() {
			return cache.stream(query.getEntityClass());
		}

		DomainStore getStore() {
			return DomainStore.this;
		}
	}

	class SubgraphTransformManagerPostProcess extends SubgraphTransformManager {
		private LocalReplacementCreationObjectResolver localReplacementCreationObjectResolver;

		public void addPropertyStore(DomainClassDescriptor descriptor) {
			((PropertyStoreAwareMultiplexingObjectCache) store.getCache())
					.addPropertyStore(descriptor);
		}

		@Override
		public Entity getObject(DomainTransformEvent dte,
				boolean ignoreSource) {
			if (dte.getTransformType() == TransformType.CREATE_OBJECT) {
				// avoid fallback to lazy loader
				return null;
			}
			return super.getObject(dte, ignoreSource);
		}

		public void setLocalReplacementCreationObjectResolver(
				LocalReplacementCreationObjectResolver localReplacementCreationObjectResolver) {
			this.localReplacementCreationObjectResolver = localReplacementCreationObjectResolver;
		}

		@Override
		protected void beforeDirectCollectionModification(Entity obj,
				String propertyName, Object newTargetValue,
				CollectionModificationType collectionModificationType) {
			Transactions.resolve(obj, true, false);
		}

		@Override
		protected void createObjectLookup() {
			store = new DetachedCacheObjectStorePsAware();
			setDomainObjects(store);
		}

		@Override
		protected Entity getEntityForCreate(DomainTransformEvent event) {
			Entity localReplacement = localReplacementCreationObjectResolver
					.apply(event.getObjectLocalId());
			if (localReplacement != null) {
				localReplacement.setId(event.getObjectId());
				// this has to happen after setId, since that will create the
				// (local) version in the cache
				store.getCache().removeLocal(localReplacement);
				store.getCache().put(localReplacement);
				TransformManager.registerLocalObjectPromotion(localReplacement);
			}
			return localReplacement;
		}

		@Override
		protected boolean isZeroCreatedObjectLocalId(Class clazz) {
			return true;
		}

		@Override
		protected void performDeleteObject(Entity entity) {
			super.performDeleteObject(entity);
		}

		void endCommit() {
			((PropertyStoreAwareMultiplexingObjectCache) store.getCache())
					.endCommit();
		}

		void startCommit() {
			((PropertyStoreAwareMultiplexingObjectCache) store.getCache())
					.startCommit();
		}
	}
}
