package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.SortedMultimap;
import cc.alcina.framework.common.client.util.SystemoutCounter;
import cc.alcina.framework.entity.projection.EntityUtils.MultiIdentityMap;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;

public class StatsFilter extends CollectionProjectionFilter {
	MultiIdentityMap ownerMap = new MultiIdentityMap();

	MultiIdentityMap owneeMap = new MultiIdentityMap();

	private Set<Class> calculateOwnerStatsFor;

	private LinkedHashSet<Class> calculatePathStatsFor;

	IdentityHashMap<Object, Object> visited = new IdentityHashMap<Object, Object>();

	Multimap<Class, List<StatsItem>> statsClassLookup = new Multimap<Class, List<StatsItem>>();

	IdentityHashMap<Object, StatsItem> statsItemLookup = new IdentityHashMap<Object, StatsFilter.StatsItem>();

	Map<Class, Multiset<String, Set>> ownershipStats = new LinkedHashMap<Class, Multiset<String, Set>>();

	CountingMap<Class> nullInstanceMap = new CountingMap<Class>();

	Map<Class, Field[]> projectableFields = new HashMap<Class, Field[]>();

	private StatsFilterSortKey sortKey;

	private boolean reverse;

	private boolean bypassGwtTransient;

	private boolean dumpPaths;

	IdentityHashMap pathDumped = new IdentityHashMap<>();

	private boolean noPathToString;

	private boolean noPath;

	public StatsFilter() {
	}

	public StatsFilter bypassGwtTransient() {
		this.bypassGwtTransient = true;
		return this;
	}

	public StatsFilter dumpPaths() {
		dumpPaths = true;
		return this;
	}

	@Override
	public <T> T filterData(T original, T projected,
			GraphProjectionContext context, GraphProjection graphProjection)
			throws Exception {
		T filtered = super.filterData(original, projected, context,
				graphProjection);
		if (bypass(context.field)) {
			return null;
		}
		String toPath = noPath ? "..." : context.toPath(!noPathToString);
		visited.put(context.projectedOwner, toPath);
		visited.put(filtered, toPath);
		ownerMap.add(context.projectedOwner, filtered);
		ownerMap.ensureKey(filtered);
		owneeMap.add(filtered, context.projectedOwner);
		return filtered;
	}

