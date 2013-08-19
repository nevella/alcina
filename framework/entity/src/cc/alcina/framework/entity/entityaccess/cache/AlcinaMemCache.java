package cc.alcina.framework.entity.entityaccess.cache;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
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

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.PropertyPathFilter;
import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId.HiliHelper;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.HiliLocatorMap;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager.HiliLocator;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformRequestPersistence.DomainTransformRequestPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformRequestPersistence.DomainTransformRequestPersistenceListener;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.TransformPersister;
import cc.alcina.framework.entity.entityaccess.cache.CacheDescriptor.CacheTask;
import cc.alcina.framework.entity.entityaccess.cache.CacheDescriptor.PreProvideTask;
import cc.alcina.framework.entity.util.GraphProjection;

public class AlcinaMemCache {
	public static final String TOPIC_UPDATE_EXCEPTION = AlcinaMemCache.class
			.getName() + ".TOPIC_UPDATE_EXCEPTION";

	public static AlcinaMemCache get() {
		if (theInstance == null) {
			theInstance = new AlcinaMemCache();
		}
		return theInstance;
	}

	private Map<PropertyDescriptor, JoinTable> joinTables;

	private Map<Class, List<PropertyDescriptor>> descriptors;

	private UnsortedMultikeyMap<PropertyDescriptor> manyToOneRev;

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

	static List<String> ignoreNames = Arrays.asList(new String[0]);

	private static AlcinaMemCache theInstance;

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

	private AlcinaMemCache() {
		super();
		ThreadlocalTransformManager.resetThreadTransformManagerListenerDelta(
				resetListener, true);
		TransformPersister.persistingTransformsListenerDelta(
				persistingListener, true);
		persistenceListener = new MemCachePersistenceListener();
	}

	public void addValues(CacheListener listener) {
		for (Object o : cache.values(listener.getListenedClass())) {
			listener.insert((HasIdAndLocalId) o);
		}
	}

