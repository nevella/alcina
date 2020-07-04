package cc.alcina.framework.entity.entityaccess.cache;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.domain.DomainDescriptor.DomainStoreTask;
import cc.alcina.framework.common.client.domain.DomainProjection;
import cc.alcina.framework.common.client.domain.DomainStoreLookupDescriptor;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.AlcinaPersistentEntityImpl;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.entityaccess.NamedThreadFactory;
import cc.alcina.framework.entity.entityaccess.cache.DomainSegmentLoader.DomainSegmentLoaderPhase;
import cc.alcina.framework.entity.entityaccess.cache.DomainSegmentLoader.DomainSegmentLoaderProperty;
import cc.alcina.framework.entity.entityaccess.cache.DomainSegmentLoader.DomainSegmentPropertyType;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.ConnResults.ConnResultsIterator;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.LaterLookup.LaterItem;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreProperty.DomainStorePropertyLoadType;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Mvcc;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccObject;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.projection.EntityUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;

/*FIXME - mvcc.4
 * The various loadtable methods are way to overloaded 
 * (and use context vars as well because of call depth). Refactor into a loadparams builder
 */
public class DomainStoreLoaderDatabase implements DomainStoreLoader {
	Logger logger = LoggerFactory.getLogger(getClass());

	private Map<PropertyDescriptor, JoinTable> joinTables;

	private Map<Class, List<PdOperator>> descriptors;

	// class,pName
	private UnsortedMultikeyMap<PropertyDescriptor> manyToOneRev;

	private UnsortedMultikeyMap<PropertyDescriptor> oneToOneRev;

	private UnsortedMultikeyMap<PropertyDescriptor> domainStoreColumnRev;

	private Multimap<Class, List<ColumnDescriptor>> columnDescriptors;

	private Map<PropertyDescriptor, Class> propertyDescriptorFetchTypes = new LinkedHashMap<PropertyDescriptor, Class>();

	private DomainStore store;

	RetargetableDataSource dataSource;

	private ThreadPoolExecutor warmupExecutor;

	private volatile Connection postInitConn;

	private AtomicInteger connectionsReopened = new AtomicInteger();

	private int originalTransactionIsolation;

	private UnsortedMultikeyMap<Field> domainStorePropertyFields = new UnsortedMultikeyMap<Field>(
			2);

	// only access via synchronized code
	CountingMap<Connection> warmupConnections = new CountingMap<>();

	MultikeyMap<PdOperator> operatorsByClass = new UnsortedMultikeyMap<>(2);

	private ReentrantLock postInitConnectionLock = new ReentrantLock(true);

	private BackupLazyLoader backupLazyLoader = new BackupLazyLoader();

	private List<LaterLookup> warmupLaterLookups = new ArrayList<>();

	DomainStoreDescriptor domainDescriptor;

	private boolean loadingSegment;

	private Map<JoinTable, DomainClassDescriptor> joinTableClassDescriptor = new LinkedHashMap<>();

	private Object loadTransformRequestLock = new Object();;

	DomainStoreTransformSequencer transformSequencer = new DomainStoreTransformSequencer(
			this);

	ThreadPoolExecutor iLoaderExecutor = (ThreadPoolExecutor) Executors
			.newFixedThreadPool(8,
					new NamedThreadFactory("domainStore-iLoader"));

	private Transaction warmupTransaction;

	public DomainStoreLoaderDatabase(DomainStore store,
			RetargetableDataSource dataSource,
			ThreadPoolExecutor warmupExecutor) {
		this.store = store;
		this.dataSource = dataSource;
		this.warmupExecutor = warmupExecutor;
	}

