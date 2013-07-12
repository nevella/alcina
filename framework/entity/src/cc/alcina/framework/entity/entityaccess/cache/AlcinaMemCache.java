package cc.alcina.framework.entity.entityaccess.cache;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import javax.persistence.EntityManager;
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
import cc.alcina.framework.common.client.collections.PropertyFilter;
import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LookupMapToMap;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformRequestPersistence.DomainTransformRequestPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformRequestPersistence.DomainTransformRequestPersistenceListener;
import cc.alcina.framework.entity.entityaccess.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.CacheDescriptor.CacheTask;
import cc.alcina.framework.entity.entityaccess.cache.CacheDescriptor.PreProvideTask;
import cc.alcina.framework.entity.util.GraphProjection;
import cc.alcina.framework.entity.util.GraphProjection.CollectionProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.GraphProjectionFilter;
import cc.alcina.framework.entity.util.GraphProjection.PermissibleFieldFilter;

public class AlcinaMemCache {
	public static final String TOPIC_UPDATE_EXCEPTION = AlcinaMemCache.class
			.getName() + ".TOPIC_UPDATE_EXCEPTION";

	 class ConnResults implements Iterable<Object[]> {
		ConnResultsIterator itr = new ConnResultsIterator();

		private Connection conn;

		private List<ColumnDescriptor> columnDescriptors;

		private Class clazz;

		private String sqlFilter;

		public ConnResults(Connection conn, Class clazz,
				List<ColumnDescriptor> columnDescriptors, String sqlFilter) {
			this.conn = conn;
			this.clazz = clazz;
			this.columnDescriptors = columnDescriptors;
			this.sqlFilter = sqlFilter;
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
		}

		ResultSet rs = null;

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
	}

	private Map<PropertyDescriptor, JoinTable> joinTables;

	private Map<Class, List<PropertyDescriptor>> descriptors;

	private LookupMapToMap<PropertyDescriptor> manyToOneRev;

	private Connection conn;

	private Multimap<Class, List<ColumnDescriptor>> columnDescriptors;

	private Map<PropertyDescriptor, Class> propertyDescriptorFetchTypes = new LinkedHashMap<PropertyDescriptor, Class>();

	private Map<CacheLookupDescriptor, CacheLookup> indicies = new LinkedHashMap<CacheLookupDescriptor, CacheLookup>();

	private LaterLookup laterLookup;

	private SubgraphTransformManager transformManager;

	private CacheDescriptor cacheDescriptor;

	private TaggedLogger sqlLogger = Registry.impl(TaggedLoggers.class)
			.getLogger(AlcinaMemCache.class, TaggedLogger.DEBUG);
	
	private TaggedLogger metricLogger = Registry.impl(TaggedLoggers.class)
			.getLogger(AlcinaMemCache.class, TaggedLogger.METRIC);

	private AlcinaMemCache() {
		super();
		persistenceListener = new MemCachePersistenceListener();
	}

