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
import java.util.concurrent.atomic.AtomicInteger;
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
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.AssociationPropogationTransformListener;
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
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvents;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceListener;
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
				new AssociationPropogationTransformListener(
						CommitType.TO_LOCAL_BEAN));
		ThreadlocalTransformManager.addThreadLocalDomainTransformListener(
				new IndexingTransformListener());
	}

	private static DomainStores domainStores;

	static final int LONG_POST_PROCESS_TRACE_LENGTH = 99999;

	public static Builder builder() {
		return new Builder();
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

	public static Topic<DomainStoreUpdateException> topicUpdateException() {
		return Topic.global(TOPIC_UPDATE_EXCEPTION);
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

	DomainStoreHealth health = new DomainStoreHealth();

	DetachedEntityCache cache;

	private DomainStorePersistenceListener persistenceListener;

	boolean initialised = false;

	boolean initialising;

	private boolean debug;

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

	Thread postProcessThread;

	public DomainStore(DomainStoreDescriptor descriptor) {
		this();
		this.domainDescriptor = descriptor;
	}

	private DomainStore() {
		persistenceListener = new DomainStorePersistenceListener();
		this.persistenceEvents = new DomainTransformPersistenceEvents(this);
		this.handler = new DomainStoreDomainHandler();
	}

	public void appShutdown() {
		loader.appShutdown();
		persistenceEvents.getQueue().appShutdown();
	}

	public void enableAndAddValues(DomainListener listener) {
		listener.setEnabled(true);
		addValues(listener);
	}

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	public DomainStoreDescriptor getDomainDescriptor() {
		return this.domainDescriptor;
	}

	public DomainStoreHealth getHealth() {
		return health;
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

	public boolean handlesAssociationsFor(Class clazz) {
		return domainDescriptor.perClass.containsKey(clazz);
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

	public void remove(Entity entity) {
		cache.remove(entity);
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

	public void warmup() throws Exception {
		MetricLogging.get().start("domainStore.warmup");
		initialised = false;
		initialising = true;
		transformManager = new SubgraphTransformManagerPostProcess();
		lazyObjectLoader = loader.getLazyObjectLoader();
		cache = transformManager.getDetachedEntityCache();
		transformManager.getStore().setLazyObjectLoader(lazyObjectLoader);
		domainDescriptor.initialise();
		domainDescriptor.registerStore(this);
		domainDescriptor.perClass.values().stream()
				.forEach(this::prepareClassDescriptor);
		mvcc = new Mvcc(this, domainDescriptor, cache);
		MetricLogging.get().start("mvcc");
		mvcc.init();
		MetricLogging.get().end("mvcc");
		Transaction.beginDomainPreparing();
		Transaction.current().setBaseTransaction(true);
		domainDescriptor.perClass.values().stream()
				.forEach(DomainClassDescriptor::initialise);
		loader.warmup();
		// loader responsible for this
		// Transaction.current().toCommitted();
		Transaction.end();
		initialising = false;
		initialised = true;
		MetricLogging.get().end("domainStore.warmup");
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
		DomainLookup lookup = getLookupFor(clazz, filter.getPropertyPath());
		if (lookup != null) {
			switch (filter.getFilterOperator()) {
			case EQ:
			case IN:
				// FIXME - mvcc.4 - if we have estimates of size, we might be
				// able to optimise here
				Set<E> lookupValues = lookup
						.getKeyMayBeCollection(filter.getPropertyValue());
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
		T t = cache.get(clazz, id);
		if (t == null) {
			if (domainDescriptor.perClass.containsKey(clazz)
					&& domainDescriptor.perClass.get(clazz).lazy && id != 0) {
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
		Class<? extends Entity> clazz = obj.entityClass();
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
		V existing = (V) cache.get(v.entityClass(), v.getId());
		return existing == v;
	}

	// we only have one thread allowed here - but they won't start blocking the
	// reader thread
	// FIXME - mvcc.2 - optimise!
	synchronized void
			postProcess(DomainTransformPersistenceEvent persistenceEvent) {
		if (persistenceEvent.getDomainTransformLayerWrapper().persistentRequests
				.isEmpty()) {
			Transaction.endAndBeginNew();
			return;
		}
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
							.isAsyncClient() ? cache.getCreatedLocals()
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
					applyTxToGraphCounter.getAndIncrement(),
					persistenceEvent.getMaxPersistedRequestId());
			postProcessThread = Thread.currentThread();
			postProcessEvent = persistenceEvent;
			health.domainStorePostProcessStartTime = System.currentTimeMillis();
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
					// This checks if we're trying to handle deletion of
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
			logger.warn("post process exception - pre final", e);
			Transaction.current().toDomainAborted();
			causes.add(e);
		} finally {
			TransformManager.get().setIgnorePropertyChanges(false);
			health.domainStorePostProcessStartTime = 0;
			postProcessThread = null;
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
				double ms = (double) (System.nanoTime() - start) / 1000000.0;
				String filters = token.lastFilterString;
				debugMetricBuilder.append(String.format("\t%.3f ms - %s\n", ms,
						CommonUtils.trimToWsChars(filters, 100, true)));
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

	public class DomainStoreHealth {
		public long domainStoreMaxPostProcessTime;

		public long domainStorePostProcessStartTime;

		AtomicInteger domainStoreExceptionCount = new AtomicInteger();

		public AtomicInteger getDomainStoreExceptionCount() {
			return this.domainStoreExceptionCount;
		}

		public long getMvccOldestTx() {
			return Transactions.stats().getOldestTxStartTime();
		}

		public long getMvccUncollectedTxCount() {
			return Transactions.stats().getUncollectedTxCount();
		}

		public long getMvccVacuumQueueLength() {
			return Transactions.stats().getVacuumQueueLength();
		}

		public long getTimeInDomainStoreWriter() {
			long time = domainStorePostProcessStartTime == 0 ? 0
					: System.currentTimeMillis()
							- domainStorePostProcessStartTime;
			if (time > 100) {
				logger.info("Long postprocess time - {} ms - {}\n{}\n\n", time,
						postProcessThread,
						SEUtilities.getStacktraceSlice(postProcessThread,
								LONG_POST_PROCESS_TRACE_LENGTH, 0));
			}
			return time;
		}

		public long getTimeInVacuum() {
			long time = Transactions.stats().getTimeInVacuum();
			if (time > 100) {
				logger.info("Long vacuum time - {} ms - {}\n{}\n\n", time,
						Transactions.stats().getVacuumThread(),
						SEUtilities.getStacktraceSlice(
								Transactions.stats().getVacuumThread(),
								LONG_POST_PROCESS_TRACE_LENGTH, 0));
			}
			return time;
		}
	}

	@RegistryLocation(registryPoint = DomainStores.class, implementationType = ImplementationType.SINGLETON)
	public static class DomainStores implements RegistrableService {
		// not concurrent, handle in methods
		private Map<DomainDescriptor, DomainStore> descriptorMap = new LinkedHashMap<>();

		private Map<Class, DomainStore> classMap = new LinkedHashMap<>();

		private DomainStore writableStore;

		DomainStoresDomainHandler storesHandler = new DomainStoresDomainHandler();

		Logger logger = LoggerFactory.getLogger(getClass());

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
			public <V extends Entity> V detachedVersion(V v) {
				return v == null ? null
						: storeHandler(v.entityClass()).detachedVersion(v);
			}

			@Override
			public <V extends Entity> V find(Class clazz, long id) {
				return storeHandler(clazz).find(clazz, id);
			}

			@Override
			public <V extends Entity> V find(V v) {
				return v == null ? null : storeHandler(v.entityClass()).find(v);
			}

			@Override
			public <V extends Entity> boolean isDomainVersion(V v) {
				return v == null ? null
						: storeHandler(v.entityClass()).isDomainVersion(v);
			}

			@Override
			public <V extends Entity> DomainQuery<V> query(Class<V> clazz) {
				return storeHandler(clazz).query(clazz);
			}

			@Override
			public Class<? extends Object>
					resolveEntityClass(Class<? extends Object> clazz) {
				if (HasId.class.isAssignableFrom(clazz)) {
					return Mvcc
							.resolveEntityClass((Class<? extends HasId>) clazz);
				} else {
					return clazz;
				}
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
			public <V extends Entity> V writeable(V v) {
				return v == null ? null
						: storeHandler(v.entityClass()).writeable(v);
			}

			DomainHandler storeHandler(Class clazz) {
				DomainStore domainStore = classMap.get(clazz);
				if (domainStore == null) {
					logger.warn(
							"No store for {} - defaulting to writable store",
							clazz);
					domainStore = writableStore;
				}
				return domainStore.handler;
			}
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
					.map(id -> cache.get(this.clazz, id))
					.filter(Objects::nonNull);
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
							filter.getPropertyPath());
					if (lookup != null) {
						switch (filter.getFilterOperator()) {
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
		public <V extends Entity> V detachedVersion(V v) {
			return (V) Domain.query(v.entityClass()).filterById(v.getId())
					.find();
		}

		@Override
		public <V extends Entity> V find(Class clazz, long id) {
			return (V) findRaw(clazz, id);
		}

		@Override
		public <V extends Entity> V find(V v) {
			if (!v.domain().wasPersisted()) {
				if (v.getLocalId() == 0) {
					return null;
				}
				EntityLocator locator = ThreadlocalTransformManager.get()
						.resolvePersistedLocal(DomainStore.this, v);
				if (locator == null) {
					return null;
				} else {
					return (V) cache.get(v.entityClass(), locator.id);
				}
			} else {
				return (V) cache.get(v.entityClass(), v.getId());
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
		public <V extends Entity> long size(Class<V> clazz) {
			return cache.size(clazz);
		}

		@Override
		public <V extends Entity> Stream<V> stream(Class<V> clazz) {
			return Domain.query(clazz).stream();
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
			if (out.domain().wasPersisted()) {
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
			TransformManager tm = TransformManager.get();
			if (!tm.handlesAssociationsFor(event.getObjectClass())) {
				return;
			}
			DomainStore store = DomainStore.stores()
					.storeFor(event.getObjectClass());
			Entity entity = tm.getObject(event);
			if (event.getTransformType() != TransformType.CREATE_OBJECT) {
				switch (event.getTransformType()) {
				case CHANGE_PROPERTY_REF:
				case CHANGE_PROPERTY_SIMPLE_VALUE:
				case NULL_PROPERTY_REF: {
					DomainProperty domainProperty = Reflections
							.propertyAccessor().getAnnotationForProperty(
									entity.entityClass(), DomainProperty.class,
									event.getPropertyName());
					if (domainProperty != null && !domainProperty.index()) {
						return;
					}
					tm.setIgnorePropertyChanges(true);
					/*
					 * undo last property change
					 */
					Reflections.propertyAccessor().setPropertyValue(entity,
							event.getPropertyName(), event.getOldValue());
					store.index(entity, false);
					/*
					 * redo
					 */
					Reflections.propertyAccessor().setPropertyValue(entity,
							event.getPropertyName(), event.getNewValue());
					tm.setIgnorePropertyChanges(false);
					break;
				}
				default:
					store.index(entity, false);
				}
			}
			if (event.getTransformType() != TransformType.DELETE_OBJECT) {
				store.index(entity, true);
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
					.anyMatch(filter -> filter.getPropertyPath().equals("id"));
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
			store = new DetachedCacheObjectStore(new DomainStoreEntityCache());
			setDomainObjects(store);
		}

		@Override
		protected Entity getEntityForCreate(DomainTransformEvent event) {
			Entity localReplacement = localReplacementCreationObjectResolver
					.apply(event.getObjectLocalId());
			if (localReplacement != null) {
				localReplacement.setId(event.getObjectId());
				// this has to be done, and has to happen after setId, since the
				// very act of setId will put the
				// (local) version into the cache in tx phase
				// TO_DOMAIN_COMMITTED by Transactions.resolve(write==true)
				//
				// only local-id objects created by this webapp client instance
				// will ever be put into the store cache in this phase - thus
				// preserving the "local ids don't collied because they're on
				// different transactio ns" logic of the cache
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
	}
}
