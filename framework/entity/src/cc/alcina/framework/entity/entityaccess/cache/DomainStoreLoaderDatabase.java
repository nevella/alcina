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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.BaseProjection;
import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.domain.DomainDescriptor;
import cc.alcina.framework.common.client.domain.DomainDescriptor.DomainStoreTask;
import cc.alcina.framework.common.client.domain.DomainLookup;
import cc.alcina.framework.common.client.domain.DomainProjection;
import cc.alcina.framework.common.client.domain.DomainStoreLookupDescriptor;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.entityaccess.cache.DomainSegmentLoader.DomainSegmentLoaderProperty;
import cc.alcina.framework.entity.entityaccess.cache.DomainSegmentLoader.DomainSegmentPropertyType;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.ConnResults.ConnResultsIterator;
import cc.alcina.framework.entity.projection.EntityUtils;

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

	private DataSource dataSource;

	private ThreadPoolExecutor warmupExecutor;

	private volatile Connection postInitConn;

	private AtomicInteger connectionsReopened = new AtomicInteger();

	private int originalTransactionIsolation;

	private UnsortedMultikeyMap<Field> domainStoreTransientFields = new UnsortedMultikeyMap<Field>(
			2);

	// only access via synchronized code
	CountingMap<Connection> warmupConnections = new CountingMap<>();

	MultikeyMap<PdOperator> operatorsByClass = new UnsortedMultikeyMap<>(2);

	private ReentrantLock postInitConnectionLock = new ReentrantLock(true);

	private BackupLazyLoader backupLazyLoader = new BackupLazyLoader();

	private List<LaterLookup> warmupLaterLookups = new ArrayList<>();

	private DomainDescriptor domainDescriptor;

	public DomainStoreLoaderDatabase(DomainStore store, DataSource dataSource,
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

	public Iterable<Object[]> getData(Connection conn, Class clazz,
			String sqlFilter) throws SQLException {
		ConnResults result = new ConnResults(conn, clazz,
				columnDescriptors.get(clazz), sqlFilter);
		return result;
	}

	@Override
	public LazyObjectLoader getLazyObjectLoader() {
		return backupLazyLoader;
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

	public <T extends HasIdAndLocalId> List<T> loadTable(Class clazz,
			String sqlFilter, ClassIdLock sublock) throws Exception {
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

	public void resolveRefs(LaterLookup laterLookup) throws Exception {
		laterLookup.resolve();
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
		createWarmupConnections();
		MetricLogging.get().start("domainStore-all");
		// get non-many-many obj
		store.threads.lock(true);
		MetricLogging.get().start("tables");
		for (DomainClassDescriptor descriptor : domainDescriptor.perClass
				.values()) {
			Class clazz = descriptor.clazz;
			prepareTable(descriptor);
			if (descriptor instanceof PropertyStoreItemDescriptor) {
				store.transformManager.addPropertyStore(descriptor);
			}
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
			resolveRefs(ll);
		}
		MetricLogging.get().end("xrefs");
		warmupLaterLookups.clear();
		// lazy tables, load a segment (for large db dev work)
		if (domainDescriptor.getDomainSegmentLoader() != null) {
			MetricLogging.get().start("domain-segment");
			loadDomainSegment();
			MetricLogging.get().end("domain-segment");
		}
		store.threads.unlock(true);
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
		invokeAllWithThrow(calls);
		MetricLogging.get().end("postLoad");
		MetricLogging.get().start("lookups");
		for (final DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
				.values()) {
			for (DomainProjection projection : descriptor.projections) {
				if (projection instanceof DomainLookup) {
					((DomainLookup) projection)
							.setModificationChecker(store.modificationChecker);
				}
			}
		}
		for (final DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
				.values()) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					for (DomainStoreLookupDescriptor lookupDescriptor : descriptor.lookupDescriptors) {
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
		// deliberately init projections after lookups
		for (final DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
				.values()) {
			for (DomainProjection projection : descriptor.projections) {
				if (projection instanceof BaseProjectionHasEquivalenceHash) {
					store.cachingProjections
							.getAndEnsure(projection.getListenedClass());
				}
				if (projection instanceof BaseProjection) {
					((BaseProjection) projection)
							.setModificationChecker(store.modificationChecker);
				}
			}
		}
		for (final DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
				.values()) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					for (DomainProjection projection : descriptor.projections) {
						if (projection.isEnabled()) {
							store.addValues(projection);
						}
						if (projection instanceof BaseProjectionHasEquivalenceHash) {
							store.cachingProjections.add(
									projection.getListenedClass(), projection);
						}
					}
					return null;
				}
			});
		}
		invokeAllWithThrow(calls);
		MetricLogging.get().end("projections");
		store.setCheckModificationWriteLock(true);
		store.initialising = false;
		if (ResourceUtilities.getBoolean(DomainStore.class, "dumpLocks")) {
			store.threads.dumpLocks = true;
		}
		// don't close, but indicate that everything write-y from now shd be
		// single-threaded
		warmupConnections.keySet().forEach(conn -> closeWarmupConnection(conn));
		warmupExecutor = null;
		MetricLogging.get().end("domainStore-all");
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

	private Connection getConnection() {
		if (store.initialising) {
			try {
				return getWarmupConnection();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		try {
			if (postInitConn == null) {
				synchronized (postInitConnectionLock) {
					if (postInitConn == null) {
						postInitConn = dataSource.getConnection();
						logger.debug("Opened new db connection (post-init) {}",
								postInitConn);
						postInitConn.setAutoCommit(true);
						postInitConn.setReadOnly(true);
					}
				}
			}
			postInitConnectionLock.lock();
			return postInitConn;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
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

	private void loadDomainSegment() throws Exception {
		List<Callable> calls = new ArrayList<Callable>();
		DomainSegmentLoader segmentLoader = (DomainSegmentLoader) domainDescriptor
				.getDomainSegmentLoader();
		segmentLoader.initialise();
		int maxPasses = 30;
		int pass = 0;
		Set<Class> segmentClasses = new LinkedHashSet<>();
		long start = System.currentTimeMillis();
		int lastTotal = 0;
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
					Class clazz = entry.getKey();
					ids = segmentLoader.filterForQueried(clazz, "id", ids);
					loadTableOrStoreSegment(clazz,
							Ax.format(" id in %s", longsToIdClause(ids)),
							laterLookup);
					entry.getValue().clear();
					return null;
				});
			}
			invokeAllWithThrow(calls);
			// resolve what we can (last pass)
			LaterLookup lastPassLookup = new LaterLookup();
			lastPassLookup.list.addAll(segmentLoader.toResolve);
			segmentLoader.toResolve.clear();
			laterLookups.add(lastPassLookup);
			lastPassLookup.resolve(segmentLoader);
			laterLookups.forEach(ll -> ll.resolve(segmentLoader));
			laterLookups.clear();
			if (segmentLoader.pendingCount() == 0) {
				for (DomainSegmentLoaderProperty property : segmentLoader.properties) {
					if (property.type == DomainSegmentPropertyType.TABLE_REF) {
						Collection<Long> ids = store.cache
								.keys(property.target);
						ids = segmentLoader.filterForQueried(property.source,
								property.propertyName, ids);
						String sqlFilter = Ax.format(" %s in %s",
								property.propertyName, longsToIdClause(ids));
						segmentClasses.add(property.source);
						calls.add(() -> {
							LaterLookup laterLookup = new LaterLookup();
							laterLookups.add(laterLookup);
							loadTableOrStoreSegment(property.source, sqlFilter,
									laterLookup);
							return null;
						});
					} else if (property.type == DomainSegmentPropertyType.STORE_REF) {
						Set<Long> keys = store.cache.keys(property.source);
						Collection<Long> ids = store.cache.fieldValues(
								property.source, property.propertyName);
						ids = segmentLoader.filterForQueried(property.target,
								"id", ids);
						String sqlFilter = Ax.format(" id in %s",
								longsToIdClause(ids));
						segmentClasses.add(property.target);
						calls.add(() -> {
							LaterLookup laterLookup = new LaterLookup();
							laterLookups.add(laterLookup);
							loadTableOrStoreSegment(property.target, sqlFilter,
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
			store.logger.info("Load domain segment - pass {} - size {} - {} ms",
					pass, size, System.currentTimeMillis() - start);
			segmentClasses.forEach(clazz -> store.logger.debug("{}: {}",
					clazz.getSimpleName(), store.cache.keys(clazz).size()));
			segmentClasses.forEach(segmentLoader::ensureClass);
		}
		if (pass >= maxPasses) {
			throw Ax.runtimeException("did our max passes and lost");
		} else {
			segmentLoader.saveSegmentData();
		}
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
			while (rs.next()) {
				HasIdAndLocalId src = (HasIdAndLocalId) store.cache
						.get(targetEntityClass, rs.getLong(1));
				assert src != null;
				if (joinHandler == null) {
					HasIdAndLocalId tgt = (HasIdAndLocalId) store.cache.get(
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
			MetricLogging.get().end(joinTableName, store.metricLogger);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			releaseConn(conn);
		}
	}

	private void loadPropertyStore(Class clazz, String sqlFilter,
			PropertyStoreItemDescriptor propertyStoreItemDescriptor)
			throws SQLException {
		Connection conn = getConnection();
		try {
			ConnResults connResults = new ConnResults(conn, clazz,
					columnDescriptors.get(clazz), sqlFilter);
			List<PdOperator> pds = descriptors.get(clazz);
			propertyStoreItemDescriptor.init(store.cache, pds);
			String simpleName = clazz.getSimpleName();
			int count = propertyStoreItemDescriptor.getRoughCount();
			SystemoutCounter ctr = new SystemoutCounter(20000, 10, count, true);
			Iterator<Object[]> iterator = connResults.iterator();
			while (iterator.hasNext()) {
				propertyStoreItemDescriptor.addRow(iterator.next());
				ctr.tick(simpleName);
			}
		} finally {
			releaseConn(conn);
		}
	}

	private List<HasIdAndLocalId> loadTable(Class clazz, String sqlFilter,
			ClassIdLock sublock, LaterLookup laterLookup) throws Exception {
		if (sublock != null) {
			store.threads.sublock(sublock, true);
		}
		try {
			List<HasIdAndLocalId> loaded = loadTable0(clazz, sqlFilter, sublock,
					laterLookup, !store.initialising);
			boolean keepDetached = LooseContext.is(
					DomainStore.CONTEXT_KEEP_LOAD_TABLE_DETACHED_FROM_GRAPH);
			if (sublock != null) {
				resolveRefs(laterLookup);
				if (!store.initialising && !keepDetached) {
					for (HasIdAndLocalId hili : loaded) {
						store.index(hili, true);
					}
				}
			}
			return loaded;
		} finally {
			if (sublock != null) {
				store.threads.sublock(sublock, false);
			}
		}
	}

	private List<HasIdAndLocalId> loadTable0(Class clazz, String sqlFilter,
			ClassIdLock sublock, LaterLookup laterLookup,
			boolean ignoreIfExisting) throws Exception {
		Connection conn = getConnection();
		List<HasIdAndLocalId> loaded;
		try {
			Iterable<Object[]> results = getData(conn, clazz, sqlFilter);
			List<PdOperator> pds = descriptors.get(clazz);
			loaded = new ArrayList<HasIdAndLocalId>();
			PdOperator idOperator = pds.stream()
					.filter(pd -> pd.name.equals("id")).findFirst().get();
			boolean keepDetached = LooseContext.is(
					DomainStore.CONTEXT_KEEP_LOAD_TABLE_DETACHED_FROM_GRAPH);
			for (Object[] objects : results) {
				HasIdAndLocalId hili = (HasIdAndLocalId) clazz.newInstance();
				if (ignoreIfExisting) {
					if (store.transformManager.store.contains(clazz,
							(Long) objects[idOperator.idx])) {
						continue;
					}
				}
				if (sublock != null) {
					loaded.add(hili);
				}
				store.ensureModificationChecker(hili);
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
				if (!keepDetached) {
					store.transformManager.store.mapObject(hili);
				}
			}
		} finally {
			releaseConn(conn);
		}
		return loaded;
	}

	private void loadTableOrStoreSegment(Class clazz, String sqlFilter,
			LaterLookup laterLookup) throws Exception {
		synchronized (clazz) {
			DomainClassDescriptor<?> descriptor = domainDescriptor.perClass
					.get(clazz);
			MetricLogging.get().start(clazz.getSimpleName());
			if (descriptor instanceof PropertyStoreItemDescriptor) {
				PropertyStoreItemDescriptor propertyStoreItemDescriptor = (PropertyStoreItemDescriptor) descriptor;
				loadPropertyStore(clazz, sqlFilter,
						propertyStoreItemDescriptor);
			} else {
				loadTable(clazz, sqlFilter, null, laterLookup);
			}
			MetricLogging.get().end(clazz.getSimpleName(), store.metricLogger);
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
			if ((rm.getAnnotation(Transient.class) != null
					&& rm.getAnnotation(DomainStoreDbColumn.class) == null)
					|| rm.getAnnotation(DomainStoreTransient.class) != null) {
				DomainStoreTransient transientAnn = rm
						.getAnnotation(DomainStoreTransient.class);
				if (transientAnn != null) {
					Field field = store.getField(clazz, pd.getName());
					field.setAccessible(true);
					domainStoreTransientFields.put(clazz, field, field);
				}
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
				joinTables.put(pd, joinTable);
				continue;
			}
			ManyToOne manyToOne = rm.getAnnotation(ManyToOne.class);
			OneToOne oneToOne = rm.getAnnotation(OneToOne.class);
			DomainStoreDbColumn domainStoreColumn = rm
					.getAnnotation(DomainStoreDbColumn.class);
			if (manyToOne != null || oneToOne != null
					|| domainStoreColumn != null) {
				Class joinEntityType = getTargetEntityType(rm);
				if (!domainDescriptor.joinPropertyCached(joinEntityType)) {
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
				if (domainStoreColumn != null) {
					domainStoreColumnRev.put(pd.getPropertyType(),
							domainStoreColumn.mappedBy(), pd);
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
			if (store.initialising) {
				releaseWarmupConnection(conn);
			} else {
				postInitConnectionLock.unlock();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setupInitialJoinTableCalls(List<Callable> calls) {
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
						if (descriptor instanceof PropertyStoreItemDescriptor) {
							PropertyStoreItemDescriptor propertyStoreItemDescriptor = (PropertyStoreItemDescriptor) descriptor;
							loadPropertyStore(clazz, null,
									propertyStoreItemDescriptor);
						} else {
							loadTable(clazz, "", null, warmupLaterLookup());
						}
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

	synchronized LaterLookup warmupLaterLookup() {
		LaterLookup result = new LaterLookup();
		warmupLaterLookups.add(result);
		return result;
	}

	public static interface DomainStoreJoinHandler {
		public String getTargetSql();

		public void injectValue(ResultSet rs, HasIdAndLocalId source);
	}

	public class LaterLookup {
		List<LaterItem> list = new ArrayList<>();

		void add(HasIdAndLocalId target, PdOperator pd,
				HasIdAndLocalId source) {
			list.add(new LaterItem(target, pd, source));
		}

		void add(long id, PdOperator pd, HasIdAndLocalId source) {
			list.add(new LaterItem(id, pd, source));
		}

		void resolve() {
			resolve(null);
		}

		void resolve(DomainSegmentLoader segmentLoader) {
			try {
				new ResolveRefsTask(list, segmentLoader).call();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private final class ResolveRefsTask implements Callable<Void> {
			private List<LaterItem> items;

			private DomainSegmentLoader segmentLoader;

			private ResolveRefsTask(List<LaterItem> items,
					DomainSegmentLoader segmentLoader) {
				this.items = items;
				this.segmentLoader = segmentLoader;
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
						Method rm = pdOperator.readMethod;
						long id = item.id;
						if (pdOperator.resolveHelper.inJoinTables) {
							if (keepDetached) {
								throw new RuntimeException(
										"Cannot keep join tables detached");
							}
							Set set = (Set) pdOperator.readMethod
									.invoke(item.source, new Object[0]);
							if (set == null) {
								set = new LinkedHashSet();
								pdOperator.writeMethod.invoke(item.source,
										new Object[] { set });
							}
							set.add(item.target);
						} else {
							Class type = propertyDescriptorFetchTypes
									.get(pdOperator.pd);
							Object target = store.cache.get(type, id);
							if (target == null) {
								if (segmentLoader == null) {
									store.logger.warn(
											"later-lookup -- missing target: {}, {} for  {}.{} #{}",
											type, id, item.source.getClass(),
											pdOperator.name,
											item.source.getId());
								} else {
									segmentLoader.notifyLater(item, type, id);
								}
							}
							pdOperator.writeMethod.invoke(item.source, target);
							if (keepDetached) {
								continue;
							}
							PropertyDescriptor targetPd = pdOperator.resolveHelper.targetPd;
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
							targetPd = pdOperator.resolveHelper.oneToOnePd;
							if (targetPd != null && target != null) {
								targetPd.getWriteMethod().invoke(target,
										new Object[] { item.source });
							}
							targetPd = pdOperator.resolveHelper.domainStorePdRev;
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

		public Object read(HasIdAndLocalId obj) {
			try {
				return field.get(obj);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		class ResolveHelper {
			public PropertyDescriptor domainStorePdRev;

			public PropertyDescriptor oneToOnePd;

			public PropertyDescriptor targetPd;

			public boolean inJoinTables;

			boolean ensured = false;

			public void ensure(Class<? extends HasIdAndLocalId> sourceClass) {
				if (!ensured) {
					inJoinTables = joinTables.containsKey(PdOperator.this.pd);
					targetPd = manyToOneRev.get(sourceClass, name);
					oneToOnePd = oneToOneRev.get(sourceClass, name);
					domainStorePdRev = domainStoreColumnRev.get(sourceClass,
							name);
					ensured = true;
				}
			}
		}
	}

	class BackupLazyLoader implements LazyObjectLoader {
		@Override
		public <T extends HasIdAndLocalId> void loadObject(Class<? extends T> c,
				long id, long localId) {
			try {
				DomainClassDescriptor itemDescriptor = domainDescriptor.perClass
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
						"EXTRACT (EPOCH FROM %s::timestamp at time zone 'utc')::float*1000 as %s",
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

	class ConnResults implements Iterable<Object[]> {
		ConnResultsIterator itr = new ConnResultsIterator();

		private Connection conn;

		private List<ColumnDescriptor> columnDescriptors;

		Class clazz;

		String sqlFilter;

		ResultSet rs = null;

		List<Object[]> cachedValues;

		ConnResultsReuse rsReuse = domainDescriptor
				.getDomainSegmentLoader() == null
						? new ConnResultsReusePassthrough()
						: (ConnResultsReuse) domainDescriptor
								.getDomainSegmentLoader();

		public ConnResults(Connection conn, Class clazz,
				List<ColumnDescriptor> columnDescriptors, String sqlFilter) {
			this.conn = conn;
			this.clazz = clazz;
			this.columnDescriptors = columnDescriptors;
			this.sqlFilter = sqlFilter;
		}

		public ResultSet ensureRs() {
			return ensureRs(0);
		}

		@Override
		public Iterator<Object[]> iterator() {
			return rsReuse.getIterator(this, itr);
		}

		private ResultSet ensureRs(int pass) {
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
					store.sqlLogger.debug(sql);
					rs = stmt.executeQuery(sql);
				}
				return rs;
			} catch (Exception e) {
				if (pass < 2 && !store.initialising
						&& connectionsReopened.get() < 20) {
					try {
						// don't close the last one, invalid
						conn = dataSource.getConnection();
					} catch (Exception e2) {
						throw new WrappedRuntimeException(e2);
					}
					System.out.println("domainstore-db-warning");
					e.printStackTrace();
					return ensureRs(pass + 1);
				}
				throw new WrappedRuntimeException(e);
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
							cached = new Object[columnDescriptors.size()];
							for (int idx = 1; idx <= columnDescriptors
									.size(); idx++) {
								ColumnDescriptor descriptor = columnDescriptors
										.get(idx - 1);
								cached[idx - 1] = descriptor.getObject(rs, idx);
							}
							ConnResults.this.rsReuse.onNext(ConnResults.this,
									cached);
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
}