	public void appShutdown() {
		theInstance = null;
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

	public synchronized <T extends HasIdAndLocalId> T find(Class<T> clazz,
			long id) {
		return new AlcinaMemCacheQuery().id(id).find(clazz);
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz, String key,
			Object value) {
		return findOrCreate(clazz, key, value, false);
	}

	public <T extends HasIdAndLocalId> T findOrCreate(Class<T> clazz,
			String key, Object value, boolean createIfNonexistent) {
		T first = new AlcinaMemCacheQuery().filter(key, value).find(clazz);
		if (first == null) {
			first = (T) TransformManager.get()
					.createDomainObject((Class) clazz);
			CommonLocator.get().propertyAccessor()
					.setPropertyValue(first, key, value);
		}
		return first;
	}

	public Iterable<Object[]> getData(Class clazz, String sqlFilter) {
		return new ConnResults(conn, clazz, columnDescriptors.get(clazz),
				sqlFilter);
	}

	public synchronized Collection<Long> getIds(
			Class<? extends HasIdAndLocalId> clazz) {
		return new ArrayList<Long>(cache.keys(clazz));
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

	public void linkFromServletLayer() {
	}

	public synchronized <T extends HasIdAndLocalId> List<T> list(Class<T> clazz) {
		return new AlcinaMemCacheQuery().ids(getIds(clazz)).raw().list(clazz);
	}

	public synchronized <T extends HasIdAndLocalId> List<T> list(
			Class<T> clazz, Collection<Long> ids) {
		return new AlcinaMemCacheQuery().ids(ids).list(clazz);
	}

	public void loadTable(Class clazz, String sqlFilter) throws Exception {
		loadTable(clazz, sqlFilter, false);
	}

	public synchronized void loadTable(Class clazz, String sqlFilter,
			boolean resolveRefs) throws Exception {
		Iterable<Object[]> results = getData(clazz, sqlFilter);
		List<PropertyDescriptor> pds = descriptors.get(clazz);
		for (Object[] objects : results) {
			HasIdAndLocalId hili = (HasIdAndLocalId) clazz.newInstance();
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
		if (resolveRefs) {
			resolveRefs();
		}
	}

	public synchronized void resolveRefs() {
		MetricLogging.get().start("resolve");
		laterLookup.resolve();
		MetricLogging.get().end("resolve", metricLogger);
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

	public void warmup(Connection conn, CacheDescriptor cacheDescriptor) {
		this.conn = conn;
		this.cacheDescriptor = cacheDescriptor;
		try {
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
						cacheFilter.propertyValue);
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

	private synchronized void loadJoinTable(
			Entry<PropertyDescriptor, JoinTable> entry) {
		JoinTable joinTable = entry.getValue();
		if (joinTable == null) {
			return;
		}
		PropertyDescriptor pd = entry.getKey();
		// get reverse
		PropertyDescriptor rev = null;
		for (Entry<PropertyDescriptor, JoinTable> entry2 : joinTables
				.entrySet()) {
			ManyToMany m = entry2.getKey().getReadMethod()
					.getAnnotation(ManyToMany.class);
			if (entry2.getValue() == null
					&& m.targetEntity() == pd.getReadMethod()
							.getDeclaringClass()
					&& pd.getName().equals(m.mappedBy())) {
				rev = entry2.getKey();
				break;
			}
		}
		if (rev == null) {
			throw new RuntimeException("No reverse key for " + pd);
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
				HasIdAndLocalId src = (HasIdAndLocalId) cache.get(pd
						.getReadMethod().getDeclaringClass(), rs.getLong(1));
				HasIdAndLocalId tgt = (HasIdAndLocalId) cache.get(rev
						.getReadMethod().getDeclaringClass(), rs.getLong(2));
				assert src != null && tgt != null;
				laterLookup.add(tgt, pd, src);
				laterLookup.add(src, rev, tgt);
			}
			MetricLogging.get().end(joinTableName, metricLogger);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void prepareTable(CacheItemDescriptor descriptor) throws Exception {
		Class clazz = descriptor.clazz;
		List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>(
				Arrays.asList(Introspector.getBeanInfo(clazz)
						.getPropertyDescriptors()));
		PropertyDescriptor id = SEUtilities.descriptorByName(clazz, "id");
		pds.remove(id);
		pds.add(0, id);
		PropertyDescriptor result = null;
		List<PropertyDescriptor> mapped = new ArrayList<PropertyDescriptor>();
		descriptors.put(clazz, mapped);
		for (PropertyDescriptor pd : pds) {
			if (pd.getReadMethod() == null || pd.getWriteMethod() == null) {
				continue;
			}
			if (descriptor.getIgnoreNames().contains(pd.getName())) {
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
				addColumnName(clazz, pd,
						getTargetEntityType(pd.getReadMethod()));
			} else {
				addColumnName(clazz, pd, pd.getPropertyType());
			}
			mapped.add(pd);
		}
	}

	private HasIdAndLocalId resolveObject(DomainTransformEvent dte) {
		if (dte.getSource() != null
				&& dte.getSource().getClass() == dte.getObjectClass()
				&& ((HasIdAndLocalId) dte.getSource()).getId() == dte
						.getObjectId()) {
			return (HasIdAndLocalId) dte.getSource();
		}
		return transformManager.getObject(dte);
	}

	public <V extends HasIdAndLocalId> boolean isRawValue(V v) {
		V existing = (V) cache.get(v.getClass(), v.getId());
		return existing == v;
	}

	private void warmup0() throws Exception {
		transformManager = new SubgraphTransformManager();
		cache = transformManager.getDetachedEntityCache();
		joinTables = new LinkedHashMap<PropertyDescriptor, JoinTable>();
		descriptors = new LinkedHashMap<Class, List<PropertyDescriptor>>();
		manyToOneRev = new UnsortedMultikeyMap<PropertyDescriptor>(2);// class,
																		// pName
		columnDescriptors = new Multimap<Class, List<ColumnDescriptor>>();
		laterLookup = new LaterLookup();
		// get non-many-many obj
		MetricLogging.get().start("tables");
		for (CacheItemDescriptor descriptor : cacheDescriptor.perClass.values()) {
			Class clazz = descriptor.clazz;
			prepareTable(descriptor);
			MetricLogging.get().start(clazz.getSimpleName());
			if (!descriptor.lazy) {
				loadTable(clazz, "");
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
		MetricLogging.get().start("postLoad");
		for (CacheTask task : cacheDescriptor.postLoadTasks) {
			MetricLogging.get().start(task.getClass().getSimpleName());
			task.run(this);
			resolveRefs();
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
		MetricLogging.get().end("lookups");
	}

	<T extends HasIdAndLocalId> List<T> list(Class<T> clazz,
			AlcinaMemCacheQuery query) {
		Set<Long> ids = query.getIds();
		for (int i = 0; i < query.getFilters().size(); i++) {
			ids = getFiltered(clazz, query.getFilters().get(i),
					(i == 0 && ids.isEmpty()) ? null : ids);
		}
		List<T> raw = new ArrayList<T>(ids.size());
		for (Long id : ids) {
			T value = cache.get(clazz, id);
			if (value != null) {
				raw.add(value);
			}
		}
		try {
			for (PreProvideTask task : cacheDescriptor.preProvideTasks) {
				if (task.run(this, clazz, raw)) {
					resolveRefs();
				}
			}
			if (query.isRaw()) {
				return raw;
			}
			return new GraphProjection(query.getPermissionsFilter(),
					query.getDataFilter()).project(raw, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	synchronized void postProcess(
			DomainTransformRequestPersistenceEvent persistenceEvent,
			TransformPersistenceToken persistenceToken) {
		List<DomainTransformEvent> dtes = (List) persistenceEvent
				.getDomainTransformLayerWrapper().persistentEvents;
		List<DomainTransformEvent> filtered = filterInterestedTransforms(dtes);
		try {
			Multimap<HiliLocator, List<DomainTransformEvent>> perObjectTransforms = CollectionFilters
					.multimap(filtered, new DteToLocatorMapper());
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
				if (dte.getTransformType() != TransformType.CREATE_OBJECT
						&& first == dte) {
					HasIdAndLocalId obj = transformManager.getObject(dte);
					index(obj, false);
				}
				Object persistentLayerSource = dte.getSource();
				transformManager.consume(dte);
				if (dte.getTransformType() != TransformType.DELETE_OBJECT
						&& last == dte) {
					HasIdAndLocalId dbObj = resolveObject(dte);
					HasIdAndLocalId memCacheObj = transformManager
							.getObject(dte);
					if (dbObj instanceof HasVersionNumber) {
						updateVersionNumber(dbObj, dte);
					}
					if (dbObj instanceof IVersionable) {
						updateIVersionable(dbObj, persistentLayerSource);
					}
					index(memCacheObj, true);
				}
			}
		} catch (Exception e) {
			GlobalTopicPublisher.get().publishTopic(TOPIC_UPDATE_EXCEPTION, e);
			throw new WrappedRuntimeException(e);
		}
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

	private Date timestampToDate(Date date) {
		if (date instanceof Timestamp) {
			return new Date(((Timestamp) date).getTime());
		}
		return date;
	}

	private void updateVersionNumber(HasIdAndLocalId obj,
			DomainTransformEvent dte) {
		((HasVersionNumber) obj).setVersionNumber(((HasVersionNumber) dte
				.getSource()).getVersionNumber());
	}

	public class Transactional {
		public volatile int transactionCount;

		public <T> T find(Class<T> clazz, long id) {
			T t = cache.get(clazz, id);
			if (transactionActiveInCurrentThread()) {
				return (T) transactions.get().ensureTransactional(
						(HasIdAndLocalId) t);
			} else {
				return t;
			}
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
				transactions.get().end();
				transactions.remove();
				transactionCount--;
			}
		}

		public <T> Map<Long, T> lookup(Class<T> clazz) {
			return (Map<Long, T>) cache.getMap(clazz);
		}

		public <V extends HasIdAndLocalId> V resolveTransactional(
				CacheListener listener, V value, Object[] path) {
			PerThreadTransaction perThreadTransaction = transactions.get();
			if (perThreadTransaction == null) {
				return value;
			}
			return perThreadTransaction.getListenerValue(listener, value, path);
		}

		public boolean transactionsActive() {
			return transactionCount != 0;
		}

		public boolean transactionActiveInCurrentThread() {
			return transactionsActive() && transactions.get() != null;
		}

		public PerThreadTransaction ensureTransaction() {
			PerThreadTransaction transaction = transactions.get();
			if (transaction == null) {
				transaction = Registry.impl(PerThreadTransaction.class);
				transactions.set(transaction);
				transaction.start();
				transactionCount++;
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
	}

	static class ColumnDescriptor {
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
				return rs.wasNull() ? null : type.getEnumConstants()[eIdx];
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
					PropertyDescriptor pd = item.pd;
					Method rm = pd.getReadMethod();
					long id = item.id;
					if (joinTables.containsKey(pd)) {
						Set set = (Set) pd.getReadMethod().invoke(item.source,
								new Object[0]);
						if (set == null) {
							set = new LinkedHashSet();
							pd.getWriteMethod().invoke(item.source,
									new Object[] { set });
						}
						set.add(item.target);
					} else {
						Object target = cache.get(
								propertyDescriptorFetchTypes.get(pd), id);
						assert target != null;
						pd.getWriteMethod().invoke(item.source, target);
						PropertyDescriptor targetPd = manyToOneRev.get(
								item.source.getClass(), pd.getName());
						if (targetPd != null) {
							Set set = (Set) targetPd.getReadMethod().invoke(
									target, new Object[0]);
							if (set == null) {
								set = new LinkedHashSet();
								targetPd.getWriteMethod().invoke(target,
										new Object[] { set });
							}
							set.add(item.source);
						}
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
		}
	}

	class MemCachePersistenceListener implements
			DomainTransformRequestPersistenceListener {
		@Override
		public void onDomainTransformRequestPersistence(
				DomainTransformRequestPersistenceEvent evt) {
			TransformPersistenceToken persistenceToken = evt
					.getTransformPersistenceToken();
			switch (evt.getPersistenceEventType()) {
			case PRE_COMMIT:
				break;
			case COMMIT_ERROR:
				break;
			case COMMIT_OK:
				postProcess(evt, persistenceToken);
				break;
			}
		}
	}

	public static <V extends HasIdAndLocalId> V ensureTransactional(V value) {
		return get().transactional.ensureTransactional(value);
	}

	public void registerForTesting(HasIdAndLocalId hili) {
		if (!AppPersistenceBase.isTest()) {
			throw new RuntimeException("Only when testing...");
		}
		cache.put(hili);
		index(hili, true);
	}

	public static void ensureReferredPropertyIsTransactional(
			HasIdAndLocalId hili, String propertyName) {
		PropertyAccessor propertyAccessor = CommonLocator.get()
				.propertyAccessor();
		// target, even if new object, will still be equals() to old, so no
		// property change will be fired, which is the desired behaviour
		HasIdAndLocalId target = (HasIdAndLocalId) propertyAccessor
				.getPropertyValue(hili, propertyName);
		if (target != null) {
			target = ensureTransactional(target);
			propertyAccessor.setPropertyValue(hili, propertyName, target);
		}
	}
}
