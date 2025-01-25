package cc.alcina.framework.entity.persistence.domain;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
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

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.DomainClassDescriptor;
import cc.alcina.framework.common.client.domain.DomainDescriptor.DomainStoreTask;
import cc.alcina.framework.common.client.domain.DomainProjection;
import cc.alcina.framework.common.client.domain.DomainStoreLookupDescriptor;
import cc.alcina.framework.common.client.domain.DomainStoreProperty;
import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyLoadOracle;
import cc.alcina.framework.common.client.domain.DomainStoreProperty.DomainStorePropertyLoadType;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformCollation.EntityCollation;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LazyObjectLoader;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.JPAImplementation;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;
import cc.alcina.framework.entity.persistence.domain.DomainSegmentDbLoader.DomainSegmentLoaderPhase;
import cc.alcina.framework.entity.persistence.domain.DomainSegmentDbLoader.DomainSegmentLoaderProperty;
import cc.alcina.framework.entity.persistence.domain.DomainSegmentDbLoader.DomainSegmentPropertyType;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.ConnResults.ConnResultsIterator;
import cc.alcina.framework.entity.persistence.domain.DomainStoreLoaderDatabase.EntityRefs.Ref;
import cc.alcina.framework.entity.persistence.domain.segment.DomainSegmentLoader;
import cc.alcina.framework.entity.persistence.mvcc.Mvcc;
import cc.alcina.framework.entity.persistence.mvcc.MvccObject;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.stat.StatCategory_DomainStore;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;
import cc.alcina.framework.entity.util.AnnotationUtils;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.entity.util.SqlUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;

/*
 */
public class DomainStoreLoaderDatabase implements DomainStoreLoader {
	static final transient String CONTEXT_ALLOW_ALL_LAZY_LOAD = DomainStoreLoaderDatabase.class
			.getName() + ".CONTEXT_ALLOW_ALL_LAZY_LOAD";

	Logger logger = LoggerFactory.getLogger(getClass());

	private Map<PropertyDescriptor, JoinTable> joinTables;

	private Map<Class, List<PdOperator>> descriptors;

	// class,pName
	private UnsortedMultikeyMap<PropertyDescriptor> manyToOneRev;

	private UnsortedMultikeyMap<PropertyDescriptor> oneToOneRev;

	private UnsortedMultikeyMap<PropertyDescriptor> domainStoreColumnRev;

	private Multimap<Class, List<ColumnDescriptor>> columnDescriptors;

	private Map<PdOperator, Class> propertyDescriptorFetchTypes = new LinkedHashMap<PdOperator, Class>();

	private DomainStore store;

	RetargetableDataSource dataSource;

	private ThreadPoolExecutor warmupExecutor;

	private AtomicInteger connectionsReopened = new AtomicInteger();

	private UnsortedMultikeyMap<Field> domainStorePropertyFields = new UnsortedMultikeyMap<Field>(
			2);

	//
	MultikeyMap<PdOperator> operatorsByClass = new UnsortedMultikeyMap<>(2);

	private BackupLazyLoader backupLazyLoader = new BackupLazyLoader();

	private List<EntityRefs> warmupEntityRefs = new ArrayList<>();

	DomainStoreDescriptor domainDescriptor;

	private Map<JoinTable, DomainClassDescriptor> joinTableClassDescriptor = new LinkedHashMap<>();

	private Object loadTransformRequestLock = new Object();

	DomainStoreTransformSequencer transformSequencer;

	ThreadPoolExecutor iLoaderExecutor = (ThreadPoolExecutor) Executors
			.newFixedThreadPool(8,
					new NamedThreadFactory("domainStore-iLoader"));

	private Transaction warmupTransaction;

	private ConnectionPool connectionPool;

	private Interns interns = new Interns();

	public DomainStoreLoaderDatabase(DomainStore store,
			RetargetableDataSource dataSource,
			ThreadPoolExecutor warmupExecutor) {
		this.store = store;
		this.dataSource = dataSource;
		connectionPool = new ConnectionPool(dataSource);
		this.warmupExecutor = warmupExecutor;
		transformSequencer = new DomainStoreTransformSequencer(this);
		store.getPersistenceEvents().getQueue()
				.setSequencer(transformSequencer);
	}

	private void addColumnName(Class clazz, PdOperator pdOperator,
			Class propertyType) {
		columnDescriptors.add(clazz,
				new ColumnDescriptor(clazz, pdOperator.pd, propertyType));
		propertyDescriptorFetchTypes.put(pdOperator, propertyType);
	}

	@Override
	public void appShutdown() {
		connectionPool.drain();
	}

	@Override
	public boolean checkTransformRequestExists(long id) {
		Class<? extends DomainTransformEvent> transformEventImplClass = domainDescriptor
				.getShadowDomainTransformEventPersistentClass();
		String sql = Ax.format(
				"select id from %s where  domainTransformRequestPersistent_id = %s limit 1;",
				transformEventImplClass.getAnnotation(Table.class).name(), id);
		Connection connection = getConnection();
		try (Statement statement = connection.createStatement()) {
			ResultSet rs = statement.executeQuery(sql);
			return rs.next();
		} catch (SQLException e) {
			throw new WrappedRuntimeException(e);
		} finally {
			releaseConn(connection);
		}
	}

	@Override
	public void close() {
		connectionPool.drain();
	}

