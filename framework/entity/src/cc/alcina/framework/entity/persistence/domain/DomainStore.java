package cc.alcina.framework.entity.persistence.domain;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.FilterOperator;
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
import cc.alcina.framework.common.client.domain.DomainQuery.Hint;
import cc.alcina.framework.common.client.domain.DomainQuery.HintResolver;
import cc.alcina.framework.common.client.domain.DomainStoreProperty;
import cc.alcina.framework.common.client.domain.FilterCost;
import cc.alcina.framework.common.client.domain.IDomainStore;
import cc.alcina.framework.common.client.domain.IndexedValueProvider;
import cc.alcina.framework.common.client.domain.IndexedValueProvider.StreamOrSet;
import cc.alcina.framework.common.client.domain.MemoryStat;
import cc.alcina.framework.common.client.domain.MemoryStat.ObjectMemory;
import cc.alcina.framework.common.client.domain.MemoryStat.StatType;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.OneToManyMultipleSummary;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.OneToManyMultipleSummary.SizeProvider;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.AssociationPropagationTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformListener;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet.NonDomainNotifier;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectStore;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.common.client.util.ObjectWrapper;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.entity.persistence.mvcc.Mvcc;
import cc.alcina.framework.entity.persistence.mvcc.ResolvedVersionState;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.mvcc.TransactionId;
import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.stat.StatCategory_DomainStore;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent.ExTransformDbMetadata;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.EntityLocatorMap;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvents;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceListener;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;
import cc.alcina.framework.entity.util.OffThreadLogger;

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
 *         FIXME - mvcc.5 - don't add listeners during postprocess
 *         (optimisation)
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class DomainStore implements IDomainStore {
	private static volatile QueryPool queryPool;

	public static final String CONTEXT_DEBUG_QUERY_METRICS = DomainStore.class
			.getName() + ".CONTEXT_DEBUG_QUERY_METRICS";

	public static final String CONTEXT_SERIAL_QUERY = DomainStore.class
			.getName() + ".CONTEXT_SERIAL_QUERY";

	public static final String CONTEXT_IN_POST_PROCESS = DomainStore.class
			.getName() + ".CONTEXT_IN_POST_PROCESS";

	public static final String CONTEXT_DO_NOT_RESOLVE_LOAD_TABLE_REFS = DomainStore.class
			.getName() + ".CONTEXT_DO_NOT_RESOLVE_LOAD_TABLE_REFS";

	public static final String CONTEXT_DO_NOT_POPULATE_LAZY_PROPERTY_VALUES = DomainStore.class
			.getName() + ".CONTEXT_DO_NOT_POPULATE_LAZY_PROPERTY_VALUES";

	public static final Logger LOGGER_WRAPPED_OBJECT_REF_INTEGRITY = AlcinaLogUtils
			.getTaggedLogger(DomainStore.class, "wrapped_object_ref_integrity");
	static {
		ThreadlocalTransformManager.addThreadLocalDomainTransformListener(
				new AssociationPropagationTransformListener(
						CommitType.TO_LOCAL_BEAN));
		ThreadlocalTransformManager.addThreadLocalDomainTransformListener(
				new IndexingTransformListener());
	}

	private static DomainStores domainStores;

	static final int LONG_POST_PROCESS_TRACE_LENGTH = 99999;

	public static final Topic<DomainStoreUpdateException> topicUpdateException = Topic
			.create();

	public static Builder builder() {
		return new Builder();
	}

	public static QueryPool queryPool() {
		synchronized (DomainStore.class) {
			if (queryPool == null) {
				queryPool = new QueryPool();
			}
		}
		return queryPool;
	}

	public static DomainStores stores() {
		synchronized (DomainStores.class) {
			if (domainStores == null) {
				domainStores = new DomainStores();
			}
		}
		return domainStores;
	}

	public static void waitUntilCurrentRequestsProcessed() {
		Transaction.ensureBegun();
		writableStore().getPersistenceEvents().getQueue()
				.waitUntilCurrentRequestsProcessed();
		Transaction.endAndBeginNew();
	}

	public static DomainStore writableStore() {
		return stores().writableStore();
	}

	static Timestamp testSensitiveTimestamp(Date transactionCommitTime) {
		return transactionCommitTime == null
				? new Timestamp(System.currentTimeMillis())
				: new Timestamp(transactionCommitTime.getTime());
	}

	Topic<DomainTransformPersistenceEvent> topicBeforeDomainCommitted = Topic
			.create();

	Topic<DomainTransformPersistenceEvent> topicAfterDomainCommitted = Topic
			.create();

	private DomainTransformPersistenceEvents persistenceEvents;

	SubgraphTransformManagerPostProcess transformManager;

	DomainStoreDescriptor domainDescriptor;

	Logger sqlLogger = AlcinaLogUtils.getTaggedLogger(getClass(), "sql");

	Logger metricLogger = AlcinaLogUtils.getMetricLogger(getClass());

	Logger logger = OffThreadLogger.getLogger(getClass());

	Mvcc mvcc;

	DomainStoreHealth health = new DomainStoreHealth();

	DomainStoreEntityCache cache;

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

	UnsortedMultikeyMap<DomainStoreProperty> domainStoreProperties = new UnsortedMultikeyMap<>(
			2);

	private LazyObjectLoader lazyObjectLoader;

	private boolean writable;

	private DomainStoreDomainHandler handler;

	public String name;

	AtomicLong applyTxToGraphCounter = new AtomicLong(0);

	Thread postProcessThread;

	private GraphProjection graphProjection;

	Map<EntityLocator, TransactionId> lazyLoadAttempted = new ConcurrentHashMap<>();

	private DomainTransformEventPersistent postProcessTransform;

	public DomainStore reuseTransformerStore;

	private ConcurrentHashMap<EntityLocator, Entity> promotedEntitiesByPrePromotion = new ConcurrentHashMap<>();

	public DomainStore(DomainStoreDescriptor descriptor) {
		this();
		this.domainDescriptor = descriptor;
	}

	private DomainStore() {
		persistenceListener = new DomainStorePersistenceListener();
		this.persistenceEvents = new DomainTransformPersistenceEvents(this);
		this.handler = new DomainStoreDomainHandler();
		graphProjection = new GraphProjection();
	}

	@Override
	public MemoryStat addMemoryStats(MemoryStat parent) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		self.objectMemory.walkStats(this, self.counter, o -> o == this
				|| !self.objectMemory.isMemoryStatProvider(o.getClass()));
		return self;
	}

	public void appShutdown() {
		if (isWritable() && queryPool != null) {
			queryPool.pool.shutdown();
		}
		domainDescriptor.onAppShutdown();
		loader.appShutdown();
		persistenceEvents.getQueue().appShutdown();
	}

	public boolean checkTransformRequestExists(long id) {
		return loader.checkTransformRequestExists(id);
	}

	public void close() {
		loader.close();
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

	public List<Field> getFields(Class clazz) {
		try {
			List<Field> fields = null;
			synchronized (graphProjection) {
				fields = graphProjection.getFieldsForClass(clazz, false);
			}
			return fields;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public DomainStoreHealth getHealth() {
		return health;
	}

	public DomainLookup getLookupFor(Class clazz, String propertyName) {
		return domainDescriptor.perClass.get(clazz).getLookupFor(propertyName);
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
		/*
		 * Add self at end (only memory not reachable from
		 * lookups/projections/entity cache)
		 */
		addMemoryStats(top);
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

	public DomainTransformCommitPosition getTransformCommitPosition() {
		return getPersistenceEvents().getQueue().getTransformCommitPosition();
	}

	public DomainTransformPersistenceQueue.Sequencer getTransformSequencer() {
		return loader.getTransformSequencer();
	}

	public boolean handlesAssociationsFor(Class clazz) {
		return domainDescriptor.handlesAssociationsFor(clazz);
	}

	public boolean isCached(Class clazz) {
		return domainDescriptor.perClass.containsKey(clazz);
	}

	public <T extends Entity> boolean isCached(Class<T> clazz, long id) {
		return cache.contains(clazz, id);
	}

	public boolean isDebug() {
		return this.debug;
	}

	public boolean isInitialised() {
		return initialised;
	}

	public boolean isWritable() {
		return this.writable;
	}

	public DomainTransformRequestPersistent loadTransformRequest(long id) {
		return loader.loadTransformRequest(id);
	}

	public <T extends Entity> void onLocalObjectCreated(T newInstance) {
		cache.put(newInstance);
	}

	public void putExternalLocal(Entity instance) {
		cache.putExternalLocal(instance);
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

	public void throwDomainStoreException(String message) {
		health.domainStoreExceptionCount.incrementAndGet();
		throw new DomainStoreException(message);
	}

	public Topic<DomainTransformPersistenceEvent> topicAfterDomainCommitted() {
		return topicAfterDomainCommitted;
	}

	public Topic<DomainTransformPersistenceEvent> topicBeforeDomainCommitted() {
		return topicBeforeDomainCommitted;
	}

	@Override
	public String toString() {
		return Ax.format("Domain store: %s %s", name,
				domainDescriptor.getClass().getSimpleName());
	}

	public void warmup() throws Exception {
		new StatCategory_DomainStore.Warmup().emit(isWritable());
		MetricLogging.get().start("domainStore.warmup");
		initialised = false;
		initialising = true;
		getPersistenceEvents().addDomainTransformPersistenceListener(
				getPersistenceListener());
		transformManager = new SubgraphTransformManagerPostProcess();
		lazyObjectLoader = loader.getLazyObjectLoader();
		cache = (DomainStoreEntityCache) transformManager
				.getDetachedEntityCache();
		cache.setThrowOnExisting(AppPersistenceBase.isTestServer());
		transformManager.getStore().setLazyObjectLoader(lazyObjectLoader);
		domainDescriptor.initialise();
		domainDescriptor.registerStore(this);
		domainDescriptor.perClass.values().stream()
				.forEach(this::prepareClassDescriptor);
		new StatCategory_DomainStore.Warmup.InitialiseDescriptor()
				.emit(isWritable());
		mvcc = new Mvcc(this, domainDescriptor, cache,
				reuseTransformerStore == null ? null
						: reuseTransformerStore.mvcc);
		MetricLogging.get().start("mvcc");
		mvcc.init();
		MetricLogging.get().end("mvcc");
		new StatCategory_DomainStore.Warmup.Mvcc().emit(isWritable());
		Transaction.beginDomainPreparing();
		Transaction.current().setBaseTransaction(true);
		domainDescriptor.perClass.keySet()
				.forEach(clazz -> cache.initialiseMap(clazz));
		domainDescriptor.perClass.values().stream().forEach(dcd -> {
			dcd.initialise();
		});
		loader.warmup();
		// loader responsible for this
		// Transaction.current().toCommitted();
		Transaction.end();
		initialising = false;
		initialised = true;
		domainDescriptor.onWarmupComplete(this);
		MetricLogging.get().end("domainStore.warmup");
		new StatCategory_DomainStore.Warmup.End().emit(isWritable());
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
		filter = maybeConvertEntityToIdFilter(clazz, filter);
		IndexedValueProvider<E> valueProvider = getValueProviderFor(clazz,
				filter.getPropertyPath());
		if (valueProvider != null) {
			switch (filter.getFilterOperator()) {
			case EQ:
			case IN:
				// FIXME - mvcc.query - if we have estimates of size, we might
				// be
				// able to optimise here
				StreamOrSet<E> indexedValues = valueProvider
						.getKeyMayBeCollection(filter.getPropertyValue());
				token.appendEvaluatedValueFilter(indexedValues);
				return;
			// all others non-optimised
			default:
				break;
			}
		}
		token.appendFilter(filter.asPredicate());
	}

	private DomainTransformEventPersistent
			filterForDomainStoreProperty(DomainTransformEventPersistent event) {
		switch (event.getTransformType()) {
		case CREATE_OBJECT:
		case DELETE_OBJECT:
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION:
			return event;
		}
		DomainStoreProperty ann = domainStoreProperties
				.get(event.getObjectClass(), event.getPropertyName());
		if (ann == null) {
			return event;
		}
		switch (ann.loadType()) {
		case EAGER:
		case CUSTOM:
		case LAZY:
			// LAZY should be filtered by the persister's transformpropagation
			// policy
			return event;
		case TRANSIENT:
			return null;
		default:
			throw new UnsupportedOperationException();
		}
	}

	private ComplexFilter getComplexFilterFor(Class clazz,
			DomainFilter... filters) {
		return domainDescriptor.complexFilters.stream()
				.filter(cf -> cf.handles(clazz, filters)).findFirst()
				.orElse(null);
	}

	private IndexedValueProvider getValueProviderFor(Class clazz,
			String propertyPath) {
		if ("id".equals(propertyPath)) {
			return new CacheIdProvider(clazz);
		} else if (Ax.matches(propertyPath, "[^.]+\\.id")) {
			String propertyName = propertyPath.replaceFirst("(.+)\\.id", "$1");
			Association association = Reflections.at(clazz)
					.property(propertyName).annotation(Association.class);
			if (association != null) {
				return new AssociationIdProvider(clazz, propertyName);
			}
		}
		return getLookupFor(clazz, propertyPath);
	}

	private DomainFilter maybeConvertEntityToIdFilter(Class clazz,
			DomainFilter filter) {
		if (filter.getPropertyPath() != null
				&& !filter.getPropertyPath().contains(".")
				&& filter.getFilterOperator() == FilterOperator.EQ
				&& filter.getPropertyValue() instanceof Entity) {
			String idPath = filter.getPropertyPath() + ".id";
			if (getValueProviderFor(clazz, idPath) != null) {
				return new DomainFilter(idPath,
						((Entity) filter.getPropertyValue()).getId());
			} else {
				return filter;
			}
		} else {
			return filter;
		}
	}

	private void prepareClassDescriptor(DomainClassDescriptor classDescriptor) {
		try {
			Class clazz = classDescriptor.clazz;
			classDescriptor.setDomainDescriptor(domainDescriptor);
			for (PropertyDescriptor pd : SEUtilities
					.getPropertyDescriptorsSortedByName(clazz)) {
				if (pd.getReadMethod() == null || pd.getWriteMethod() == null) {
					continue;
				}
				Method rm = pd.getReadMethod();
				Property property = Reflections.at(rm.getDeclaringClass())
						.property(pd.getName());
				DomainStoreProperty domainStorePropertyAnnotation = classDescriptor
						.resolveDomainStoreProperty(
								new AnnotationLocation(clazz, property));
				if ((rm.getAnnotation(Transient.class) != null
						&& rm.getAnnotation(DomainStoreDbColumn.class) == null)
						|| domainStorePropertyAnnotation != null) {
					if (domainStorePropertyAnnotation != null) {
						Field field = getField(clazz, pd.getName());
						field.setAccessible(true);
						domainStoreProperties.put(clazz, field.getName(),
								domainStorePropertyAnnotation);
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private <T extends Entity> Stream<T> query0(Class<T> clazz,
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
		Stream stream = token.applyEndOfStreamOperators();
		List<PreProvideTask<T>> preProvideTasks = domainDescriptor
				.getPreProvideTasks(clazz);
		for (PreProvideTask<T> preProvideTask : preProvideTasks) {
			stream = preProvideTask.wrap(stream);
		}
		return stream;
	}

	private List<DomainTransformEventPersistent> removeNonApplicableTransforms(
			Collection<DomainTransformEventPersistent> events) {
		return events.stream().filter(new InSubgraphFilter())
				.filter(domainDescriptor::customFilterPostProcess)
				.filter(event -> {
					if (!isCached(event.getObjectClass())) {
						return false;
					}
					if (event.getValueClass() != null && Entity.class
							.isAssignableFrom(event.getValueClass())) {
						if (!isCached(event.getValueClass())) {
							return false;
						}
					}
					return true;
				}).map(event -> filterForDomainStoreProperty(event))
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	void addValues(DomainListener listener) {
		try {
			listener.onAddValues(false);
			cache.stream(listener.getListenedClass()).forEach(listener::insert);
		} finally {
			listener.onAddValues(true);
		}
	}

	Entity ensureEntity(Class<? extends Entity> clazz, long id, long localId) {
		EntityLocatorMap map = EntityLayerObjects.get()
				.getServerAsClientInstanceEntityLocatorMap();
		localId = id < 0 ? localId : map.getLocalId(clazz, id);
		ClassIdLock lock = LockUtils.obtainClassIdLock(clazz, id);
		synchronized (lock) {
			// thereby preserving the 'entity uniqueness unless vacuumed'
			// constraint
			Entity visible = cache.get(clazz, id);
			if (visible != null) {
				return visible;
			}
			Entity existing = null;
			try {
				existing = cache.getAnyTransaction(clazz, id);
			} catch (RuntimeException e) {
				logger.warn("Exception in ensureEntity :: {}/{}/{}",
						clazz.getSimpleName(), id, localId);
				throw e;
			}
			// the Transactions.resolve calls force a visible version
			if (existing != null) {
				Transactions.resolve(existing, ResolvedVersionState.WRITE,
						false);
				cache.ensureVersion(existing);
				return existing;
			}
			if (localId != 0) {
				// this below is all a bit fancy - but is the only place where
				// said fanciness is needed
				Entity local = cache.getCreatedLocals().get(localId);
				Transactions.resolve(local, ResolvedVersionState.WRITE, false);
				Preconditions.checkState(local != null);
				// The created local is the 'domain identity', so need to
				// preserve it but revert
				// its field values before transforming
				Transactions.revertToDefaultFieldValues(local);
				local.setLocalId(localId);
				local.setId(id);
				// the current version (which will be committed)
				Transactions.copyIdFieldsToCurrentVersion(local);
				// only local-id objects created by this webapp client instance
				// will ever be put into the store cache in this phase - thus
				// preserving the "local ids don't collide because they're on
				// different transactions" logic of the cache
				cache.removeLocal(local);
				cache.put(local);
				TransformManager.registerLocalObjectPromotion(local);
				return local;
			} else {
				Entity entity = Transaction.current().create(clazz, this, id,
						0L);
				cache.put(entity);
				return entity;
			}
		}
	}

	<T extends Entity> T find(Class<T> clazz, long id) {
		T t = cache.get(clazz, id);
		if (t == null) {
			if (domainDescriptor.perClass.containsKey(clazz)
					&& domainDescriptor.perClass.get(clazz)
							.provideNotFullyLoadedOnStartup()
					&& id != 0) {
				EntityLocator locator = new EntityLocator(clazz, id, 0L);
				boolean toDomainCommitting = Transaction.current()
						.isToDomainCommitting();
				TransactionId lazyLoadTxId = lazyLoadAttempted.get(locator);
				if (lazyLoadTxId == null
						|| !Transactions.isCommitted(lazyLoadTxId)) {
					lazyObjectLoader.loadObject(clazz, id, 0);
					t = cache.get(clazz, id);
					if (t != null && !toDomainCommitting
							&& domainDescriptor.isEnqueueLazyLoad(locator)) {
						Transactions.enqueueLazyLoad(locator);
					}
				}
				// can only record checks during 'toDomainCommitting' (otherwise
				// located objects (and refs to them) will be discarded at end
				// of tx)
				if (toDomainCommitting) {
					lazyLoadAttempted.put(locator,
							Transaction.current().getId());
				}
			}
		}
		if (t != null) {
			for (PreProvideTask task : domainDescriptor
					.getPreProvideTasks(clazz)) {
				try {
					if (!task.filter(t)) {
						return null;
					}
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
		Optional<Field> field = getFields(clazz).stream()
				.filter(f -> f.getName().equals(name)).findFirst();
		if (!field.isPresent()) {
			throw new RuntimeException(
					String.format("Field not available - %s.%s",
							clazz.getSimpleName(), name));
		}
		return field.get();
	}

	ObjectStore getTmDomainObjects() {
		return new DetachedCacheObjectStore(new DomainStoreEntityCache());
	}

	void index(Entity obj, boolean add, EntityCollation entityCollation,
			boolean committed) {
		Class<? extends Entity> clazz = obj.entityClass();
		DomainClassDescriptor<?> itemDescriptor = domainDescriptor.perClass
				.get(clazz);
		if (itemDescriptor != null) {
			itemDescriptor.index(obj, add, committed, entityCollation);
			itemDescriptor
					.getDependentObjectsWithDerivedProjections(obj,
							entityCollation == null ? null
									: entityCollation
											.getTransformedPropertyNames())
					.forEach(e -> index(e, add, entityCollation, committed));
		}
	}

	<V extends Entity> boolean isRawValue(V v) {
		if (!Transaction.isInTransaction()) {
			return v.getId() != 0 || v.getLocalId() != 0;
		}
		V existing = (V) cache.get(v.toLocator());
		return existing == v;
	}

	// We only have one thread allowed here - but that doesn't block any
	// non-to-domain transactions
	// FIXME - mvcc.5 - review optimiseation
	/*
	 * Main remaining optimisation would be to remove unneccessary index() calls
	 * - where the before-and-after states are identical. But time spent is now
	 * ~1ms for 1000 transforms, so speed issues are possibly elsewhere
	 *
	 * That, however, is possibly better left to application-level code (at
	 * least to the DomainClassDescriptor instance) - see
	 * cc.alcina.framework.common.client.domain.DomainProjection.
	 * isIgnoreForIndexing(EntityCollation)
	 */
	synchronized void
			postProcess(DomainTransformPersistenceEvent persistenceEvent) {
		if (persistenceEvent.getDomainTransformLayerWrapper().persistentRequests
				.isEmpty()) {
			Transaction.endAndBeginNew();
			return;
		}
		Set<Throwable> causes = new LinkedHashSet<Throwable>();
		StringBuilder warnBuilder = new StringBuilder();
		long postProcessStart = 0;
		DomainStoreHealth health = getHealth();
		try {
			LooseContext.pushWithTrue(
					TransformManager.CONTEXT_DO_NOT_POPULATE_SOURCE);
			LooseContext.setTrue(CONTEXT_IN_POST_PROCESS);
			LooseContext.set(LiSet.CONTEXT_NON_DOMAIN_NOTIFIER,
					new NonDomainNotifier() {
						@Override
						public void notifyNonDomain(LiSet liSet, Entity e) {
							logger.warn(
									"Non-domain delta in tx: \nliset: {}\nentity: {}\nevent:\n{}",
									liSet, e, persistenceEvent);
						}
					});
			TransformManager.get().setIgnorePropertyChanges(true);
			postProcessStart = System.currentTimeMillis();
			MetricLogging.get().start("post-process");
			Transaction.ensureEnded();
			Transaction.beginDomainPreparing();
			Date transactionCommitTime = persistenceEvent
					.getDomainTransformLayerWrapper().persistentRequests.get(0)
							.getTransactionCommitTime();
			Transaction.current().toDomainCommitting(
					testSensitiveTimestamp(transactionCommitTime), this,
					applyTxToGraphCounter.getAndIncrement(),
					persistenceEvent.getMaxPersistedRequestId());
			// opportunistically load any lazy loads in this tx phase, to ensure
			// they become a non-vacuumable part of the graph
			Transactions.getEnqueuedLazyLoads().forEach(Domain::find);
			postProcessThread = Thread.currentThread();
			postProcessEvent = persistenceEvent;
			health.domainStorePostProcessStartTime = System.currentTimeMillis();
			List<DomainTransformEventPersistent> events = (List) persistenceEvent
					.getDomainTransformLayerWrapper().persistentEvents;
			List<DomainTransformEventPersistent> filtered = removeNonApplicableTransforms(
					events);
			TransformCollation collation = new TransformCollation(filtered);
			// this is also checked in TransformCommit
			// filtered.removeIf(collation::isCreatedAndDeleted);
			Set<Long> uncommittedToLocalGraphLids = new LinkedHashSet<Long>();
			for (DomainTransformEventPersistent transform : filtered) {
				postProcessTransform = transform;
				// remove from indicies before first change - and only if
				// preexisting object
				EntityCollation entityCollation = collation
						.forLocator(transform.toObjectLocator());
				DomainTransformEvent last = entityCollation.last();
				DomainTransformEvent first = entityCollation.first();
				Entity entity = transform
						.getTransformType() == TransformType.CREATE_OBJECT
								? null
								: transformManager.getObject(transform, true);
				if (last.getTransformType() == TransformType.DELETE_OBJECT
						&& first.getTransformType() != TransformType.CREATE_OBJECT) {
					// This checks if we're trying to handle deletion of
					// lazy objects
					if (entity == null) {
						continue;
					}
				}
				if (transform.getTransformType() != TransformType.CREATE_OBJECT
						&& first == transform) {
					if (entity != null) {
						index(entity, false, entityCollation, true);
					} else {
						logger.warn("Null entity for index - {}",
								transform.toObjectLocator());
					}
				}
				try {
					transformManager.apply(transform);
				} catch (DomainTransformException dtex) {
					if (dtex.getType() == DomainTransformExceptionType.SOURCE_ENTITY_NOT_FOUND
							&& transform
									.getTransformType() == TransformType.DELETE_OBJECT) {
						warnBuilder.append(String.format("%s\n%s\n\n",
								transform, dtex.getType(), dtex.getMessage()));
					} else if (dtex
							.getType() == DomainTransformExceptionType.TARGET_ENTITY_NOT_FOUND
							&& transform
									.getTransformType() == TransformType.REMOVE_REF_FROM_COLLECTION) {
						warnBuilder.append(String.format("%s\n%s\n\n",
								transform, dtex.getType(), dtex.getMessage()));
					} else {
						causes.add(dtex);
					}
				}
				if (transform
						.getTransformType() == TransformType.CREATE_OBJECT) {
					ClientInstance requestInstance = persistenceEvent
							.getPersistedRequests().iterator().next()
							.getClientInstance();
					transformManager.registerClusterLocalObjectPromotion(
							transform, requestInstance);
				}
				if (transform.getTransformType() != TransformType.DELETE_OBJECT
						&& last == transform) {
					if (entity == null && transform
							.getTransformType() == TransformType.CREATE_OBJECT) {
						entity = transformManager
								.getObjectForCreationTransform(transform, true);
					}
					if (entity != null) {
						ExTransformDbMetadata dbMetadata = transform
								.getExTransformDbMetadata();
						if (dbMetadata != null) {
							dbMetadata.applyTo(entity);
						} else {
							logger.warn("No db metadata for {}",
									entity.toStringId());
						}
						index(entity, true, entityCollation, true);
					} else {
						logger.warn("Null entity for index - {}",
								transform.toObjectLocator());
					}
				}
			}
			topicBeforeDomainCommitted().publish(persistenceEvent);
			Transaction.current()
					.toDomainCommitted(persistenceEvent.getPosition());
		} catch (Exception e) {
			logger.warn("post process exception - pre final", e);
			Transaction.current().toDomainAborted();
			causes.add(e);
		} finally {
			topicAfterDomainCommitted().publish(persistenceEvent);
			TransformManager.get().setIgnorePropertyChanges(false);
			health.domainStorePostProcessStartTime = 0;
			postProcessThread = null;
			postProcessTransform = null;
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
					AlcinaTopics.devWarning.publish(warn);
				}
				if (!causes.isEmpty()) {
					UmbrellaException umby = new UmbrellaException(causes);
					causes.iterator().next().printStackTrace();
					DomainStoreUpdateException updateException = new DomainStoreUpdateException(
							umby);
					topicUpdateException.publish(updateException);
					if (updateException.ignoreForDomainStoreExceptionCount) {
						logger.warn("Domain store update warning [non-fatal]",
								updateException);
					} else {
						health.onException(updateException);
						logger.warn("Update exception persistence event :: {}",
								persistenceEvent);
						try {
							String debugString = persistenceEvents.getQueue()
									.toDebugString();
							logger.warn(
									"Update exception transform request queue data :: {}",
									debugString);
						} catch (Exception e) {
							logger.warn(
									"Update exception - exception getting queue debug string");
							e.printStackTrace();
						}
						throw updateException;
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
			LooseContext.push();
			for (Hint hint : query.getHints()) {
				boolean resolved = Registry.query(HintResolver.class)
						.implementations()
						.anyMatch(resolver -> resolver.resolve(hint, query));
				Preconditions.checkState(resolved);
			}
			query.getContextProperties()
					.forEach((k, v) -> LooseContext.set(k, v));
			return query0(clazz, query);
		} finally {
			LooseContext.pop();
		}
	}

	public static class Builder {
		private DomainStoreDescriptor descriptor;

		private ThreadPoolExecutor warmupExecutor;

		private RetargetableDataSource dataSource;

		private DomainLoaderType loaderType;

		private boolean readOnly = false;

		private String name = "store.default";

		private DomainStore reuseStore;

		public DomainStore register() {
			return register(true);
		}

		public DomainStore register(boolean registerDescriptorClasses) {
			DomainStore domainStore = new DomainStore();
			domainStore.domainDescriptor = descriptor;
			domainStore.writable = !readOnly;
			domainStore.name = name;
			domainStore.reuseTransformerStore = reuseStore;
			Preconditions.checkNotNull(loaderType);
			switch (loaderType) {
			case Database:
				domainStore.loader = new DomainStoreLoaderDatabase(domainStore,
						dataSource, warmupExecutor);
				break;
			default:
				throw new UnsupportedOperationException();
			}
			stores().register(domainStore, registerDescriptorClasses);
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

		public Builder withReuseTransformer(DomainStore reuseStore) {
			this.reuseStore = reuseStore;
			return this;
		}

		enum DomainLoaderType {
			Database, Remote
		}
	}

	@Registration.Singleton(SizeProvider.class)
	public static class DomainSizeProvider implements SizeProvider {
		@Override
		public int getSize(OneToManyMultipleSummary summary, Entity source) {
			try {
				String providerMethodName = summary
						.getCollectionAccessorMethodName();
				Method method = source.getClass().getMethod(providerMethodName,
						new Class[0]);
				Collection<? extends Entity> collection = (Collection<? extends Entity>) method
						.invoke(source, new Object[0]);
				return collection.size();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
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

		public Exception lastException;

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
			DomainTransformEventPersistent postProcessTransform2 = postProcessTransform;
			Thread postProcessThread2 = postProcessThread;
			if (time > 100 && postProcessThread2 != null) {
				String prefix = time > 5000 ? "Very " : "";
				logger.warn(
						"{}Long postprocess time - {} ms - {}\n{}\n\n{}\n\n",
						prefix, time, postProcessThread2, postProcessTransform2,
						SEUtilities.getStacktraceSlice(postProcessThread2,
								LONG_POST_PROCESS_TRACE_LENGTH, 0));
			}
			return time;
		}

		public long getTimeInVacuum() {
			long time = Transactions.stats().getTimeInVacuum();
			Thread vacuumThread = Transactions.stats().getActiveVacuumThread();
			if (time > 100) {
				if (vacuumThread != null) {
					logger.warn("Long vacuum time - {} ms - {}\n{}\n\n", time,
							vacuumThread,
							SEUtilities.getStacktraceSlice(vacuumThread,
									LONG_POST_PROCESS_TRACE_LENGTH, 0));
				} else {
					// very unlikely this is null...wazzup?
					Transactions.stats().debugVacuum();
				}
			}
			return time;
		}

		public long getTransformEventQueueLength() {
			return getPersistenceEvents().getQueue().getLength();
		}

		public long getTransformEventQueueOldestTx() {
			return getPersistenceEvents().getQueue().getOldestTx();
		}

		void onException(DomainStoreUpdateException updateException) {
			domainStoreExceptionCount.incrementAndGet();
			lastException = updateException;
		}
	}

	@Registration.Singleton
	public static class DomainStores {
		// not concurrent, handle in methods
		private Map<DomainDescriptor, DomainStore> descriptorMap = new LinkedHashMap<>();

		private Map<Class, DomainStore> classMap = Collections
				.unmodifiableMap(new LinkedHashMap<>());

		// immutable, swap on change
		private Collection<DomainStore> stores;

		private DomainStore writableStore;

		DomainStoresDomainHandler storesHandler = new DomainStoresDomainHandler();

		Logger logger = LoggerFactory.getLogger(getClass());

		private DomainStores() {
			Domain.registerHandler(storesHandler);
		}

		public void appShutdown() {
			descriptorMap.values().forEach(DomainStore::appShutdown);
		}

		public synchronized void deregister(DomainStore store) {
			store.close();
			descriptorMap.remove(store.domainDescriptor);
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

		public synchronized void register(DomainStore store,
				boolean registerDescriptorClasses) {
			descriptorMap.put(store.domainDescriptor, store);
			if (registerDescriptorClasses) {
				Map<Class, DomainStore> classMap = new LinkedHashMap<>(
						this.classMap);
				store.domainDescriptor.getHandledClasses().forEach(clazz -> {
					Preconditions.checkState(!classMap.containsKey(clazz));
					classMap.put(clazz, store);
				});
				this.classMap = Collections.unmodifiableMap(classMap);
			}
			stores = Collections.unmodifiableCollection(descriptorMap.values()
					.stream().collect(Collectors.toList()));
		}

		// not synchronized - requires that classMap modification not conflict
		// (in code) - which it doesn't, since classMap is swapped
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

		// not synchronized, since used for each transaction (but immutable
		// source)
		public Stream<DomainStore> stream() {
			return stores.stream();
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
			public <V extends Entity> V find(EntityLocator locator) {
				return storeHandler(locator.getClazz()).find(locator);
			}

			@Override
			public <V extends Entity> V find(V v) {
				return v == null ? null : storeHandler(v.entityClass()).find(v);
			}

			@Override
			public <V extends Entity> boolean isDomainVersion(V v) {
				return v == null ? false
						: classMap.get(v.entityClass()) == null
								? (v.getId() != 0 || v.getLocalId() != 0)
								: storeHandler(v.entityClass())
										.isDomainVersion(v);
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
			public <V extends Entity> int size(Class<V> clazz) {
				return storeHandler(clazz).size(clazz);
			}

			@Override
			public <V extends Entity> Stream<V> stream(Class<V> clazz) {
				return storeHandler(clazz).stream(clazz);
			}

			@Override
			public boolean wasRemoved(Entity entity) {
				Class clazz = entity.entityClass();
				return storeHandler(clazz).wasRemoved(entity);
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
			super(umby);
			this.umby = umby;
		}
	}

	public static class QueryPool {
		private final ForkJoinPool pool;

		private volatile Transaction transaction;

		private LooseContextInstance contextInstance;

		QueryPool() {
			pool = new ForkJoinPool(
					ResourceUtilities.getInteger(DomainStore.class,
							"queryPoolSize"),
					new WorkerThreadFactory(), null, false);
			try {
				Field workerNamePrefix = ForkJoinPool.class
						.getDeclaredField("workerNamePrefix");
				workerNamePrefix.setAccessible(true);
				workerNamePrefix.set(pool, "domainStore-queryPool");
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		/*
		 * The use of a reference to the stream (rather than the stream) is so
		 * that the actual stream used by the callable can be changed based on
		 * the context and serial arguments
		 */
		public <T> T call(Callable<T> callable,
				ObjectWrapper<Stream<? extends Entity>> streamRef,
				boolean parallel) {
			boolean runInPool = false;
			parallel &= !LooseContext.is(CONTEXT_SERIAL_QUERY);
			if (parallel) {
				synchronized (this) {
					Transaction current = Transaction.current();
					if (transaction == null) {
						runInPool = true;
						transaction = current;
						this.contextInstance = LooseContext.getContext()
								.snapshot();
					}
				}
			}
			if (runInPool) {
				try {
					streamRef.set(streamRef.get().parallel());
					return pool.submit(callable).get();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				} finally {
					synchronized (this) {
						contextInstance = null;
						transaction = null;
					}
				}
			} else {
				streamRef.set(streamRef.get().sequential());
				try {
					return callable.call();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}

		class WorkerThread extends ForkJoinWorkerThread {
			protected WorkerThread(ForkJoinPool pool) {
				super(pool);
			}

			@Override
			public void run() {
				Transaction.setSupplier(() -> transaction);
				try {
					LooseContext.push();
					if (contextInstance != null) {
						LooseContext.putSnapshotProperties(contextInstance);
					}
					super.run();
				} finally {
					LooseContext.pop();
					Transaction.setSupplier(null);
				}
			}
		}

		class WorkerThreadFactory implements ForkJoinWorkerThreadFactory {
			@Override
			public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
				return new WorkerThread(pool);
			}
		}
	}

	private class AssociationIdProvider implements IndexedValueProvider {
		private Class clazz;

		private String propertyName;

		public AssociationIdProvider(Class clazz, String propertyName) {
			this.clazz = clazz;
			this.propertyName = propertyName;
		}

		@Override
		public FilterCost estimateFilterCost(int entityCount,
				DomainFilter... filters) {
			return FilterCost.lookupProjectionCost();
		}

		@Override
		public StreamOrSet getKeyMayBeCollection(Object value) {
			Property property = Reflections.at(clazz).property(propertyName);
			Association association = property.annotation(Association.class);
			Class<? extends Entity> associationClass = association
					.implementationClass();
			Property associationProperty = Reflections.at(associationClass)
					.property(association.propertyName());
			Collection<Long> ids = CommonUtils.wrapInCollection(value);
			Stream<Entity> stream = ids.stream()
					.map(id -> (Entity) cache.get(associationClass, id))
					.filter(Objects::nonNull);
			boolean propertyIsSet = Set.class
					.isAssignableFrom(associationProperty.getType());
			if (propertyIsSet) {
				stream = stream
						.map(e -> ((Set<Entity>) associationProperty.get(e)))
						.flatMap(Collection::stream);
			} else {
				stream = stream.map(e -> (Entity) associationProperty.get(e))
						.filter(Objects::nonNull);
			}
			return new StreamOrSet<>(stream);
		}
	}

	private class CacheIdProvider implements IndexedValueProvider {
		private final Class clazz;

		private CacheIdProvider(Class clazz) {
			this.clazz = clazz;
		}

		@Override
		public FilterCost estimateFilterCost(int entityCount,
				DomainFilter... filters) {
			return FilterCost.lookupProjectionCost();
		}

		@Override
		public StreamOrSet getKeyMayBeCollection(Object value) {
			if (value instanceof Collection) {
				return new StreamOrSet(((Collection<Long>) value).stream()
						.map(id -> (Entity) cache.get(this.clazz, id))
						.filter(Objects::nonNull));
			} else {
				long id = (long) value;
				return new StreamOrSet(Stream.of(cache.get(this.clazz, id))
						.filter(Objects::nonNull));
			}
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
					IndexedValueProvider valueProvider = getValueProviderFor(
							clazz, filter.getPropertyPath());
					if (valueProvider != null) {
						switch (filter.getFilterOperator()) {
						case EQ:
						case IN:
							filterCost = valueProvider
									.estimateFilterCost(entityCount, filter);
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
			return Domain.query(clazz).filter(propertyName, value).find();
		}

		@Override
		public <V extends Entity> V detachedVersion(V v) {
			return (V) Domain.query(v.entityClass()).filterById(v.getId())
					.find();
		}

		@Override
		public <V extends Entity> V find(Class clazz, long id) {
			return (V) DomainStore.this.find(clazz, id);
		}

		@Override
		public <V extends Entity> V find(EntityLocator locator) {
			if (locator.id != 0) {
				return find(locator.clazz, locator.id);
			}
			V entity = cache.get(locator);
			if (entity == null) {
				entity = (V) promotedEntitiesByPrePromotion.get(locator);
				if (entity != null) {
					logger.warn(
							"Found entity {} for locator {} from promoted map",
							entity.toLocator(), locator);
				} else {
					logger.warn(
							"Did not find entity  for locator {} from promoted map",
							locator);
				}
			}
			if (entity == null) {
				ClientInstance clientInstance = AuthenticationPersistence.get()
						.getClientInstance(locator.getClientInstanceId());
				if (clientInstance == null) {
					logger.warn(
							"Unable to find clientinstance for local find:\n\t locator: {}"
									+ "\n\t Current user: {}"
									+ "\n\t Current pm instance: {}"
									+ "\n\t Service instance: {}",
							locator, PermissionsManager.get().getUser(),
							PermissionsManager.get().getClientInstance(),
							EntityLayerObjects.get()
									.getServerAsClientInstance());
				} else {
					EntityLocatorMap locatorMap = TransformCommit.get()
							.getLocatorMapForClient(clientInstance,
									Ax.isTest());
					EntityLocator persistentLocator = locatorMap
							.getForLocalId(locator.getLocalId());
					if (persistentLocator != null) {
						return find(persistentLocator);
					}
				}
			}
			return entity;
		}

		@Override
		public <V extends Entity> V find(V v) {
			if (!v.domain().wasPersisted()) {
				if (v.getLocalId() == 0) {
					return null;
				}
				/*
				 * not yet persisted, tx-local
				 */
				return (V) cache.get(v.toLocator());
			} else {
				return (V) find(v.entityClass(), v.getId());
			}
		}

		@Override
		public <V extends Entity> boolean isDomainVersion(V v) {
			return isRawValue(v);
		}

		@Override
		public <V extends Entity> List<V> listByProperty(Class<V> clazz,
				String propertyName, Object value) {
			return Domain.query(clazz).filter(propertyName, value).list();
		}

		@Override
		public <V extends Entity> DomainQuery<V> query(Class<V> clazz) {
			return new DomainStoreQuery<>(clazz, DomainStore.this);
		}

		@Override
		public <V extends Entity> V resolve(V v) {
			return Transactions.resolve(v, ResolvedVersionState.READ, false);
		}

		@Override
		public <V extends Entity> int size(Class<V> clazz) {
			return cache.size(clazz);
		}

		@Override
		public <V extends Entity> Stream<V> stream(Class<V> clazz) {
			return Domain.query(clazz).stream();
		}

		@Override
		public boolean wasRemoved(Entity entity) {
			return !cache.contains(entity);
		}

		<T extends Entity> List<T> list(Class<T> clazz) {
			return Domain.query(clazz).list();
		}
	}

	class DomainStorePersistenceListener
			implements DomainTransformPersistenceListener {
		@Override
		public boolean isAllVmEventsListener() {
			return true;
		}

		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformPersistenceEvent evt) {
			switch (evt.getPersistenceEventType()) {
			case PREPARE_COMMIT:
				break;
			case COMMIT_ERROR:
				break;
			case COMMIT_OK:
				postProcess(evt);
				break;
			}
		}
	}

	/*
	 * This listener is only fired from property changes (not, for instance,
	 * during postProcess() - so use of DomainTransformEvent.getNewValue() etc
	 * is appropriate
	 */
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
			Property property = event.getPropertyName() == null ? null
					: Reflections.at(event.getObjectClass())
							.property(event.getPropertyName());
			switch (event.getTransformType()) {
			case ADD_REF_TO_COLLECTION:
			case REMOVE_REF_FROM_COLLECTION:
				/*
				 * These don't cause a reindex
				 */
				return;
			}
			if (event.getTransformType() != TransformType.CREATE_OBJECT) {
				switch (event.getTransformType()) {
				case CHANGE_PROPERTY_REF:
				case CHANGE_PROPERTY_SIMPLE_VALUE:
				case NULL_PROPERTY_REF: {
					DomainProperty domainProperty = property
							.annotation(DomainProperty.class);
					if (domainProperty != null
							&& !domainProperty.reindexOnChange()) {
						return;
					}
				}
				}
			}
			Entity entity = tm.getObject(event);
			if (event.getTransformType() != TransformType.CREATE_OBJECT) {
				switch (event.getTransformType()) {
				case CHANGE_PROPERTY_REF:
				case CHANGE_PROPERTY_SIMPLE_VALUE:
				case NULL_PROPERTY_REF: {
					try {
						tm.setIgnorePropertyChanges(true);
						/*
						 * undo last property change
						 */
						property.set(entity, event.getOldValue());
						store.index(entity, false, null, false);
						/*
						 * redo
						 */
						property.set(entity, event.getNewValue());
					} finally {
						tm.setIgnorePropertyChanges(false);
					}
					break;
				}
				default:
					store.index(entity, false, null, false);
				}
			}
			if (event.getTransformType() != TransformType.DELETE_OBJECT) {
				store.index(entity, true, null, false);
			}
		}
	}

	class InSubgraphFilter
			implements Predicate<DomainTransformEventPersistent> {
		private boolean filterUnknownTransformProperties;

		public InSubgraphFilter() {
			filterUnknownTransformProperties = ResourceUtilities
					.is(DomainStore.class, "filterUnknownTransformProperties");
		}

		@Override
		public boolean test(DomainTransformEventPersistent evt) {
			if (!domainDescriptor.applyPostTransform(evt.getObjectClass(),
					evt)) {
				return false;
			}
			if (filterUnknownTransformProperties) {
				if (Ax.notBlank(evt.getPropertyName())) {
					PropertyDescriptor descriptor = SEUtilities
							.getPropertyDescriptorByName(evt.getObjectClass(),
									evt.getPropertyName());
					if (descriptor == null) {
						return false;
					}
				}
			}
			switch (evt.getTransformType()) {
			case ADD_REF_TO_COLLECTION:
			case REMOVE_REF_FROM_COLLECTION:
			case CHANGE_PROPERTY_REF:
				return CommonUtils.isEnumOrEnumSubclass(evt.getValueClass())
						|| domainDescriptor
								.applyPostTransform(evt.getValueClass(), evt);
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

		public void appendEvaluatedValueFilter(StreamOrSet<E> indexedValues) {
			if (indexedValues == null) {
				stream = Stream.empty();
				empty = true;
			} else {
				if (stream == null) {
					stream = indexedValues.provideStream();
				} else {
					stream = stream
							.filter(indexedValues.provideSet()::contains);
				}
			}
		}

		public void appendFilter(Predicate predicate) {
			stream = ensureStream().filter(predicate);
		}

		public Stream applyEndOfStreamOperators() {
			ensureStream();
			if (query.getComparator() != null) {
				stream = stream.sorted(query.getComparator());
			}
			if (query.getLimit() != -1) {
				stream = stream.limit(query.getLimit());
			}
			return stream;
		}

		public Stream<E> ensureStream() {
			if (stream == null) {
				if (query.getSourceStream().isPresent()) {
					stream = query.getSourceStream().get();
				} else {
					stream = streamFromCacheValues();
				}
				if (LooseContext.has(DomainQuery.CONTEXT_DEBUG_CONSUMER)) {
					DomainQuery.DebugConsumer consumer = LooseContext
							.get(DomainQuery.CONTEXT_DEBUG_CONSUMER);
					consumer.queryToken = this;
					stream = stream.peek(LooseContext
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
		@Override
		public Entity getObject(DomainTransformEvent dte,
				boolean ignoreSource) {
			if (dte.getTransformType() == TransformType.CREATE_OBJECT) {
				// avoid fallback to lazy loader
				return null;
			}
			return super.getObject(dte, ignoreSource);
		}

		public Entity getObjectForCreationTransform(DomainTransformEvent dte,
				boolean ignoreSource) {
			return super.getObject(dte, ignoreSource);
		}

		@Override
		public boolean isIgnorePropertyChanges() {
			return true;
		}

		public void registerClusterLocalObjectPromotion(
				DomainTransformEventPersistent transform,
				ClientInstance clientInstance) {
			Entity promoted = getObjectForCreationTransform(transform, true);
			EntityLocator locator = new EntityLocator();
			locator.setLocalId(transform.getObjectLocalId());
			locator.setClientInstanceId(clientInstance.getId());
			locator.setClazz(promoted.entityClass());
			promotedEntitiesByPrePromotion.put(locator, promoted);
		}

		@Override
		protected void beforeDirectCollectionModification(Entity obj,
				String propertyName, Object newTargetValue,
				CollectionModificationType collectionModificationType) {
			Transactions.resolve(obj, ResolvedVersionState.WRITE, false);
		}

		@Override
		protected Entity getEntityForCreate(DomainTransformEvent event) {
			return ensureEntity(event.getObjectClass(), event.getObjectId(),
					event.getObjectLocalId());
		}

		@Override
		protected void initObjectStore() {
			store = (DetachedCacheObjectStore) getTmDomainObjects();
			setObjectStore(store);
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
