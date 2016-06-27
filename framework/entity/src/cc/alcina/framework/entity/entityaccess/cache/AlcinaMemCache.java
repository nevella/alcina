package cc.alcina.framework.entity.entityaccess.cache;

import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.sql.DataSource;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.cache.BaseProjection;
import cc.alcina.framework.common.client.cache.CacheDescriptor;
import cc.alcina.framework.common.client.cache.CacheDescriptor.CacheTask;
import cc.alcina.framework.common.client.cache.CacheDescriptor.PreProvideTask;
import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.cache.CacheItemDescriptor;
import cc.alcina.framework.common.client.cache.CacheListener;
import cc.alcina.framework.common.client.cache.CacheLookup;
import cc.alcina.framework.common.client.cache.CacheLookupDescriptor;
import cc.alcina.framework.common.client.cache.CacheProjection;
import cc.alcina.framework.common.client.cache.ComplexFilter;
import cc.alcina.framework.common.client.cache.Domain;
import cc.alcina.framework.common.client.cache.Domain.DomainHandler;
import cc.alcina.framework.common.client.cache.ModificationChecker;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;
import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException.DomainTransformExceptionType;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedCacheObjectStore;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceListener;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.entityaccess.TransformPersister;
import cc.alcina.framework.entity.projection.GraphProjection;