	@Override
	public void appShutdown() {
		if (postInitConn != null) {
			try {
				postInitConn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public LazyObjectLoader getLazyObjectLoader() {
		return backupLazyLoader;
	}

	public DomainStore getStore() {
		return this.store;
	}

	@Override
	public DomainStoreTransformSequencer getTransformSequencer() {
		return this.transformSequencer;
	}

	public <T extends Entity> List<T> loadTable(Class clazz, String sqlFilter,
			ClassIdLock sublock) throws Exception {
		assert sublock != null;
		try {
			LooseContext.push();
			LooseContext.remove(DomainStore.CONTEXT_NO_LOCKS);
			return (List<T>) loadTable(clazz, sqlFilter, sublock,
					new LaterLookup());
		} finally {
			LooseContext.pop();
		}
	}

	@Override
	public List<DomainTransformRequestPersistent> loadTransformRequests(
			Collection<Long> ids, Logger logger) throws Exception {
		synchronized (loadTransformRequestLock) {
			return loadTransformRequests0(ids, logger);
		}
	}

	@Override
	public void onTransformsPersisted() {
		try {
			transformSequencer.ensureTransactionCommitTimes();
		} catch (SQLException e) {
			logger.warn("Exception in ensureTransactionCommitTimes ", e);
		}
	}

	public void setConnectionUrl(String newUrl) {
		dataSource.setConnectionUrl(newUrl);
	}

	@Override
	public void warmup() throws Exception {
		this.domainDescriptor = store.domainDescriptor;
		joinTables = new LinkedHashMap<PropertyDescriptor, JoinTable>();
		descriptors = new LinkedHashMap<Class, List<PdOperator>>();
		manyToOneRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		oneToOneRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		domainStoreColumnRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		columnDescriptors = new Multimap<Class, List<ColumnDescriptor>>();
		transformSequencer.ensureTransactionCommitTimes();
		warmupTransaction = Transaction.current();
		createWarmupConnections();
		{
			Connection conn = getConnection();
			transformSequencer.markHighestVisibleTransformList(conn);
			releaseConn(conn);
		}
		warmupTransaction.toDomainCommitting(
				transformSequencer.getHighestVisibleTransactionTimestamp(),
				store, store.applyTxToGraphCounter.getAndIncrement(), 0L);
		// get non-many-many obj
		// lazy tables, load a segment (for large db dev work)
		if (domainDescriptor.getDomainSegmentLoader() != null) {
			MetricLogging.get().start("initialise-domain-segment");
			initialiseDomainSegment();
			MetricLogging.get().end("initialise-domain-segment");
		}
		MetricLogging.get().start("tables");
		for (DomainClassDescriptor descriptor : domainDescriptor.perClass
				.values()) {
			Class clazz = descriptor.clazz;
			prepareTable(descriptor);
			// warmup threadsafe
			store.cache.getMap(clazz);
		}
		List<Callable> calls = new ArrayList<Callable>();
		setupInitialLoadTableCalls(calls);
		invokeAllWithThrow(calls);
		setupInitialJoinTableCalls(calls);
		invokeAllWithThrow(calls);
		MetricLogging.get().end("tables");
		MetricLogging.get().start("xrefs");
		for (LaterLookup ll : warmupLaterLookups) {
			ll.resolve();
		}
		MetricLogging.get().end("xrefs");
		warmupLaterLookups.clear();
		// lazy tables, load a segment (for large db dev work)
		if (domainDescriptor.getDomainSegmentLoader() != null) {
			MetricLogging.get().start("domain-segment");
			loadingSegment = true;
			loadDomainSegment();
			loadingSegment = false;
			MetricLogging.get().end("domain-segment");
		}
		MetricLogging.get().start("postLoad");
		for (final DomainStoreTask task : domainDescriptor.postLoadTasks) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					MetricLogging.get().start(task.getClass().getSimpleName());
					task.run();
					MetricLogging.get().end(task.getClass().getSimpleName(),
							store.metricLogger);
					return null;
				}
			});
		}
		/*
		 * running synchronously (do they collide? they do...)
		 */
		for (Callable callable : calls) {
			callable.call();
		}
		// invokeAllWithThrow(calls);
		MetricLogging.get().end("postLoad");
		MetricLogging.get().start("lookups");
		for (final DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
				.values()) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					for (DomainStoreLookupDescriptor lookupDescriptor : descriptor.lookupDescriptors) {
						lookupDescriptor.setDomainStore(store);
						lookupDescriptor.createLookup();
						if (lookupDescriptor.isEnabled()) {
							store.addValues(lookupDescriptor.getLookup());
						}
					}
					return null;
				}
			});
		}
		invokeAllWithThrow(calls);
		MetricLogging.get().end("lookups");
		MetricLogging.get().start("projections");
		for (final DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
				.values()) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					for (DomainProjection projection : descriptor.projections) {
						if (projection.isEnabled()) {
							store.addValues(projection);
						}
					}
					return null;
				}
			});
		}
		invokeAllWithThrow(calls);
		MetricLogging.get().end("projections");
		store.initialising = false;
		// don't close, but indicate that everything write-y from now shd be
		// single-threaded
		warmupConnections.keySet().forEach(conn -> closeWarmupConnection(conn));
		warmupExecutor = null;
		warmupTransaction = null;
		Transaction.current().toDomainCommitted();
		Transaction.endAndBeginNew();
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
			if (ResourceUtilities.is(DomainStore.class, "warmStandbyDb")) {
				conn.setTransactionIsolation(
						Connection.TRANSACTION_REPEATABLE_READ);
			} else {
				conn.setTransactionIsolation(
						Connection.TRANSACTION_SERIALIZABLE);
			}
			logger.debug("Opening new warmup connection {}", conn);
			warmupConnections.put(conn, 0);
		}
	}

	private synchronized PdOperator ensurePdOperator(PropertyDescriptor pd,
			Class clazz) {
		return operatorsByClass.ensure(() -> {
			try {
				Collection<Object> values = operatorsByClass.values(clazz);
				return new PdOperator(pd, clazz,
						values == null ? 0 : values.size());
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}, clazz, pd);
	}

	private Class getEntityType(Class entityType) {
		if (MvccObject.class.isAssignableFrom(entityType)) {
			return entityType.getSuperclass();
		} else {
			return entityType;
		}
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
		DomainStoreDbColumn domainStoreColumn = rm
				.getAnnotation(DomainStoreDbColumn.class);
		if (domainStoreColumn != null
				&& domainStoreColumn.targetEntity() != void.class) {
			return domainStoreColumn.targetEntity();
		}
		return rm.getReturnType();
	}

	private synchronized Connection getWarmupConnection() throws Exception {
		Connection min = warmupConnections.min();
		warmupConnections.add(min);
		return min;
	}

	private void initialiseDomainSegment() throws Exception {
		DomainSegmentLoader segmentLoader = (DomainSegmentLoader) domainDescriptor
				.getDomainSegmentLoader();
		segmentLoader.initialise();
	}

	private void invokeAllWithThrow(List tasks) throws Exception {
		invokeAllWithThrow(tasks, warmupExecutor);
	}

	private void invokeAllWithThrow(List tasks, ThreadPoolExecutor executor)
			throws Exception {
		List initialList = tasks;
		if (executor == warmupExecutor) {
			tasks = (List) tasks.stream().map(WarmupTxCallable::new)
					.collect(Collectors.toList());
		}
		List<Future> futures = (List) executor.invokeAll(tasks);
		for (Future future : futures) {
			// will throw if there was an exception
			future.get();
		}
		initialList.clear();
	}

	private void loadDomainSegment() throws Exception {
		List<Callable> calls = new ArrayList<Callable>();
		DomainSegmentLoader segmentLoader = (DomainSegmentLoader) domainDescriptor
				.getDomainSegmentLoader();
		Set<Class> segmentClasses = new LinkedHashSet<>();
		for (DomainSegmentLoaderPhase phase : DomainSegmentLoaderPhase
				.iterateOver()) {
			segmentLoader.phase = phase;
			int maxPasses = 240;
			int pass = 0;
			long start = System.currentTimeMillis();
			int lastTotal = -1;
			int total = 0;
			while ((segmentLoader.pendingCount() != 0 || lastTotal != total)
					&& pass++ < maxPasses) {
				Set<Entry<Class, Set<Long>>> perClass = segmentLoader.toLoadIds
						.entrySet();
				List<LaterLookup> laterLookups = new ArrayList<>();
				for (Entry<Class, Set<Long>> entry : perClass) {
					LaterLookup laterLookup = new LaterLookup();
					laterLookups.add(laterLookup);
					segmentClasses.add(entry.getKey());
					calls.add(() -> {
						Collection<Long> ids = entry.getValue();
						ids = ids.stream().distinct().sorted()
								.collect(Collectors.toList());
						Class clazz = entry.getKey();
						ids = segmentLoader.filterForQueried(clazz, "id", ids);
						if (ids.size() > 0) {
							loadTableSegment(clazz, Ax.format(" id in %s",
									longsToIdClause(ids)), laterLookup);
						}
						segmentLoader.loadedInPhase(clazz, ids);
						entry.getValue().clear();
						return null;
					});
				}
				invokeAllWithThrow(calls);
				// resolve what we can (last pass)
				LaterLookup lastPassLookup = new LaterLookup();
				lastPassLookup.list.addAll(segmentLoader.toResolve);
				segmentLoader.toResolve.clear();
				lastPassLookup.resolve(segmentLoader);
				laterLookups.forEach(ll -> ll.resolve(segmentLoader));
				laterLookups.clear();
				if (segmentLoader.pendingCount() == 0) {
					for (DomainSegmentLoaderProperty property : segmentLoader.properties) {
						if (property.isIgnoreForPhase(phase)) {
							continue;
						}
						if (property.type == DomainSegmentPropertyType.TABLE_REF_CLAZZ_1_RSCOL_REFS_CLAZZ_2) {
							Collection<Long> ids = store.cache
									.keys(property.clazz2);
							ids = ids.stream().distinct().sorted()
									.collect(Collectors.toList());
							ids = segmentLoader.filterForQueried(
									property.clazz1, property.propertyName1,
									ids);
							String sqlFilter = Ax.format(" %s in %s",
									property.propertyName1,
									longsToIdClause(ids));
							segmentClasses.add(property.clazz1);
							if (ids.size() > 0) {
								calls.add(() -> {
									LaterLookup laterLookup = new LaterLookup();
									laterLookups.add(laterLookup);
									loadTableSegment(property.clazz1, sqlFilter,
											laterLookup);
									return null;
								});
							}
						} else if (property.type == DomainSegmentPropertyType.STORE_REF_CLAZZ_1_PROP_EQ_CLAZZ_2_ID_LOAD_CLAZZ_2) {
							if (store.cache.size(property.clazz1) == 0) {
								continue;
							}
							Collection<Long> ids = store.cache.fieldValues(
									property.clazz1, property.propertyName1);
							ids = ids.stream().distinct().sorted()
									.collect(Collectors.toList());
							ids = segmentLoader.filterForQueried(
									property.clazz2, "id", ids);
							String sqlFilter = Ax.format(" id in %s",
									longsToIdClause(ids));
							segmentClasses.add(property.clazz2);
							calls.add(() -> {
								LaterLookup laterLookup = new LaterLookup();
								laterLookups.add(laterLookup);
								loadTableSegment(property.clazz2, sqlFilter,
										laterLookup);
								return null;
							});
						}
					}
					invokeAllWithThrow(calls);
					laterLookups.forEach(ll -> ll.resolve(segmentLoader));
				}
				lastTotal = total;
				Integer size = segmentClasses.stream().collect(Collectors
						.summingInt(clazz -> store.cache.keys(clazz).size()));
				total = size;
				store.logger.info(
						"Load domain segment - pass {} {} - size {} - {} ms",
						phase, pass, size, System.currentTimeMillis() - start);
				segmentClasses.forEach(clazz -> store.logger.debug("{}: {}",
						clazz.getSimpleName(), store.cache.keys(clazz).size()));
				segmentClasses.forEach(segmentLoader::ensureClass);
			}
			if (pass >= maxPasses) {
				throw Ax.runtimeException("did our max passes and lost");
			}
		}
		segmentLoader.saveSegmentData();
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
		// targetEntityClass may be a subclass of the declaring class
		Class targetEntityClass = declaringClass;
		for (Entry<PropertyDescriptor, JoinTable> entry2 : joinTables
				.entrySet()) {
			ManyToMany m = entry2.getKey().getReadMethod()
					.getAnnotation(ManyToMany.class);
			if (m != null && entry2.getValue() == null
					&& declaringClass.isAssignableFrom(m.targetEntity())
					&& pd.getName().equals(m.mappedBy())) {
				targetEntityClass = m.targetEntity();
				rev = entry2.getKey();
				break;
			}
		}
		DomainStoreJoinHandler joinHandler = null;
		if (rev == null) {
			Type genericReturnType = pd.getReadMethod().getGenericReturnType();
			if (genericReturnType instanceof ParameterizedType) {
				Type genericType = ((ParameterizedType) genericReturnType)
						.getActualTypeArguments()[0];
				if (genericType == targetEntityClass) {
					// self-reference, probably
					rev = pd;
				}
			}
			if (rev == null) {
				joinHandler = Registry.impl(JPAImplementation.class)
						.getDomainStoreJoinHandler(pd);
				if (joinHandler != null) {
				} else {
					throw new RuntimeException("No reverse key for " + pd);
				}
			}
		}
		Connection conn = getConnection();
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
			ConnResults connResults = ConnResults.builder().withLoader(this)
					.withConn(conn).withSqlFilter(sql)
					.withJoinHandler(joinHandler).build();
			for (Object[] row : connResults) {
				Entity src = (Entity) store.cache.get(targetEntityClass,
						(Long) row[0]);
				assert src != null;
				if (joinHandler == null) {
					Entity tgt = (Entity) store.cache.get(
							rev.getReadMethod().getDeclaringClass(),
							(Long) row[1]);
					assert tgt != null;
					laterLookup.add(tgt, pdFwd, src);
					laterLookup.add(src, pdRev, tgt);
				} else {
					joinHandler.injectValue(row, src);
				}
			}
			stmt.close();
			MetricLogging.get().end(joinTableName, store.metricLogger);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			releaseConn(conn);
		}
	}

	/*
	 * sublock is now just used to say "this is a lazy load - so index" - no
	 * actual locking (not needed since the load only applies to this tx)
	 */
	private List<Entity> loadTable(Class clazz, String sqlFilter,
			ClassIdLock sublock, LaterLookup laterLookup) throws Exception {
		List<Entity> loaded = (List) loadTable0(clazz, sqlFilter, sublock,
				laterLookup, (!store.initialising || loadingSegment), false);
		boolean keepDetached = LooseContext
				.is(DomainStore.CONTEXT_KEEP_LOAD_TABLE_DETACHED_FROM_GRAPH);
		if (sublock != null) {
			laterLookup.resolve();
			if (!store.initialising && !keepDetached) {
				for (Entity entity : loaded) {
					store.index(entity, true);
				}
			}
		}
		return loaded;
	}

	private List<HasId> loadTable0(Class clazz, String sqlFilter,
			ClassIdLock sublock, LaterLookup laterLookup,
			boolean ignoreIfExisting, boolean keepDetached) throws Exception {
		Connection conn = getConnection();
		try {
			return loadTable0(conn, clazz, sqlFilter, sublock, laterLookup,
					ignoreIfExisting, keepDetached, true);
		} finally {
			releaseConn(conn);
		}
	}

	private List<HasId> loadTable0(Connection conn, Class clazz,
			String sqlFilter, ClassIdLock sublock, LaterLookup laterLookup,
			boolean ignoreIfExisting, boolean keepDetached,
			boolean ensureModificationChecker) throws Exception {
		keepDetached |= LooseContext
				.is(DomainStore.CONTEXT_KEEP_LOAD_TABLE_DETACHED_FROM_GRAPH);
		boolean populateLazyPropertyValues = LooseContext
				.is(DomainStore.CONTEXT_POPULATE_LAZY_PROPERTY_VALUES);
		List<HasId> loaded;
		ConnResults connResults = ConnResults.builder().withClazz(clazz)
				.withConn(conn)
				.withColumnDescriptors(columnDescriptors.get(clazz))
				.withPopulateLazyPropertyValues(populateLazyPropertyValues)
				.withLoader(this).withSqlFilter(sqlFilter).build();
		List<PdOperator> pds = descriptors.get(clazz);
		loaded = new ArrayList<>();
		PdOperator idOperator = pds.stream().filter(pd -> pd.name.equals("id"))
				.findFirst().get();
		Transaction transaction = Transaction.current();
		boolean transactional = DomainStore.stores().storeFor(clazz) != null;
		ignoreIfExisting &= sublock == null;
		for (Object[] objects : connResults) {
			HasId hasId = (HasId) (transactional
					? transaction.create(clazz, store)
					: clazz.newInstance());
			if (ignoreIfExisting) {
				if (store.transformManager.store.contains(clazz,
						(Long) objects[idOperator.idx])) {
					continue;
				}
			}
			if (sublock != null) {
				loaded.add(hasId);
			}
			for (int i = 0; i < objects.length; i++) {
				PdOperator pdOperator = pds.get(i);
				ColumnDescriptor columnDescriptor = connResults.columnDescriptors
						.get(i);
				Method rm = pdOperator.readMethod;
				if (pdOperator.manyToOne != null
						|| pdOperator.oneToOne != null) {
					Long id = (Long) objects[i];
					if (id != null) {
						if (hasId != null) {
							laterLookup.add(id, pdOperator, hasId);
						}
					}
				} else {
					pdOperator.field.set(hasId, objects[i]);
				}
			}
			if (!keepDetached && hasId instanceof Entity) {
				store.transformManager.store.mapObject((Entity) hasId);
			}
		}
		return loaded;
	}

	private void loadTableSegment(Class clazz, String sqlFilter,
			LaterLookup laterLookup) throws Exception {
		synchronized (clazz) {
			DomainClassDescriptor<?> descriptor = domainDescriptor.perClass
					.get(clazz);
			MetricLogging.get().start(clazz.getSimpleName());
			loadTable(clazz, sqlFilter, null, laterLookup);
			MetricLogging.get().end(clazz.getSimpleName(), store.metricLogger);
		}
	}

	/*
	 * Logger parameter currently unused
	 */
	private List<DomainTransformRequestPersistent> loadTransformRequests0(
			Collection<Long> ids, Logger logger) throws Exception {
		store.logger.info("{} - loading transform request {}", store.name, ids);
		Connection conn = getConnection();
		try {
			CachingMap<Long, DomainTransformRequestPersistent> loadedRequests = new CachingMap<>(
					id -> {
						DomainTransformRequestPersistent request = AlcinaPersistentEntityImpl
								.getNewImplementationInstance(
										DomainTransformRequestPersistent.class);
						request.setId(id);
						return request;
					});
			Class<? extends DomainTransformEvent> transformEventImplClass = domainDescriptor
					.getShadowDomainTransformEventPersistentClass();
			Class<? extends ClassRef> classRefImplClass = domainDescriptor
					.getShadowClassRefClass();
			if (!columnDescriptors.containsKey(transformEventImplClass)) {
				DomainClassDescriptor classDescriptor = new DomainClassDescriptor(
						transformEventImplClass);
				prepareTable(classDescriptor);
			}
			String sqlFilter = Ax.format(
					" domainTransformRequestPersistent_id in %s order by id",
					EntityUtils.longsToIdClause(ids));
			LaterLookup laterLookup = new LaterLookup();
			List<? extends DomainTransformEventPersistent> transforms = null;
			Transaction.ensureDomainPreparingActive();
			transforms = (List) loadTable0(transformEventImplClass, sqlFilter,
					new ClassIdLock(DomainTransformRequestPersistent.class,
							ids.iterator().next()),
					laterLookup, false, true);
			laterLookup.resolve(new CustomResolver() {
				@Override
				public boolean handles(PdOperator pdOperator) {
					switch (pdOperator.name) {
					case "objectClassRef":
					case "valueClassRef":
					case "domainTransformRequestPersistent":
						return true;
					default:
						return false;
					}
				}

				@Override
				public Object resolveCustom(PdOperator pdOperator,
						LaterItem item) {
					switch (pdOperator.name) {
					case "objectClassRef":
					case "valueClassRef":
						long storeDomainClassRefId = item.id;
						ClassRef storeClassRef = store.find(classRefImplClass,
								storeDomainClassRefId);
						ClassRef writableDomainClassRef = ClassRef
								.forName(storeClassRef.getRefClassName());
						return writableDomainClassRef;
					case "domainTransformRequestPersistent":
						return loadedRequests.get(item.id);
					default:
						throw new UnsupportedOperationException();
					}
				}
			});
			transforms.removeIf(event -> event.getObjectClassRef() == null
					|| event.getObjectClassRef().notInVm()
					|| (event.getValueClassRef() != null
							&& event.getValueClassRef().notInVm()));
			// TODO - populate source - see
			// cc.alcina.framework.entity.domaintransform.event.DomainTransformPersistenceQueue.FireEventsThread.publishTransformEvent(Long)
			MultikeyMap<Entity> classIdTransformee = new UnsortedMultikeyMap<>();
			for (DomainTransformEventPersistent transform : transforms) {
				transform.getDomainTransformRequestPersistent().getEvents()
						.add(transform);
				Class<? extends Entity> transformeeClass = transform
						.getObjectClass();
				long id = transform.getObjectId();
				if (transform
						.getTransformType() == TransformType.DELETE_OBJECT) {
					classIdTransformee.remove(transformeeClass, id);
				} else {
					Entity source = classIdTransformee.ensure(() -> {
						Entity instance = Reflections.classLookup()
								.newInstance(transformeeClass);
						instance.setId(id);
						return instance;
					}, transformeeClass, id);
					transform.setSource(source);
				}
			}
			Transaction.current().setMultithreadedWritePermitted(true);
			List<Callable> tasks = new ArrayList<>();
			for (Class clazz : (Set<Class>) (Set) classIdTransformee.keySet()) {
				if (IVersionable.class.isAssignableFrom(clazz)
						&& store.isCached(clazz)) {
					Collection<Entity> iversionables = classIdTransformee
							.asMap(clazz).allValues();
					tasks.add(new IVersionableLoaderTask(conn, clazz,
							iversionables, Transaction.current()));
				}
			}
			invokeAllWithThrow(tasks, iLoaderExecutor);
			return loadedRequests.values().stream()
					.collect(Collectors.toList());
		} finally {
			releaseConn(conn);
		}
	}

	private String longsToIdClause(Collection<Long> ids) {
		return EntityUtils.longsToIdClause(
				ids.stream().sorted().collect(Collectors.toList()));
	}

	private void prepareTable(DomainClassDescriptor classDescriptor)
			throws Exception {
		Class clazz = classDescriptor.clazz;
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
			boolean ignore = classDescriptor.isIgnoreColumn(pd.getName());
			if ((rm.getAnnotation(Transient.class) != null
					&& rm.getAnnotation(DomainStoreDbColumn.class) == null)
					|| rm.getAnnotation(DomainStoreProperty.class) != null) {
				ignore = true;
				DomainStoreProperty propertyAnnotation = rm
						.getAnnotation(DomainStoreProperty.class);
				if (propertyAnnotation != null) {
					Field field = store.getField(clazz, pd.getName());
					field.setAccessible(true);
					domainStorePropertyFields.put(clazz, field, field);
					ignore = propertyAnnotation
							.loadType() == DomainStorePropertyLoadType.TRANSIENT;
				}
			}
			if (ignore) {
				continue;
			}
			if (classDescriptor.ignoreField(pd.getName())) {
				continue;
			}
			OneToMany oneToMany = rm.getAnnotation(OneToMany.class);
			if (oneToMany != null) {
				if (Set.class.isAssignableFrom(pd.getPropertyType())) {
					try {
						Field field = store.getField(clazz, pd.getName());
						field.setAccessible(true);
						if (field != null) {
							ParameterizedType pt = (ParameterizedType) field
									.getGenericType();
							Type implementationType = pt
									.getActualTypeArguments()[0];
							Association association = rm
									.getAnnotation(Association.class);
							if (association != null && association
									.implementationClass() != null) {
								implementationType = association
										.implementationClass();
							}
							manyToOneRev.put(implementationType,
									oneToMany.mappedBy(), pd);
						}
					} catch (Exception e) {
						e.printStackTrace();
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
				joinTableClassDescriptor.put(joinTable, classDescriptor);
				joinTables.put(pd, joinTable);
				continue;
			}
			ManyToOne manyToOne = rm.getAnnotation(ManyToOne.class);
			OneToOne oneToOne = rm.getAnnotation(OneToOne.class);
			DomainStoreDbColumn domainStoreColumn = rm
					.getAnnotation(DomainStoreDbColumn.class);
			if (manyToOne != null || oneToOne != null
					|| domainStoreColumn != null) {
				if (domainStoreColumn != null
						&& domainStoreColumn.customHandler()) {
					// we'll just to conversion in code - targetEntityType
					// irrelevant
				} else {
					Class joinEntityType = getTargetEntityType(rm);
					if (!domainDescriptor.joinPropertyCached(joinEntityType)) {
						System.out.format("  not loading: %s.%s -- %s\n",
								clazz.getSimpleName(), pd.getName(),
								pd.getPropertyType().getSimpleName());
						continue;
					}
					if (oneToOne != null && !oneToOne.mappedBy().isEmpty()) {
						oneToOneRev.put(pd.getPropertyType(),
								oneToOne.mappedBy(), pd);
						continue;
					}
					if (domainStoreColumn != null) {
						domainStoreColumnRev.put(pd.getPropertyType(),
								domainStoreColumn.mappedBy(), pd);
						continue;
					}
				}
				addColumnName(clazz, pd,
						getEntityType(getTargetEntityType(pd.getReadMethod())));
			} else {
				addColumnName(clazz, pd, getEntityType(pd.getPropertyType()));
			}
			mapped.add(ensurePdOperator(pd, clazz));
		}
		boolean addLazyPropertyLoadTask = columnDescriptors.get(clazz).stream()
				.anyMatch(
						cd -> cd.loadType == DomainStorePropertyLoadType.LAZY);
		if (addLazyPropertyLoadTask) {
			domainDescriptor.preProvideTasks
					.add(new LazyPropertyLoadTask<>(clazz, store));
		}
	}

	private void setupInitialJoinTableCalls(List<Callable> calls) {
		for (Entry<PropertyDescriptor, JoinTable> entry : joinTables
				.entrySet()) {
			final Entry<PropertyDescriptor, JoinTable> entryF = entry;
			if (joinTableClassDescriptor.get(entryF.getValue()).lazy) {
				return;
			}
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					loadJoinTable(entryF, warmupLaterLookup());
					return null;
				}
			});
		}
	}

	private void setupInitialLoadTableCalls(List<Callable> calls) {
		for (DomainClassDescriptor descriptor : domainDescriptor.perClass
				.values()) {
			final Class clazz = descriptor.clazz;
			if (!descriptor.lazy) {
				calls.add(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						MetricLogging.get().start(clazz.getSimpleName());
						loadTable(clazz, descriptor.getInitialLoadFilter(),
								null, warmupLaterLookup());
						MetricLogging.get().end(clazz.getSimpleName(),
								store.metricLogger);
						return null;
					}
				});
			}
		}
	}

	protected void closeWarmupConnection(Connection conn) {
		try {
			conn.commit();
			conn.setAutoCommit(true);
			conn.setReadOnly(false);
			conn.setTransactionIsolation(originalTransactionIsolation);
			logger.debug("Closing warmup db connection (post-init) {}", conn);
			conn.close();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected synchronized void releaseWarmupConnection(Connection conn) {
		warmupConnections.add(conn, -1);
	}

	/*
	 * Use the writed method of the domain entity (to force a transactional
	 * version) but don't record the property change. The populated version will
	 * be vacuumed - neatly avoiding any cache invalidation requirements
	 */
	<T extends Entity> void copyLazyPropertyValues(T tableEntity,
			T domainEntity) {
		try {
			TransformManager.get().setIgnorePropertyChanges(true);
			Class<? extends Entity> clazz = tableEntity.entityClass();
			List<ColumnDescriptor> columnDescriptors = this.columnDescriptors
					.get(clazz);
			List<PdOperator> pds = descriptors.get(clazz);
			int idx = 0;
			for (idx = 0; idx < columnDescriptors.size(); idx++) {
				ColumnDescriptor columnDescriptor = columnDescriptors.get(idx);
				PdOperator pd = pds.get(idx);
				if (columnDescriptor.loadType == DomainStorePropertyLoadType.LAZY) {
					try {
						Object tableValue = pd.field.get(tableEntity);
						pd.writeMethod.invoke(domainEntity,
								new Object[] { tableValue });
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}
		} finally {
			TransformManager.get().setIgnorePropertyChanges(false);
		}
	}

	Connection getConnection() {
		if (store.initialising) {
			try {
				return getWarmupConnection();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		try {
			synchronized (postInitConnectionLock) {
				if (postInitConn == null) {
					resetConnection();
				}
			}
			postInitConnectionLock.lock();
			return postInitConn;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	void releaseConn(Connection conn) {
		if (conn == null) {
			return;
		}
		try {
			if (store.initialising) {
				releaseWarmupConnection(conn);
			} else {
				postInitConnectionLock.unlock();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// calling thread already has connection lock
	Connection resetConnection() throws Exception {
		synchronized (postInitConnectionLock) {
			postInitConn = dataSource.getConnection();
			logger.debug("Opened new db connection (post-init) {}",
					postInitConn);
			postInitConn.setAutoCommit(true);
			postInitConn.setReadOnly(true);
		}
		return postInitConn;
	}

	synchronized LaterLookup warmupLaterLookup() {
		LaterLookup result = new LaterLookup();
		warmupLaterLookups.add(result);
		return result;
	}

	public static interface DomainStoreJoinHandler {
		public String getTargetSql();

		public void injectValue(Object[] row, Entity source);
	}

	public class PdOperator {
		ResolveHelper resolveHelper = new ResolveHelper();

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

		public PdOperator(PropertyDescriptor pd, Class clazz, int idx)
				throws Exception {
			this.pd = pd;
			this.clazz = clazz;
			this.idx = idx;
			this.field = store.getField(clazz, pd.getName());
			this.name = pd.getName();
			this.readMethod = pd.getReadMethod();
			this.writeMethod = pd.getWriteMethod();
			this.manyToMany = readMethod.getAnnotation(ManyToMany.class);
			this.manyToOne = readMethod.getAnnotation(ManyToOne.class);
			this.joinTable = readMethod.getAnnotation(JoinTable.class);
			this.oneToMany = readMethod.getAnnotation(OneToMany.class);
			this.oneToOne = readMethod.getAnnotation(OneToOne.class);
			DomainStoreMapping mapping = readMethod
					.getAnnotation(DomainStoreMapping.class);
			this.mappedClass = mapping == null ? null : mapping.mapping();
		}

		public Object read(Entity obj) {
			try {
				return field.get(obj);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		class ResolveHelper {
			PropertyDescriptor domainStorePdRev;

			PropertyDescriptor oneToOnePd;

			PropertyDescriptor targetPd;

			PdOperator targetOperator;

			boolean inJoinTables;

			boolean ensured = false;

			PdOperator oneToOneOperator;

			PdOperator domainStoreRevOperator;

			void ensure(Class<? extends HasId> sourceClass) {
				sourceClass = Mvcc.resolveEntityClass(sourceClass);
				if (!ensured) {
					inJoinTables = joinTables.containsKey(PdOperator.this.pd);
					targetPd = manyToOneRev.get(sourceClass, name);
					if (targetPd != null) {
						targetOperator = ensurePdOperator(targetPd,
								targetPd.getReadMethod().getDeclaringClass());
					}
					oneToOnePd = oneToOneRev.get(sourceClass, name);
					if (oneToOnePd != null) {
						oneToOneOperator = ensurePdOperator(oneToOnePd,
								oneToOnePd.getReadMethod().getDeclaringClass());
					}
					domainStorePdRev = domainStoreColumnRev.get(sourceClass,
							name);
					if (domainStorePdRev != null) {
						domainStoreRevOperator = ensurePdOperator(
								domainStorePdRev, domainStorePdRev
										.getReadMethod().getDeclaringClass());
					}
					ensured = true;
				}
			}

			boolean isCustom(CustomResolver customResolver) {
				return customResolver != null
						&& customResolver.handles(PdOperator.this);
			}

			Object resolveCustom(CustomResolver customResolver,
					LaterItem item) {
				return customResolver.resolveCustom(PdOperator.this, item);
			}
		}
	}

	private class IVersionableLoaderTask implements Callable<Void> {
		private Class<Entity> clazz;

		private Collection<Entity> sources;

		private Connection conn;

		private Transaction transaction;

		public IVersionableLoaderTask(Connection conn, Class<Entity> clazz,
				Collection<Entity> sources, Transaction transaction) {
			this.conn = conn;
			this.clazz = clazz;
			this.sources = sources;
			this.transaction = transaction;
		}

		@Override
		public Void call() {
			try {
				Transaction.join(transaction);
				LaterLookup laterLookup = new LaterLookup();
				String sqlFilter = Ax.format(" id in %s ",
						EntityUtils.hasIdsToIdClause(sources));
				ClassIdLock dummySublock = new ClassIdLock(clazz, 0L);
				List<? extends Entity> persistentSources = (List) loadTable0(
						conn, clazz, sqlFilter, dummySublock, laterLookup,
						false, true, false);
				Map<Long, ? extends Entity> idMap = EntityHelper
						.toIdMap(sources);
				laterLookup.resolve();
				for (Entity persistentSource : persistentSources) {
					Entity transformee = idMap.get(persistentSource.getId());
					if (transformee instanceof HasVersionNumber) {
						((HasVersionNumber) transformee).setVersionNumber(
								((HasVersionNumber) persistentSource)
										.getVersionNumber());
					}
					if (transformee instanceof IVersionable) {
						IVersionable iVersionable = (IVersionable) transformee;
						IVersionable persistent = (IVersionable) persistentSource;
						iVersionable.setCreationDate(SEUtilities
								.toJavaDate(persistent.getCreationDate()));
						iVersionable.setLastModificationDate(
								SEUtilities.toJavaDate((persistent
										.getLastModificationDate())));
						Class<? extends IUser> iUserClass = store.domainDescriptor
								.getIUserClass();
						if (iUserClass == null) {
							return null;
						}
					}
				}
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				throw new WrappedRuntimeException(e);
			} finally {
				Transaction.split();
			}
		}
	}

	private class WarmupTxCallable implements Callable {
		private Callable delegate;

		WarmupTxCallable(Object delegate) {
			this.delegate = (Callable) delegate;
		}

		@Override
		public Object call() throws Exception {
			try {
				Transaction.join(warmupTransaction);
				return delegate.call();
			} finally {
				Transaction.split();
			}
		}
	}

	class BackupLazyLoader implements LazyObjectLoader {
		@Override
		public <T extends Entity> void loadObject(Class<? extends T> c, long id,
				long localId) {
			try {
				DomainClassDescriptor itemDescriptor = domainDescriptor.perClass
						.get(c);
				if (itemDescriptor != null
						&& itemDescriptor.provideNotFullyLoadedOnStartup()) {
					// only one thread should load a given class, for
					// threadsafety reasons
					ClassIdLock lock = LockUtils.obtainClassIdLock(c, 0L);
					logger.debug("Backup lazy load: {} - {}\n",
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

		private boolean typeIdHasId;

		private EnumType enumType = EnumType.ORDINAL;

		private DomainStorePropertyLoadType loadType = DomainStorePropertyLoadType.EAGER;

		public ColumnDescriptor(PropertyDescriptor pd, Class propertyType) {
			this.pd = pd;
			type = propertyType;
			typeIdHasId = HasId.class.isAssignableFrom(type);
			Enumerated enumerated = pd.getReadMethod()
					.getAnnotation(Enumerated.class);
			if (enumerated != null) {
				enumType = enumerated.value();
			}
			DomainStoreProperty domainStoreProperty = pd.getReadMethod()
					.getAnnotation(DomainStoreProperty.class);
			if (domainStoreProperty != null) {
				loadType = domainStoreProperty.loadType();
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

		public String getColumnSql(boolean populateLazyPropertyValues) {
			String columnName = getColumnName();
			if (!populateLazyPropertyValues
					&& loadType == DomainStorePropertyLoadType.LAZY) {
				return Ax.format("null as %s", columnName);
			}
			if (type == Date.class) {
				return String.format(
						"EXTRACT (EPOCH FROM %s::timestamp at time zone 'utc')::float*1000 as %s",
						columnName, columnName);
			} else {
				return columnName;
			}
		}

		public Object getObject(ResultSet rs, int idx) throws Exception {
			if (typeIdHasId || type == Long.class || type == long.class) {
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
				// it seems getLong mostly returns utc timestamp (not locale)
				// now mandating that with the 'at timezone utc' above
				// note these cols are currently pg timestamp without tz
				int persistOffset = store.startupTz.getOffset(utcTime);
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
						store.logger.warn("Invalid enum index : {}:{}",
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
						store.logger.warn("Invalid enum value : {}:{}",
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

	static class ConnResults implements Iterable<Object[]> {
		public static Builder builder() {
			return new Builder();
		}

		ConnResultsIterator itr = new ConnResultsIterator();

		private Connection conn;

		private List<ColumnDescriptor> columnDescriptors;

		Class clazz;

		String sqlFilter;

		ResultSet rs = null;

		List<Object[]> cachedValues;

		ConnResultsReuse rsReuse;

		private boolean joinTable;

		private DomainStoreJoinHandler joinHandler;

		private String tableNameOverride;

		private DomainStoreDescriptor domainDescriptor;

		private DomainStoreLoaderDatabase loader;

		String rsSql;

		private boolean lazyProperties;

		private Statement stmt;

		private ConnResults(Builder builder) {
			this.conn = builder.conn;
			this.clazz = builder.clazz;
			this.sqlFilter = builder.sqlFilter;
			this.tableNameOverride = builder.tableNameOverride;
			this.loader = builder.loader;
			this.domainDescriptor = loader.domainDescriptor;
			this.columnDescriptors = builder.columnDescriptors;
			this.joinHandler = builder.joinHandler;
			this.joinTable = clazz == null;
			this.rsReuse = domainDescriptor.getDomainSegmentLoader() == null
					? new ConnResultsReusePassthrough()
					: (ConnResultsReuse) domainDescriptor
							.getDomainSegmentLoader();
			this.lazyProperties = builder.populateLazyPropertyValues;
		}

		public ResultSet ensureRs() {
			return ensureRs(0);
		}

		@Override
		public Iterator<Object[]> iterator() {
			return rsReuse.getIterator(this, itr);
		}

		private ResultSet ensureRs(int pass) {
			rsSql = null;
			try {
				if (rs == null) {
					stmt = conn.createStatement();
					stmt.setFetchSize(20000);
					if (joinTable) {
						rsSql = sqlFilter;
					} else {
						String template = "select %s from %s";
						List<String> columnNames = new ArrayList<String>();
						for (ColumnDescriptor descr : columnDescriptors) {
							columnNames.add(descr.getColumnSql(lazyProperties));
						}
						Table table = (Table) clazz.getAnnotation(Table.class);
						String tableName = table.name();
						tableName = tableNameOverride != null
								? tableNameOverride
								: tableName;
						rsSql = String.format(template,
								CommonUtils.join(columnNames, ","), tableName);
						if (CommonUtils.isNotNullOrEmpty(sqlFilter)) {
							rsSql += String.format(" where %s", sqlFilter);
						}
					}
					loader.store.sqlLogger.debug(rsSql);
					rs = stmt.executeQuery(rsSql);
				}
				return rs;
			} catch (Exception e) {
				if (pass < 2 && !loader.store.initialising
						&& loader.connectionsReopened.get() < 20) {
					try {
						// don't close the last one, invalid
						conn = loader.resetConnection();
						Ax.out("{} - got new connection - %s",
								loader.store.name, conn);
					} catch (Exception e2) {
						throw new WrappedRuntimeException(e2);
					}
					System.out.println("domainstore-db-warning");
					e.printStackTrace();
					return ensureRs(pass + 1);
				}
				Ax.out("rs sql:\n\t%s", rsSql);
				throw new WrappedRuntimeException(e);
			}
		}

		public static final class Builder {
			private DomainStoreJoinHandler joinHandler;

			private List<ColumnDescriptor> columnDescriptors;

			private Connection conn;

			private Class clazz;

			private String sqlFilter;

			private String tableNameOverride;

			private DomainStoreLoaderDatabase loader;

			private boolean populateLazyPropertyValues;

			private Builder() {
			}

			public ConnResults build() {
				return new ConnResults(this);
			}

			public Builder withClazz(Class clazz) {
				this.clazz = clazz;
				return this;
			}

			public Builder withColumnDescriptors(
					List<ColumnDescriptor> columnDescriptors) {
				this.columnDescriptors = columnDescriptors;
				return this;
			}

			public Builder withConn(Connection conn) {
				this.conn = conn;
				return this;
			}

			public Builder withJoinHandler(DomainStoreJoinHandler joinHandler) {
				this.joinHandler = joinHandler;
				return this;
			}

			public Builder withLoader(DomainStoreLoaderDatabase loader) {
				this.loader = loader;
				return this;
			}

			public Builder withPopulateLazyPropertyValues(
					boolean populateLazyPropertyValues) {
				this.populateLazyPropertyValues = populateLazyPropertyValues;
				return this;
			}

			public Builder withSqlFilter(String sqlFilter) {
				this.sqlFilter = sqlFilter;
				return this;
			}

			public Builder withTableNameOverride(String tableNameOverride) {
				this.tableNameOverride = tableNameOverride;
				return this;
			}
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
							if (joinTable) {
								cached = new Object[2];
								cached[0] = rs.getLong(1);
								if (joinHandler != null) {
									// currently only one implementation (that
									// expects a String)
									cached[1] = rs.getString(2);
								} else {
									cached[1] = rs.getLong(2);
								}
							} else {
								cached = new Object[columnDescriptors.size()];
								for (int idx = 1; idx <= columnDescriptors
										.size(); idx++) {
									ColumnDescriptor descriptor = columnDescriptors
											.get(idx - 1);
									Object value = descriptor.getObject(rs,
											idx);
									cached[idx - 1] = value;
								}
							}
							ConnResults.this.rsReuse.onNext(ConnResults.this,
									cached);
						} else {
							finished = true;
							rs.close();
							stmt.close();
						}
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}
		}
	}

	interface ConnResultsReuse {
		default Iterator<Object[]> getIterator(ConnResults connResults,
				ConnResultsIterator itr) {
			return itr;
		}

		default void onNext(ConnResults connResults, Object[] cached) {
		}
	}

	static class ConnResultsReusePassthrough implements ConnResultsReuse {
	}

	interface CustomResolver {
		boolean handles(PdOperator pdOperator);

		Object resolveCustom(PdOperator pdOperator, LaterItem item);
	}

	class LaterLookup {
		List<LaterItem> list = new LaterLookupList();

		private CustomResolver customResolver;

		private DomainSegmentLoader segmentLoader;

		void add(HasId target, PdOperator pd, HasId source) {
			list.add(new LaterItem(target, pd, source));
		}

		void add(Long id, PdOperator pd, HasId hasId) {
			list.add(new LaterItem(id, pd, hasId));
		}

		void doResolve() {
			try {
				new ResolveRefsTask(list).call();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		void resolve() {
			doResolve();
		}

		void resolve(CustomResolver customResolver) {
			this.customResolver = customResolver;
			doResolve();
		}

		void resolve(DomainSegmentLoader segmentLoader) {
			this.segmentLoader = segmentLoader;
			doResolve();
		}

		private final class ResolveRefsTask implements Callable<Void> {
			private List<LaterItem> items;

			int missingWarningCount = 0;

			private ResolveRefsTask(List<LaterItem> items) {
				this.items = items;
			}

			@Override
			/*
			 * multithread Problem here is that set() methods need to be synced
			 * per class (really, pd) ..so run linear
			 */
			public Void call() throws Exception {
				boolean keepDetached = LooseContext.is(
						DomainStore.CONTEXT_KEEP_LOAD_TABLE_DETACHED_FROM_GRAPH);
				if (LooseContext.is(
						DomainStore.CONTEXT_DO_NOT_RESOLVE_LOAD_TABLE_REFS)) {
					this.items.clear();
					return null;
				}
				for (LaterItem item : this.items) {
					try {
						PdOperator pdOperator = item.pdOperator;
						pdOperator.resolveHelper.ensure(item.source.getClass());
						long id = item.id;
						if (pdOperator.resolveHelper.isCustom(customResolver)) {
							pdOperator.field.set(item.source,
									pdOperator.resolveHelper.resolveCustom(
											customResolver, item));
						} else if (pdOperator.resolveHelper.inJoinTables) {
							if (keepDetached) {
								throw new RuntimeException(
										"Cannot keep join tables detached");
							}
							Set set = (Set) pdOperator.field.get(item.source);
							if (set == null) {
								set = new LiSet();
								pdOperator.field.set(item.source, set);
							}
							set.add(item.target);
						} else if (customResolver != null) {
							// ignore, customResolver exists and not custom
							// resolvable
						} else {
							Class type = propertyDescriptorFetchTypes
									.get(pdOperator.pd);
							Object target = store.cache.get(type, id);
							if (target == null) {
								if (segmentLoader == null) {
									if (missingWarningCount++ < 5) {
										store.logger.warn(
												"later-lookup -- missing target: {}, {} for  {}.{} #{}",
												type, id,
												item.source.getClass(),
												pdOperator.name,
												item.source.getId());
									}
								} else {
									segmentLoader.notifyLater(item, type, id);
								}
							}
							pdOperator.field.set(item.source, target);
							if (keepDetached) {
								continue;
							}
							PdOperator targetOperator = pdOperator.resolveHelper.targetOperator;
							if (targetOperator != null && target != null) {
								Set set = (Set) targetOperator.field
										.get(target);
								if (set == null) {
									set = new LiSet();
									targetOperator.field.set(target, set);
								}
								set.add(item.source);
							}
							targetOperator = pdOperator.resolveHelper.oneToOneOperator;
							if (targetOperator != null && target != null) {
								targetOperator.field.set(target, item.source);
							}
							targetOperator = pdOperator.resolveHelper.domainStoreRevOperator;
							if (targetOperator != null && target != null) {
								targetOperator.field.set(target, item.source);
							}
						}
					} catch (Exception e) {
						if (!store.initialising) {
							Ax.sysLogHigh(
									"Issue with later lookup - gotta check");
							/*
							 * This can also be caused by very lazy loads with
							 * multiple backing stores*
							 */
						}
						// possibly a delta during warmup::
						System.out.println(item);
						e.printStackTrace();
					}
				}
				if (store.initialising) {
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

			HasId source;

			HasId target;

			LaterItem() {
			}

			LaterItem(HasId target, PdOperator pd, HasId source) {
				this.target = target;
				this.pdOperator = pd;
				this.source = source;
			}

			LaterItem(long id, PdOperator pd, HasId source) {
				this.id = id;
				this.pdOperator = pd;
				this.source = source;
			}
		}

		class LaterLookupList extends AbstractList<LaterItem> {
			LaterItem view = new LaterItem();

			LongArrayList ids = new LongArrayList();

			List<PdOperator> pdOperators = new ArrayList<>();

			List<HasId> sources = new ArrayList<>();

			List<HasId> targets = new ArrayList<>();

			@Override
			public void add(int index, LaterItem element) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean add(LaterItem e) {
				add(e.id, e.pdOperator, e.source, e.target);
				return true;
			}

			@Override
			public void clear() {
				ids.clear();
				pdOperators.clear();
				sources.clear();
				targets.clear();
			}

			@Override
			public LaterItem get(int index) {
				view.id = ids.getLong(index);
				view.pdOperator = pdOperators.get(index);
				view.source = sources.get(index);
				view.target = targets.get(index);
				return view;
			}

			@Override
			public Iterator<LaterItem> iterator() {
				return new Itr();
			}

			@Override
			public int size() {
				return ids.size();
			}

			private void add(long id, PdOperator pdOperator, HasId source,
					HasId target) {
				ids.add(id);
				pdOperators.add(pdOperator);
				sources.add(source);
				targets.add(target);
			}

			void add(HasId target, PdOperator pd, HasId source) {
				add(0L, pd, source, target);
			}

			void add(long id, PdOperator pd, HasId source) {
				add(id, pd, source, null);
			}

			private class Itr implements Iterator<LaterItem> {
				/**
				 * Index of element to be returned by subsequent call to next.
				 */
				int cursor = 0;

				/**
				 * The modCount value that the iterator believes that the
				 * backing List should have. If this expectation is violated,
				 * the iterator has detected concurrent modification.
				 */
				int expectedModCount = modCount;

				@Override
				public boolean hasNext() {
					return cursor != size();
				}

				@Override
				public LaterItem next() {
					checkForComodification();
					try {
						int index = cursor;
						get(index);
						cursor = index + 1;
						return view;
					} catch (IndexOutOfBoundsException e) {
						checkForComodification();
						throw new NoSuchElementException();
					}
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				final void checkForComodification() {
					if (modCount != expectedModCount)
						throw new ConcurrentModificationException();
				}
			}
		}
	}
}
