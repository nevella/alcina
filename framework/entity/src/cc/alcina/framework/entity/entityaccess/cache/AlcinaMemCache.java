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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.PropertyPathFilter;
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
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
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
import cc.alcina.framework.entity.entityaccess.TransformPersister;
import cc.alcina.framework.entity.entityaccess.cache.CacheDescriptor.CacheTask;
import cc.alcina.framework.entity.entityaccess.cache.CacheDescriptor.PreProvideTask;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.SqlUtils;

import com.google.gwt.event.shared.UmbrellaException;

/**
 * <h3>Locking notes:</h3>
 * <p>
 * main lock (post-process) - normal lock sublock - basically go from read
 * (possibly write::main) to write (subgraph) - so we know we'll have a main
 * lock
 * </p>
 * 
 * @author nick@alcina.cc
 * 
 */
public class AlcinaMemCache {
	public static final String TOPIC_UPDATE_EXCEPTION = AlcinaMemCache.class
			.getName() + ".TOPIC_UPDATE_EXCEPTION";

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
		AlcinaMemCache singleton = Registry
				.checkSingleton(AlcinaMemCache.class);
		if (singleton == null) {
			singleton = new AlcinaMemCache();
			Registry.registerSingleton(AlcinaMemCache.class, singleton);
		}
		return singleton;
	}

	private Map<PropertyDescriptor, JoinTable> joinTables;

	private Map<Class, List<PropertyDescriptor>> descriptors;

	// class,pName
	private UnsortedMultikeyMap<PropertyDescriptor> manyToOneRev;

	private UnsortedMultikeyMap<PropertyDescriptor> oneToOneRev;

	private Connection conn;

	private Multimap<Class, List<ColumnDescriptor>> columnDescriptors;

	private Map<PropertyDescriptor, Class> propertyDescriptorFetchTypes = new LinkedHashMap<PropertyDescriptor, Class>();

	private LaterLookup laterLookup;

	SubgraphTransformManager transformManager;

	private CacheDescriptor cacheDescriptor;

	private TaggedLogger sqlLogger = Registry.impl(TaggedLoggers.class)
			.getLogger(AlcinaMemCache.class, TaggedLogger.DEBUG);

	private TaggedLogger metricLogger = Registry.impl(TaggedLoggers.class)
			.getLogger(AlcinaMemCache.class, TaggedLogger.METRIC);

	private TaggedLogger warnLogger = Registry.impl(TaggedLoggers.class)
			.getLogger(AlcinaMemCache.class, TaggedLogger.WARN,
					TaggedLogger.INFO, TaggedLogger.DEBUG);

	private ThreadLocal<PerThreadTransaction> transactions = new ThreadLocal() {
	};

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

	private MemCachePersistenceListener persistenceListener;

	private boolean initialised = false;

	public Transactional transactional = new Transactional();

	private MutablePropertyChangeSupport modificationChecker;

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

	private ReentrantReadWriteLock mainLock = new ReentrantReadWriteLock(false);

	private ReentrantReadWriteLock subgraphLock = new ReentrantReadWriteLock(
			false);

	private BackupLazyLoader backupLazyLoader;

	private boolean initialising;

	private boolean lockingDisabled;

	private long lastLockingDisabledMessage;

	private Set<Thread> waitingOnWriteLock = Collections
			.synchronizedSet(new LinkedHashSet<Thread>());

	private int maxLockQueueLength;

	private int originalTransactionIsolation;

	public static final String WRAPPED_OBJECT_REF_INTEGRITY = "WRAPPED_OBJECT_REF_INTEGRITY";

	public AlcinaMemCache() {
		ThreadlocalTransformManager
				.threadTransformManagerWasResetListenerDelta(resetListener,
						true);
		TransformPersister.persistingTransformsListenerDelta(
				persistingListener, true);
		persistenceListener = new MemCachePersistenceListener();
		maxLockQueueLength = ResourceUtilities.getInteger(AlcinaMemCache.class,
				"maxLockQueueLength", 40);
	}

	public void addValues(CacheListener listener) {
		for (Object o : cache.values(listener.getListenedClass())) {
			listener.insert((HasIdAndLocalId) o);
		}
	}

	public <T extends HasIdAndLocalId> Set<T> asSet(Class<T> clazz) {
		return new AlcinaMemCacheQuery().ids(getIds(clazz)).raw().asSet(clazz);
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

	public List<DomainTransformEvent> filterInterestedTransforms(
			Collection<DomainTransformEvent> dtes) {
		List<DomainTransformEvent> filtered = CollectionFilters.filter(dtes,
				new InSubgraphFilter());
		return filtered;
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz, long id) {
		return new AlcinaMemCacheQuery().id(id).find(clazz);
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz, String key,
			Object value) {
		return findOrCreate(clazz, key, value, false);
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
		if (raw) {
			query.raw();
		}
		if (raw) {
			query.raw();
		}
		T first = query.find(clazz);
		if (first == null && createIfNonexistent) {
			first = (T) TransformManager.get()
					.createDomainObject((Class) clazz);
			Reflections.propertyAccessor()
					.setPropertyValue(first, key1, value1);
			if (key2 != null) {
				Reflections.propertyAccessor().setPropertyValue(first, key2,
						value2);
			}
		}
		return first;
	}

	public <T extends HasIdAndLocalId> T findRaw(Class<T> clazz, long id) {
		return new AlcinaMemCacheQuery().id(id).raw().find(clazz);
	}

	public Iterable<Object[]> getData(Class clazz, String sqlFilter)
			throws SQLException {
		ConnResults result = new ConnResults(conn, clazz,
				columnDescriptors.get(clazz), sqlFilter);
		return result;
	}

	public Collection<Long> getIds(Class<? extends HasIdAndLocalId> clazz) {
		try {
			lock(false);
			return new ArrayList<Long>(cache.keys(clazz));
		} finally {
			unlock(false);
		}
	}

	public <CL extends CacheLookup> CL getLookupFor(
			CacheLookupDescriptor descriptor) {
		return (CL) descriptor.getLookup();
	}

	public int getLookupSize(CacheLookupDescriptor descriptor, Object value) {
		return descriptor.getLookup().size(value);
	}

	public MemCachePersistenceListener getPersistenceListener() {
		return this.persistenceListener;
	}

	public int getSize(Class clazz) {
		return cache.size(clazz);
	}

	public boolean isCached(Class clazz) {
		return cacheDescriptor.perClass.containsKey(clazz);
	}

	public boolean isInitialised() {
		return initialised;
	}

	public <V extends HasIdAndLocalId> boolean isRawValue(V v) {
		V existing = (V) cache.get(v.getClass(), v.getId());
		return existing == v;
	}

	public void linkFromServletLayer() {
	}

	public <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
		return new AlcinaMemCacheQuery().ids(getIds(clazz)).raw().list(clazz);
	}

	public <T extends HasIdAndLocalId> List<T> list(Class<T> clazz,
			Collection<Long> ids) {
		return new AlcinaMemCacheQuery().ids(ids).list(clazz);
	}

	public void loadTable(Class clazz, String sqlFilter, ClassIdLock sublock)
			throws Exception {
		if (sublock != null) {
			sublock(sublock, true);
		}
		try {
			List<HasIdAndLocalId> loaded = loadTable0(clazz, sqlFilter, sublock);
			if (sublock != null) {
				resolveRefs();
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
			releaseConnectionLocks();
		}
	}

	public void lock(boolean write) {
		if (lockingDisabled) {
			if (System.currentTimeMillis() - lastLockingDisabledMessage > TimeConstants.ONE_MINUTE_MS) {
				System.out.format("memcache - lock %s - locking disabled\n",
						write);
			}
			lastLockingDisabledMessage = System.currentTimeMillis();
			return;
		}
		try {
			if (mainLock.getQueueLength() > maxLockQueueLength) {
				System.out
						.println("Disabling locking due to deadlock:\n***************\n");
				System.out.println(CommonUtils.join(recentLockAcquisitions,
						"\n"));
				AlcinaTopics.notifyDevWarning(new MemcacheException(
						"Disabling locking to long queue/deadlock"));
				lockingDisabled = true;
				for (Thread t : waitingOnWriteLock) {
					t.interrupt();
				}
				waitingOnWriteLock.clear();
				return;
			}
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
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
		maybeLogLock("lock", write);
	}

	public List<Long> notInStore(Collection<Long> ids, Class clazz) {
		return cache.notContained(ids, clazz);
	}

	public <H extends HasIdAndLocalId> List<H> rawForSql(Class<H> clazz,
			String fieldName, String sqlFormatString, Object... objects)
			throws SQLException {
		String sql = String.format(sqlFormatString, objects);
		Statement stmt = conn.createStatement();
		List<H> result = new ArrayList<H>();
		Set<Long> ids = SqlUtils.toIdList(stmt, sql, fieldName);
		for (Long id : ids) {
			result.add(cache.get(clazz, id));
		}
		return result;
	}

	public void registerForTesting(HasIdAndLocalId hili) {
		if (!AppPersistenceBase.isTest()) {
			throw new RuntimeException("Only when testing...");
		}
		cache.put(hili);
		index(hili, true);
	}

	public void reset() {
		Registry.registerSingleton(AlcinaMemCache.class, new AlcinaMemCache());
	}

	public void resolveRefs() {
		MetricLogging.get().start("resolve");
		laterLookup.resolve();
		MetricLogging.get().end("resolve", metricLogger);
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

	/**
	 * Given sublock-guarded code should be able to be run concurrently (as long
	 * as the sublock objects are different), will rework this
	 */
	public void sublock(Object sublock, boolean lock) {
		if (lockingDisabled) {
			return;
		}
		if (lock) {
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
		maybeLogLock("sublock", lock);
	}

	public void unlock(boolean write) {
		if (lockingDisabled) {
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
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
		maybeLogLock("unlock", write);
	}

	public void warmup(Connection conn, CacheDescriptor cacheDescriptor) {
		this.conn = conn;
		this.cacheDescriptor = cacheDescriptor;
		try {
			conn.setAutoCommit(false);
			originalTransactionIsolation = conn.getTransactionIsolation();
			try {
				conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
			} catch (Exception e) {
				// postgres patch
				if (CommonUtils
						.nullToEmpty(e.getMessage())
						.toLowerCase()
						.contains(
								"cannot use serializable mode in a hot standby")) {
					conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
				} else {
					throw e;
				}
			}
			warmup0();
			initialised = true;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void addColumnName(Class clazz, PropertyDescriptor pd,
			Class propertyType) {
		columnDescriptors.add(clazz, new ColumnDescriptor(pd, propertyType));
		propertyDescriptorFetchTypes.put(pd, propertyType);
	}

	private void ensureModificationChecker(HasIdAndLocalId hili)
			throws Exception {
		if (modificationCheckerField != null
				&& hili instanceof BaseSourcesPropertyChangeEvents) {
			modificationCheckerField.set(hili, modificationChecker);
		}
	}

	private Set<Long> getFiltered(final Class clazz, CacheFilter cacheFilter,
			Set<Long> existing) {
		CacheLookup lookup = getLookupFor(clazz, cacheFilter.propertyPath);
		if (lookup != null) {
			Set<Long> set = lookup
					.getMaybeCollectionKey(cacheFilter.propertyValue);
			set = set != null ? set : new LinkedHashSet<Long>();
			return (Set<Long>) (existing == null ? set : CommonUtils
					.intersection(existing, set));
		}
		final CollectionFilter filter = cacheFilter.collectionFilter != null ? cacheFilter.collectionFilter
				: new PropertyPathFilter(cacheFilter.propertyPath,
						cacheFilter.propertyValue, cacheFilter.filterOperator);
		if (existing == null) {
			List filtered = CollectionFilters.filter(cache.rawValues(clazz),
					filter);
			return HiliHelper.toIdSet(filtered);
		} else {
			CollectionFilter withIdFilter = new CollectionFilter<Long>() {
				@Override
				public boolean allow(Long id) {
					return filter.allow(cache.get(clazz, id));
				}
			};
			existing = new LinkedHashSet<Long>(existing);
			CollectionFilters.filterInPlace(existing, withIdFilter);
			return existing;
		}
	}

	private Set getFilteredTransactional(final Class clazz,
			CacheFilter cacheFilter, Set existing) {
		final CollectionFilter filter = cacheFilter.collectionFilter != null ? cacheFilter.collectionFilter
				: new PropertyPathFilter(cacheFilter.propertyPath,
						cacheFilter.propertyValue);
		existing = existing != null ? existing : transactional.rawValues(clazz);
		CollectionFilters.filterInPlace(existing, filter);
		return existing;
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

	private Class getTargetEntityType(Method rm) {
		ManyToOne manyToOne = rm.getAnnotation(ManyToOne.class);
		if (manyToOne != null && manyToOne.targetEntity() != void.class) {
			return manyToOne.targetEntity();
		}
		OneToOne oneToOne = rm.getAnnotation(OneToOne.class);
		if (oneToOne != null && oneToOne.targetEntity() != void.class) {
			return oneToOne.targetEntity();
		}
		return rm.getReturnType();
	}

	private void index(HasIdAndLocalId obj, boolean add) {
		CacheItemDescriptor cacheItemDescriptor = cacheDescriptor.perClass
				.get(obj.getClass());
		for (CacheLookupDescriptor lookupDescriptor : cacheItemDescriptor.lookupDescriptors) {
			CacheLookup lookup = lookupDescriptor.getLookup();
			if (add) {
				lookup.insert(obj);
			} else {
				lookup.remove(obj);
			}
		}
		for (CacheProjection projection : cacheItemDescriptor.projections) {
			if (add) {
				projection.insert(obj);
			} else {
				projection.remove(obj);
			}
		}
	}

	private void loadJoinTable(Entry<PropertyDescriptor, JoinTable> entry) {
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
			if (entry2.getValue() == null && m.targetEntity() == declaringClass
					&& pd.getName().equals(m.mappedBy())) {
				rev = entry2.getKey();
				break;
			}
		}
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
				throw new RuntimeException("No reverse key for " + pd);
			}
		}
		try {
			String joinTableName = joinTable.name();
			MetricLogging.get().start(joinTableName);
			String sql = String.format("select %s, %s from %s;",
					joinTable.joinColumns()[0].name(),
					joinTable.inverseJoinColumns()[0].name(), joinTableName);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				HasIdAndLocalId src = (HasIdAndLocalId) cache.get(
						declaringClass, rs.getLong(1));
				HasIdAndLocalId tgt = (HasIdAndLocalId) cache.get(rev
						.getReadMethod().getDeclaringClass(), rs.getLong(2));
				// if (src == null || tgt == null) {
				// int j = 3;
				// {
				// src = (HasIdAndLocalId) cache.get(pd.getReadMethod()
				// .getDeclaringClass(), rs.getLong(1));
				// tgt = (HasIdAndLocalId) cache.get(rev.getReadMethod()
				// .getDeclaringClass(), rs.getLong(2));
				// }
				// }
				assert src != null && tgt != null;
				laterLookup.add(tgt, pd, src);
				laterLookup.add(src, rev, tgt);
			}
			MetricLogging.get().end(joinTableName, metricLogger);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			releaseConnectionLocks();
		}
	}

	private List<HasIdAndLocalId> loadTable0(Class clazz, String sqlFilter,
			ClassIdLock sublock) throws Exception {
		Iterable<Object[]> results = getData(clazz, sqlFilter);
		List<PropertyDescriptor> pds = descriptors.get(clazz);
		List<HasIdAndLocalId> loaded = new ArrayList<HasIdAndLocalId>();
		for (Object[] objects : results) {
			HasIdAndLocalId hili = (HasIdAndLocalId) clazz.newInstance();
			if (sublock != null) {
				loaded.add(hili);
			}
			ensureModificationChecker(hili);
			for (int i = 0; i < objects.length; i++) {
				PropertyDescriptor pd = pds.get(i);
				Method rm = pd.getReadMethod();
				ManyToOne manyToOne = rm.getAnnotation(ManyToOne.class);
				OneToOne oneToOne = rm.getAnnotation(OneToOne.class);
				if (manyToOne != null || oneToOne != null) {
					Long id = (Long) objects[i];
					if (id != null) {
						laterLookup.add(id, pd, hili);
					}
				} else {
					pd.getWriteMethod().invoke(hili, objects[i]);
				}
			}
			cache.put(hili);
		}
		return loaded;
	}

	private void maybeLogLock(String action, boolean write) {
		if (dumpLocks
				|| (collectLockAcquisitionPoints && (write || mainLock
						.getQueueLength() > maxLockQueueLength / 3))) {
			String message = String.format("Memcache lock - %s - %s\n",
					write ? "write" : "read", action);
			Thread t = Thread.currentThread();
			String log = CommonUtils.formatJ("\tid:%s\n\treadHoldCount:"
					+ " %s\n\twriteHoldcount: %s\n\tsublock: %s\n\n ",
					t.getId(), mainLock.getReadHoldCount(),
					mainLock.getWriteHoldCount(), subgraphLock);
			StackTraceElement[] trace = t.getStackTrace();
			for (int i = 2; i < trace.length && i < 10; i++) {
				log += trace[i] + "\n";
			}
			log += "\n\n";
			message += log;
			if (dumpLocks) {
				System.out.println(message);
			}
			if (collectLockAcquisitionPoints) {
				synchronized (recentLockAcquisitions) {
					recentLockAcquisitions.add(message);
					if (recentLockAcquisitions.size() > 100) {
						recentLockAcquisitions.removeFirst();
					}
				}
			}
		}
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
		List<PropertyDescriptor> mapped = new ArrayList<PropertyDescriptor>();
		descriptors.put(clazz, mapped);
		for (PropertyDescriptor pd : pds) {
			if (pd.getReadMethod() == null || pd.getWriteMethod() == null) {
				continue;
			}
			Method rm = pd.getReadMethod();
			if (rm.getAnnotation(Transient.class) != null
					|| rm.getAnnotation(AlcinaMemCacheTransient.class) != null) {
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
				if (manyToMany.mappedBy().isEmpty() && joinTable == null) {
					System.out
							.format("**warn - manytomany association with no join table: %s.%s\n",
									rm.getDeclaringClass().getSimpleName(),
									pd.getName());
				}
				joinTables.put(pd, joinTable);
				continue;
			}
			ManyToOne manyToOne = rm.getAnnotation(ManyToOne.class);
			OneToOne oneToOne = rm.getAnnotation(OneToOne.class);
			if (manyToOne != null || oneToOne != null) {
				Class joinEntityType = getTargetEntityType(rm);
				if (!cacheDescriptor.joinPropertyCached(joinEntityType)) {
					System.out.format("  not loading: %s.%s -- %s\n", clazz
							.getSimpleName(), pd.getName(), pd
							.getPropertyType().getSimpleName());
					continue;
				}
				if (oneToOne != null && !oneToOne.mappedBy().isEmpty()) {
					oneToOneRev.put(pd.getPropertyType(), oneToOne.mappedBy(),
							pd);
					continue;
				}
				addColumnName(clazz, pd,
						getTargetEntityType(pd.getReadMethod()));
			} else {
				addColumnName(clazz, pd, pd.getPropertyType());
			}
			mapped.add(pd);
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
		graph.setLastModificationDate(timestampToDate(persistent
				.getCreationDate()));
		Class<? extends IUser> iUserClass = cacheDescriptor.getIUserClass();
		Long persistentCreationUserId = HiliHelper.getIdOrNull(persistent
				.getCreationUser());
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
		((HasVersionNumber) obj).setVersionNumber(((HasVersionNumber) dte
				.getSource()).getVersionNumber());
	}

	private void warmup0() throws Exception {
		initialising = true;
		transformManager = new SubgraphTransformManager();
		backupLazyLoader = new BackupLazyLoader();
		cache = transformManager.getDetachedEntityCache();
		transformManager.getStore().setLazyObjectLoader(backupLazyLoader);
		joinTables = new LinkedHashMap<PropertyDescriptor, JoinTable>();
		descriptors = new LinkedHashMap<Class, List<PropertyDescriptor>>();
		manyToOneRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		oneToOneRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		columnDescriptors = new Multimap<Class, List<ColumnDescriptor>>();
		laterLookup = new LaterLookup();
		if (ResourceUtilities.getBoolean(AlcinaMemCache.class,
				"memcacheModificationCheck")) {
			modificationCheckerField = BaseSourcesPropertyChangeEvents.class
					.getDeclaredField("propertyChangeSupport");
			modificationCheckerField.setAccessible(true);
			modificationChecker = new ModificationCheckerSupport(null);
		}
		// get non-many-many obj
		lock(true);
		MetricLogging.get().start("tables");
		for (CacheItemDescriptor descriptor : cacheDescriptor.perClass.values()) {
			Class clazz = descriptor.clazz;
			prepareTable(descriptor);
			MetricLogging.get().start(clazz.getSimpleName());
			if (!descriptor.lazy) {
				loadTable(clazz, "", null);
			}
			MetricLogging.get().end(clazz.getSimpleName(), metricLogger);
		}
		for (Entry<PropertyDescriptor, JoinTable> entry : joinTables.entrySet()) {
			loadJoinTable(entry);
		}
		MetricLogging.get().end("tables");
		MetricLogging.get().start("xrefs");
		resolveRefs();
		MetricLogging.get().end("xrefs");
		unlock(true);
		MetricLogging.get().start("postLoad");
		for (CacheTask task : cacheDescriptor.postLoadTasks) {
			MetricLogging.get().start(task.getClass().getSimpleName());
			task.run(this);
			MetricLogging.get().end(task.getClass().getSimpleName(),
					metricLogger);
		}
		MetricLogging.get().end("postLoad");
		MetricLogging.get().start("lookups");
		for (CacheItemDescriptor descriptor : cacheDescriptor.perClass.values()) {
			for (CacheLookupDescriptor lookupDescriptor : descriptor.lookupDescriptors) {
				lookupDescriptor.createLookup();
				if (lookupDescriptor.isEnabled()) {
					addValues(lookupDescriptor.getLookup());
				}
			}
		}
		// deliberately init projections after lookups
		for (CacheItemDescriptor descriptor : cacheDescriptor.perClass.values()) {
			for (CacheProjection projection : descriptor.projections) {
				if (projection.isEnabled()) {
					addValues(projection);
				}
			}
		}
		initialising = false;
		if (ResourceUtilities.getBoolean(AlcinaMemCache.class, "dumpLocks")) {
			dumpLocks = true;
		}
		if (ResourceUtilities.getBoolean(AlcinaMemCache.class,
				"collectLockAcquisitionPoints")) {
			collectLockAcquisitionPoints = true;
		}
		releaseConnectionLocks();
		MetricLogging.get().end("lookups");
	}

	protected void releaseConnectionLocks() {
		try {
			if (initialised) {
				if (!conn.getAutoCommit()) {
					conn.commit();
					conn.setTransactionIsolation(originalTransactionIsolation);
					conn.setAutoCommit(true);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	<T extends HasIdAndLocalId> List<T> list(Class<T> clazz,
			AlcinaMemCacheQuery query) {
		try {
			lock(false);
			List<T> raw = null;
			Set<Long> ids = query.getIds();
			boolean transaction = transactional
					.transactionActiveInCurrentThread();
			if (!transaction || !ids.isEmpty()) {
				for (int i = 0; i < query.getFilters().size(); i++) {
					ids = getFiltered(clazz, query.getFilters().get(i),
							(i == 0 && ids.isEmpty()) ? null : ids);
				}
				raw = new ArrayList<T>(ids.size());
				for (Long id : ids) {
					T value = cache.get(clazz, id);
					if (value != null) {
						raw.add(value);
					}
				}
			} else {
				Set<T> rawTransactional = null;
				for (int i = 0; i < query.getFilters().size(); i++) {
					rawTransactional = getFilteredTransactional(clazz, query
							.getFilters().get(i), (i == 0) ? null
							: rawTransactional);
				}
				raw = new ArrayList<T>(rawTransactional);
			}
			try {
				for (PreProvideTask task : cacheDescriptor.preProvideTasks) {
					task.run(this, clazz, raw);
				}
				if (query.isRaw()) {
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

	void postProcess(DomainTransformPersistenceEvent persistenceEvent) {
		Set<Throwable> causes = new LinkedHashSet<Throwable>();
		StringBuilder warnBuilder = new StringBuilder();
		try {
			MetricLogging.get().start("post-process");
			lock(true);
			List<DomainTransformEvent> dtes = (List) persistenceEvent
					.getDomainTransformLayerWrapper().persistentEvents;
			List<DomainTransformEvent> filtered = filterInterestedTransforms(dtes);
			Multimap<HiliLocator, List<DomainTransformEvent>> perObjectTransforms = CollectionFilters
					.multimap(filtered, new DteToLocatorMapper());
			Map<HiliLocator, HasIdAndLocalId> locatorOriginalSourceMap = new LinkedHashMap<HiliLocator, HasIdAndLocalId>();
			for (DomainTransformEvent dte : filtered) {
				HiliLocator locator = HiliLocator.fromDte(dte);
				locatorOriginalSourceMap.put(locator, dte.getSource());
			}
			cacheDescriptor.loadLazyPreApplyPersist(persistenceEvent);
			Set<Long> uncommittedToLocalGraphLids = new LinkedHashSet<Long>();
			for (DomainTransformEvent dte : filtered) {
				dte.setNewValue(null);// force a lookup from the subgraph
			}
			for (DomainTransformEvent dte : filtered) {
				// remove from indicies before first change - and only if
				// preexisting object
				DomainTransformEvent first = CommonUtils
						.first(perObjectTransforms.get(HiliLocator.fromDte(dte)));
				DomainTransformEvent last = CommonUtils
						.last(perObjectTransforms.get(HiliLocator.fromDte(dte)));
				if (last.getTransformType() == TransformType.DELETE_OBJECT
						&& first.getTransformType() != TransformType.CREATE_OBJECT) {
					// this a check against deletion during cache warmup.
					// shouldn't happen anyway (trans. isolation)
					// TODO - check if necessary
					// (note) also a check against trying to handle deletion of
					// lazy objects
					HasIdAndLocalId memCacheObj = transformManager
							.getObject(dte);
					if (memCacheObj == null) {
						continue;
					}
				}
				if (dte.getTransformType() != TransformType.CREATE_OBJECT
						&& first == dte) {
					HasIdAndLocalId obj = transformManager.getObject(dte);
					if (obj != null) {
						index(obj, false);
					} else {
						warnLogger.format(
								"Null memcacheObject for index - %s\n", dte);
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
					} else if (dtex.getType() == DomainTransformExceptionType.TARGET_ENTITY_NOT_FOUND
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
							.get(HiliLocator.fromDte(dte));
					HasIdAndLocalId memCacheObj = transformManager
							.getObject(dte);
					if (dbObj instanceof HasVersionNumber) {
						updateVersionNumber(memCacheObj, dte);
					}
					if (dbObj instanceof IVersionable) {
						updateIVersionable(memCacheObj, persistentLayerSource);
					}
					ensureModificationChecker(memCacheObj);
					if (memCacheObj != null) {
						index(memCacheObj, true);
					} else {
						warnLogger.format(
								"Null memcacheObject for index - %s\n", dte);
					}
				}
			}
		} catch (Exception e) {
			causes.add(e);
		} finally {
			unlock(true);
			MetricLogging.get().end("post-process");
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
					GlobalTopicPublisher.get().publishTopic(
							TOPIC_UPDATE_EXCEPTION, umby);
					throw new MemcacheException(umby);
				}
			} catch (Throwable t) {
				t.printStackTrace();
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

	public class Transactional {
		public volatile int transactionCount;

		Set<Long> activeTransactionThreadIds = new LinkedHashSet<Long>();

		public PerThreadTransaction ensureTransaction() {
			PerThreadTransaction transaction = transactions.get();
			if (transaction == null) {
				LinkedHashSet<DomainTransformEvent> localTransforms = TransformManager
						.get().getTransformsByCommitType(
								CommitType.TO_LOCAL_BEAN);
				int pendingTransformCount = localTransforms.size();
				if (pendingTransformCount != 0) {
					for (DomainTransformEvent dte : localTransforms) {
						if (cacheDescriptor.perClass.keySet().contains(
								dte.getObjectClass())) {
							throw new RuntimeException(
									String.format(
											"Starting a memcache transaction with an existing transform of a graphed object - %s."
													+ " In certain cases that might work -- but better practice to not do so",
											dte));
						}
					}
				}
				transaction = Registry.impl(PerThreadTransaction.class);
				transactions.set(transaction);
				synchronized (this) {
					activeTransactionThreadIds.add(Thread.currentThread()
							.getId());
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
				return (T) transactions.get().ensureTransactional(
						(HasIdAndLocalId) t);
			} else {
				return t;
			}
		}

		public <T> Map<Long, T> lookup(Class<T> clazz) {
			return (Map<Long, T>) cache.getMap(clazz);
		}

		public Set rawValues(Class clazz) {
			PerThreadTransaction perThreadTransaction = transactions.get();
			if (perThreadTransaction == null) {
				return new LinkedHashSet(cache.rawValues(clazz));
			}
			return new LinkedHashSet(perThreadTransaction.rawValues(clazz,
					cache));
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
				transactions.remove();
				synchronized (this) {
					activeTransactionThreadIds.remove(Thread.currentThread()
							.getId());
					transactionCount = activeTransactionThreadIds.size();
				}
			}
		}

		public boolean transactionsActive() {
			return transactionCount != 0;
		}
	}

	class BackupLazyLoader implements LazyObjectLoader {
		@Override
		public <T extends HasIdAndLocalId> void loadObject(
				Class<? extends T> c, long id, long localId) {
			try {
				CacheItemDescriptor itemDescriptor = cacheDescriptor.perClass
						.get(c);
				if (itemDescriptor.lazy && id != 0) {
					ClassIdLock lock = LockUtils.obtainClassIdLock(c, id);
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

		public ColumnDescriptor(PropertyDescriptor pd, Class propertyType) {
			this.pd = pd;
			type = propertyType;
			hili = HasIdAndLocalId.class.isAssignableFrom(type);
		}

		public String getColumnName() {
			Column col = pd.getReadMethod().getAnnotation(Column.class);
			JoinColumn joinColumn = pd.getReadMethod().getAnnotation(
					JoinColumn.class);
			if (col == null && joinColumn == null) {
				if (HasId.class.isAssignableFrom(pd.getPropertyType())) {
					return pd.getName() + "_id";
				}
				return pd.getName();
			}
			return col != null ? col.name() : joinColumn.name();
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
				Timestamp v = rs.getTimestamp(idx);
				return v == null ? null : new Date(v.getTime());
			}
			if (Enum.class.isAssignableFrom(type)) {
				int eIdx = rs.getInt(idx);
				Object[] enumConstants = type.getEnumConstants();
				if (eIdx >= enumConstants.length) {
					warnLogger.format("Invalid enum index : %s:%s\n",
							type.getSimpleName(), eIdx);
					return null;
				}
				return rs.wasNull() ? null : enumConstants[eIdx];
			}
			throw new RuntimeException("Unhandled rs type: "
					+ type.getSimpleName());
		}
	}

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

		public void ensureRs() {
			try {
				if (rs == null) {
					conn.setAutoCommit(false);
					Statement stmt = conn.createStatement();
					stmt.setFetchSize(20000);
					String template = "select %s from %s";
					List<String> columnNames = new ArrayList<String>();
					for (ColumnDescriptor descr : columnDescriptors) {
						columnNames.add(descr.getColumnName());
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
							for (int idx = 1; idx <= columnDescriptors.size(); idx++) {
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

	class InSubgraphFilter implements CollectionFilter<DomainTransformEvent> {
		@Override
		public boolean allow(DomainTransformEvent o) {
			if (!cacheDescriptor.cachePostTransform(o.getObjectClass())) {
				return false;
			}
			switch (o.getTransformType()) {
			case ADD_REF_TO_COLLECTION:
			case REMOVE_REF_FROM_COLLECTION:
			case CHANGE_PROPERTY_REF:
				return cacheDescriptor.cachePostTransform(o.getValueClass());
			}
			return true;
		}
	}

	class LaterLookup {
		List<LaterItem> items = new ArrayList<AlcinaMemCache.LaterLookup.LaterItem>(
				8000000);

		void add(HasIdAndLocalId target, PropertyDescriptor pd,
				HasIdAndLocalId source) {
			items.add(new LaterItem(target, pd, source));
		}

		void add(long id, PropertyDescriptor pd, HasIdAndLocalId source) {
			items.add(new LaterItem(id, pd, source));
		}

		void resolve() {
			try {
				for (LaterItem item : items) {
					try {
						PropertyDescriptor pd = item.pd;
						Method rm = pd.getReadMethod();
						long id = item.id;
						if (joinTables.containsKey(pd)) {
							Set set = (Set) pd.getReadMethod().invoke(
									item.source, new Object[0]);
							if (set == null) {
								set = new LinkedHashSet();
								pd.getWriteMethod().invoke(item.source,
										new Object[] { set });
							}
							set.add(item.target);
						} else {
							Object target = cache.get(
									propertyDescriptorFetchTypes.get(pd), id);
							if (target == null) {
								System.out
										.format("later-lookup -- missing target: %s, %s for  %s.%s #%s",
												propertyDescriptorFetchTypes
														.get(pd), id,
												item.source.getClass(), pd
														.getName(), item.source
														.getId());
							}
							pd.getWriteMethod().invoke(item.source, target);
							PropertyDescriptor targetPd = manyToOneRev.get(
									item.source.getClass(), pd.getName());
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
									pd.getName());
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
				items.clear();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		class LaterItem {
			long id;

			PropertyDescriptor pd;

			HasIdAndLocalId source;

			HasIdAndLocalId target;

			public LaterItem(HasIdAndLocalId target, PropertyDescriptor pd,
					HasIdAndLocalId source) {
				this.target = target;
				this.pd = pd;
				this.source = source;
			}

			public LaterItem(long id, PropertyDescriptor pd,
					HasIdAndLocalId source) {
				this.id = id;
				this.pd = pd;
				this.source = source;
			}

			@Override
			public String toString() {
				// TODO Auto-generated method stub
				return super.toString();
			}
		}
	}

	class MemCachePersistenceListener implements
			DomainTransformPersistenceListener {
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

	class ModificationCheckerSupport extends MutablePropertyChangeSupport {
		public ModificationCheckerSupport(Object sourceBean) {
			super(sourceBean);
		}

		@Override
		public synchronized void addPropertyChangeListener(
				PropertyChangeListener listener) {
			handle("add");
		}

		@Override
		public synchronized void addPropertyChangeListener(String propertyName,
				PropertyChangeListener listener) {
			handle("add");
		}

		@Override
		public void fireNullPropertyChange(String name) {
			handle("fire");
		}

		@Override
		public void firePropertyChange(PropertyChangeEvent evt) {
			handle("fire");
		}

		@Override
		public void firePropertyChange(String propertyName, Object oldValue,
				Object newValue) {
			if (!(CommonUtils.equalsWithNullEquality(oldValue, newValue))) {
				handle("fire");
			}
		}

		@Override
		public PropertyChangeListener[] getPropertyChangeListeners() {
			handle("get");
			return null;
		}

		@Override
		public void removePropertyChangeListener(PropertyChangeListener listener) {
			handle("remove");
		}

		@Override
		public void removePropertyChangeListener(String propertyName,
				PropertyChangeListener listener) {
			handle("remove");
		}

		private void handle(String key) {
			// add-remove - well, there's a bunch of automated adds (e.g.
			// cc.alcina.framework.entity.domaintransform.ServerTransformManagerSupport
			// .removeParentAssociations(HasIdAndLocalId)
			// that add em by default. fix them first
			// TODO - memcache
			if (!lockingDisabled
					&& key.equals("fire")
					&& !mainLock.isWriteLockedByCurrentThread()
					&& (subgraphLock == null || !subgraphLock
							.isWriteLockedByCurrentThread())) {
				throw new MemcacheException(
						"Modification of graph object outside writer thread - "
								+ key);
			}
		}
	}

	public static void checkActiveTransaction() {
		if (!get().transactional.transactionActiveInCurrentThread()) {
			throw new RuntimeException("requires transaction in current thread");
		}
	}

	public void dumpLocks() {
		System.out.println("MemCache-main: " + mainLock);
		System.out.println("MemCache-subgraph: " + subgraphLock);
	}

	public static PerThreadTransaction ensureActiveTransaction() {
		return get().transactional.ensureTransaction();
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

	public <T> T replaceWithRawValues(T t) {
		return (T) new RawValueReplacer().read(t);
	}
}