	public void getGraphStats(Object source, Class[] calculateOwnerStatsFor,
			Class[] calculatePathStatsFor, StatsFilterSortKey sortKey,
			boolean reverse) {
		this.sortKey = sortKey;
		this.reverse = reverse;
		this.calculateOwnerStatsFor = new LinkedHashSet<Class>(
				Arrays.asList(calculateOwnerStatsFor));
		this.calculatePathStatsFor = new LinkedHashSet<Class>(
				Arrays.asList(calculatePathStatsFor));
		DetachedEntityCache cache = new DetachedEntityCache();
		try {
			GraphProjectionNoLisetNulls projection = new GraphProjectionNoLisetNulls(
					null, this);
			projection.project(source, null);
			System.err.println(projection.constructorExceptions);
			dumpStats();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public StatsFilter noPath() {
		noPath = true;
		return this;
	}

	public StatsFilter noPathToString() {
		noPathToString = true;
		return this;
	}

	private boolean bypass(Field field) {
		if (bypassGwtTransient) {
			if (field != null
					&& field.getAnnotation(GwtTransient.class) != null) {
				return true;
			}
		}
		return false;
	}

	private Field[] getFieldsForClass(Object projected) {
		Class<? extends Object> clazz = projected.getClass();
		if (!projectableFields.containsKey(clazz)) {
			List<Field> allFields = new ArrayList<Field>();
			Set<Field> dynamicPermissionFields = new HashSet<Field>();
			Class c = clazz;
			while (c != Object.class) {
				Field[] fields = c.getDeclaredFields();
				for (Field f : fields) {
					if (Modifier.isTransient(f.getModifiers())
							|| Modifier.isStatic(f.getModifiers())) {
						continue;
					}
					f.setAccessible(true);
					allFields.add(f);
				}
				c = c.getSuperclass();
			}
			projectableFields.put(clazz,
					(Field[]) allFields.toArray(new Field[allFields.size()]));
		}
		return projectableFields.get(clazz);
	}

	void dumpStats() {
		try {
			Set<Object> owned = new LinkedHashSet<Object>();
			SystemoutCounter counter = new SystemoutCounter(10000, 50,
					visited.size(), true);
			for (Object o : visited.keySet()) {
				if (o == null) {
					continue;
				}
				Class<? extends Object> clazz = o.getClass();
				StatsItem item = new StatsItem(o);
				statsClassLookup.add(clazz, item);
				statsItemLookup.put(o, item);
				Field[] fields = getFieldsForClass(o);
				for (Field field : fields) {
					if (bypass(field)) {
						continue;
					}
					Object o3 = field.get(o);
					if (o3 == null) {
						nullInstanceMap.add(field.getType());
						item.size++;
						continue;
					}
					Collection coll = new ArrayList();
					coll.add(o3);
					Set addedCollections = new LinkedHashSet();
					while (true) {
						int size = coll.size();
						LinkedHashSet add = new LinkedHashSet();
						for (Iterator i = coll.iterator(); i.hasNext();) {
							Object o2 = i.next();
							if (o2 instanceof Collection) {
								add.addAll((Collection) o2);
								addedCollections.add(o2);
								i.remove();
							} else if (o2 instanceof Map) {
								addedCollections.add(o2);
								add.addAll(((Map) o2).values());
								add.addAll(((Map) o2).keySet());
								i.remove();
							}
						}
						if (add.isEmpty()) {
							break;
						} else {
							coll.addAll(add);
						}
					}
					coll.addAll(addedCollections);
					for (Object o1 : coll) {
						if (o1 == null) {
							nullInstanceMap.add(field.getType());
							item.size++;
						} else {
							Class<? extends Object> clazz2 = o1.getClass();
							if (CommonUtils.stdAndPrimitives.contains(clazz2)) {
								statsClassLookup.add(clazz2, null);
								int length = o1.toString().length();
								if (length > 10000) {
									// System.out.format(
									// "large item string - %s - %s",
									// item.o, length);
									int j = 3;
								}
								item.size += length;
							} else {
								if (calculateOwnerStatsFor.contains(clazz2)) {
									if (owneeMap.get(o1) == null) {
										/*
										 * TODO - shouldn't // but something //
										 * odd re collection projection
										 */
										continue;
									}
									if (owneeMap.get(o1).size() == 1) {
										item.owned.add(o1);
										owned.add(o1);
									}
								}
							}
							if (calculatePathStatsFor.contains(clazz2)) {
								String key = GraphProjection.classSimpleName(
										clazz) + "." + field.getName();
								if (!ownershipStats.containsKey(clazz2)) {
									ownershipStats.put(clazz2,
											new Multiset<String, Set>());
								}
								ownershipStats.get(clazz2).add(key, o1);
								if (dumpPaths) {
									if (pathDumped.containsKey(o1)) {
									} else {
										pathDumped.put(o1, o1);
										System.out.println(
												visited.get(o1) + "::" + o1);
									}
								}
							}
						}
					}
				}
				counter.tick();
			}
			for (Object o : owned) {
				statsItemLookup.get(o).retainedElsewhere = true;
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		List<Class> keys = new ArrayList<Class>(statsClassLookup.keySet());
		final CountingMap<Class> retainedSizes = new CountingMap<Class>();
		final CountingMap<Class> rawSizes = new CountingMap<Class>();
		int objCount = 0;
		for (Class clazz : keys) {
			for (StatsItem item : statsClassLookup.get(clazz)) {
				if (item != null) {
					retainedSizes.add(clazz, item.retainedSize());
					rawSizes.add(clazz, item.size());
				}
			}
			objCount += statsClassLookup.size();
		}
		Collections.sort(keys, new Comparator<Class>() {
			@Override
			public int compare(Class o1, Class o2) {
				switch (sortKey) {
				case CLASSNAME:
					return o1.getSimpleName().compareTo(o2.getSimpleName());
				case RETAINED_SIZE:
					return CommonUtils.compareInts(
							CommonUtils.iv(retainedSizes.get(o1)),
							CommonUtils.iv(retainedSizes.get(o2)));
				case RAW_SIZE:
					return CommonUtils.compareInts(
							CommonUtils.iv(rawSizes.get(o1)),
							CommonUtils.iv(rawSizes.get(o2)));
				}
				return 0;
			}
		});
		if (reverse) {
			Collections.reverse(keys);
		}
		System.out.println("Graph stats dump\n----------");
		System.out.format(
				"%30s -- %10s -- %10s self -- %10s retained -- %10s \n", "Name",
				"Instances", "Size", "Retained", "Nulls");
		System.out.println(CommonUtils.padStringLeft("", 70, '-'));
		for (Class clazz : keys) {
			System.out.format(
					"%30s -- %10s -- %10s self -- %10s retained -- %10s \n",
					clazz.getSimpleName(), statsClassLookup.get(clazz).size(),
					rawSizes.countFor(clazz), retainedSizes.countFor(clazz),
					nullInstanceMap.countFor(clazz));
		}
		System.out.println(CommonUtils.padStringLeft("", 70, '-'));
		System.out.format(
				"%30s -- %10s -- %10s self -- %10s retained -- %10s \n",
				"Total", objCount, rawSizes.total(), retainedSizes.total(),
				nullInstanceMap.total());
		System.out.println("\n----------\n\n");
		for (Class clazz : keys) {
			if (ownershipStats.containsKey(clazz)) {
				System.out.println("Paths: " + clazz.getSimpleName());
				final Multiset<String, Set> cm = ownershipStats.get(clazz);
				List<String> sKeys = new ArrayList<String>(cm.keySet());
				Collections.sort(sKeys, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						return cm.getAndEnsure(o2).size()
								- cm.getAndEnsure(o1).size();
					}
				});
				for (String sk : sKeys) {
					System.out.format("%30s -- %10s\n", sk, cm.get(sk).size());
				}
			}
		}
		System.out.println("\n----------\n\n");
	}

	public enum StatsFilterSortKey {
		CLASSNAME, RAW_SIZE, RETAINED_SIZE
	}

	static class CountingMap<K> extends LinkedHashMap<K, Integer> {
		public void add(K key) {
			if (!containsKey(key)) {
				put(key, 1);
			} else {
				put(key, get(key) + 1);
			}
		}

		public void add(K key, int i) {
			if (!containsKey(key)) {
				put(key, i);
			} else {
				put(key, get(key) + i);
			}
		}

		public int countFor(K key) {
			if (!containsKey(key)) {
				return 0;
			}
			return get(key);
		}

		public K max() {
			K max = null;
			Integer maxCount = 0;
			for (K k : keySet()) {
				if (max == null) {
					max = k;
					maxCount = get(k);
				} else {
					if (get(k).compareTo(maxCount) > 0) {
						max = k;
						maxCount = get(k);
					}
				}
			}
			return max;
		}

		public SortedMultimap<Integer, List<K>> reverseMap(boolean descending) {
			SortedMultimap<Integer, List<K>> result = descending
					? new SortedMultimap<Integer, List<K>>(
							Collections.reverseOrder())
					: new SortedMultimap<Integer, List<K>>();
			for (K key : keySet()) {
				result.add(get(key), key);
			}
			return result;
		}

		public int size(K key) {
			if (!containsKey(key)) {
				return 0;
			}
			return get(key);
		}

		public int total() {
			int result = 0;
			for (Integer i : values()) {
				result += i;
			}
			return result;
		}
	}

	static class GraphProjectionNoLisetNulls extends GraphProjection {
		Set<Class> constructorExceptions = new LinkedHashSet<>();

		Map<Class, ObjectInstantiator> instantiators = new LinkedHashMap<>();

		Objenesis objenesis = new ObjenesisStd(); // or ObjenesisSerializer

		public GraphProjectionNoLisetNulls() {
			super();
		}

		public GraphProjectionNoLisetNulls(
				GraphProjectionFieldFilter fieldFilter,
				GraphProjectionDataFilter dataFilter) {
			super(fieldFilter, dataFilter);
		}

		// TODO - shouldn't this be package-private?
		@Override
		public Collection projectCollection(Collection coll,
				GraphProjectionContext context) throws Exception {
			Collection c = null;
			if (coll instanceof ArrayList || coll instanceof LinkedList) {
				c = coll.getClass().newInstance();
				// no "persistentLists", at least
				// um...persistentBag??
			} else if (coll instanceof Vector) {
				c = new Vector();
			} else if (coll instanceof List) {
				c = new ArrayList();
			} else if (coll instanceof LiSet) {
				c = new LiSet();
			} else if (coll instanceof Set) {
				c = new LinkedHashSet();
			} else if (coll instanceof ConcurrentLinkedQueue) {
				c = new ConcurrentLinkedQueue();
			}
			reached.put(coll, c == null ? NULL_MARKER : c);
			Iterator itr = coll.iterator();
			Object value;
			for (; itr.hasNext();) {
				value = itr.next();
				Object projected = project(value, context);
				if (projected == null && c instanceof LiSet) {
					continue;// why does this happen? one of those never 'ave
								// time bugs
				}
				if (value == null || projected != null) {
					c.add(projected);
				}
			}
			return c;
		}

		private <T> T tryWithObjenesis(Class sourceClass) {
			ObjectInstantiator objectInstantiator = instantiators
					.computeIfAbsent(sourceClass,
							sc -> objenesis.getInstantiatorOf(sc));
			return (T) objectInstantiator.newInstance();
		}

		@Override
		protected <T> T newInstance(Class sourceClass,
				GraphProjectionContext context) throws Exception {
			try {
				return super.newInstance(sourceClass, context);
			} catch (Exception e) {
				try {
					return tryWithObjenesis(sourceClass);
				} catch (Throwable e1) {
					constructorExceptions.add(sourceClass);
					return null;
				}
			}
		}
	}

	class StatsItem {
		int size;

		List<Object> owned = new ArrayList<Object>();

		Object o;

		public boolean retainedElsewhere;

		public StatsItem(Object o) {
			this.o = o;
		}

		public int retainedSize() {
			if (retainedElsewhere) {
				return 0;
			}
			int i = size;
			for (Object o : owned) {
				i += statsItemLookup.get(o).size;
			}
			return i;
		}

		public int size() {
			return size;
		}
	}
}