import com.google.gwt.event.shared.UmbrellaException;

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
@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class AlcinaMemCache implements RegistrableService {
	private static final int LONG_LOCK_TRACE_LENGTH = 160;

	private static final int MAX_QUEUED_TIME = 500;

	private static AlcinaMemCache singleton;

	public static final String TOPIC_UPDATE_EXCEPTION = AlcinaMemCache.class
			.getName() + ".TOPIC_UPDATE_EXCEPTION";

	public static final String CONTEXT_DEBUG_QUERY_METRICS = AlcinaMemCache.class
			.getName() + ".CONTEXT_DEBUG_QUERY_METRICS";

	public static final String CONTEXT_WILL_PROJECT_AFTER_READ_LOCK = AlcinaMemCache.class
			.getName() + ".CONTEXT_WILL_PROJECT_AFTER_READ_LOCK";

	// while debugging, prevent reentrant locks
	public static final String CONTEXT_NO_LOCKS = AlcinaMemCache.class.getName()
			+ ".CONTEXT_NO_LOCKS";

	public static final String WRAPPED_OBJECT_REF_INTEGRITY = "WRAPPED_OBJECT_REF_INTEGRITY";

	public static void checkActiveTransaction() {
		if (!get().transactional.transactionActiveInCurrentThread()) {
			throw new RuntimeException(
					"requires transaction in current thread");
		}
	}

	public static PerThreadTransaction ensureActiveTransaction() {
		return get().transactional.ensureTransaction();
	}

	public static void ensureReferredPropertyIsTransactional(
			HasIdAndLocalId hili, String propertyName) {
		// target, even if new object, will still be equals() to old, so no
		// property change will be fired, which is the desired behaviour
		HasIdAndLocalId target = (HasIdAndLocalId) Reflections
				.propertyAccessor().getPropertyValue(hili, propertyName);
		if (target != null) {
			target = ensureTransactional(target);
			Reflections.propertyAccessor().setPropertyValue(hili, propertyName,
					target);
		}
	}

	public static <V extends HasIdAndLocalId> V ensureTransactional(V value) {
		return get().transactional.ensureTransactional(value);
	}

	public static AlcinaMemCache get() {
		if (singleton == null) {
			// not thread-safe, make sure it's initialised single-threaded on
			// app startup
			singleton = new AlcinaMemCache();
			Registry.registerSingleton(AlcinaMemCache.class, singleton);
		}
		return singleton;
	}

	private Map<PropertyDescriptor, JoinTable> joinTables;

	private Map<Class, List<PdOperator>> descriptors;

	// class,pName
	private UnsortedMultikeyMap<PropertyDescriptor> manyToOneRev;

	private UnsortedMultikeyMap<PropertyDescriptor> oneToOneRev;

	private UnsortedMultikeyMap<PropertyDescriptor> memCacheColumnRev;

	private Multimap<Class, List<ColumnDescriptor>> columnDescriptors;

	private Map<PropertyDescriptor, Class> propertyDescriptorFetchTypes = new LinkedHashMap<PropertyDescriptor, Class>();

	SubgraphTransformManagerRemoteOnly transformManager;

	private CacheDescriptor cacheDescriptor;

	private TaggedLogger sqlLogger = Registry.impl(TaggedLoggers.class)
			.getLogger(Domain.class, TaggedLogger.DEBUG);

	private TaggedLogger metricLogger = Registry.impl(TaggedLoggers.class)
			.getLogger(Domain.class, TaggedLogger.METRIC);

	private TaggedLogger warnLogger = Registry.impl(TaggedLoggers.class)
			.getLogger(Domain.class, TaggedLogger.WARN, TaggedLogger.INFO,
					TaggedLogger.DEBUG);

	private ThreadLocal<PerThreadTransaction> transactions = new ThreadLocal() {
	};

	private ConcurrentHashMap<Thread, Long> lockStartTime = new ConcurrentHashMap<>();

	private TopicListener<Thread> resetListener = new TopicListener<Thread>() {
		@Override
		public void topicPublished(String key, Thread message) {
			transactional.transactionFinished();
		}
	};

	private TopicListener<Thread> persistingListener = new TopicListener<Thread>() {
		@Override
		public void topicPublished(String key, Thread message) {
			transactional.transactionCommitting();
		}
	};

	DetachedEntityCache cache;

	public DetachedEntityCache getCache() {
		return this.cache;
	}

	private MemCachePersistenceListener persistenceListener;

	private boolean initialised = false;

	public Transactional transactional = new Transactional();

	private ModificationCheckerSupport modificationChecker;

	private Field modificationCheckerField;

	private boolean dumpLocks;

	private boolean collectLockAcquisitionPoints;

	private LinkedList<String> recentLockAcquisitions = new LinkedList<String>();

	/**
	 * Certain post-list triggers can writeLock() without causing readlock
	 * issues (because they deal with areas of the subgraph that the app
	 * guarantees won't cause problems with other reads) - but they do block
	 * writeLock acquisition
	 */
	volatile Object writeLockSubLock = null;

	private ReentrantReadWriteLockWithThreadAccess mainLock = new ReentrantReadWriteLockWithThreadAccess(
			true);

	private ReentrantReadWriteLock subgraphLock = new ReentrantReadWriteLock(
			true);

	private BackupLazyLoader backupLazyLoader;

	private boolean initialising;

	private boolean lockingDisabled;

	private long lastLockingDisabledMessage;

	private Set<Thread> waitingOnWriteLock = Collections
			.synchronizedSet(new LinkedHashSet<Thread>());

	private int maxLockQueueLength;

	private int originalTransactionIsolation;

	private UnsortedMultikeyMap<Field> memcacheTransientFields = new UnsortedMultikeyMap<Field>(
			2);

	private UnsortedMultikeyMap<AlcinaMemCacheTransient> memcacheTransientProperties = new UnsortedMultikeyMap<>(
			2);

	private ThreadPoolExecutor warmupExecutor;

	private DataSource dataSource;

	// only access via synchronized code
	CountingMap<Connection> warmupConnections = new CountingMap<>();

	MultikeyMap<PdOperator> operatorsByClass = new UnsortedMultikeyMap<>(2);

	private boolean debug;

	Multimap<Class, List<BaseProjectionHasEquivalenceHash>> cachingProjections = new Multimap<Class, List<BaseProjectionHasEquivalenceHash>>();

	private List<LaterLookup> warmupLaterLookups = new ArrayList<>();

	boolean checkModificationWriteLock = false;

	private AlcinaMemCacheHealth health = new AlcinaMemCacheHealth();

	private Thread postProcessWriterThread;

	private Timer timer;

	public AlcinaMemCache() {
		ThreadlocalTransformManager.threadTransformManagerWasResetListenerDelta(
				resetListener, true);
		TransformPersister.persistingTransformsListenerDelta(persistingListener,
				true);
		persistenceListener = new MemCachePersistenceListener();
		maxLockQueueLength = ResourceUtilities.getInteger(AlcinaMemCache.class,
				"maxLockQueueLength", 120);
		Domain.registerHandler(new AlcinaMemCacheDomainHandler());
	}

	class AlcinaMemCacheDomainHandler implements DomainHandler {
		@Override
		public <V extends HasIdAndLocalId> V resolveTransactional(
				CacheListener listener, V value, Object[] path) {
			return transactional.resolveTransactional(listener, value, path);
		}

		@Override
		public <V extends HasIdAndLocalId> V transactionalFind(Class clazz,
				long id) {
			return (V) transactional.find(clazz, id);
		}
	}

	public void addValues(CacheListener listener) {
		for (Object o : cache.values(listener.getListenedClass())) {
			listener.insert((HasIdAndLocalId) o);
		}
	}

	@Override
	public void appShutdown() {
		if (timer != null) {
			timer.cancel();
		}
	}

	public <T extends HasIdAndLocalId> Set<T> asSet(Class<T> clazz) {
		return new AlcinaMemCacheQuery().ids(getIds(clazz)).raw().asSet(clazz);
	}

	public void dumpLocks() {
		System.out.println("MemCache-main: " + mainLock);
		System.out.println("MemCache-subgraph: " + subgraphLock);
	}

	public void enableAndAddValues(CacheListener listener) {
		listener.setEnabled(true);
		addValues(listener);
	}

	public Set<Long> filterByExisting(Class clazz, boolean returnIfNotInGraph,
			List<Long> ids) {
		Set<Long> result = new LinkedHashSet<Long>();
		for (Long id : ids) {
			boolean add = cache.get(clazz, id) == null ^ !returnIfNotInGraph;
			if (add) {
				result.add(id);
			}
		}
		return result;
	}

	public List<DomainTransformEvent>
			filterInterestedTransforms(Collection<DomainTransformEvent> dtes) {
		return dtes.stream().filter(new InSubgraphFilter())
				.map(dte -> filterForMemcacheTransient(dte))
				.filter(Objects::nonNull).collect(Collectors.toList());
	}

	private DomainTransformEvent
			filterForMemcacheTransient(DomainTransformEvent dte) {
		switch (dte.getTransformType()) {
		case CREATE_OBJECT:
		case DELETE_OBJECT:
		case ADD_REF_TO_COLLECTION:
		case REMOVE_REF_FROM_COLLECTION:
			return dte;
		}
		AlcinaMemCacheTransient ann = memcacheTransientProperties
				.get(dte.getObjectClass(), dte.getPropertyName());
		if (ann == null) {
			return dte;
		}
		if (!ann.translatePropertyStoreWrites()) {
			return null;
		}
		return dte;
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz, long id) {
		return new AlcinaMemCacheQuery().id(id).find(clazz);
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz, String key,
			Object value) {
		return findOrCreate(clazz, key, value, false);
	}

	public <T extends HasIdAndLocalId> T find(HiliLocator locator) {
		return (T) find(locator.clazz, locator.id);
	}

	public <T extends HasIdAndLocalId> T find(T t) {
		return find(new HiliLocator(t));
	}

	public <T extends HasIdAndLocalId> T findOrCreate(Class<T> clazz,
			String key, Object value, boolean createIfNonexistent) {
		return findOrCreate(clazz, key, value, null, null, createIfNonexistent,
				false);
	}

	public <T extends HasIdAndLocalId> T findOrCreate(Class<T> clazz,
			String key1, Object value1, String key2, Object value2,
			boolean createIfNonexistent, boolean raw) {
		AlcinaMemCacheQuery query = new AlcinaMemCacheQuery().filter(key1,
				value1);
		if (raw) {
			query.raw();
		}
		if (key2 != null) {
			query.filter(key2, value2);
		}
		T first = query.find(clazz);
		if (first == null && createIfNonexistent) {
			first = (T) TransformManager.get()
					.createDomainObject((Class) clazz);
			Reflections.propertyAccessor().setPropertyValue(first, key1,
					value1);
			if (key2 != null) {
				Reflections.propertyAccessor().setPropertyValue(first, key2,
						value2);
			}
		}
		return first;
	}

	public <T extends HasIdAndLocalId> T findRaw(Class<T> clazz, long id) {
		return cache.get(clazz, id);
	}

	public <T extends HasIdAndLocalId> boolean isCached(Class<T> clazz,
			long id) {
		return cache.contains(clazz, id);
	}

	public <T extends HasIdAndLocalId> T findRaw(T t) {
		return (T) findRaw(t.getClass(), t.getId());
	}

	public Iterable<Object[]> getData(Connection conn, Class clazz,
			String sqlFilter) throws SQLException {
		ConnResults result = new ConnResults(conn, clazz,
				columnDescriptors.get(clazz), sqlFilter);
		return result;
	}

	public AlcinaMemCacheHealth getHealth() {
		return health;
	}

	public Collection<Long> getIds(Class<? extends HasIdAndLocalId> clazz) {
		try {
			lock(false);
			return new ArrayList<Long>(cache.keys(clazz));
		} finally {
			unlock(false);
		}
	}

	public <CL extends CacheLookup> CL
			getLookupFor(CacheLookupDescriptor descriptor) {
		return (CL) descriptor.getLookup();
	}

	public int getLookupSize(CacheLookupDescriptor descriptor, Object value) {
		return descriptor.getLookup().size(value);
	}

	public UnsortedMultikeyMap<Field> getMemcacheTransientFields() {
		return this.memcacheTransientFields;
	}

	public MemCachePersistenceListener getPersistenceListener() {
		return this.persistenceListener;
	}

	public int getSize(Class clazz) {
		return cache.size(clazz);
	}

	public void invokeAllWithThrow(List tasks) throws Exception {
		if (warmupExecutor != null) {
			List<Future> futures = (List) warmupExecutor
					.invokeAll((List) tasks);
			for (Future future : futures) {
				// will throw if there was an exception
				future.get();
			}
			tasks.clear();
		}
	}

	public boolean isCached(Class clazz) {
		return cacheDescriptor.perClass.containsKey(clazz);
	}

	public boolean isCachedTransactional(Class clazz) {
		return isCached(clazz)
				&& cacheDescriptor.perClass.get(clazz).isTransactional();
	}

	public boolean isCheckModificationWriteLock() {
		return this.checkModificationWriteLock;
	}

	public boolean isDebug() {
		return this.debug;
	}

	public boolean isInitialised() {
		return initialised;
	}

	public static <V extends HasIdAndLocalId> boolean isRawValue(V v) {
		V existing = (V) get().cache.get(v.getClass(), v.getId());
		return existing == v;
	}

	public boolean isWillProjectLater() {
		return LooseContext.is(CONTEXT_WILL_PROJECT_AFTER_READ_LOCK);
	}

	public void linkFromServletLayer() {
	}

	public <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
		return new AlcinaMemCacheQuery().ids(getIds(clazz)).list(clazz);
	}

	public <T extends HasIdAndLocalId> List<T> list(Class<T> clazz,
			Collection<Long> ids) {
		return new AlcinaMemCacheQuery().ids(ids).list(clazz);
	}

	public <T extends HasIdAndLocalId> List<T> listRaw(Class<T> clazz) {
		return new AlcinaMemCacheQuery().ids(getIds(clazz)).raw().list(clazz);
	}

	public void loadTable(Class clazz, String sqlFilter, ClassIdLock sublock)
			throws Exception {
		assert sublock != null;
		try {
			LooseContext.push();
			LooseContext.remove(AlcinaMemCache.CONTEXT_NO_LOCKS);
			loadTable(clazz, sqlFilter, sublock, new LaterLookup());
		} finally {
			LooseContext.pop();
		}
	}

	public void lock(boolean write) {
		if (LooseContext.is(CONTEXT_NO_LOCKS)) {
			return;
		}
		if (lockingDisabled) {
			if (System.currentTimeMillis()
					- lastLockingDisabledMessage > TimeConstants.ONE_MINUTE_MS) {
				System.out.format("memcache - lock %s - locking disabled\n",
						write);
			}
			lastLockingDisabledMessage = System.currentTimeMillis();
			return;
		}
		try {
			if (mainLock.getQueueLength() > maxLockQueueLength) {
				System.out.println(
						"Disabling locking due to deadlock:\n***************\n");
				mainLock.getQueuedThreads().forEach(
						t -> System.out.println(t + "\n" + t.getStackTrace()));
				System.out.println(
						"Recent lock acquisitions:\n***************\n");
				System.out.println(
						CommonUtils.join(recentLockAcquisitions, "\n"));
				AlcinaTopics.notifyDevWarning(new MemcacheException(
						"Disabling locking to long queue/deadlock"));
				lockingDisabled = true;
				for (Thread t : waitingOnWriteLock) {
					t.interrupt();
				}
				waitingOnWriteLock.clear();
				return;
			}
			maybeLogLock(LockAction.PRE_LOCK, write);
			if (write) {
				int readHoldCount = mainLock.getReadHoldCount();
				if (readHoldCount > 0) {
					throw new RuntimeException(
							"Trying to acquire write lock from read-locked thread");
				}
				try {
					waitingOnWriteLock.add(Thread.currentThread());
					mainLock.writeLock().lockInterruptibly();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				mainLock.readLock().lock();
			}
			lockStartTime.put(Thread.currentThread(),
					System.currentTimeMillis());
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
		maybeLogLock(LockAction.MAIN_LOCK_ACQUIRED, write);
	}

	enum LockAction {
		PRE_LOCK, MAIN_LOCK_ACQUIRED, SUB_LOCK_ACQUIRED, UNLOCK
	}

	public List<Long> notInStore(Collection<Long> ids, Class clazz) {
		return cache.notContained(ids, clazz);
	}

	public void registerForTesting(HasIdAndLocalId hili) {
		if (!AppPersistenceBase.isTest()) {
			throw new RuntimeException("Only when testing...");
		}
		cache.put(hili);
		index(hili, true);
	}

	public <T> T replaceWithRawValues(T t) {
		return (T) new RawValueReplacer().read(t);
	}

	public void reset() {
		singleton = new AlcinaMemCache();
	}

	public void resolveRefs(LaterLookup laterLookup) throws Exception {
		laterLookup.resolve();
	}

	public void runWithWriteLock(Runnable runnable) {
		try {
			lock(true);
			runnable.run();
		} finally {
			unlock(true);
		}
	}

	/**
	 * Normally should be true, expect in warmup (where we know threads will be
	 * non-colliding)
	 */
	public void
			setCheckModificationWriteLock(boolean checkModificationWriteLock) {
		this.checkModificationWriteLock = checkModificationWriteLock;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Given sublock-guarded code should be able to be run concurrently (as long
	 * as the sublock objects are different), will rework this
	 */
	public void sublock(Object sublock, boolean lock) {
		if (lockingDisabled || LooseContext.is(CONTEXT_NO_LOCKS)) {
			return;
		}
		if (lock) {
			maybeLogLock(LockAction.PRE_LOCK, lock);
			subgraphLock.writeLock().lock();
			writeLockSubLock = sublock;
		} else {
			if (sublock == writeLockSubLock) {
				subgraphLock.writeLock().unlock();
				sublock = null;
			} else {
				// should not be possible
				throw new RuntimeException(String.format(
						"releasing incorrect writer sublock: %s %s", sublock,
						writeLockSubLock));
			}
		}
		maybeLogLock(lock ? LockAction.SUB_LOCK_ACQUIRED : LockAction.UNLOCK,
				lock);
	}

	public void unlock(boolean write) {
		if (lockingDisabled || LooseContext.is(CONTEXT_NO_LOCKS)) {
			return;
		}
		try {
			if (write) {
				if (mainLock.writeLock().isHeldByCurrentThread()) {
					// if not held, we had an exception acquiring the
					// lock...ignore
					mainLock.writeLock().unlock();
					waitingOnWriteLock.remove(Thread.currentThread());
				}
			} else {
				mainLock.readLock().unlock();
			}
			lockStartTime.remove(Thread.currentThread());
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
		maybeLogLock(LockAction.UNLOCK, write);
	}

	public void warmup(DataSource dataSource, CacheDescriptor cacheDescriptor,
			ThreadPoolExecutor warmupExecutor) {
		this.dataSource = dataSource;
		this.cacheDescriptor = cacheDescriptor;
		this.warmupExecutor = warmupExecutor;
		try {
			createWarmupConnections();
			this.warmup0();
			initialised = true;
			this.timer = new Timer("Timer-AlcinaMemCache-check-stats");
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					maybeDebugLongLockHolders();
				}
			}, 0, 100);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void addColumnName(Class clazz, PropertyDescriptor pd,
			Class propertyType) {
		columnDescriptors.add(clazz, new ColumnDescriptor(pd, propertyType));
		propertyDescriptorFetchTypes.put(pd, propertyType);
	}

	private void createWarmupConnections() throws Exception {
		for (int i = 0; i < warmupExecutor.getMaximumPoolSize(); i++) {
			Connection conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			conn.setReadOnly(true);
			originalTransactionIsolation = conn.getTransactionIsolation();
			if (ResourceUtilities.is(AlcinaMemCache.class, "warmStandbyDb")) {
				conn.setTransactionIsolation(
						Connection.TRANSACTION_REPEATABLE_READ);
			} else {
				conn.setTransactionIsolation(
						Connection.TRANSACTION_SERIALIZABLE);
			}
			warmupConnections.put(conn, 0);
		}
	}

	private void ensureModificationChecker(HasIdAndLocalId hili)
			throws Exception {
		if (modificationCheckerField != null
				&& hili instanceof BaseSourcesPropertyChangeEvents) {
			modificationCheckerField.set(hili, modificationChecker);
		}
	}

	private synchronized PdOperator ensurePdOperator(PropertyDescriptor pd,
			Class clazz) {
		return operatorsByClass.ensure(() -> {
			Collection<Object> values = operatorsByClass.values(clazz);
			return new PdOperator(pd, clazz,
					values == null ? 0 : values.size());
		}, clazz, pd);
	}

	private ComplexFilter getComplexFilterFor(Class clazz,
			CacheFilter... filters) {
		return cacheDescriptor.complexFilters.stream()
				.filter(cf -> cf.handles(clazz, filters)).findFirst()
				.orElse(null);
	}

	private Connection getConn() {
		if (initialising) {
			try {
				return getWarmupConnection();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		try {
			Connection postInitConn = dataSource.getConnection();
			postInitConn.setAutoCommit(true);
			postInitConn.setReadOnly(true);
			return postInitConn;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private Field getDescriptorField(Class<?> clazz, String name) {
		Class original = clazz;
		while (clazz != null) {
			try {
				Field f = clazz.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (Exception e) {
			}
			clazz = clazz.getSuperclass();
		}
		throw new RuntimeException(String.format("Field not available - %s.%s",
				original.getSimpleName(), name));
	}

	private Set<Long> getFiltered(final Class clazz, CacheFilter cacheFilter,
			CacheFilter nextFilter, FilterContext ctr, Set<Long> existing) {
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
		CacheLookup lookup = getLookupFor(clazz, cacheFilter.propertyPath);
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
		return cacheDescriptor.perClass.get(clazz).evaluateFilter(cache,
				existing, filter);
	}

	private Set getFilteredTransactional(final Class clazz,
			CacheFilter cacheFilter, Set existing) {
		CollectionFilter filter = cacheFilter.asCollectionFilter();
		existing = existing != null ? existing
				: transactional.immutableRawValues(clazz);
		return CollectionFilters.filterAsSet(existing, filter);
	}

	private CacheLookup getLookupFor(Class clazz, String propertyName) {
		for (CacheLookupDescriptor descriptor : cacheDescriptor.perClass
				.get(clazz).lookupDescriptors) {
			if (descriptor.handles(clazz, propertyName)) {
				return descriptor.getLookup();
			}
		}
		return null;
	}

	private String getStacktraceSlice(Thread t) {
		return getStacktraceSlice(t, 20);
	}

	private String getStacktraceSlice(Thread t, int size) {
		String log = "";
		StackTraceElement[] trace = t.getStackTrace();
		for (int i = 0; i < trace.length && i < size; i++) {
			log += trace[i] + "\n";
		}
		log += "\n\n";
		return log;
	}

	private Class getTargetEntityType(Method rm) {
		ManyToOne manyToOne = rm.getAnnotation(ManyToOne.class);
		if (manyToOne != null && manyToOne.targetEntity() != void.class) {
			return manyToOne.targetEntity();
		}
		OneToOne oneToOne = rm.getAnnotation(OneToOne.class);
		if (oneToOne != null && oneToOne.targetEntity() != void.class) {
			return oneToOne.targetEntity();
		}
		AlcinaMemCacheColumn memCacheColumn = rm
				.getAnnotation(AlcinaMemCacheColumn.class);
		if (memCacheColumn != null
				&& memCacheColumn.targetEntity() != void.class) {
			return memCacheColumn.targetEntity();
		}
		return rm.getReturnType();
	}

	private synchronized Connection getWarmupConnection() throws Exception {
		Connection min = warmupConnections.min();
		warmupConnections.add(min);
		return min;
	}

	private void index(HasIdAndLocalId obj, boolean add) {
		Class<? extends HasIdAndLocalId> clazz = obj.getClass();
		if (obj instanceof MemCacheProxy) {
			clazz = (Class<? extends HasIdAndLocalId>) clazz.getSuperclass();
		}
		cacheDescriptor.perClass.get(clazz).index(obj, add);
	}

	private void loadJoinTable(Entry<PropertyDescriptor, JoinTable> entry,
			LaterLookup laterLookup) {
		JoinTable joinTable = entry.getValue();
		if (joinTable == null) {
			return;
		}
		PropertyDescriptor pd = entry.getKey();
		// get reverse
		PropertyDescriptor rev = null;
		Class<?> declaringClass = pd.getReadMethod().getDeclaringClass();
		for (Entry<PropertyDescriptor, JoinTable> entry2 : joinTables
				.entrySet()) {
			ManyToMany m = entry2.getKey().getReadMethod()
					.getAnnotation(ManyToMany.class);
			if (m != null && entry2.getValue() == null
					&& m.targetEntity() == declaringClass
					&& pd.getName().equals(m.mappedBy())) {
				rev = entry2.getKey();
				break;
			}
		}
		MemcacheJoinHandler joinHandler = null;
		if (rev == null) {
			Type genericReturnType = pd.getReadMethod().getGenericReturnType();
			if (genericReturnType instanceof ParameterizedType) {
				Type genericType = ((ParameterizedType) genericReturnType)
						.getActualTypeArguments()[0];
				if (genericType == declaringClass) {
					// self-reference, probably
					rev = pd;
				}
			}
			if (rev == null) {
				joinHandler = Registry.impl(JPAImplementation.class)
						.getMemcacheJoinHandler(pd);
				if (joinHandler != null) {
				} else {
					throw new RuntimeException("No reverse key for " + pd);
				}
			}
		}
		Connection conn = getConn();
		try {
			String joinTableName = joinTable.name();
			MetricLogging.get().start(joinTableName);
			String targetFieldSql = joinHandler != null
					? joinHandler.getTargetSql()
					: joinTable.inverseJoinColumns()[0].name();
			String sql = String.format("select %s, %s from %s;",
					joinTable.joinColumns()[0].name(), targetFieldSql,
					joinTableName);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			PdOperator pdFwd = ensurePdOperator(pd,
					pd.getReadMethod().getDeclaringClass());
			// will be null if it's an enumerated type
			PdOperator pdRev = joinHandler != null ? null
					: ensurePdOperator(rev,
							rev.getReadMethod().getDeclaringClass());
			while (rs.next()) {
				HasIdAndLocalId src = (HasIdAndLocalId) cache
						.get(declaringClass, rs.getLong(1));
				assert src != null;
				if (joinHandler == null) {
					HasIdAndLocalId tgt = (HasIdAndLocalId) cache.get(
							rev.getReadMethod().getDeclaringClass(),
							rs.getLong(2));
					assert tgt != null;
					laterLookup.add(tgt, pdFwd, src);
					laterLookup.add(src, pdRev, tgt);
				} else {
					joinHandler.injectValue(rs, src);
				}
			}
			stmt.close();
			MetricLogging.get().end(joinTableName, metricLogger);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			releaseConn(conn);
		}
	}

	private void loadPropertyStore(Class clazz,
			PropertyStoreItemDescriptor propertyStoreItemDescriptor)
			throws SQLException {
		Connection conn = getConn();
		try {
			ConnResults connResults = new ConnResults(conn, clazz,
					columnDescriptors.get(clazz),
					propertyStoreItemDescriptor.getSqlFilter());
			List<PdOperator> pds = descriptors.get(clazz);
			propertyStoreItemDescriptor.init(cache, pds);
			String simpleName = clazz.getSimpleName();
			int count = propertyStoreItemDescriptor.getRoughCount();
			SystemoutCounter ctr = new SystemoutCounter(20000, 10, count, true);
			ResultSet rs = connResults.ensureRs();
			while (rs.next()) {
				propertyStoreItemDescriptor.addRow(rs);
				ctr.tick(simpleName);
			}
			rs.close();
		} finally {
			releaseConn(conn);
		}
	}

	private void loadTable(Class clazz, String sqlFilter, ClassIdLock sublock,
			LaterLookup laterLookup) throws Exception {
		if (sublock != null) {
			sublock(sublock, true);
		}
		try {
			List<HasIdAndLocalId> loaded = loadTable0(clazz, sqlFilter, sublock,
					laterLookup);
			if (sublock != null) {
				resolveRefs(laterLookup);
				if (!initialising) {
					for (HasIdAndLocalId hili : loaded) {
						index(hili, true);
					}
				}
			}
		} finally {
			if (sublock != null) {
				sublock(sublock, false);
			}
		}
	}

	private List<HasIdAndLocalId> loadTable0(Class clazz, String sqlFilter,
			ClassIdLock sublock, LaterLookup laterLookup) throws Exception {
		Connection conn = getConn();
		List<HasIdAndLocalId> loaded;
		try {
			Iterable<Object[]> results = getData(conn, clazz, sqlFilter);
			List<PdOperator> pds = descriptors.get(clazz);
			loaded = new ArrayList<HasIdAndLocalId>();
			for (Object[] objects : results) {
				HasIdAndLocalId hili = (HasIdAndLocalId) clazz.newInstance();
				if (sublock != null) {
					loaded.add(hili);
				}
				ensureModificationChecker(hili);
				for (int i = 0; i < objects.length; i++) {
					PdOperator pdOperator = pds.get(i);
					Method rm = pdOperator.readMethod;
					if (pdOperator.manyToOne != null
							|| pdOperator.oneToOne != null) {
						Long id = (Long) objects[i];
						if (id != null) {
							laterLookup.add(id, pdOperator, hili);
						}
					} else {
						pdOperator.field.set(hili, objects[i]);
					}
				}
				cache.put(hili);
			}
		} finally {
			releaseConn(conn);
		}
		return loaded;
	}

	long lastQueueDumpTime = 0;

	Map<Long, Long> threadQueueTimes = new ConcurrentHashMap<>();

	CountingMap<Thread> activeThreads = new CountingMap<>();

	private void maybeLogLock(LockAction action, boolean write) {
		long time = System.currentTimeMillis();
		Thread currentThread = Thread.currentThread();
		if (action == LockAction.PRE_LOCK) {
			threadQueueTimes.put(currentThread.getId(), time);
		} else {
			synchronized (activeThreads) {
				switch (action) {
				case MAIN_LOCK_ACQUIRED:
				case SUB_LOCK_ACQUIRED:
					activeThreads.add(currentThread);
					break;
				case UNLOCK:
					activeThreads.add(currentThread, -1);
					if (activeThreads.get(currentThread) == 0) {
						activeThreads.remove(currentThread);
					}
					break;
				}
			}
			threadQueueTimes.remove(currentThread.getId());
		}
		long queuedTime = health.getMaxQueuedTime();
		if (dumpLocks || (collectLockAcquisitionPoints
				&& (write || queuedTime > MAX_QUEUED_TIME))) {
			String lockDumpCause = String.format("Memcache lock - %s - %s\n",
					write ? "write" : "read", action);
			String log = getLockStats();
			lockDumpCause += log;
			if (dumpLocks || (queuedTime > MAX_QUEUED_TIME)) {
				System.out.println(getLockDumpString(lockDumpCause, time
						- lastQueueDumpTime > 5 * TimeConstants.ONE_MINUTE_MS));
			}
			if (collectLockAcquisitionPoints) {
				synchronized (recentLockAcquisitions) {
					recentLockAcquisitions.add(lockDumpCause);
					if (recentLockAcquisitions.size() > 100) {
						recentLockAcquisitions.removeFirst();
					}
				}
			}
		}
	}

	public String getLockDumpString(String lockDumpCause, boolean full) {
		FormatBuilder fullLockDump = new FormatBuilder();
		Thread writerThread = postProcessWriterThread;
		if (writerThread != null) {
			fullLockDump.format(
					"Memcache log debugging----------\n"
							+ "Writer thread trace:----------\n" + "%s\n",
					getStacktraceSlice(postProcessWriterThread, 200));
		}
		fullLockDump.line(lockDumpCause);
		long time = System.currentTimeMillis();
		if (full) {
			fullLockDump.line("Current locked thread dump:\n***************\n");
			mainLock.getQueuedThreads()
					.forEach(t2 -> fullLockDump.line("id:%s %s\n%s", t2.getId(),
							t2, getStacktraceSlice(t2,
									LONG_LOCK_TRACE_LENGTH)));
			fullLockDump.line("\n\nThread pause times:\n***************\n");
			threadQueueTimes.forEach((id, t2) -> fullLockDump
					.format("id: %s - time: %s\n", id, time - t2));
			synchronized (activeThreads) {
				fullLockDump.line("\n\nActive threads:\n***************\n");
				activeThreads.keySet().forEach(t2 -> fullLockDump.line(
						"id:%s %s\n%s", t2.getId(), t2,
						getStacktraceSlice(t2, LONG_LOCK_TRACE_LENGTH)));
			}
			fullLockDump
					.line("\n\nRecent lock acquisitions:\n***************\n");
			fullLockDump.line(CommonUtils.join(recentLockAcquisitions, "\n"));
			fullLockDump.line("\n===========\n\n");
			lastQueueDumpTime = time;
		}
		return fullLockDump.toString();
	}

	String getLockStats() {
		Thread t = Thread.currentThread();
		String log = CommonUtils.formatJ(
				"\tid:%s\n\ttime: %s\n\treadHoldCount:"
						+ " %s\n\twriteHoldcount: %s\n\tsublock: %s\n\n ",
				t.getId(), new Date(), mainLock.getQueuedReaderThreads().size(),
				mainLock.getQueuedWriterThreads().size(), subgraphLock);
		log += getStacktraceSlice(t);
		return log;
	}

	private void prepareTable(CacheItemDescriptor descriptor) throws Exception {
		Class clazz = descriptor.clazz;
		List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>(
				Arrays.asList(Introspector.getBeanInfo(clazz)
						.getPropertyDescriptors()));
		PropertyDescriptor id = SEUtilities.getPropertyDescriptorByName(clazz,
				"id");
		pds.remove(id);
		pds.add(0, id);
		PropertyDescriptor result = null;
		List<PdOperator> mapped = new ArrayList<PdOperator>();
		descriptors.put(clazz, mapped);
		for (PropertyDescriptor pd : pds) {
			if (pd.getReadMethod() == null || pd.getWriteMethod() == null) {
				continue;
			}
			Method rm = pd.getReadMethod();
			if ((rm.getAnnotation(Transient.class) != null
					&& rm.getAnnotation(AlcinaMemCacheColumn.class) == null)
					|| rm.getAnnotation(
							AlcinaMemCacheTransient.class) != null) {
				AlcinaMemCacheTransient transientAnn = rm
						.getAnnotation(AlcinaMemCacheTransient.class);
				if (transientAnn != null) {
					Field field = clazz.getDeclaredField(pd.getName());
					field.setAccessible(true);
					memcacheTransientFields.put(clazz, field, field);
					memcacheTransientProperties.put(clazz, field.getName(),
							transientAnn);
				}
				continue;
			}
			if (descriptor.ignoreField(pd.getName())) {
				continue;
			}
			OneToMany oneToMany = rm.getAnnotation(OneToMany.class);
			if (oneToMany != null) {
				if (Set.class.isAssignableFrom(pd.getPropertyType())) {
					Field field = clazz.getDeclaredField(pd.getName());
					field.setAccessible(true);
					if (field != null) {
						ParameterizedType pt = (ParameterizedType) field
								.getGenericType();
						manyToOneRev.put(pt.getActualTypeArguments()[0],
								oneToMany.mappedBy(), pd);
					}
				}
				continue;
			}
			ManyToMany manyToMany = rm.getAnnotation(ManyToMany.class);
			JoinTable joinTable = rm.getAnnotation(JoinTable.class);
			if (manyToMany != null || joinTable != null) {
				if ((manyToMany != null && manyToMany.mappedBy().isEmpty())
						&& joinTable == null) {
					System.out.format(
							"**warn - manytomany association with no join table: %s.%s\n",
							rm.getDeclaringClass().getSimpleName(),
							pd.getName());
				}
				joinTables.put(pd, joinTable);
				continue;
			}
			ManyToOne manyToOne = rm.getAnnotation(ManyToOne.class);
			OneToOne oneToOne = rm.getAnnotation(OneToOne.class);
			AlcinaMemCacheColumn memCacheColumn = rm
					.getAnnotation(AlcinaMemCacheColumn.class);
			if (manyToOne != null || oneToOne != null
					|| memCacheColumn != null) {
				Class joinEntityType = getTargetEntityType(rm);
				if (!cacheDescriptor.joinPropertyCached(joinEntityType)) {
					System.out.format("  not loading: %s.%s -- %s\n",
							clazz.getSimpleName(), pd.getName(),
							pd.getPropertyType().getSimpleName());
					continue;
				}
				if (oneToOne != null && !oneToOne.mappedBy().isEmpty()) {
					oneToOneRev.put(pd.getPropertyType(), oneToOne.mappedBy(),
							pd);
					continue;
				}
				if (memCacheColumn != null) {
					memCacheColumnRev.put(pd.getPropertyType(),
							memCacheColumn.mappedBy(), pd);
					continue;
				}
				addColumnName(clazz, pd,
						getTargetEntityType(pd.getReadMethod()));
			} else {
				addColumnName(clazz, pd, pd.getPropertyType());
			}
			mapped.add(ensurePdOperator(pd, clazz));
		}
	}

	private void releaseConn(Connection conn) {
		if (conn == null) {
			return;
		}
		try {
			if (initialising) {
				releaseWarmupConnection(conn);
			} else {
				conn.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Date timestampToDate(Date date) {
		if (date instanceof Timestamp) {
			return new Date(((Timestamp) date).getTime());
		}
		return date;
	}

	private void updateIVersionable(HasIdAndLocalId obj,
			Object persistentLayerSource) {
		IVersionable graph = (IVersionable) obj;
		IVersionable persistent = (IVersionable) persistentLayerSource;
		graph.setCreationDate(timestampToDate(persistent.getCreationDate()));
		graph.setLastModificationDate(
				timestampToDate(persistent.getCreationDate()));
		Class<? extends IUser> iUserClass = cacheDescriptor.getIUserClass();
		Long persistentCreationUserId = HiliHelper
				.getIdOrNull(persistent.getCreationUser());
		IUser creationUser = cache.get(iUserClass, persistentCreationUserId);
		graph.setCreationUser(creationUser);
		Long persistentLastModificationUserId = HiliHelper
				.getIdOrNull(persistent.getLastModificationUser());
		IUser lastModificationUser = cache.get(iUserClass,
				persistentLastModificationUserId);
		graph.setLastModificationUser(lastModificationUser);
	}

	private void updateVersionNumber(HasIdAndLocalId obj,
			DomainTransformEvent dte) {
		((HasVersionNumber) obj).setVersionNumber(
				((HasVersionNumber) dte.getSource()).getVersionNumber());
	}

	private void warmup0() throws Exception {
		initialising = true;
		transformManager = new SubgraphTransformManagerRemoteOnly();
		backupLazyLoader = new BackupLazyLoader();
		cache = transformManager.getDetachedEntityCache();
		transformManager.getStore().setLazyObjectLoader(backupLazyLoader);
		joinTables = new LinkedHashMap<PropertyDescriptor, JoinTable>();
		descriptors = new LinkedHashMap<Class, List<PdOperator>>();
		manyToOneRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		oneToOneRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		memCacheColumnRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		columnDescriptors = new Multimap<Class, List<ColumnDescriptor>>();
		modificationCheckerField = BaseSourcesPropertyChangeEvents.class
				.getDeclaredField("propertyChangeSupport");
		modificationCheckerField.setAccessible(true);
		modificationChecker = new ModificationCheckerSupport(null);
		checkModificationWriteLock = false;
		MetricLogging.get().start("memcache-all");
		// get non-many-many obj
		lock(true);
		MetricLogging.get().start("tables");
		for (CacheItemDescriptor descriptor : cacheDescriptor.perClass
				.values()) {
			Class clazz = descriptor.clazz;
			prepareTable(descriptor);
			if (descriptor instanceof PropertyStoreItemDescriptor) {
				transformManager.addPropertyStore(descriptor);
			}
			// warmup threadsafe
			cache.getMap(clazz);
		}
		List<Callable> calls = new ArrayList<Callable>();
		for (CacheItemDescriptor descriptor : cacheDescriptor.perClass
				.values()) {
			final Class clazz = descriptor.clazz;
			if (!descriptor.lazy) {
				calls.add(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						MetricLogging.get().start(clazz.getSimpleName());
						if (descriptor instanceof PropertyStoreItemDescriptor) {
							PropertyStoreItemDescriptor propertyStoreItemDescriptor = (PropertyStoreItemDescriptor) descriptor;
							loadPropertyStore(clazz,
									propertyStoreItemDescriptor);
						} else {
							loadTable(clazz, "", null, warmupLaterLookup());
						}
						MetricLogging.get().end(clazz.getSimpleName(),
								metricLogger);
						return null;
					}
				});
			}
		}
		invokeAllWithThrow(calls);
		for (Entry<PropertyDescriptor, JoinTable> entry : joinTables
				.entrySet()) {
			final Entry<PropertyDescriptor, JoinTable> entryF = entry;
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					loadJoinTable(entryF, warmupLaterLookup());
					return null;
				}
			});
		}
		invokeAllWithThrow(calls);
		MetricLogging.get().end("tables");
		MetricLogging.get().start("xrefs");
		for (LaterLookup ll : warmupLaterLookups) {
			// calls.add(new Callable<Void>() {
			// @Override
			// public Void call() throws Exception {
			resolveRefs(ll);
			// return null;
			// }
			// });
		}
		invokeAllWithThrow(calls);
		MetricLogging.get().end("xrefs");
		warmupLaterLookups.clear();
		unlock(true);
		MetricLogging.get().start("postLoad");
		for (final CacheTask task : cacheDescriptor.postLoadTasks) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					MetricLogging.get().start(task.getClass().getSimpleName());
					task.run();
					MetricLogging.get().end(task.getClass().getSimpleName(),
							metricLogger);
					return null;
				}
			});
		}
		invokeAllWithThrow(calls);
		MetricLogging.get().end("postLoad");
		MetricLogging.get().start("lookups");
		for (final CacheItemDescriptor descriptor : cacheDescriptor.perClass
				.values()) {
			for (CacheProjection projection : descriptor.projections) {
				if (projection instanceof CacheLookup) {
					((CacheLookup) projection)
							.setModificationChecker(modificationChecker);
				}
			}
		}
		for (final CacheItemDescriptor descriptor : cacheDescriptor.perClass
				.values()) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					for (CacheLookupDescriptor lookupDescriptor : descriptor.lookupDescriptors) {
						lookupDescriptor.createLookup();
						if (lookupDescriptor.isEnabled()) {
							addValues(lookupDescriptor.getLookup());
						}
					}
					return null;
				}
			});
		}
		invokeAllWithThrow(calls);
		MetricLogging.get().end("lookups");
		MetricLogging.get().start("projections");
		// deliberately init projections after lookups
		for (final CacheItemDescriptor descriptor : cacheDescriptor.perClass
				.values()) {
			for (CacheProjection projection : descriptor.projections) {
				if (projection instanceof BaseProjectionHasEquivalenceHash) {
					cachingProjections
							.getAndEnsure(projection.getListenedClass());
				}
				if (projection instanceof BaseProjection) {
					((BaseProjection) projection)
							.setModificationChecker(modificationChecker);
				}
			}
		}
		for (final CacheItemDescriptor descriptor : cacheDescriptor.perClass
				.values()) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					for (CacheProjection projection : descriptor.projections) {
						if (projection.isEnabled()) {
							addValues(projection);
						}
						if (projection instanceof BaseProjectionHasEquivalenceHash) {
							cachingProjections.add(
									projection.getListenedClass(), projection);
						}
					}
					return null;
				}
			});
		}
		invokeAllWithThrow(calls);
		MetricLogging.get().end("projections");
		checkModificationWriteLock = true;
		initialising = false;
		if (ResourceUtilities.getBoolean(AlcinaMemCache.class, "dumpLocks")) {
			dumpLocks = true;
		}
		if (ResourceUtilities.getBoolean(AlcinaMemCache.class,
				"collectLockAcquisitionPoints")) {
			collectLockAcquisitionPoints = true;
		}
		// don't close, but indicate that everything write-y from now shd be
		// single-threaded
		warmupConnections.keySet().forEach(conn -> closeWarmupConnection(conn));
		warmupExecutor = null;
		MetricLogging.get().end("memcache-all");
	}

	protected void closeWarmupConnection(Connection conn) {
		try {
			conn.commit();
			conn.setAutoCommit(true);
			conn.setReadOnly(false);
			conn.setTransactionIsolation(originalTransactionIsolation);
			conn.close();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void maybeDebugLongLockHolders() {
		long time = System.currentTimeMillis();
		for (Entry<Thread, Long> e : lockStartTime.entrySet()) {
			long duration = time - e.getValue();
			if (duration > 250 || (duration > 50
					&& e.getKey() == postProcessWriterThread)) {
				if (ResourceUtilities.is(AlcinaMemCache.class,
						"debugLongLocks")) {
					System.out.format("Long lock holder - %s ms - %s\n%s\n\n",
							duration, e.getKey(), getStacktraceSlice(e.getKey(),
									LONG_LOCK_TRACE_LENGTH));
				}
			}
		}
	}

	protected synchronized void releaseWarmupConnection(Connection conn) {
		warmupConnections.add(conn, -1);
	}

	void ensureProxyModificationChecker(HasIdAndLocalId hili) throws Exception {
		if (modificationCheckerField != null
				&& hili instanceof BaseSourcesPropertyChangeEvents) {
			modificationCheckerField.set(hili, modificationChecker);
		}
	}

	String getCanonicalPropertyPath(Class clazz, String propertyPath) {
		return cacheDescriptor.perClass.get(clazz)
				.getCanonicalPropertyPath(propertyPath);
	}

	<T extends HasIdAndLocalId> List<T> list(Class<T> clazz,
			AlcinaMemCacheQuery query) {
		try {
			lock(false);
			List<T> raw = null;
			Set<Long> ids = query.getIds();
			boolean transaction = transactional
					.transactionActiveInCurrentThread();
			boolean debugMetrics = isDebug()
					&& LooseContext.is(CONTEXT_DEBUG_QUERY_METRICS);
			StringBuilder debugMetricBuilder = new StringBuilder();
			int filterSize = query.getFilters().size();
			if (!transaction || !ids.isEmpty() || query.isNonTransactional()) {
				FilterContext ctx = new FilterContext();
				for (; ctx.idx < filterSize; ctx.idx++) {
					int i = ctx.idx;
					long start = System.nanoTime();
					CacheFilter cacheFilter = query.getFilters().get(i);
					CacheFilter nextFilter = i == filterSize - 1 ? null
							: query.getFilters().get(i + 1);
					ids = (i == 0 && ids.isEmpty()) ? null : ids;
					ids = getFiltered(clazz, cacheFilter, nextFilter, ctx, ids);
					if (debugMetrics) {
						double ms = (double) (System.nanoTime() - start)
								/ 1000000.0;
						String filters = ctx.lastFilterString;
						debugMetricBuilder.append(String.format(
								"\t%.3f ms - %s\n", ms,
								CommonUtils.trimToWsChars(filters, 100, true)));
					}
					if (ids.isEmpty()) {
						break;
					}
				}
				if (debugMetrics && CommonUtils.isNullOrEmpty(query.getIds())) {
					metricLogger.log(
							String.format("Query metrics:\n========\n%s\n%s",
									query, debugMetricBuilder.toString()));
				}
				raw = cacheDescriptor.perClass.get(clazz).getRawValues(ids,
						cache);
			} else {
				Set<T> rawTransactional = null;
				for (int i = 0; i < filterSize; i++) {
					rawTransactional = getFilteredTransactional(clazz,
							query.getFilters().get(i),
							(i == 0) ? null : rawTransactional);
				}
				if (rawTransactional == null) {
					raw = new ArrayList<T>();
				} else {
					raw = new ArrayList<T>(rawTransactional);
				}
			}
			try {
				for (PreProvideTask task : cacheDescriptor.preProvideTasks) {
					task.run(clazz, raw);
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
			unlock(false);
		}
	}

	// we only have one thread allowed here - but they won't start blocking the
	// reader thread
	synchronized void
			postProcess(DomainTransformPersistenceEvent persistenceEvent) {
		Set<Throwable> causes = new LinkedHashSet<Throwable>();
		StringBuilder warnBuilder = new StringBuilder();
		long postProcessStart = 0;
		try {
			lock(true);
			postProcessStart = System.currentTimeMillis();
			MetricLogging.get().start("post-process");
			postProcessWriterThread = Thread.currentThread();
			health.memcachePostProcessStartTime = System.currentTimeMillis();
			transformManager.startCommit();
			List<DomainTransformEvent> dtes = (List) persistenceEvent
					.getDomainTransformLayerWrapper().persistentEvents;
			List<DomainTransformEvent> filtered = filterInterestedTransforms(
					dtes);
			Multimap<HiliLocator, List<DomainTransformEvent>> perObjectTransforms = CollectionFilters
					.multimap(filtered, new DteToLocatorMapper());
			Map<HiliLocator, HasIdAndLocalId> locatorOriginalSourceMap = new LinkedHashMap<HiliLocator, HasIdAndLocalId>();
			for (DomainTransformEvent dte : filtered) {
				HiliLocator locator = HiliLocator.objectLocator(dte);
				locatorOriginalSourceMap.put(locator, dte.getSource());
			}
			if (cacheDescriptor instanceof PreApplyPersistListener) {
				((PreApplyPersistListener) cacheDescriptor)
						.loadLazyPreApplyPersist(persistenceEvent);
			}
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
					HasIdAndLocalId memCacheObj = transformManager
							.getObject(dte, true);
					if (memCacheObj == null) {
						continue;
					}
				}
				if (dte.getTransformType() != TransformType.CREATE_OBJECT
						&& first == dte) {
					HasIdAndLocalId obj = transformManager.getObject(dte, true);
					if (obj != null) {
						index(obj, false);
					} else {
						warnLogger.format(
								"Null memcacheObject for index - %s\n",
								HiliLocator.objectLocator(dte));
					}
				}
				HasIdAndLocalId persistentLayerSource = dte.getSource();
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
					HasIdAndLocalId dbObj = locatorOriginalSourceMap
							.get(HiliLocator.objectLocator(dte));
					HasIdAndLocalId memCacheObj = transformManager
							.getObject(dte, true);
					if (memCacheObj != null) {
						if (dbObj instanceof HasVersionNumber) {
							updateVersionNumber(memCacheObj, dte);
						}
						if (dbObj instanceof IVersionable) {
							updateIVersionable(memCacheObj,
									persistentLayerSource);
						}
						ensureModificationChecker(memCacheObj);
						index(memCacheObj, true);
					} else {
						warnLogger.format(
								"Null memcacheObject for index - %s\n",
								HiliLocator.objectLocator(dte));
					}
				}
			}
		} catch (Exception e) {
			causes.add(e);
		} finally {
			transformManager.endCommit();
			health.memcachePostProcessStartTime = 0;
			postProcessWriterThread = null;
			long postProcessTime = System.currentTimeMillis()
					- postProcessStart;
			health.memcacheMaxPostProcessTime = Math
					.max(health.memcacheMaxPostProcessTime, postProcessTime);
			MetricLogging.get().end("post-process");
			unlock(true);
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
					GlobalTopicPublisher.get()
							.publishTopic(TOPIC_UPDATE_EXCEPTION, umby);
					health.memcacheExceptionCount.incrementAndGet();
					throw new MemcacheException(umby);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	synchronized LaterLookup warmupLaterLookup() {
		LaterLookup result = new LaterLookup();
		warmupLaterLookups.add(result);
		return result;
	}

	public class AlcinaMemCacheHealth {
		public long memcacheMaxPostProcessTime;

		public long memcachePostProcessStartTime;

		private AtomicInteger memcacheExceptionCount = new AtomicInteger();

		public AtomicInteger getMemcacheExceptionCount() {
			return this.memcacheExceptionCount;
		}

		public int getMemcacheQueueLength() {
			return mainLock.getQueueLength();
		}

		public long getTimeInMemcacheWriter() {
			return memcachePostProcessStartTime == 0 ? 0
					: System.currentTimeMillis() - memcachePostProcessStartTime;
		}

		public boolean isLockingDisabled() {
			return lockingDisabled;
		}

		public long getMaxQueuedTime() {
			return threadQueueTimes.values().stream()
					.min(Comparator.naturalOrder())
					.map(t -> System.currentTimeMillis() - t).orElse(0L);
		}
	}

	/*
	 * Note - not synchronized - per-thread access only
	 */
	public class LaterLookup {
		List<LaterItem> list = new ArrayList<>();

		void add(HasIdAndLocalId target, PdOperator pd,
				HasIdAndLocalId source) {
			list.add(new LaterItem(target, pd, source));
		}

		void add(long id, PdOperator pd, HasIdAndLocalId source) {
			list.add(new LaterItem(id, pd, source));
		}

		void resolve() throws Exception {
			new ResolveRefsTask(list).call();
		}

		private final class ResolveRefsTask implements Callable<Void> {
			private List<LaterItem> items;

			private ResolveRefsTask(List<LaterItem> items) {
				this.items = items;
			}

			@Override
			/*
			 * multithread Problem here is that set() methods need to be synced
			 * per class (really, pd) ..so run linear
			 */
			public Void call() throws Exception {
				for (LaterItem item : this.items) {
					try {
						PdOperator pdOperator = item.pdOperator;
						Method rm = pdOperator.readMethod;
						long id = item.id;
						if (joinTables.containsKey(pdOperator.pd)) {
							Set set = (Set) pdOperator.readMethod
									.invoke(item.source, new Object[0]);
							if (set == null) {
								set = new LinkedHashSet();
								pdOperator.writeMethod.invoke(item.source,
										new Object[] { set });
							}
							set.add(item.target);
						} else {
							Object target = cache
									.get(propertyDescriptorFetchTypes
											.get(pdOperator.pd), id);
							if (target == null) {
								System.out.format(
										"later-lookup -- missing target: %s, %s for  %s.%s #%s",
										propertyDescriptorFetchTypes
												.get(pdOperator.pd),
										id, item.source.getClass(),
										pdOperator.name, item.source.getId());
							}
							pdOperator.writeMethod.invoke(item.source, target);
							PropertyDescriptor targetPd = manyToOneRev.get(
									item.source.getClass(), pdOperator.name);
							if (targetPd != null && target != null) {
								Set set = (Set) targetPd.getReadMethod()
										.invoke(target, new Object[0]);
								if (set == null) {
									set = new LinkedHashSet();
									targetPd.getWriteMethod().invoke(target,
											new Object[] { set });
								}
								set.add(item.source);
							}
							targetPd = oneToOneRev.get(item.source.getClass(),
									pdOperator.name);
							if (targetPd != null && target != null) {
								targetPd.getWriteMethod().invoke(target,
										new Object[] { item.source });
							}
							targetPd = memCacheColumnRev.get(
									item.source.getClass(), pdOperator.name);
							if (targetPd != null && target != null) {
								targetPd.getWriteMethod().invoke(target,
										new Object[] { item.source });
							}
						}
					} catch (Exception e) {
						// possibly a delta during warmup::
						System.out.println(item);
						e.printStackTrace();
					}
				}
				if (initialising) {
					// System.out.format("resolverefs - %s - %s\n",
					// clazz.getSimpleName(), items.size());
				}
				// leave the class keys at the top
				this.items.clear();
				return null;
			}
		}

		class LaterItem {
			long id;

			PdOperator pdOperator;

			HasIdAndLocalId source;

			HasIdAndLocalId target;

			public LaterItem(HasIdAndLocalId target, PdOperator pd,
					HasIdAndLocalId source) {
				this.target = target;
				this.pdOperator = pd;
				this.source = source;
			}

			public LaterItem(long id, PdOperator pd, HasIdAndLocalId source) {
				this.id = id;
				this.pdOperator = pd;
				this.source = source;
			}
		}
	}

	public static class MemcacheException extends RuntimeException {
		public MemcacheException(Exception e) {
			super(e);
		}

		public MemcacheException(String message) {
			super(message);
		}
	}

	public static interface MemcacheJoinHandler {
		public String getTargetSql();

		public void injectValue(ResultSet rs, HasIdAndLocalId source);
	}

	public class PdOperator {
		Method readMethod;

		ManyToMany manyToMany;

		ManyToOne manyToOne;

		JoinTable joinTable;

		OneToMany oneToMany;

		OneToOne oneToOne;

		Method writeMethod;

		PropertyDescriptor pd;

		public String name;

		Field field;

		Class clazz;

		public int idx;

		Class mappedClass;

		public PdOperator(PropertyDescriptor pd, Class clazz, int idx) {
			this.pd = pd;
			this.clazz = clazz;
			this.idx = idx;
			this.field = getDescriptorField(clazz, pd.getName());
			this.name = pd.getName();
			this.readMethod = pd.getReadMethod();
			this.writeMethod = pd.getWriteMethod();
			this.manyToMany = readMethod.getAnnotation(ManyToMany.class);
			this.manyToOne = readMethod.getAnnotation(ManyToOne.class);
			this.joinTable = readMethod.getAnnotation(JoinTable.class);
			this.oneToMany = readMethod.getAnnotation(OneToMany.class);
			this.oneToOne = readMethod.getAnnotation(OneToOne.class);
			AlcinaMemCacheMapping mapping = readMethod
					.getAnnotation(AlcinaMemCacheMapping.class);
			this.mappedClass = mapping == null ? null : mapping.mapping();
		}

		public Object read(HasIdAndLocalId obj) {
			try {
				return field.get(obj);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	public class RawValueReplacer<I> extends MemCacheReader<I, I> {
		@Override
		protected I read0(I input) throws Exception {
			if (input == null) {
				return null;
			}
			Field[] fields = new GraphProjection().getFieldsForClass(input);
			for (Field field : fields) {
				if (HasIdAndLocalId.class.isAssignableFrom(field.getType())) {
					HasIdAndLocalId value = (HasIdAndLocalId) field.get(input);
					if (value != null) {
						I raw = (I) cache.get(field.getType(), value.getId());
						field.set(input, raw);
					}
				}
			}
			return input;
		}
	}

	public class Transactional {
		public volatile int transactionCount;

		Set<Long> activeTransactionThreadIds = new LinkedHashSet<Long>();

		public PerThreadTransaction ensureTransaction() {
			PerThreadTransaction transaction = transactions.get();
			if (transaction == null) {
				LinkedHashSet<DomainTransformEvent> localTransforms = TransformManager
						.get()
						.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
				int pendingTransformCount = localTransforms.size();
				if (pendingTransformCount != 0) {
					for (DomainTransformEvent dte : localTransforms) {
						if (cacheDescriptor.perClass.keySet()
								.contains(dte.getObjectClass())) {
							throw new MemcacheException(String.format(
									"Starting a memcache transaction with an existing transform of a graphed object - %s."
											+ " In certain cases that might work -- but better practice to not do so",
									dte));
						}
					}
				}
				transaction = Registry.impl(PerThreadTransaction.class);
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
			T t = cache.get(clazz, id);
			if (transactionActiveInCurrentThread()) {
				return (T) transactions.get()
						.ensureTransactional((HasIdAndLocalId) t);
			} else {
				return t;
			}
		}

		public Set immutableRawValues(Class clazz) {
			PerThreadTransaction perThreadTransaction = transactions.get();
			if (perThreadTransaction == null) {
				return Collections
						.unmodifiableSet((Set) cache.immutableRawValues(clazz));
			}
			return perThreadTransaction.immutableRawValues(clazz, cache);
		}

		public <T> Map<Long, T> lookup(Class<T> clazz) {
			return (Map<Long, T>) cache.getMap(clazz);
		}

		public <V extends HasIdAndLocalId> V resolveTransactional(
				CacheListener listener, V value, Object[] path) {
			PerThreadTransaction perThreadTransaction = transactions.get();
			if (perThreadTransaction == null
					|| (value != null && !isCached(value.getClass()))) {
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
				for (BaseProjectionHasEquivalenceHash listener : AlcinaMemCache
						.get().cachingProjections.allItems()) {
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

	private final class ReentrantReadWriteLockWithThreadAccess
			extends ReentrantReadWriteLock {
		private ReentrantReadWriteLockWithThreadAccess(boolean fair) {
			super(fair);
		}

		@Override
		public Collection<Thread> getQueuedReaderThreads() {
			return super.getQueuedReaderThreads();
		}

		public java.util.Collection<Thread> getQueuedThreads() {
			return super.getQueuedThreads();
		}

		@Override
		public Collection<Thread> getQueuedWriterThreads() {
			return super.getQueuedWriterThreads();
		}
	}

	class BackupLazyLoader implements LazyObjectLoader {
		@Override
		public <T extends HasIdAndLocalId> void loadObject(Class<? extends T> c,
				long id, long localId) {
			try {
				CacheItemDescriptor itemDescriptor = cacheDescriptor.perClass
						.get(c);
				if (itemDescriptor != null && itemDescriptor.lazy) {
					// only one thread should load a given class, for
					// threadsafety reasons
					ClassIdLock lock = LockUtils.obtainClassIdLock(c, 0L);
					System.out.format("Backup lazy load: %s - %s\n",
							c.getSimpleName(), id);
					loadTable(c, "id=" + id, lock);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	class ColumnDescriptor {
		private PropertyDescriptor pd;

		private Class<?> type;

		private boolean hili;

		private EnumType enumType = EnumType.ORDINAL;

		public ColumnDescriptor(PropertyDescriptor pd, Class propertyType) {
			this.pd = pd;
			type = propertyType;
			hili = HasIdAndLocalId.class.isAssignableFrom(type);
			Enumerated enumerated = pd.getReadMethod()
					.getAnnotation(Enumerated.class);
			if (enumerated != null) {
				enumType = enumerated.value();
			}
		}

		public String getColumnName() {
			Column col = pd.getReadMethod().getAnnotation(Column.class);
			JoinColumn joinColumn = pd.getReadMethod()
					.getAnnotation(JoinColumn.class);
			if (col == null && joinColumn == null) {
				if (HasId.class.isAssignableFrom(pd.getPropertyType())) {
					return pd.getName() + "_id";
				}
				return pd.getName();
			}
			return col != null ? col.name() : joinColumn.name();
		}

		public String getColumnSql() {
			String columnName = getColumnName();
			if (type == Date.class) {
				return String.format(
						"EXTRACT (EPOCH FROM %s::timestamp)::float*1000 as %s",
						columnName, columnName);
			} else {
				return columnName;
			}
		}

		public Object getObject(ResultSet rs, int idx) throws Exception {
			if (hili || type == Long.class || type == long.class) {
				Long v = rs.getLong(idx);
				if (rs.wasNull()) {
					v = null;
				}
				return v;
			}
			if (type == String.class) {
				return rs.getString(idx);
			}
			if (type == Double.class || type == double.class) {
				Double v = rs.getDouble(idx);
				if (rs.wasNull()) {
					v = null;
				}
				return v;
			}
			if (type == Float.class || type == float.class) {
				Float v = rs.getFloat(idx);
				if (rs.wasNull()) {
					v = null;
				}
				return v;
			}
			if (type == Integer.class || type == int.class) {
				Integer v = rs.getInt(idx);
				if (rs.wasNull()) {
					v = null;
				}
				return v;
			}
			if (type == Boolean.class || type == boolean.class) {
				Boolean v = rs.getBoolean(idx);
				if (rs.wasNull()) {
					v = null;
				}
				return v;
			}
			if (type == Date.class) {
				long utcTime = rs.getLong(idx);
				if (rs.wasNull()) {
					return null;
				}
				// was persisted by hibernate to utc, need to convert to local
				// tz
				// assume same tz for persist/retrieve
				// int currentOffset =
				// startupTz.getOffset(System.currentTimeMillis());
				int persistOffset = startupTz.getOffset(utcTime);
				long timeLocal = utcTime - persistOffset;
				return rs.wasNull() ? null : new Date(timeLocal);
				// Timestamp v = rs.getTimestamp(idx);
				// return v == null ? null : new Date(v.getTime());
			}
			if (Enum.class.isAssignableFrom(type)) {
				switch (enumType) {
				case ORDINAL:
					int eIdx = rs.getInt(idx);
					Object[] enumConstants = type.getEnumConstants();
					if (eIdx >= enumConstants.length) {
						warnLogger.format("Invalid enum index : %s:%s\n",
								type.getSimpleName(), eIdx);
						return null;
					}
					return rs.wasNull() ? null : enumConstants[eIdx];
				case STRING:
					String enumString = rs.getString(idx);
					if (enumString == null) {
						return null;
					}
					Enum enumValue = CommonUtils
							.getEnumValueOrNull((Class) type, enumString);
					if (enumValue == null) {
						warnLogger.format("Invalid enum value : %s:%s\n",
								type.getSimpleName(), enumString);
						return null;
					}
					return enumValue;
				}
			}
			throw new RuntimeException(
					"Unhandled rs type: " + type.getSimpleName());
		}
	}

	long timzoneOffset = -1;

	Calendar startupCal = Calendar.getInstance();

	TimeZone startupTz = (TimeZone) startupCal.getTimeZone().clone();

	class ConnResults implements Iterable<Object[]> {
		ConnResultsIterator itr = new ConnResultsIterator();

		private Connection conn;

		private List<ColumnDescriptor> columnDescriptors;

		private Class clazz;

		private String sqlFilter;

		ResultSet rs = null;

		public ConnResults(Connection conn, Class clazz,
				List<ColumnDescriptor> columnDescriptors, String sqlFilter) {
			this.conn = conn;
			this.clazz = clazz;
			this.columnDescriptors = columnDescriptors;
			this.sqlFilter = sqlFilter;
		}

		public ResultSet ensureRs() {
			try {
				if (rs == null) {
					conn.setAutoCommit(false);
					Statement stmt = conn.createStatement();
					stmt.setFetchSize(20000);
					String template = "select %s from %s";
					List<String> columnNames = new ArrayList<String>();
					for (ColumnDescriptor descr : columnDescriptors) {
						columnNames.add(descr.getColumnSql());
					}
					Table table = (Table) clazz.getAnnotation(Table.class);
					String sql = String.format(template,
							CommonUtils.join(columnNames, ","), table.name());
					if (CommonUtils.isNotNullOrEmpty(sqlFilter)) {
						sql += String.format(" where %s", sqlFilter);
					}
					sqlLogger.log(sql);
					rs = stmt.executeQuery(sql);
				}
				return rs;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public Iterator<Object[]> iterator() {
			return itr;
		}

		class ConnResultsIterator implements Iterator<Object[]> {
			Object[] cached = null;

			boolean finished = false;

			@Override
			public boolean hasNext() {
				peekNext();
				return !finished;
			}

			@Override
			public Object[] next() {
				if (finished) {
					throw new NoSuchElementException();
				}
				peekNext();
				Object[] result = cached;
				cached = null;
				return result;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			private void peekNext() {
				if (cached == null && !finished) {
					ensureRs();
					try {
						if (rs.next()) {
							cached = new Object[columnDescriptors.size()];
							for (int idx = 1; idx <= columnDescriptors
									.size(); idx++) {
								ColumnDescriptor descriptor = columnDescriptors
										.get(idx - 1);
								cached[idx - 1] = descriptor.getObject(rs, idx);
							}
						} else {
							finished = true;
							rs.close();
						}
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}
		}
	}

	class DetachedCacheObjectStorePsAware extends DetachedCacheObjectStore {
		public DetachedCacheObjectStorePsAware() {
			super(new PsAwareMultiplexingObjectCache());
		}
	}

	static class FilterContext {
		int idx = 0;

		public String lastFilterString;
	}

	class InSubgraphFilter implements CollectionFilter<DomainTransformEvent> {
		@Override
		public boolean allow(DomainTransformEvent o) {
			if (!cacheDescriptor.cachePostTransform(o.getObjectClass(), o)) {
				return false;
			}
			switch (o.getTransformType()) {
			case ADD_REF_TO_COLLECTION:
			case REMOVE_REF_FROM_COLLECTION:
			case CHANGE_PROPERTY_REF:
				return cacheDescriptor.cachePostTransform(o.getValueClass(), o);
			}
			return true;
		}
	}

	class MemCachePersistenceListener
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

	class ModificationCheckerSupport extends MutablePropertyChangeSupport
			implements ModificationChecker {
		public ModificationCheckerSupport(Object sourceBean) {
			super(sourceBean);
		}

		@Override
		public synchronized void
				addPropertyChangeListener(PropertyChangeListener listener) {
			check("add");
		}

		@Override
		public synchronized void addPropertyChangeListener(String propertyName,
				PropertyChangeListener listener) {
			check("add");
		}

		@Override
		public void fireNullPropertyChange(String name) {
			check("fire");
		}

		@Override
		public void firePropertyChange(PropertyChangeEvent evt) {
			check("fire");
		}

		@Override
		public void firePropertyChange(String propertyName, Object oldValue,
				Object newValue) {
			if (!(CommonUtils.equalsWithNullEquality(oldValue, newValue))) {
				check("fire");
			}
		}

		@Override
		public PropertyChangeListener[] getPropertyChangeListeners() {
			check("get");
			return null;
		}

		@Override
		public void
				removePropertyChangeListener(PropertyChangeListener listener) {
			check("remove");
		}

		@Override
		public void removePropertyChangeListener(String propertyName,
				PropertyChangeListener listener) {
			check("remove");
		}

		public void check(String key) {
			// add-remove - well, there's a bunch of automated adds (e.g.
			// cc.alcina.framework.entity.domaintransform.ServerTransformManagerSupport
			// .removeParentAssociations(HasIdAndLocalId)
			// that add em by default. fix them first
			// TODO - memcache
			if (!checkModificationWriteLock) {
				return;
			}
			if (!lockingDisabled && key.equals("fire")
					&& !mainLock.isWriteLockedByCurrentThread()
					&& (subgraphLock == null
							|| !subgraphLock.isWriteLockedByCurrentThread())) {
				throw new MemcacheException(
						"Modification of graph object outside writer thread - "
								+ key);
			}
		}
	}

	class SubgraphTransformManagerRemoteOnly extends SubgraphTransformManager {
		public void addPropertyStore(CacheItemDescriptor descriptor) {
			((PsAwareMultiplexingObjectCache) store.getCache())
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

		void endCommit() {
			((PsAwareMultiplexingObjectCache) store.getCache()).endCommit();
		}

		void startCommit() {
			((PsAwareMultiplexingObjectCache) store.getCache()).startCommit();
		}
	}

	public static <T extends HasIdAndLocalId> T ensureNonRaw(T t) {
		return isRawValue(t) ? get().find(t) : t;
	}
}