	String createDateClause(String columnName) {
		return String.format(
				"EXTRACT (EPOCH FROM %s::timestamp at time zone 'utc')::float*1000 as %s",
				columnName, columnName);
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

	Connection getConnection() {
		return connectionPool.getConnection();
	}

	private Class getEntityType(Class entityType) {
		if (MvccObject.class.isAssignableFrom(entityType)) {
			return entityType.getSuperclass();
		} else {
			return entityType;
		}
	}

	@Override
	public LazyObjectLoader getLazyObjectLoader() {
		return backupLazyLoader;
	}

	public DomainStore getStore() {
		return this.store;
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

	@Override
	public DomainTransformPersistenceQueue.Sequencer getTransformSequencer() {
		return this.transformSequencer;
	}

	private void initialiseDomainSegment() throws Exception {
		getSegmentLoader().init();
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
		CommonUtils.throwIfCompletedWithException(futures);
		initialList.clear();
	}

	private void loadDomainDbSegment() throws Exception {
		List<Callable> calls = new ArrayList<Callable>();
		DomainSegmentDbLoader segmentDbLoader = (DomainSegmentDbLoader) domainDescriptor
				.getDomainSegmentLoader();
		Set<Class> segmentClasses = new LinkedHashSet<>();
		for (DomainSegmentLoaderPhase phase : DomainSegmentLoaderPhase
				.iterateOver()) {
			segmentDbLoader.phase = phase;
			int maxPasses = 240;
			int pass = 0;
			long start = System.currentTimeMillis();
			int lastTotal = -1;
			int total = 0;
			while ((segmentDbLoader.pendingCount() != 0 || lastTotal != total)
					&& pass++ < maxPasses) {
				Set<Entry<Class, Set<Long>>> perClass = segmentDbLoader.toLoadIds
						.entrySet();
				List<EntityRefs> entityRefss = new ArrayList<>();
				for (Entry<Class, Set<Long>> entry : perClass) {
					EntityRefs entityRefs = new EntityRefs(segmentDbLoader);
					entityRefss.add(entityRefs);
					segmentClasses.add(entry.getKey());
					calls.add(() -> {
						Collection<Long> ids = entry.getValue();
						ids = ids.stream().distinct().sorted()
								.collect(Collectors.toList());
						Class clazz = entry.getKey();
						ids = segmentDbLoader.filterForQueried(clazz, "id",
								ids);
						if (ids.size() > 0) {
							loadTableSegment(clazz, Ax.format(" id in %s",
									longsToIdClause(ids)), entityRefs);
						}
						segmentDbLoader.loadedInPhase(clazz, ids);
						entry.getValue().clear();
						return null;
					});
				}
				invokeAllWithThrow(calls);
				// resolve what we can (last pass)
				EntityRefs lastPassLookup = new EntityRefs();
				lastPassLookup.list.addAll(segmentDbLoader.toResolve);
				segmentDbLoader.toResolve.clear();
				lastPassLookup.resolve(segmentDbLoader);
				entityRefss.forEach(ll -> ll.resolve(segmentDbLoader));
				entityRefss.clear();
				if (segmentDbLoader.pendingCount() == 0) {
					for (DomainSegmentLoaderProperty property : segmentDbLoader.properties) {
						if (property.isIgnoreForPhase(phase)) {
							continue;
						}
						if (property.type == DomainSegmentPropertyType.CLAZZ_1_RSCOL_REFS_CLAZZ_2) {
							Collection<Long> ids = store.cache
									.keys(property.clazz2);
							ids = ids.stream().distinct().sorted()
									.collect(Collectors.toList());
							ids = segmentDbLoader.filterForQueried(
									property.clazz1, property.propertyName1,
									ids);
							String sqlFilter = Ax.format(" %s in %s",
									property.columnName1(),
									longsToIdClause(ids));
							segmentClasses.add(property.clazz1);
							if (ids.size() > 0) {
								calls.add(() -> {
									EntityRefs entityRefs = new EntityRefs(
											segmentDbLoader);
									entityRefss.add(entityRefs);
									loadTableSegment(property.clazz1, sqlFilter,
											entityRefs);
									return null;
								});
							}
						} else if (property.type == DomainSegmentPropertyType.CLAZZ_1_PROP_EQ_CLAZZ_2_ID_LOAD_CLAZZ_2) {
							if (store.cache.size(property.clazz1) == 0) {
								continue;
							}
							Collection<Long> ids = store.cache
									.stream(property.clazz1).map(HasId::getId)
									.map(id -> segmentDbLoader.segmentRefs.get(
											property.clazz1,
											property.propertyName1, id))
									.distinct().filter(Objects::nonNull)
									.sorted().collect(Collectors.toList());
							ids = segmentDbLoader.filterForQueried(
									property.clazz2, "id", ids);
							String sqlFilter = Ax.format(" id in %s",
									longsToIdClause(ids));
							segmentClasses.add(property.clazz2);
							calls.add(() -> {
								EntityRefs entityRefs = new EntityRefs(
										segmentDbLoader);
								entityRefss.add(entityRefs);
								loadTableSegment(property.clazz2, sqlFilter,
										entityRefs);
								return null;
							});
						}
					}
					invokeAllWithThrow(calls);
					entityRefss.forEach(ll -> ll.resolve(segmentDbLoader));
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
				segmentClasses.forEach(segmentDbLoader::ensureClass);
			}
			if (pass >= maxPasses) {
				throw Ax.runtimeException("did our max passes and lost");
			}
		}
		segmentDbLoader.saveSegmentData();
	}

	Loader loader() {
		return new Loader();
	}

	private void loadJoinTable(Entry<PropertyDescriptor, JoinTable> entry,
			EntityRefs entityRefs) {
		JoinTable joinTable = entry.getValue();
		if (joinTable == null) {
			return;
		}
		Thread.currentThread().setName("loader-database-" + joinTable.name());
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
			for (ValueContainer[] row : connResults) {
				Entity src = (Entity) store.cache.get(targetEntityClass,
						row[0].l);
				assert src != null;
				if (joinHandler == null) {
					Entity tgt = (Entity) store.cache.get(
							rev.getReadMethod().getDeclaringClass(), row[1].l);
					assert tgt != null;
					entityRefs.add(tgt, pdFwd, src);
					entityRefs.add(src, pdRev, tgt);
				} else {
					joinHandler.injectValue((String) row[1].o, src);
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

	// TODO - this used to require 'keep entities detached' (see rev 41cc5b3d7)
	// - removed, but check on next use
	private void loadTableSegment(Class clazz, String sqlFilter,
			EntityRefs entityRefs) throws Exception {
		synchronized (clazz) {
			Connection conn = getConnection();
			try {
				DomainClassDescriptor<?> descriptor = domainDescriptor.perClass
						.get(clazz);
				MetricLogging.get().start(clazz.getSimpleName());
				logger.info("load segment :: {} {}", clazz.getSimpleName(),
						sqlFilter);
				loader().withConnection(conn).withClazz(clazz)
						.withSqlFilter(sqlFilter).withEntityRefs(entityRefs)
						.withIgnoreDoubleCreationDueToFilter(true).loadHasIds();
				MetricLogging.get().end(clazz.getSimpleName(),
						store.metricLogger);
			} finally {
				releaseConn(conn);
			}
		}
	}

	@Override
	public DomainTransformRequestPersistent loadTransformRequest(long id) {
		synchronized (loadTransformRequestLock) {
			try {
				return loadTransformRequest0(id);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	private DomainTransformRequestPersistent
			loadTransformRequest0(long requestId) throws Exception {
		store.logger.info("{} - loading transform request {}", store.name,
				requestId);
		Connection conn = getConnection();
		try {
			if (!checkTransformRequestExists(requestId)) {
				return null;
			}
			DomainTransformRequestPersistent request = store.domainDescriptor
					.getDomainTransformRequestPersistentClass()
					.getDeclaredConstructor().newInstance();
			request.setId(requestId);
			Statement statement = conn.createStatement();
			String requestTableName = request.getClass()
					.getAnnotation(Table.class).name();
			request.setTransactionCommitTime(SqlUtils.getValue(statement,
					Ax.format(
							"select transactioncommittime from %s where id=%s",
							requestTableName, requestId),
					Timestamp.class));
			request.setClientInstance(PersistentImpl.find(ClientInstance.class,
					SqlUtils.getValue(statement, Ax.format(
							"select clientInstance_id from %s where id=%s",
							request.getClass().getAnnotation(Table.class)
									.name(),
							requestId), Long.class)));
			statement.close();
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
					" domainTransformRequestPersistent_id = %s ", requestId);
			EntityRefs entityRefs = new EntityRefs();
			Transaction.ensureDomainPreparingActive();
			Loader loader = loader().withConnection(conn)
					.withClazz(transformEventImplClass).withSqlFilter(sqlFilter)
					.withReturnResults(true).withEntityRefs(entityRefs);
			List<? extends DomainTransformEventPersistent> transforms = (List<? extends DomainTransformEventPersistent>) (List<?>) loader
					.loadHasIds();
			entityRefs.resolve(new CustomResolver() {
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
				public Object resolveCustom(PdOperator pdOperator, Ref item) {
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
						return request;
					default:
						throw new UnsupportedOperationException();
					}
				}
			});
			transforms.removeIf(event -> event.getObjectClassRef() == null
					|| event.getObjectClassRef().notInVm()
					|| (event.getValueClassRef() != null
							&& event.getValueClassRef().notInVm()));
			TransformCollation collation = new TransformCollation(transforms);
			Multimap<Class, List<EntityCollation>> toLoad = collation
					.allEntityCollations().filter(ec -> !ec.isDeleted()
							&& store.isCached(ec.getEntityClass())
							// collation may have zero transforms (if entity is
							// only the target of a transform)
							&& ec.getTransforms().size() > 0
							&& IVersionable.class
									.isAssignableFrom(ec.getEntityClass()))
					.collect(AlcinaCollectors
							.toKeyMultimap(EntityCollation::getEntityClass));
			List<Callable> tasks = new ArrayList<>();
			toLoad.entrySet().forEach(e -> {
				tasks.add(new IVersionableLoaderTask(conn, e.getKey(),
						e.getValue()));
			});
			invokeAllWithThrow(tasks, iLoaderExecutor);
			transforms.stream()
					.sorted(Comparator
							.comparing(DomainTransformEventPersistent::getId))
					.forEach(request.getEvents()::add);
			return request;
		} finally {
			releaseConn(conn);
		}
	}

	private String longsToIdClause(Collection<Long> ids) {
		return EntityPersistenceHelper
				.toInClause(ids.stream().sorted().collect(Collectors.toList()));
	}

	private void prepareTable(DomainClassDescriptor classDescriptor)
			throws Exception {
		Class clazz = classDescriptor.clazz;
		List<PropertyDescriptor> pds = SEUtilities
				.getPropertyDescriptorsSortedByName(clazz).stream()
				.collect(Collectors.toList());
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
			DomainStoreProperty domainStoreProperty = store.domainStoreProperties
					.get(clazz, pd.getName());
			if ((rm.getAnnotation(Transient.class) != null
					&& rm.getAnnotation(DomainStoreDbColumn.class) == null)
					|| domainStoreProperty != null) {
				ignore = true;
				if (domainStoreProperty != null) {
					Field field = store.getField(clazz, pd.getName());
					field.setAccessible(true);
					domainStorePropertyFields.put(clazz, field, field);
					ignore = domainStoreProperty
							.loadType() == DomainStorePropertyLoadType.TRANSIENT;
				}
			}
			if (ignore) {
				continue;
			}
			if (classDescriptor.ignoreField(pd.getName())) {
				continue;
			}
			AnnotationUtils.checkNotObscuredAnnotation(pd,
					DomainTransformPropagation.class);
			AnnotationUtils.checkNotObscuredAnnotation(pd,
					DomainStoreProperty.class);
			if (domainStoreProperty != null && domainStoreProperty
					.loadType() == DomainStorePropertyLoadType.LAZY) {
				Preconditions.checkArgument(rm
						.getAnnotation(DomainTransformPropagation.class) != null
						&& rm.getAnnotation(DomainTransformPropagation.class)
								.value() == PropagationType.NONE,
						Ax.format(
								"Incorrect propagation for lazy load property: %s.%s",
								clazz.getSimpleName(), rm.getName()));
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
			PdOperator pdOperator = ensurePdOperator(pd, clazz);
			if (manyToOne != null || oneToOne != null
					|| domainStoreColumn != null) {
				if (domainStoreColumn != null
						&& domainStoreColumn.customHandler()) {
					// we'll just to conversion in code - targetEntityType
					// irrelevant
				} else {
					Class joinEntityType = getTargetEntityType(rm);
					if (!domainDescriptor.joinPropertyCached(joinEntityType)) {
						logger.debug("  not loading: {}.{} -- {}",
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
				addColumnName(clazz, pdOperator,
						getEntityType(getTargetEntityType(pd.getReadMethod())));
			} else {
				addColumnName(clazz, pdOperator,
						getEntityType(pd.getPropertyType()));
			}
			mapped.add(pdOperator);
		}
		boolean addLazyPropertyLoadTask = columnDescriptors.get(clazz).stream()
				.anyMatch(
						cd -> cd.loadType == DomainStorePropertyLoadType.LAZY);
		if (addLazyPropertyLoadTask) {
			domainDescriptor.preProvideTasks
					.add(new LazyPropertyLoadTask<>(clazz, store));
			logger.trace("Adding lazy property load task for: {}", clazz);
		}
	}

	void releaseConn(Connection conn) {
		if (conn == null) {
			return;
		}
		connectionPool.releaseConnection(conn);
	}

	public void setConnectionUrl(String newUrl) {
		dataSource.setConnectionUrl(newUrl);
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
					loadJoinTable(entryF, warmupEntityRefs());
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
						Thread.currentThread().setName(
								"loader-database-" + clazz.getSimpleName());
						loader().withClazz(clazz)
								.withSqlFilter(
										descriptor.getInitialLoadFilter())
								.withEntityRefs(warmupEntityRefs())
								.loadEntities();
						MetricLogging.get().end(clazz.getSimpleName(),
								store.metricLogger);
						return null;
					}
				});
			}
		}
	}

	protected Date utcTimeToDate(long utcTime) {
		int persistOffset = store.startupTz.getOffset(utcTime);
		long timeLocal = utcTime - persistOffset;
		return new Date(timeLocal);
	}

	DomainSegmentLoader getSegmentLoader() {
		return domainDescriptor.getDomainSegmentLoader();
	}

	@Override
	public void warmup() throws Exception {
		new StatCategory_DomainStore.Warmup.Loader().emit();
		this.domainDescriptor = store.domainDescriptor;
		joinTables = new LinkedHashMap<PropertyDescriptor, JoinTable>();
		descriptors = new LinkedHashMap<Class, List<PdOperator>>();
		manyToOneRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		oneToOneRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		domainStoreColumnRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);
		columnDescriptors = new Multimap<Class, List<ColumnDescriptor>>();
		warmupTransaction = Transaction.current();
		warmupTransaction.setMaxAge(60 * TimeConstants.ONE_MINUTE_MS);
		transformSequencer.setInitialised(true);
		transformSequencer.waitForWritableTransactionsToTerminate();
		{
			Connection conn = getConnection();
			transformSequencer.markHighestVisibleTransformList(conn);
			releaseConn(conn);
		}
		DomainTransformCommitPosition highestVisibleCommitPosition = transformSequencer.highestVisiblePosition;
		warmupTransaction.toDomainCommitting(
				highestVisibleCommitPosition.getCommitTimestamp(), store,
				store.applyTxToGraphCounter.getAndIncrement(), 0L, null);
		store.getPersistenceEvents().getQueue().setMuteEventsOnOrBefore(
				highestVisibleCommitPosition.getCommitTimestamp());
		new StatCategory_DomainStore.Warmup.Loader.Mark().emit();
		// get non-many-many obj
		// lazy tables, load a segment (for large db dev work)
		if (getSegmentLoader() != null) {
			MetricLogging.get().start("initialise-domain-segment");
			initialiseDomainSegment();
			MetricLogging.get().end("initialise-domain-segment");
		}
		MetricLogging.get().start("tables");
		for (DomainClassDescriptor descriptor : domainDescriptor
				.getWarmupClasses()) {
			Class clazz = descriptor.clazz;
			prepareTable(descriptor);
			// warmup threadsafe
			store.cache.getMap(clazz);
		}
		List<Callable> calls = new ArrayList<Callable>();
		setupInitialLoadTableCalls(calls);
		invokeAllWithThrow(calls);
		new StatCategory_DomainStore.Warmup.Loader.Tables().emit();
		setupInitialJoinTableCalls(calls);
		invokeAllWithThrow(calls);
		new StatCategory_DomainStore.Warmup.Loader.JoinTables().emit();
		MetricLogging.get().end("tables");
		// clear existing interns, but intern incoming changes - optimal
		// allocation
		interns.setRotating(true);
		MetricLogging.get().start("xrefs");
		for (EntityRefs ll : warmupEntityRefs) {
			calls.add(() -> {
				ll.resolve();
				return null;
			});
		}
		invokeAllWithThrow(calls);
		MetricLogging.get().end("xrefs");
		new StatCategory_DomainStore.Warmup.Loader.Xrefs().emit();
		warmupEntityRefs.clear();
		// lazy tables, load a segment (for large db dev work)
		if (getSegmentLoader() != null) {
			MetricLogging.get().start("domain-segment");
			if (getSegmentLoader() instanceof DomainSegmentDbLoader) {
				loadDomainDbSegment();
			} else {
				// hookup domainsegment as the connrs provider
				throw new UnsupportedOperationException();
			}
			MetricLogging.get().end("domain-segment");
		}
		new StatCategory_DomainStore.Warmup.Loader.Segment().emit();
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
		calls.clear();
		// invokeAllWithThrow(calls);
		MetricLogging.get().end("postLoad");
		new StatCategory_DomainStore.Warmup.Loader.PostLoad().emit();
		MetricLogging.get().start("lookups");
		for (final DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
				.values()) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					Thread.currentThread().setName("loader-lookup-"
							+ descriptor.clazz.getSimpleName());
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
		new StatCategory_DomainStore.Warmup.Loader.Lookups().emit();
		MetricLogging.get().start("projections");
		for (final DomainClassDescriptor<?> descriptor : domainDescriptor.perClass
				.values()) {
			calls.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					Thread.currentThread().setName("loader-projections-"
							+ descriptor.clazz.getSimpleName());
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
		new StatCategory_DomainStore.Warmup.Loader.Projections().emit();
		store.initialising = false;
		connectionPool.drain();
		warmupExecutor.shutdown();
		warmupExecutor = null;
		warmupTransaction = null;
		Transaction.current().toDomainCommitted(highestVisibleCommitPosition);
		store.getPersistenceEvents().getQueue()
				.setTransformLogPosition(highestVisibleCommitPosition);
		Transaction.endAndBeginNew();
		new StatCategory_DomainStore.Warmup.Loader.End().emit();
	}

	synchronized EntityRefs warmupEntityRefs() {
		EntityRefs result = new EntityRefs();
		warmupEntityRefs.add(result);
		return result;
	}

	class BackupLazyLoader implements LazyObjectLoader {
		@Override
		public <T extends Entity> void loadObject(Class<? extends T> clazz,
				long id, long localId) {
			boolean ignorePropertyChanges = TransformManager.get()
					.isIgnorePropertyChanges();
			try {
				TransformManager.get().setIgnorePropertyChanges(true);
				DomainClassDescriptor itemDescriptor = domainDescriptor.perClass
						.get(clazz);
				if (itemDescriptor != null
						&& itemDescriptor.provideNotFullyLoadedOnStartup()) {
					logger.debug("Backup lazy load: {} - {}\n",
							clazz.getSimpleName(), id);
					List<Entity> entities = loader().withClazz(clazz)
							.withSqlFilter("id=" + id).
							// handle cascading resolution in initialising phase
							withResolveRefs(true).withReturnResults(true)
							.loadEntities();
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				TransformManager.get()
						.setIgnorePropertyChanges(ignorePropertyChanges);
			}
		}
	}

	class ColumnDescriptor {
		private PropertyDescriptor pd;

		private Class<?> type;

		private boolean typeIdHasId;

		private EnumType enumType = EnumType.ORDINAL;

		private DomainStorePropertyLoadType loadType = DomainStorePropertyLoadType.EAGER;

		// debugging
		Class clazz;

		public ColumnDescriptor(Class clazz, PropertyDescriptor pd,
				Class propertyType) {
			this.clazz = clazz;
			this.pd = pd;
			type = propertyType;
			typeIdHasId = HasId.class.isAssignableFrom(type);
			Enumerated enumerated = pd.getReadMethod()
					.getAnnotation(Enumerated.class);
			if (enumerated != null) {
				enumType = enumerated.value();
			}
			DomainStoreProperty domainStoreProperty = store.domainStoreProperties
					.get(clazz, pd.getName());
			if (domainStoreProperty != null) {
				loadType = domainStoreProperty.loadType();
				if (!domainStoreProperty
						.optimiseOneToManyCollectionModifications()) {
					Preconditions.checkState(
							loadType == DomainStorePropertyLoadType.EAGER);
				}
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
				return createDateClause(columnName);
			} else {
				return columnName;
			}
		}

		private Object intern(Object object) {
			if (object == null) {
				return null;
			}
			return interns.get(object);
		}

		public void loadObject(ResultSet rs, int idx,
				ValueContainer valueContainer) throws Exception {
			if (typeIdHasId || type == long.class) {
				valueContainer.l = rs.getLong(idx);
				return;
			}
			if (type == Long.class) {
				Long v = rs.getLong(idx);
				if (rs.wasNull()) {
					v = null;
				}
				valueContainer.o = intern(v);
				return;
			}
			if (type == String.class) {
				valueContainer.o = intern(rs.getString(idx));
				return;
			}
			if (type == double.class) {
				valueContainer.d = rs.getDouble(idx);
				return;
			}
			if (type == Double.class) {
				Double v = rs.getDouble(idx);
				if (rs.wasNull()) {
					v = null;
				}
				valueContainer.o = intern(v);
				return;
			}
			if (type == float.class) {
				valueContainer.f = rs.getFloat(idx);
				return;
			}
			if (type == Float.class) {
				Float v = rs.getFloat(idx);
				if (rs.wasNull()) {
					v = null;
				}
				valueContainer.o = intern(v);
				return;
			}
			if (type == int.class) {
				valueContainer.i = rs.getInt(idx);
				return;
			}
			if (type == Integer.class) {
				Integer v = rs.getInt(idx);
				if (rs.wasNull()) {
					v = null;
				}
				valueContainer.o = intern(v);
				return;
			}
			if (type == boolean.class) {
				valueContainer.b = rs.getBoolean(idx);
				return;
			}
			if (type == Boolean.class) {
				Boolean v = rs.getBoolean(idx);
				if (rs.wasNull()) {
					v = null;
				}
				valueContainer.o = intern(v);
				return;
			}
			if (type == Date.class) {
				long utcTime = rs.getLong(idx);
				Date v = null;
				if (rs.wasNull()) {
					//
				} else {
					// it seems getLong mostly returns utc timestamp (not
					// locale)
					// now mandating that with the 'at timezone utc' above
					// note these cols are currently pg timestamp without tz
					v = utcTimeToDate(utcTime);
				}
				valueContainer.o = intern(v);
				return;
			}
			if (Enum.class.isAssignableFrom(type)) {
				valueContainer.o = null;
				switch (enumType) {
				case ORDINAL:
					int eIdx = rs.getInt(idx);
					Object[] enumConstants = type.getEnumConstants();
					if (eIdx >= enumConstants.length) {
						store.logger.warn("Invalid enum index : {}:{}",
								type.getSimpleName(), eIdx);
						return;
					}
					valueContainer.o = rs.wasNull() ? null
							: enumConstants[eIdx];
					return;
				case STRING:
					String enumString = rs.getString(idx);
					if (enumString == null) {
						return;
					}
					Enum enumValue = CommonUtils
							.getEnumValueOrNull((Class) type, enumString);
					if (enumValue == null) {
						store.logger.warn("Invalid enum value : {}:{}",
								type.getSimpleName(), enumString);
						return;
					}
					valueContainer.o = enumValue;
					return;
				}
			}
			throw new RuntimeException(
					"Unhandled rs type: " + type.getSimpleName());
		}
	}

	/*
	 * This pool provides per-thread connections, allowing reentrancy (threads
	 * attempting to acquire a connection will get the one already allocated, if
	 * any)
	 *
	 */
	class ConnectionPool {
		int maxConnections = Configuration
				.getInt(DomainStoreLoaderDatabase.class, "maxConnections");

		private Connection initialWarmupConnection;

		private int originalWarmupTransactionIsolation;

		// only access via synchronized code
		private List<Member> members = new ArrayList<>();

		WarmupConnectionCreatorPg warmupCreator = new WarmupConnectionCreatorPg();

		public ConnectionPool(RetargetableDataSource dataSource) {
		}

		boolean availableForCurrentThread(Member member) {
			return member.threads.isEmpty() || member.threads.stream()
					.allMatch(t -> t == Thread.currentThread());
		}

		synchronized void drain() {
			members.forEach(Member::close);
			members.clear();
			synchronized (this) {
				notifyAll();
			}
		}

		synchronized Connection getConnection() {
			Member member = getMember();
			Preconditions.checkState(availableForCurrentThread(member));
			member.threads.add(Thread.currentThread());
			return member.connection;
		}

		private Member getMember() {
			long start = System.currentTimeMillis();
			synchronized (this) {
				Optional<Member> o_member = getMember0();
				if (o_member.isPresent()) {
					return o_member.get();
				}
			}
			synchronized (this) {
				Optional<Member> o_member = null;
				while (true) {
					try {
						wait(1000);
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
					long now = System.currentTimeMillis();
					if (now - start > 1000 && store.initialised) {
						logger.warn("Long wait for connection - {} threads",
								members.stream()
										.collect(Collectors.summingInt(
												m -> m.threads.size()))
										.toString());
					}
					o_member = getMember0();
					if (o_member.isPresent()) {
						return o_member.get();
					}
				}
			}
		}

		/*
		 * look for a free member; then allocate up to maxconnections; then wait
		 */
		private Optional<Member> getMember0() {
			Optional<Member> o_member = members.stream()
					.filter(this::availableForCurrentThread).findFirst();
			if (o_member.isPresent()) {
				return o_member;
			}
			if (members.size() < maxConnections) {
				Member member = new Member();
				members.add(member);
				return Optional.of(member);
			}
			return Optional.empty();
		}

		synchronized void markInvalidConnection(Connection connection) {
			Optional<Member> o_member = members.stream()
					.filter(m -> m.connection == connection).findFirst();
			if (o_member.isPresent()) {
				Member member = o_member.get();
				members.remove(member);
			}
		}

		synchronized void releaseConnection(Connection connection) {
			Optional<Member> o_member = members.stream()
					.filter(m -> m.connection == connection).findFirst();
			if (o_member.isPresent()) {
				Member member = o_member.get();
				member.threads.remove(Thread.currentThread());
				member.lastRelease = System.currentTimeMillis();
				notifyAll();
			}
		}

		class Member {
			Connection connection;

			List<Thread> threads = new ArrayList<>();

			boolean invalid;

			long lastRelease;

			private boolean warmup;

			public Member() {
				try {
					if (store.initialising) {
						connection = warmupCreator.create();
						warmup = true;
					} else {
						connection = dataSource.getConnection();
					}
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}

			void close() {
				try {
					logger.debug("Closing db connection  {}", connection);
					if (invalid) {
						Exception exception = new Exception(
								"Closing closed/invalid connection");
						exception.printStackTrace();
						return;
					}
					invalid = true;
					connection.commit();
					if (warmup) {
						connection.setTransactionIsolation(
								originalWarmupTransactionIsolation);
					}
					connection.setAutoCommit(true);
					connection.setReadOnly(false);
					connection.close();
				} catch (SQLException e) {
					if (e.toString().contains(
							"Cannot commit when autoCommit is enabled")) {
					} else {
						e.printStackTrace();
					}
				}
			}
		}

		class WarmupConnectionCreator {
			Connection create() throws SQLException {
				Connection conn = dataSource.getConnection();
				conn.setAutoCommit(false);
				conn.setReadOnly(true);
				originalWarmupTransactionIsolation = conn
						.getTransactionIsolation();
				conn.setTransactionIsolation(
						Connection.TRANSACTION_REPEATABLE_READ);
				logger.debug("Opening new warmup connection {}", conn);
				return conn;
			}
		}

		class WarmupConnectionCreatorPg {
			private String snapshotId;

			Connection create() throws SQLException {
				String isolationLevel = "SERIALIZABLE";
				if (Configuration.is(DomainStore.class, "warmStandbyDb")) {
					isolationLevel = "REPEATABLE_READ";
				}
				if (initialWarmupConnection == null) {
					initialWarmupConnection = dataSource.getConnection();
					initialWarmupConnection.setAutoCommit(false);
					originalWarmupTransactionIsolation = initialWarmupConnection
							.getTransactionIsolation();
					try (Statement stmt = initialWarmupConnection
							.createStatement()) {
						stmt.execute(Ax.format(
								"BEGIN TRANSACTION ISOLATION LEVEL %s DEFERRABLE;",
								isolationLevel));
						ResultSet rs = stmt
								.executeQuery("SELECT pg_export_snapshot();");
						rs.next();
						snapshotId = rs.getString(1);
					}
					return initialWarmupConnection;
				} else {
					Connection conn = dataSource.getConnection();
					conn.setAutoCommit(false);
					try (Statement stmt = conn.createStatement()) {
						stmt.execute(Ax.format(
								"BEGIN TRANSACTION ISOLATION LEVEL %s DEFERRABLE;",
								isolationLevel));
						stmt.execute(Ax.format("SET TRANSACTION SNAPSHOT '%s';",
								snapshotId));
					}
					return conn;
				}
			}
			//
		}
	}

	static class ConnResults implements Iterable<ValueContainer[]> {
		public static Builder builder() {
			return new Builder();
		}

		ConnResultsIterator itr;

		private Connection conn;

		private List<ColumnDescriptor> columnDescriptors;

		Class clazz;

		String sqlFilter;

		ResultSet rs = null;

		List<ValueContainer[]> cachedValues;

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
			DomainSegmentLoader segmentLoader = domainDescriptor
					.getDomainSegmentLoader();
			boolean hasSegmentLoader = segmentLoader != null;
			this.rsReuse = hasSegmentLoader ? (ConnResultsReuse) segmentLoader
					: new ConnResultsReusePassthrough();
			this.lazyProperties = builder.populateLazyPropertyValues;
			itr = new ConnResultsIterator(hasSegmentLoader);
		}

		public ResultSet ensureRs() {
			return ensureRs(0);
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
					loader.store.sqlLogger
							.debug(CommonUtils.trimToWsChars(rsSql, 1000));
					rs = stmt.executeQuery(rsSql);
				}
				return rs;
			} catch (Exception e) {
				if (pass < 2 && !loader.store.initialising
						&& loader.connectionsReopened.get() < 20) {
					try {
						// don't close the last one, invalid
						loader.connectionPool.markInvalidConnection(conn);
						conn = loader.getConnection();
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

		@Override
		public Iterator<ValueContainer[]> iterator() {
			return rsReuse.getIterator(this, itr);
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

		class ConnResultsIterator implements Iterator<ValueContainer[]> {
			ValueContainer[] current = null;

			boolean peeked = false;

			boolean finished = false;

			private boolean hasSegmentLoader;

			public ConnResultsIterator(boolean hasSegmentLoader) {
				this.hasSegmentLoader = hasSegmentLoader;
				createValueContainersForRow();
			}

			protected void createValueContainersForRow() {
				int length = joinTable ? 2 : columnDescriptors.size();
				current = new ValueContainer[length];
				for (int idx = 0; idx < length; idx++) {
					current[idx] = new ValueContainer();
				}
			}

			@Override
			public boolean hasNext() {
				peekNext();
				return !finished;
			}

			@Override
			public ValueContainer[] next() {
				if (finished) {
					throw new NoSuchElementException();
				}
				peekNext();
				peeked = false;
				return current;
			}

			private void peekNext() {
				if (!peeked && !finished) {
					ensureRs();
					try {
						if (rs.next()) {
							if (hasSegmentLoader) {
								createValueContainersForRow();
							}
							if (joinTable) {
								current[0].l = rs.getLong(1);
								if (joinHandler != null) {
									// currently only one implementation (that
									// expects a String)
									current[1].o = rs.getString(2);
								} else {
									current[1].l = rs.getLong(2);
								}
							} else {
								for (int idx = 1; idx <= columnDescriptors
										.size(); idx++) {
									ColumnDescriptor descriptor = columnDescriptors
											.get(idx - 1);
									descriptor.loadObject(rs, idx,
											current[idx - 1]);
								}
							}
							ConnResults.this.rsReuse.onNext(ConnResults.this,
									current);
						} else {
							finished = true;
							current = null;
							rs.close();
							stmt.close();
						}
						peeked = true;
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
	}

	public interface ConnResultsReuse {
		default Iterator<ValueContainer[]> getIterator(ConnResults connResults,
				ConnResultsIterator itr) {
			return itr;
		}

		default void onNext(ConnResults connResults, ValueContainer[] cached) {
		}

		public interface Has {
			ConnResultsReuse getConnResultsReuse();
		}
	}

	public EntityValuesMapper
			getEntityValuesMapper(Class<? extends Entity> clazz) {
		return new EntityValuesMapper(clazz);
	}

	public class EntityValuesMapper {
		Class<? extends Entity> clazz;

		EntityValuesMapper(Class<? extends Entity> clazz) {
			this.clazz = clazz;
		}

		public ValueContainer[] getValues(Entity entity) {
			List<PdOperator> operators = descriptors.get(entity.entityClass());
			List<ValueContainer> list = operators.stream()
					.map(op -> ValueContainer.of(op, entity)).toList();
			return list.toArray(new ValueContainer[list.size()]);
		}
	}

	static class ConnResultsReusePassthrough implements ConnResultsReuse {
	}

	interface CustomResolver {
		boolean handles(PdOperator pdOperator);

		Object resolveCustom(PdOperator pdOperator, Ref item);
	}

	public static interface DomainStoreJoinHandler {
		public String getTargetSql();

		public void injectValue(String stringValue, Entity source);
	}

	class EntityRefs {
		List<Ref> list = new RefList();

		private CustomResolver customResolver;

		private DomainSegmentDbLoader segmentLoader;

		public EntityRefs() {
		}

		public EntityRefs(DomainSegmentDbLoader segmentLoader) {
			this.segmentLoader = segmentLoader;
		}

		void add(HasId target, PdOperator pd, HasId source) {
			list.add(new Ref(target, pd, source));
		}

		void add(long id, PdOperator pd, HasId hasId) {
			list.add(new Ref(id, pd, hasId));
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

		void resolve(DomainSegmentDbLoader segmentLoader) {
			this.segmentLoader = segmentLoader;
			doResolve();
		}

		class Ref {
			long id;

			PdOperator pdOperator;

			HasId source;

			HasId target;

			Ref() {
			}

			Ref(HasId target, PdOperator pd, HasId source) {
				this.target = target;
				this.pdOperator = pd;
				this.source = source;
			}

			Ref(long id, PdOperator pd, HasId source) {
				this.id = id;
				this.pdOperator = pd;
				this.source = source;
			}
		}

		class RefList extends AbstractList<Ref> {
			Ref view = new Ref();

			LongArrayList ids = new LongArrayList();

			List<PdOperator> pdOperators = new ArrayList<>();

			List<HasId> sources = new ArrayList<>();

			List<HasId> targets = new ArrayList<>();

			void add(HasId target, PdOperator pd, HasId source) {
				add(0L, pd, source, target);
			}

			@Override
			public void add(int index, Ref element) {
				throw new UnsupportedOperationException();
			}

			void add(long id, PdOperator pd, HasId source) {
				add(id, pd, source, null);
			}

			private void add(long id, PdOperator pdOperator, HasId source,
					HasId target) {
				ids.add(id);
				pdOperators.add(pdOperator);
				sources.add(source);
				targets.add(target);
			}

			@Override
			public boolean add(Ref e) {
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
			public Ref get(int index) {
				view.id = ids.getLong(index);
				view.pdOperator = pdOperators.get(index);
				view.source = sources.get(index);
				view.target = targets.get(index);
				return view;
			}

			@Override
			public Iterator<Ref> iterator() {
				return new Itr();
			}

			@Override
			public int size() {
				return ids.size();
			}

			private class Itr implements Iterator<Ref> {
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

				final void checkForComodification() {
					if (modCount != expectedModCount)
						throw new ConcurrentModificationException();
				}

				@Override
				public boolean hasNext() {
					return cursor != size();
				}

				@Override
				public Ref next() {
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
			}
		}

		private final class ResolveRefsTask implements Callable<Void> {
			private List<Ref> items;

			int missingWarningCount = 0;

			private ResolveRefsTask(List<Ref> items) {
				this.items = items;
			}

			@Override
			public /*
					 * multithread Problem here is that set() methods need to be
					 * synced per class (really, pd) ..so run linear
					 */
			Void call() throws Exception {
				String entryName = Thread.currentThread().getName();
				try {
					call0();
				} finally {
					Thread.currentThread().setName(entryName);
				}
				return null;
			}

			private void call0() throws Exception {
				if (LooseContext.is(
						DomainStore.CONTEXT_DO_NOT_RESOLVE_LOAD_TABLE_REFS)) {
					this.items.clear();
					return;
				}
				if (this.items.size() > 0) {
					Thread.currentThread().setName("loader-ref-"
							+ items.get(0).source.getClass().getSimpleName());
				}
				for (Ref item : this.items) {
					try {
						PdOperator pdOperator = item.pdOperator;
						pdOperator.resolveHelper.ensure(item.source.getClass());
						long id = item.id;
						if (pdOperator.resolveHelper.isCustom(customResolver)) {
							// non-entity, so field access tx-OK
							pdOperator.field.set(item.source,
									pdOperator.resolveHelper.resolveCustom(
											customResolver, item));
						} else if (pdOperator.resolveHelper.inJoinTables) {
							Set set = (Set) pdOperator
									.get((Entity) item.source);
							if (set == null) {
								set = new LiSet();
								pdOperator.set(item.source, set);
							}
							set.add(item.target);
						} else if (customResolver != null) {
							// ignore, customResolver exists and not custom
							// resolvable
						} else {
							Class type = propertyDescriptorFetchTypes
									.get(pdOperator);
							Object target = store.cache.get(type, id);
							if (target == null) {
								boolean loadLazy = domainDescriptor.perClass
										.get(type)
										.provideNotFullyLoadedOnStartup();
								DomainStoreProperty storeProperty = pdOperator.domainStoreProperty;
								DomainStorePropertyLoadType loadType = DomainStorePropertyLoadType.EAGER;
								if (storeProperty != null) {
									loadType = storeProperty.loadType();
								}
								switch (loadType) {
								case EAGER:
									if (store.isInitialised() || LooseContext
											.is(CONTEXT_ALLOW_ALL_LAZY_LOAD)) {
										loadLazy = true;
										break;
									} else {
										if (segmentLoader == null) {
											throw Ax.runtimeException(
													"Warmup: eager loading specified of non-fully "
															+ "loaded target entity class %s :: %s.%s",
													type.getSimpleName(),
													pdOperator.clazz
															.getSimpleName(),
													pdOperator.name);
										}
									}
								case LAZY:
									loadLazy = store.isInitialised();
									break;
								case TRANSIENT:
									loadLazy = false;
									break;
								case CUSTOM:
									loadLazy = pdOperator.lazyLoadOracle
											.shouldLoad((Entity) item.source,
													store.isInitialised());
									break;
								default:
									throw new UnsupportedOperationException();
								}
								if (loadLazy) {
									target = MethodContext.instance()
											.withContextTrue(
													CONTEXT_ALLOW_ALL_LAZY_LOAD)
											.call(() -> store.find(type, id));
								}
								if (target == null) {
									if (segmentLoader == null) {
										if (missingWarningCount++ < 5) {
											new Exception().printStackTrace();
											store.logger.warn(
													"later-lookup -- missing target: {}, {} for  {}.{} #{}",
													type, id,
													item.source.getClass(),
													pdOperator.name,
													item.source.getId());
										}
									} else {
										segmentLoader.notifyLater(item, type,
												id);
									}
								}
							}
							pdOperator.set(item.source, target);
							PdOperator targetOperator = pdOperator.resolveHelper.targetOperator;
							if (targetOperator != null && target != null) {
								Set set = (Set) targetOperator.field
										.get(target);
								if (set == null) {
									set = new LiSet();
									targetOperator.set((HasId) target, set);
								}
								set.add(item.source);
							}
							targetOperator = pdOperator.resolveHelper.oneToOneOperator;
							if (targetOperator != null && target != null) {
								targetOperator.set((HasId) target, item.source);
							}
							targetOperator = pdOperator.resolveHelper.domainStoreRevOperator;
							if (targetOperator != null && target != null) {
								targetOperator.set((HasId) target, item.source);
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
				return;
			}
		}
	}

	class Interns {
		private static final int ROTATE_SIZE = 2000;

		boolean rotating;

		Map<Object, Object> map = new ConcurrentHashMap<>();

		public Object get(Object object) {
			// don't use computeIfAbsent - we're not concerned about the odd
			// duplicate
			// return interns.computeIfAbsent(object, Function.identity());
			Object v;
			if ((v = map.get(object)) == null) {
				// map ref may have changed via rotate, but we don't care
				map.put(object, object);
				v = object;
			}
			if (rotating) {
				// fairly arbitrary size - gain most of the benefits of Date
				// interning,
				// at least
				if (map.size() > ROTATE_SIZE) {
					rotate();
				}
			}
			return v;
		}

		public boolean isRotating() {
			return this.rotating;
		}

		private synchronized void rotate() {
			if (map.size() > 0) {
				map = new ConcurrentHashMap<>();
			}
		}

		public void setRotating(boolean rotating) {
			rotate();
			this.rotating = rotating;
		}
	}

	private class IVersionableLoaderTask implements Callable<Void> {
		private Class<Entity> clazz;

		private Collection<EntityCollation> collations;

		private Connection conn;

		public IVersionableLoaderTask(Connection conn, Class<Entity> clazz,
				List<EntityCollation> collations) {
			this.conn = conn;
			this.clazz = clazz;
			this.collations = collations;
		}

		@Override
		public Void call() {
			try {
				EntityRefs entityRefs = new EntityRefs();
				String sql = Ax.format(
						"select id,%s,%s from %s where id in %s ",
						createDateClause("creationDate"),
						createDateClause("lastModificationDate"),
						clazz.getAnnotation(Table.class).name(),
						EntityPersistenceHelper.toInClause(collations));
				try (Statement statement = conn.createStatement()) {
					ResultSet rs = SqlUtils.executeQuery(statement, sql);
					Map<EntityLocator, EntityCollation> locatorCollation = collations
							.stream().collect(AlcinaCollectors
									.toKeyMap(EntityCollation::getLocator));
					while (rs.next()) {
						VersionableEntity persistentSource = (VersionableEntity) Reflections
								.newInstance(clazz);
						persistentSource.setId(rs.getLong("id"));
						persistentSource.setCreationDate(
								utcTimeToDate(rs.getLong("creationDate")));
						persistentSource.setLastModificationDate(utcTimeToDate(
								rs.getLong("lastModificationDate")));
						EntityCollation collation = locatorCollation
								.get(persistentSource.toLocator());
						((DomainTransformEventPersistent) collation.last())
								.populateDbMetadata(persistentSource);
					}
					return null;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new WrappedRuntimeException(e);
			}
		}
	}

	class Loader {
		Class clazz;

		String sqlFilter;

		EntityRefs entityRefs = new EntityRefs();

		Connection connection;

		boolean populateLazyPropertyValues;

		boolean returnResults;

		private boolean resolveRefs;

		boolean ignoreDoubleCreationDueToFilter = false;

		private List<HasId> load0() throws Exception {
			List<HasId> loaded = new ArrayList<>();
			ConnResults connResults = ConnResults.builder().withClazz(clazz)
					.withConn(connection)
					.withColumnDescriptors(columnDescriptors.get(clazz))
					.withPopulateLazyPropertyValues(populateLazyPropertyValues)
					.withLoader(DomainStoreLoaderDatabase.this)
					.withSqlFilter(sqlFilter).build();
			List<PdOperator> pds = descriptors.get(clazz);
			PdOperator idOperator = pds.stream()
					.filter(pd -> pd.name.equals("id")).findFirst().get();
			Transaction transaction = Transaction.isInTransaction()
					? Transaction.current()
					: null;
			boolean transactional = DomainStore.stores()
					.storeFor(clazz) != null;
			for (ValueContainer[] row : connResults) {
				long id = row[idOperator.idx].l;
				HasId hasId = null;
				if (transactional) {
					if (store.initialising) {
						if (ignoreDoubleCreationDueToFilter) {
							// the query may return multiple copies of the
							// entity - ignore
							if (store.cache.contains(clazz, id)) {
								continue;
							}
						}
						hasId = transaction.create(clazz, store, id, 0L);
						store.transformManager.store.mapObject((Entity) hasId);
					} else {
						hasId = store.ensureEntity(clazz, id, 0L);
					}
				} else {
					hasId = (HasId) clazz.getDeclaredConstructor()
							.newInstance();
				}
				if (returnResults) {
					loaded.add(hasId);
				}
				for (int i = 0; i < row.length; i++) {
					PdOperator pdOperator = pds.get(i);
					ColumnDescriptor columnDescriptor = connResults.columnDescriptors
							.get(i);
					Method rm = pdOperator.readMethod;
					ValueContainer value = row[i];
					if (pdOperator.manyToOne != null
							|| pdOperator.oneToOne != null) {
						long refId = value.l;
						if (refId != 0) {
							if (hasId != null) {
								entityRefs.add(refId, pdOperator, hasId);
							}
						}
					} else {
						pdOperator.setValue(hasId, value,
								columnDescriptor.type);
					}
				}
			}
			return loaded;
		}

		<T extends Entity> List<T> loadEntities() throws Exception {
			boolean ignorePropertyChanges = TransformManager.get()
					.isIgnorePropertyChanges();
			try {
				connection = getConnection();
				TransformManager.get().setIgnorePropertyChanges(true);
				List<T> result = (List<T>) (List<?>) loadHasIds();
				if (store.initialised || resolveRefs) {
					entityRefs.resolve();
				}
				if (store.initialised) {
					result.forEach(e -> store.index(e, true, null, true));
				}
				return result;
			} finally {
				releaseConn(connection);
				TransformManager.get()
						.setIgnorePropertyChanges(ignorePropertyChanges);
			}
		}

		// caller is responsible for connection acquire/release
		List<HasId> loadHasIds() throws Exception {
			List<HasId> result = load0();
			return result;
		}

		public Loader withClazz(Class clazz) {
			this.clazz = clazz;
			return this;
		}

		public Loader withConnection(Connection connection) {
			this.connection = connection;
			return this;
		}

		public Loader withEntityRefs(EntityRefs entityRefs) {
			this.entityRefs = entityRefs;
			return this;
		}

		public Loader withIgnoreDoubleCreationDueToFilter(
				boolean ignoreDoubleCreationDueToFilter) {
			this.ignoreDoubleCreationDueToFilter = ignoreDoubleCreationDueToFilter;
			return this;
		}

		public Loader withPopulateLazyPropertyValues(
				boolean populateLazyPropertyValues) {
			this.populateLazyPropertyValues = populateLazyPropertyValues;
			return this;
		}

		public Loader withResolveRefs(boolean resolveRefs) {
			this.resolveRefs = resolveRefs;
			return this;
		}

		public Loader withReturnResults(boolean returnResults) {
			this.returnResults = returnResults;
			return this;
		}

		public Loader withSqlFilter(String sqlFilter) {
			this.sqlFilter = sqlFilter;
			return this;
		}
	}

	class PdOperator {
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

		DomainStoreProperty domainStoreProperty;

		DomainStorePropertyLoadOracle lazyLoadOracle;

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
			this.domainStoreProperty = store.domainStoreProperties.get(clazz,
					pd.getName());
			if (this.domainStoreProperty != null && this.domainStoreProperty
					.loadType() == DomainStorePropertyLoadType.CUSTOM) {
				this.lazyLoadOracle = Registry
						.impl(this.domainStoreProperty.customLoadOracle());
			}
		}

		public Object get(Entity obj) {
			try {
				return field.get(obj);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		void set(HasId hasId, Object object) throws Exception {
			if (store.initialising) {
				field.set(hasId, object);
			} else {
				writeMethod.invoke(hasId, new Object[] { object });
			}
		}

		public void setValue(HasId hasId, ValueContainer value, Class<?> type)
				throws Exception {
			if (store.initialising) {
				if (type == long.class) {
					field.setLong(hasId, value.l);
					return;
				}
				if (type == double.class) {
					field.setDouble(hasId, value.d);
					return;
				}
				if (type == float.class) {
					field.setFloat(hasId, value.f);
					return;
				}
				if (type == boolean.class) {
					field.setBoolean(hasId, value.b);
					return;
				}
				if (type == int.class) {
					field.setInt(hasId, value.i);
					return;
				}
				field.set(hasId, value.o);
			} else {
				writeMethod.invoke(hasId, new Object[] { value.boxed(type) });
			}
		}

		@Override
		public String toString() {
			return Ax.format("%s.%s [%s]", clazz.getSimpleName(), name,
					readMethod.getReturnType().getSimpleName());
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

			Object resolveCustom(CustomResolver customResolver, Ref item) {
				return customResolver.resolveCustom(PdOperator.this, item);
			}
		}
	}

	public static class ValueContainer implements Serializable {
		boolean b;

		int i;

		float f;

		double d;

		long l;

		Object o;

		public Object boxed(Class<?> type) {
			if (type == long.class) {
				return l;
			} else if (type == double.class) {
				return d;
			} else if (type == float.class) {
				return f;
			} else if (type == boolean.class) {
				return b;
			} else if (type == int.class) {
				return i;
			} else {
				return o;
			}
		}

		static ValueContainer of(PdOperator op, Entity entity) {
			ValueContainer result = new ValueContainer();
			Object value = op.get(entity);
			Class<?> type = op.field.getType();
			result.unbox(type, value);
			return result;
		}

		void unbox(Class<?> type, Object value) {
			if (value instanceof Long) {
				l = (Long) value;
			} else if (value instanceof Double) {
				d = (Double) value;
			} else if (value instanceof Float) {
				f = (Float) value;
			} else if (value instanceof Boolean) {
				b = (Boolean) value;
			} else if (value instanceof Integer) {
				i = (Integer) value;
			} else if (value instanceof Entity) {
				l = ((Entity) value).getId();
			} else {
				o = value;
			}
		}

		@Override
		public String toString() {
			if (l != 0) {
				return "long: " + l;
			} else {
				return "(object)";
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
}