	public void linkFromServletLayer() {
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
				java.sql.Date v = rs.getDate(idx);
				return v == null ? null : new Date(v.getTime());
			}
			if (Enum.class.isAssignableFrom(type)) {
				int eIdx = rs.getInt(idx);
				return rs.wasNull() ? null : type.getEnumConstants()[eIdx];
			}
			throw new RuntimeException("Unhandled rs type: "
					+ type.getSimpleName());
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
	}

	public void warmup(EntityManager em, Connection conn,
			CacheDescriptor cacheDescriptor) {
		this.conn = conn;
		this.cacheDescriptor = cacheDescriptor;
		try {
			warmup0(em);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	class LaterLookup {
		List<LaterItem> items = new ArrayList<AlcinaMemCache.LaterLookup.LaterItem>(
				8000000);

		void add(long id, PropertyDescriptor pd, HasIdAndLocalId source) {
			items.add(new LaterItem(id, pd, source));
		}

		void add(HasIdAndLocalId target, PropertyDescriptor pd,
				HasIdAndLocalId source) {
			items.add(new LaterItem(target, pd, source));
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

			public LaterItem(long id, PropertyDescriptor pd,
					HasIdAndLocalId source) {
				this.id = id;
				this.pd = pd;
				this.source = source;
			}

			public LaterItem(HasIdAndLocalId target, PropertyDescriptor pd,
					HasIdAndLocalId source) {
				this.target = target;
				this.pd = pd;
				this.source = source;
			}
		}
	}

	private void warmup0(EntityManager em) throws Exception {
		transformManager = new SubgraphTransformManager();
		cache = transformManager.getDetachedEntityCache();
		joinTables = new LinkedHashMap<PropertyDescriptor, JoinTable>();
		descriptors = new LinkedHashMap<Class, List<PropertyDescriptor>>();
		manyToOneRev = new LookupMapToMap<PropertyDescriptor>(2);// class, pName
		columnDescriptors = new Multimap<Class, List<ColumnDescriptor>>();
		laterLookup = new LaterLookup();
		// get non-many-many obj
		MetricLogging.get().start("tables");
		for (CacheItemDescriptor descriptor : cacheDescriptor.perClass.values()) {
			Class clazz = descriptor.clazz;
			prepareTable(em, clazz);
			MetricLogging.get().start(clazz.getSimpleName());
			if (!descriptor.lazy) {
				loadTable(em, clazz, "");
			}
			MetricLogging.get().end(clazz.getSimpleName(),metricLogger);
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
			task.run(this, em);
			MetricLogging.get().end(task.getClass().getSimpleName(),metricLogger);
		}
		MetricLogging.get().end("postLoad");
		MetricLogging.get().start("lookups");
		for (CacheItemDescriptor descriptor : cacheDescriptor.perClass.values()) {
			for (CacheLookupDescriptor lookupDescriptor : descriptor.lookupDescriptors) {
				mapLookup(lookupDescriptor);
			}
		}
		MetricLogging.get().end("lookups");
	}

	private void mapLookup(CacheLookupDescriptor lookupDescriptor) {
		CacheLookup cacheLookup = new CacheLookup(lookupDescriptor);
		indicies.put(lookupDescriptor, cacheLookup);
		for (Object o : cache.values(lookupDescriptor.clazz)) {
			updateLookup((HasIdAndLocalId) o, lookupDescriptor, cacheLookup);
		}
	}

	private void updateLookup(HasIdAndLocalId hili,
			CacheLookupDescriptor lookupDescriptor, CacheLookup cacheLookup) {
		Object v1 = CommonLocator.get().propertyAccessor()
				.getPropertyValue(hili, lookupDescriptor.fieldName1);
		cacheLookup.add(v1, hili.getId());
	}

	public void resolveRefs() {
		MetricLogging.get().start("resolve");
		laterLookup.resolve();
		MetricLogging.get().end("resolve");
	}

	public void loadTable(EntityManager em, Class clazz, String sqlFilter)
			throws Exception {
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
	}

	public Iterable<Object[]> getData(Class clazz, String sqlFilter) {
		return new ConnResults(conn, clazz, columnDescriptors.get(clazz),
				sqlFilter);
	}

	private void loadJoinTable(Entry<PropertyDescriptor, JoinTable> entry) {
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
			MetricLogging.get().end(joinTableName,metricLogger);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	List<String> ignoreNames = Arrays.asList(new String[] { "creationUser",
			"creationDate", "lastModificationDate", "lastModificationUser" });

	private void prepareTable(EntityManager em, Class clazz) throws Exception {
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
			if (ignoreNames.contains(pd.getName())) {
				continue;
			}
			Method rm = pd.getReadMethod();
			if (rm.getAnnotation(Transient.class) != null) {
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
			if (manyToMany != null) {
				JoinTable joinTable = rm.getAnnotation(JoinTable.class);
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

	private void addColumnName(Class clazz, PropertyDescriptor pd,
			Class propertyType) {
		columnDescriptors.add(clazz, new ColumnDescriptor(pd, propertyType));
		propertyDescriptorFetchTypes.put(pd, propertyType);
	}

	private static AlcinaMemCache theInstance;

	public static AlcinaMemCache get() {
		if (theInstance == null) {
			theInstance = new AlcinaMemCache();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	DetachedEntityCache cache;

	private MemCachePersistenceListener persistenceListener;

	class InSubgraphFilter implements CollectionFilter<DomainTransformEvent> {
		@Override
		public boolean allow(DomainTransformEvent o) {
			if (!cacheDescriptor.cachePostTransform(o.getObjectClass())) {
				return false;
			}
			switch (o.getTransformType()) {
			case ADD_REF_TO_COLLECTION:
			case REMOVE_REF_FROM_COLLECTION:
			case NULL_PROPERTY_REF:
			case CHANGE_PROPERTY_REF:
				return cacheDescriptor.cachePostTransform(o.getValueClass());
			}
			return true;
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

	public MemCachePersistenceListener getPersistenceListener() {
		return this.persistenceListener;
	}

	public synchronized <T extends HasIdAndLocalId> T getObject(Class<T> clazz,
			long id) {
		return getObjects(clazz, Collections.singletonList(id)).get(0);
	}

	public synchronized Collection<Long> getIds(
			Class<? extends HasIdAndLocalId> clazz) {
		return new ArrayList<Long>(cache.keys(clazz));
	}

	public synchronized <T extends HasIdAndLocalId> List<T> getObjects(
			Class<T> clazz, List<CacheFilter> filters) {
		Set<Long> ids = new LinkedHashSet<Long>();
		for (int i = 0; i < filters.size(); i++) {
			ids = getFiltered(clazz, filters.get(i), i == 0 ? null : ids);
		}
		return getObjects(clazz, ids, null);
	}

	public synchronized <T extends HasIdAndLocalId> T getObject(Class<T> clazz,
			List<CacheFilter> filters) {
		return CommonUtils.first(getObjects(clazz, filters));
	}

	private Set<Long> getFiltered(final Class clazz, CacheFilter cacheFilter,
			Set<Long> existing) {
		CacheLookup lookup = getLookupFor(clazz, cacheFilter.propertyName);
		if (lookup != null) {
			Set<Long> set = lookup.get(cacheFilter.propertyValue);
			return (Set<Long>) (existing == null ? set : CommonUtils
					.intersection(existing, set));
		}
		final CollectionFilter filter = cacheFilter.collectionFilter != null ? cacheFilter.collectionFilter
				: new PropertyFilter(cacheFilter.propertyName,
						cacheFilter.propertyValue);
		if (existing == null) {
			return HiliHelper.toIdSet(CollectionFilters.filter(
					cache.rawValues(clazz), filter));
		} else {
			CollectionFilter withIdFilter = new CollectionFilter<Long>() {
				@Override
				public boolean allow(Long id) {
					return filter.allow(cache.get(clazz, id));
				}
			};
			CollectionFilters.filterInPlace(existing, withIdFilter);
			return existing;
		}
	}

	private CacheLookup getLookupFor(Class clazz, String propertyName) {
		for (CacheLookupDescriptor descriptor : indicies.keySet()) {
			if (descriptor.handles(clazz, propertyName)) {
				return indicies.get(descriptor);
			}
		}
		return null;
	}

	public synchronized <T extends HasIdAndLocalId> List<T> getObjects(
			Class<T> clazz, Collection<Long> ids) {
		return getObjects(clazz, ids, null);
	}

	public synchronized <T extends HasIdAndLocalId> List<T> getObjects(
			Class<T> clazz) {
		return getObjects(clazz, getIds(clazz), null);
	}

	public synchronized <T extends HasIdAndLocalId> List<T> getObjects(
			Class<T> clazz, Collection<Long> ids, GraphProjectionFilter filter) {
		List<T> raw = new ArrayList<T>(ids.size());
		for (Long id : ids) {
			raw.add(cache.get(clazz, id));
		}
		try {
			for (PreProvideTask task : cacheDescriptor.preProvideTasks) {
				task.run(this, clazz, raw);
			}
			filter = filter != null ? filter : new CollectionProjectionFilter();
			return new GraphProjection(new PermissibleFieldFilter(), filter)
					.project(raw, null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	synchronized void postProcess(DomainTransformRequestPersistenceEvent evt,
			TransformPersistenceToken persistenceToken) {
		DomainTransformRequest request = evt.getTransformPersistenceToken()
				.getRequest();
		List<DomainTransformEvent> dtes = request.allTransforms();
		List<DomainTransformEvent> filtered = CollectionFilters.filter(dtes,
				new InSubgraphFilter());
		try {
			for (DomainTransformEvent dte : filtered) {
				if (dte.getObjectId() == 0 && dte.getObjectLocalId() != 0) {
					dte.setObjectId(TransformManager.get().getObject(dte)
							.getId());
					dte.setObjectLocalId(0);
				}
				if (dte.getValueId() == 0 && dte.getValueLocalId() != 0) {
					dte.setValueId(TransformManager
							.get()
							.getObject(dte.getValueClass(), 0,
									dte.getValueLocalId()).getId());
					dte.setValueLocalId(0);
					dte.setNewValue(null);// force a lookup
				}
				transformManager.consume(dte);
			}
		} catch (Exception e) {
			GlobalTopicPublisher.get().publishTopic(TOPIC_UPDATE_EXCEPTION, e);
			throw new WrappedRuntimeException(e);
		}
	}

	public boolean isCached(Class clazz) {
		return cacheDescriptor.perClass.containsKey(clazz);
	}

	public <T> T getItemByKeyValue(Class<T> clazz, String key, Object value,
			boolean createIfNonexistent) {
		T first = CollectionFilters.first(cache.values(clazz), key, value);
		if (first == null) {
			first = (T) TransformManager.get()
					.createDomainObject((Class) clazz);
			CommonLocator.get().propertyAccessor()
					.setPropertyValue(first, key, value);
		}
		return first;
	}

	public <T extends HasIdAndLocalId> T find(Class<T> clazz, Object... kvs) {
		return getObject(clazz, CacheFilter.fromKvs(kvs));
	}
}
