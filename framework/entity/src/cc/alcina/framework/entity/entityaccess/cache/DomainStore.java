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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Transient;
import javax.sql.DataSource;

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
import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.domain.MemoryStat;
import cc.alcina.framework.common.client.domain.MemoryStat.ObjectMemory;
import cc.alcina.framework.common.client.domain.MemoryStat.StatType;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
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
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager.AssociationPropogationTransformListener;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvents;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceListener;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.TransformPersister;
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

	// while debugging, prevent reentrant locks
	public static final String CONTEXT_NO_LOCKS = DomainStore.class.getName()
			+ ".CONTEXT_NO_LOCKS";

	public static final Logger LOGGER_WRAPPED_OBJECT_REF_INTEGRITY = AlcinaLogUtils
			.getTaggedLogger(DomainStore.class, "wrapped_object_ref_integrity");
	static {
		ThreadlocalTransformManager.addThreadLocalDomainTransformListener(
				new AssociationPropogationTransformListener());
		ThreadlocalTransformManager.addThreadLocalDomainTransformListener(
				new IndexingTransformListener());
	}

	private static DomainStores domainStores;

	public static Builder builder() {
		return new Builder();
	}

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
		return stores().writableStore().transactions().ensureTransaction();
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

	public static TopicSupport<HasIdAndLocalId> topicMappingEvent() {
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

	private UnsortedMultikeyMap<DomainStoreTransient> domainStoreTransientProperties = new UnsortedMultikeyMap<>(
			2);

	private LazyObjectLoader lazyObjectLoader;

	private boolean writable;

	private DomainStoreDomainHandler handler;

	public String name;

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
		/*
		 * Add cache stats first (notionally it's the owner of the hilis)
		 */
		cache.addMemoryStats(top, type);
		getDomainDescriptor().addMemoryStats(top, type);
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

	public <T extends HasIdAndLocalId> boolean isCached(Class<T> clazz,
			long id) {
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

	public void onTransformsPersisted() {
		loader.onTransformsPersisted();
	}

	public void readLockExpectLongRunning(boolean lock) {
		threads.readLockExpectLongRunning(lock);
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
		Transaction.ensureActive();
		Transaction.current().setBaseTransaction(true, this);
		domainDescriptor.perClass.values().stream()
				.forEach(DomainClassDescriptor::initialise);
		loader.warmup();
		// loader responsible for this
		// Transaction.current().toCommitted();
		Transaction.endAndBeginNew();
		initialising = false;
		initialised = true;
		threads.startLongLockHolderCheck();
		if (ResourceUtilities.is("checkAccessWithoutLock")) {
			threads.setupLockedAccessCheck();
		}
		MetricLogging.get().end("domainStore.warmup");
	}

	private void doEvictions() {
		domainDescriptor.preProvideTasks
				.forEach(PreProvideTask::writeLockedCleanup);
	}

	private DomainTransformEvent
			filterForDomainStoreTransient(DomainTransformEvent dte) {
		switch (dte.getTransformType()) {
		case CREATE_OBJECT:
		case DELETE_OBJECT:
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION:
			return dte;
		}
		DomainStoreTransient ann = domainStoreTransientProperties
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
				.map(dte -> filterForDomainStoreTransient(dte))
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	private ComplexFilter getComplexFilterFor(Class clazz,
			DomainFilter... filters) {
		return domainDescriptor.complexFilters.stream()
				.filter(cf -> cf.handles(clazz, filters)).findFirst()
				.orElse(null);
	}

	private Set<Long> getFiltered(final Class clazz, DomainFilter cacheFilter,
			DomainFilter nextFilter, FilterContext ctr, Set<Long> existing) {
		ComplexFilter complexFilter = getComplexFilterFor(clazz, cacheFilter,
				nextFilter);
		if (complexFilter != null) {
			Set<Long> ids = complexFilter.evaluate(existing, cacheFilter,
					nextFilter);
			ctr.idx += complexFilter.topLevelFiltersConsumed() - 1;
			if (isDebug()) {
				ctr.lastFilterString = String.format("Complex - %s - %s %s",
						complexFilter, cacheFilter, nextFilter);
			}
			return ids;
		}
		if (isDebug()) {
			ctr.lastFilterString = cacheFilter.toString();
		}
		DomainLookup lookup = getLookupFor(clazz, cacheFilter.propertyPath);
		if (lookup != null) {
			switch (cacheFilter.filterOperator) {
			case EQ:
			case IN:
				Set<Long> set = lookup.getMaybeCollectionKey(
						cacheFilter.propertyValue, existing);
				if (set != null && existing != null
						&& set.size() > existing.size() * 1000) {
					// heuristic - faster to just filter existing
					break;
				} else {
					set = set != null ? new LinkedHashSet<Long>(set)
							: new LinkedHashSet<Long>();
					return (Set<Long>) (existing == null ? set
							: CommonUtils.intersection(existing, set));
				}
				// all others non-optimised
			default:
				break;
			}
		}
		final CollectionFilter filter = cacheFilter.asCollectionFilter();
		return domainDescriptor.perClass.get(clazz).evaluateFilter(cache,
				existing, filter);
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
								DomainStoreTransient.class) != null) {
					DomainStoreTransient transientAnn = rm
							.getAnnotation(DomainStoreTransient.class);
					if (transientAnn != null) {
						Field field = getField(clazz, pd.getName());
						field.setAccessible(true);
						domainStoreTransientProperties.put(clazz,
								field.getName(), transientAnn);
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
			listener.insert((HasIdAndLocalId) o);
		}
	}

	<T extends HasIdAndLocalId> T findRaw(Class<T> clazz, long id) {
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

	<T extends HasIdAndLocalId> T findRaw(T t) {
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

	void index(HasIdAndLocalId obj, boolean add) {
		Class<? extends HasIdAndLocalId> clazz = obj.provideEntityClass();
		if (obj instanceof DomainProxy) {
			clazz = (Class<? extends HasIdAndLocalId>) clazz.getSuperclass();
		}
		DomainClassDescriptor<?> itemDescriptor = domainDescriptor.perClass
				.get(clazz);
		itemDescriptor.index(obj, add);
		for (HasIdAndLocalId dependentObject : itemDescriptor
				.getDependentObjectsWithDerivedProjections(obj)) {
			index(dependentObject, add);
		}
	}

	<V extends HasIdAndLocalId> boolean isRawValue(V v) {
		V existing = (V) cache.get(v.provideEntityClass(), v.getId());
		return existing == v;
	}

	// we only have one thread allowed here - but they won't start blocking the
	// reader thread
	synchronized void
			postProcess(DomainTransformPersistenceEvent persistenceEvent) {
		DomainModificationMetadataProvider metadataProvider = persistenceEvent
				.getMetadataProvider();
		if (metadataProvider == null) {
			metadataProvider = new SourceMetadataProvider(this);
		}
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
			Multimap<HiliLocator, List<DomainTransformEvent>> perObjectTransforms = CollectionFilters
					.multimap(filtered, new DteToLocatorMapper());
			for (DomainTransformEvent dte : filtered) {
				DomainTransformEvent first = CommonUtils
						.first(perObjectTransforms
								.get(HiliLocator.objectLocator(dte)));
				DomainTransformEvent last = CommonUtils.last(perObjectTransforms
						.get(HiliLocator.objectLocator(dte)));
				if (dte.getTransformType() != TransformType.CREATE_OBJECT
						&& first == dte) {
					HasIdAndLocalId obj = transformManager.getObject(dte, true);
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
			Transaction.ensureEnded();
			Transaction.begin();
			Transaction.current().toCommitting();
			threads.postProcessWriterThread = Thread.currentThread();
			postProcessEvent = persistenceEvent;
			health.domainStorePostProcessStartTime = System.currentTimeMillis();
			transformManager.startCommit();
			List<DomainTransformEvent> dtes = (List) persistenceEvent
					.getDomainTransformLayerWrapper().persistentEvents;
			List<DomainTransformEvent> filtered = filterInterestedTransforms(
					dtes);
			Multimap<HiliLocator, List<DomainTransformEvent>> perObjectTransforms = CollectionFilters
					.multimap(filtered, new DteToLocatorMapper());
			Set<HasIdAndLocalId> indexAtEnd = new LinkedHashSet<>();
			metadataProvider.registerTransforms(filtered);
			Set<Long> uncommittedToLocalGraphLids = new LinkedHashSet<Long>();
			for (DomainTransformEvent dte : filtered) {
				dte.setNewValue(null);// force a lookup from the subgraph
			}
			for (DomainTransformEvent dte : filtered) {
				// remove from indicies before first change - and only if
				// preexisting object
				DomainTransformEvent first = CommonUtils
						.first(perObjectTransforms
								.get(HiliLocator.objectLocator(dte)));
				DomainTransformEvent last = CommonUtils.last(perObjectTransforms
						.get(HiliLocator.objectLocator(dte)));
				if (last.getTransformType() == TransformType.DELETE_OBJECT
						&& first.getTransformType() != TransformType.CREATE_OBJECT) {
					// this a check against deletion during cache warmup.
					// shouldn't happen anyway (trans. isolation)
					// TODO - check if necessary
					// (note) also a check against trying to handle deletion of
					// lazy objects
					HasIdAndLocalId domainStoreObj = transformManager
							.getObject(dte, true);
					if (domainStoreObj == null) {
						continue;
					}
				}
				if (dte.getTransformType() != TransformType.CREATE_OBJECT
						&& first == dte) {
					HasIdAndLocalId obj = transformManager.getObject(dte, true);
					if (obj != null) {
						index(obj, false);
					} else {
						logger.warn("Null domain store object for index - {}",
								HiliLocator.objectLocator(dte));
					}
				}
				try {
					transformManager.consume(dte);
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
					HasIdAndLocalId domainStoreObject = transformManager
							.getObject(dte, true);
					if (domainStoreObject != null) {
						metadataProvider.updateMetadata(dte, domainStoreObject);
						index(domainStoreObject, true);
					} else {
						logger.warn("Null domain store object for index - {}",
								HiliLocator.objectLocator(dte));
					}
				}
			}
			indexAtEnd.forEach(domainStoreObject -> {
				List<DomainTransformEvent> list = perObjectTransforms
						.get(new HiliLocator(domainStoreObject));
				DomainTransformEvent last = list == null ? null
						: CommonUtils.last(list);
				if (last != null && last
						.getTransformType() == TransformType.DELETE_OBJECT) {
				} else {
					index(domainStoreObject, true);
				}
			});
			doEvictions();
			Date transactionCommitTime = persistenceEvent
					.getDomainTransformLayerWrapper().persistentRequests.get(0)
							.getTransactionCommitTime();
			Transaction.current().toCommitted(
					new Timestamp(transactionCommitTime.getTime()));
		} catch (Exception e) {
			Transaction.current().toAborted();
			causes.add(e);
		} finally {
			transformManager.endCommit();
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

	<T extends HasIdAndLocalId> Set<T> query(Class<T> clazz,
			DomainStoreQuery<T> query) {
		try {
			threads.lock(false);
			Set<T> raw = null;
			Set<Long> ids = query.getFilterByIds();
			boolean debugMetrics = isDebug()
					&& LooseContext.is(CONTEXT_DEBUG_QUERY_METRICS);
			StringBuilder debugMetricBuilder = new StringBuilder();
			int filterSize = query.getFilters().size();
			FilterContext ctx = new FilterContext();
			for (; ctx.idx < filterSize; ctx.idx++) {
				int idx = ctx.idx;
				long start = System.nanoTime();
				DomainFilter cacheFilter = query.getFilters().get(idx);
				DomainFilter nextFilter = idx == filterSize - 1 ? null
						: query.getFilters().get(idx + 1);
				ids = (idx == 0 && ids.isEmpty()) ? null : ids;
				ids = getFiltered(clazz, cacheFilter, nextFilter, ctx, ids);
				if (debugMetrics) {
					double ms = (double) (System.nanoTime() - start)
							/ 1000000.0;
					String filters = ctx.lastFilterString;
					debugMetricBuilder.append(String.format("\t%.3f ms - %s\n",
							ms, CommonUtils.trimToWsChars(filters, 100, true)));
				}
				if (ids.isEmpty()) {
					break;
				}
			}
			if (debugMetrics
					&& CommonUtils.isNullOrEmpty(query.getFilterByIds())) {
				metricLogger.debug("Query metrics:\n========\n{}\n{}", query,
						debugMetricBuilder.toString());
			}
			if (filterSize == 0 && ids.isEmpty()) {
				// Domain.list()
				raw = cache.values(clazz);
			} else {
				raw = (Set<T>) domainDescriptor.perClass.get(clazz)
						.getRawValues(ids, cache);
			}
			try {
				for (PreProvideTask task : domainDescriptor.preProvideTasks) {
					task.run(clazz, raw, true);
				}
				if (query.isRaw() || isWillProjectLater()) {
					return raw;
				}
				return new GraphProjection(query.getFieldFilter(),
						query.getDataFilter()).project(raw, null);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		} finally {
			threads.unlock(false);
		}
	}

	public static class Builder {
		private DomainStoreDescriptor descriptor;

		private ThreadPoolExecutor warmupExecutor;

		private DataSource dataSource;

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
				DataSource dataSource) {
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

		public <V extends HasIdAndLocalId> DomainStoreQuery<V>
				query(Class<V> clazz) {
			return new DomainStoreQuery(clazz, storeFor(clazz));
		}

		public synchronized void register(DomainStore store) {
			descriptorMap.put(store.domainDescriptor, store);
			store.domainDescriptor.perClass.keySet().forEach(clazz -> {
				Preconditions.checkState(!classMap.containsKey(clazz));
				classMap.put(clazz, store);
			});
		}

		public DomainStore storeFor(Class clazz) {
			return classMap.get(clazz);
		}

		public Stream<DomainStore> stream() {
			return descriptorMap.values().stream().collect(Collectors.toList())
					.stream();
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
			public <V extends HasIdAndLocalId> void async(Class<V> clazz,
					long objectId, boolean create, Consumer<V> resultConsumer) {
				storeHandler(clazz).async(clazz, objectId, create,
						resultConsumer);
			}

			@Override
			public <V extends HasIdAndLocalId> V byProperty(Class<V> clazz,
					String propertyName, Object value) {
				return storeHandler(clazz).byProperty(clazz, propertyName,
						value);
			}

			@Override
			public void commitPoint() {
				// Noop
			}

			@Override
			public <V extends HasIdAndLocalId> V create(Class<V> clazz) {
				return storeHandler(clazz).create(clazz);
			}

			@Override
			public <V extends HasIdAndLocalId> V detachedVersion(V v) {
				return v == null ? null
						: storeHandler(v.provideEntityClass())
								.detachedVersion(v);
			}

			@Override
			public <V extends HasIdAndLocalId> V find(Class clazz, long id) {
				return storeHandler(clazz).find(clazz, id);
			}

			@Override
			public <V extends HasIdAndLocalId> V find(V v) {
				return v == null ? null
						: storeHandler(v.provideEntityClass()).find(v);
			}

			@Override
			public <V extends HasIdAndLocalId> List<Long> ids(Class<V> clazz) {
				return storeHandler(clazz).ids(clazz);
			}

			@Override
			public <V extends HasIdAndLocalId> boolean isDomainVersion(V v) {
				return v == null ? null
						: storeHandler(v.provideEntityClass())
								.isDomainVersion(v);
			}

			@Override
			public <V extends HasIdAndLocalId> DomainQuery<V>
					query(Class<V> clazz) {
				return storeHandler(clazz).query(clazz);
			}

			@Override
			public <V extends HasIdAndLocalId> V resolveTransactional(
					DomainListener listener, V value, Object[] path) {
				return ((DomainStore) listener.getDomainStore()).handler
						.resolveTransactional(listener, value, path);
			}

			@Override
			public <V extends HasIdAndLocalId> Stream<V>
					stream(Class<V> clazz) {
				return storeHandler(clazz).stream(clazz);
			}

			@Override
			public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
					long id) {
				return storeHandler(clazz).transactionalFind(clazz, id);
			}

			@Override
			public <V extends HasIdAndLocalId> V transactionalVersion(V v) {
				return v == null ? null
						: storeHandler(v.provideEntityClass())
								.transactionalVersion(v);
			}

			@Override
			public <V extends HasIdAndLocalId> Collection<V>
					values(Class<V> clazz) {
				return storeHandler(clazz).values(clazz);
			}

			@Override
			public <V extends HasIdAndLocalId> V writeable(V v) {
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

		public void ensureReferredPropertyIsTransactional(HasIdAndLocalId hili,
				String propertyName) {
			// target, even if new object, will still be equals() to old, so no
			// property change will be fired, which is the desired behaviour
			HasIdAndLocalId target = (HasIdAndLocalId) Reflections
					.propertyAccessor().getPropertyValue(hili, propertyName);
			if (target != null) {
				target = ensureTransactional(target);
				Reflections.propertyAccessor().setPropertyValue(hili,
						propertyName, target);
			}
		}

		public PerThreadTransaction ensureTransaction() {
			PerThreadTransaction transaction = transactions.get();
			if (transaction == null) {
				LinkedHashSet<DomainTransformEvent> localTransforms = TransformManager
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

		public <V extends HasIdAndLocalId> V ensureTransactional(V value) {
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
				return (T) transactions.get()
						.ensureTransactional((HasIdAndLocalId) t);
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

		public <V extends HasIdAndLocalId> V resolveTransactional(
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
				if (HasIdAndLocalId.class.isAssignableFrom(field.getType())) {
					HasIdAndLocalId value = (HasIdAndLocalId) field.get(input);
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

	class DetachedCacheObjectStorePsAware extends DetachedCacheObjectStore {
		public DetachedCacheObjectStorePsAware() {
			super(new PropertyStoreAwareMultiplexingObjectCache());
		}

		@Override
		public void mapObject(HasIdAndLocalId obj) {
			if (publishMappingEvents) {
				topicMappingEvent().publish(obj);
			}
			super.mapObject(obj);
		}
	}

	class DomainStoreDomainHandler implements DomainHandler {
		@Override
		public <V extends HasIdAndLocalId> void async(Class<V> clazz,
				long objectId, boolean create, Consumer<V> resultConsumer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <V extends HasIdAndLocalId> V byProperty(Class<V> clazz,
				String propertyName, Object value) {
			return Domain.query(clazz).raw().filter(propertyName, value).find();
		}

		@Override
		public void commitPoint() {
			// do nothing, assume explicit commit in servlet layer
		}

		@Override
		public <V extends HasIdAndLocalId> V create(Class<V> clazz) {
			return Transaction.current().create(clazz, DomainStore.this);
		}

		@Override
		public <V extends HasIdAndLocalId> V detachedVersion(V v) {
			return (V) Domain.query(v.provideEntityClass())
					.filterById(v.getId()).find();
		}

		@Override
		public <V extends HasIdAndLocalId> V find(Class clazz, long id) {
			return (V) findRaw(clazz, id);
		}

		@Override
		public <V extends HasIdAndLocalId> V find(V v) {
			checkInLockedSection();
			if (!v.provideWasPersisted()) {
				HiliLocator locator = ThreadlocalTransformManager.get()
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
		public <V extends HasIdAndLocalId> List<Long> ids(Class<V> clazz) {
			try {
				threads.lock(false);
				return new ArrayList<Long>(cache.keys(clazz));
			} finally {
				threads.unlock(false);
			}
		}

		@Override
		public <V extends HasIdAndLocalId> boolean isDomainVersion(V v) {
			return isRawValue(v);
		}

		@Override
		public <V extends HasIdAndLocalId> List<V> listByProperty(
				Class<V> clazz, String propertyName, Object value) {
			return Domain.query(clazz).raw().filter(propertyName, value).list();
		}

		@Override
		public <V extends HasIdAndLocalId> DomainQuery<V>
				query(Class<V> clazz) {
			return new DomainStoreQuery<>(clazz, DomainStore.this);
		}

		@Override
		public <V extends HasIdAndLocalId> V resolve(V v) {
			return Transactions.resolve(v, false);
		}

		@Override
		public <V extends HasIdAndLocalId> V resolveTransactional(
				DomainListener listener, V value, Object[] path) {
			return transactions().resolveTransactional(listener, value, path);
		}

		@Override
		public <V extends HasIdAndLocalId> Stream<V> stream(Class<V> clazz) {
			return list(clazz).stream();
		}

		@Override
		public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
				long id) {
			return (V) transactions().find(clazz, id);
		}

		@Override
		public <V extends HasIdAndLocalId> V transactionalVersion(V v) {
			return (V) transactions().ensureTransactional(v);
		}

		@Override
		public <V extends HasIdAndLocalId> Collection<V>
				values(Class<V> clazz) {
			checkInLockedSection();
			return cache.immutableRawValues(clazz);
		}

		@Override
		public <V extends HasIdAndLocalId> V writeable(V v) {
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

		private <V extends HasIdAndLocalId> V project(V v) {
			if (LooseContext.has(CONTEXT_WRITEABLE_PROJECTOR)) {
				Function<V, V> projector = LooseContext
						.get(CONTEXT_WRITEABLE_PROJECTOR);
				return projector.apply(v);
			} else {
				return GraphProjections.defaultProjections().project(v);
			}
		}

		<T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
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

	static class FilterContext {
		int idx = 0;

		public String lastFilterString;
	}

	static class IndexingTransformListener implements DomainTransformListener {
		@Override
		public void domainTransform(DomainTransformEvent evt)
				throws DomainTransformException {
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

	class SubgraphTransformManagerPostProcess extends SubgraphTransformManager {
		public void addPropertyStore(DomainClassDescriptor descriptor) {
			((PropertyStoreAwareMultiplexingObjectCache) store.getCache())
					.addPropertyStore(descriptor);
		}

		@Override
		protected void createObjectLookup() {
			store = new DetachedCacheObjectStorePsAware();
			setDomainObjects(store);
		}

		@Override
		protected boolean isZeroCreatedObjectLocalId(Class clazz) {
			return true;
		}

		@Override
		protected void updateAssociation(DomainTransformEvent evt,
				HasIdAndLocalId object, HasIdAndLocalId targetObject,
				boolean remove, boolean collectionPropertyChange) {
			/*
			 * Definitely *don't* need property changes/collection mods here
			 */
			super.updateAssociation(evt, object, targetObject, remove, false);
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
