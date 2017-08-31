/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity.projection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gwt.safehtml.shared.SafeHtml;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.GArrayList;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.HasReadPermission;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.ProjectByValue;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.DomainObjectCloner;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.NullWrappingMap;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.cache.MemCacheProxy;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter.AllFieldsFilter;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class GraphProjection {
	private static final int LOOKUP_SIZE = 1000;

	public static boolean replaceTimestampsWithDates = true;

	static Map<Field, Type> genericTypeLookup = new NullWrappingMap<Field, Type>(
			new ConcurrentHashMap(LOOKUP_SIZE));

	static Map<Field, Boolean> genericHiliTypeLookup = new NullWrappingMap<Field, Boolean>(
			new ConcurrentHashMap(LOOKUP_SIZE));

	static Map<Class, List<Field>> perClassDeclaredFields = new NullWrappingMap<Class, List<Field>>(
			new ConcurrentHashMap(LOOKUP_SIZE));

	static Map<Field, PropertyPermissions> propertyPermissionLookup = new NullWrappingMap<Field, PropertyPermissions>(
			new ConcurrentHashMap(LOOKUP_SIZE));

	static Map<Class, ConstructorMethod> constructorMethodsLookup = new LinkedHashMap<Class, GraphProjection.ConstructorMethod>(
			LOOKUP_SIZE);

	static Map<Class, Constructor> constructorLookup = new ConcurrentHashMap<Class, Constructor>(
			LOOKUP_SIZE);

	public static final String CONTEXT_REPLACE_MAP = GraphProjection.class
			+ ".CONTEXT_REPLACE_MAP";

	public static final String CONTEXT_DUMP_PROJECTION_STATS = GraphProjection.class
			+ ".CONTEXT_DUMP_PROJECTION_STATS";

	public static final String CONTEXT_DISABLE_PER_OBJECT_PERMISSIONS = GraphProjection.class
			+ ".CONTEXT_DISABLE_PER_OBJECT_PERMISSIONS";

	public static final String CONTEXT_PROJECTION_CONTEXT = GraphProjection.class
			.getName() + ".CONTEXT_PROJECTION_CONTEXT";

	public static final String TOPIC_PROJECTION_COUNT_DELTA = GraphProjection.class
			.getName() + ".TOPIC_PROJECTION_COUNT_DELTA";

	public static <T> T getContextObject(String key, Supplier<T> supplier) {
		Map ctx = LooseContext.get(CONTEXT_PROJECTION_CONTEXT);
		if (ctx == null) {
			return null;
		} else {
			T result = (T) ctx.get(key);
			if (result == null) {
				result = supplier.get();
				ctx.put(key, result);
			}
			return result;
		}
	}

	public static Type getGenericType(Field field) {
		if (!genericTypeLookup.containsKey(field)) {
			genericTypeLookup.put(field, field.getGenericType());
		}
		return genericTypeLookup.get(field);
	}

	public static boolean isEnumSubclass(Class c) {
		return c.getSuperclass() != null && c.getSuperclass().isEnum();
	}
	public static boolean isEnumOrEnumSubclass(Class c) {
        return c.isEnum()||isEnumSubclass(c);
    }

	public static boolean isGenericHiliType(Field field) {
		if (!genericHiliTypeLookup.containsKey(field)) {
			Type pt = getGenericType(field);
			boolean isHili = false;
			if (pt instanceof ParameterizedType) {
				Type genericType = ((ParameterizedType) pt)
						.getActualTypeArguments()[0];
				if (genericType instanceof Class) {
					Class type = (Class) genericType;
					isHili = HasIdAndLocalId.class.isAssignableFrom(type);
				}
			}
			genericHiliTypeLookup.put(field, isHili);
		}
		return genericHiliTypeLookup.get(field);
	}

	public static boolean isPrimitiveOrDataClass(Class c) {
		return c.isPrimitive() || c == String.class || c == Boolean.class
				|| c == Character.class || c.isEnum() || c == Class.class
				|| Number.class.isAssignableFrom(c)
				|| Date.class.isAssignableFrom(c) || isEnumSubclass(c)
				|| ProjectByValue.class.isAssignableFrom(c)
				|| SafeHtml.class.isAssignableFrom(c);
	}

	public static synchronized void registerConstructorMethods(
			List<? extends ConstructorMethod> methods) {
		for (ConstructorMethod constructorMethod : methods) {
			constructorMethodsLookup.put(constructorMethod.getReturnClass(),
					constructorMethod);
		}
	}

	public static <T> T shallowProjection(T original, int depth) {
		GraphProjections projections = GraphProjections.defaultProjections()
				.maxDepth(depth);
		return projections.project(original);
	}

	static PropertyPermissions getPropertyPermission(Field field) {
		if (!propertyPermissionLookup.containsKey(field)) {
			try {
				Method method = field.getDeclaringClass().getMethod(
						SEUtilities.getAccessorName(field), new Class[0]);
				propertyPermissionLookup.put(field,
						method.getAnnotation(PropertyPermissions.class));
			} catch (NoSuchMethodException nsme) {
				propertyPermissionLookup.put(field, null);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return propertyPermissionLookup.get(field);
	}

	private GraphProjectionDataFilter dataFilter;

	private GraphProjectionFieldFilter fieldFilter;

	public static Supplier<Map> reachedSupplier = () -> new IdentityHashMap(
			10 * LOOKUP_SIZE);

	protected Map reached = reachedSupplier.get();

	Map<Class, Permission> perClassReadPermission = new HashMap<Class, Permission>(
			LOOKUP_SIZE);

	Map<Field, PropertyPermissions> perFieldPermission = new LinkedHashMap<Field, PropertyPermissions>(
			LOOKUP_SIZE);

	Map<Class, Field[]> projectableFields = new HashMap<Class, Field[]>(
			LOOKUP_SIZE);

	Map<String, Set<Field>> perObjectPermissionFields = new HashMap<String, Set<Field>>(
			LOOKUP_SIZE);

	Map<Class, Boolean> perObjectPermissionClasses = new HashMap<Class, Boolean>(
			LOOKUP_SIZE);

	private int maxDepth = Integer.MAX_VALUE;

	private LinkedHashMap<HasIdAndLocalId, HasIdAndLocalId> replaceMap = null;

	private List<GraphProjectionContext> contexts = new ArrayList<GraphProjectionContext>();

	private boolean dumpProjectionStats;

	private CountingMap<String> contextStats = new CountingMap<String>();

	private boolean disablePerObjectPermissions;

	public GraphProjection() {
		this.dumpProjectionStats = LooseContext
				.is(CONTEXT_DUMP_PROJECTION_STATS);
		replaceMap = LooseContext.get(CONTEXT_REPLACE_MAP);
		this.disablePerObjectPermissions = LooseContext
				.is(CONTEXT_DISABLE_PER_OBJECT_PERMISSIONS);
	}

	public GraphProjection(GraphProjectionFieldFilter fieldFilter,
			GraphProjectionDataFilter dataFilter) {
		this();
		setFilters(fieldFilter, dataFilter);
	}

	public Field[] getFieldsForClass(Class clazz) throws Exception {
		Field[] result = projectableFields.get(clazz);
		if (result == null) {
			List<Field> allFields = new ArrayList<Field>();
			Set<Field> dynamicPermissionFields = new HashSet<Field>();
			Class c = clazz;
			while (c != Object.class) {
				List<Field> fields = ensureDeclaredNonStaticFields(c);
				for (Field field : fields) {
					if (fieldFilter != null) {
						if (Modifier.isTransient(field.getModifiers())
								&& !fieldFilter.permitTransient(field)) {
							continue;
						}
						if (!fieldFilter.permitField(field,
								dynamicPermissionFields, clazz)) {
							continue;
						}
					} else {
						if (Modifier.isTransient(field.getModifiers())) {
							continue;
						}
					}
					allFields.add(field);
				}
				c = c.getSuperclass();
			}
			result = (Field[]) allFields.toArray(new Field[allFields.size()]);
			projectableFields.put(clazz, result);
			perObjectPermissionFields.put(clazz.getName(),
					dynamicPermissionFields);
			for (Field field : dynamicPermissionFields) {
				PropertyPermissions pp = getPropertyPermission(field);
				perFieldPermission.put(field, pp);
			}
		}
		return result;
	}

	public Field[] getFieldsForClass(Object projected) throws Exception {
		Class<? extends Object> clazz = projected.getClass();
		return getFieldsForClass(clazz);
	}

	/**
	 * May want to pass the underlying field to filters, rather than accessor
	 * for ++performance
	 *
	 * @return the current portion of the source graph that has already been
	 *         reached in the traversal
	 */
	public Map getReached() {
		return reached;
	}

	/*
	 * if we have: a.b .equals c - but not a.b==c and we want to project c, not
	 * a.b - put c in this map
	 */
	public LinkedHashMap<HasIdAndLocalId, HasIdAndLocalId> getReplaceMap() {
		return this.replaceMap;
	}

	public <T> T project(T source, GraphProjectionContext context)
			throws Exception {
		if (context != null) {
			return project(source, null, context, false);
		} else {
			try {
				LooseContext.pushWithKey(CONTEXT_PROJECTION_CONTEXT,
						new LinkedHashMap<>());
				GlobalTopicPublisher.get()
						.publishTopic(TOPIC_PROJECTION_COUNT_DELTA, 1);
				start = System.nanoTime();
				return project(source, null, context, false);
			} finally {
				GlobalTopicPublisher.get()
						.publishTopic(TOPIC_PROJECTION_COUNT_DELTA, -1);
				LooseContext.pop();
				if (context == null) {
					if (dumpProjectionStats) {
						System.out.format(
								"Projection stats:\n===========\n%s\n",
								contextStats.reverseMap(true).entrySet()
										.stream()
										.map(e -> e.getKey() + ":"
												+ CommonUtils.join(e.getValue(),
														"\n\t: "))
										.collect(Collectors.joining("\t\n")));
					}
					if (ResourceUtilities.is(GraphProjection.class,
							"projectionMetrics")) {
						System.out.format(
								"Graph projection - %.3f ms - %s traversals %s creations\n",
								((double) (System.nanoTime() - start))
										/ 1000000,
								traversalCount, creationCount);
					}
				}
			}
		}
	}

	protected static final Object NULL_MARKER = new Object();

	boolean reachableBySinglePath(Class clazz) {
		if (clazz == Set.class || clazz == List.class
				|| clazz == ArrayList.class || clazz == Multimap.class
				|| clazz == UnsortedMultikeyMap.class) {
			return true;
		} else {
			return false;
		}
	}

	private int traversalCount = 0;

	private int creationCount = 0;

	private long start = 0;

	public <T> T project(T source, Object alsoMapTo,
			GraphProjectionContext context, boolean easysChecked)
			throws Exception {
		traversalCount++;
		if (source == null) {
			return null;
		}
		Class sourceClass = source.getClass();
		if (!easysChecked) {
			if (sourceClass == Timestamp.class && replaceTimestampsWithDates) {
				// actually breaks the (T) contract here - naughty
				// this is because the arithmetic involved in reconstructing
				// timestamps in a gwt js client
				// is expensive
				return (T) new Date(((Timestamp) source).getTime());
			}
			if (isPrimitiveOrDataClass(sourceClass)) {
				return source;
			}
			if (replaceMap != null && source instanceof HasIdAndLocalId
					&& replaceMap.containsKey(source)) {
				source = (T) replaceMap.get(source);
			}
			// check here unlikely to matter
			// if (!reachableBySinglePath) {
			Object projected = reached.get(source);
			if (projected != null) {
				if (projected == NULL_MARKER) {
					return null;
				} else {
					return (T) projected;
				}
			}
			// }
		}
		if (!checkObjectPermissions(source)) {
			return null;
		}
		creationCount++;
		if (dumpProjectionStats && context != null) {
			contextStats.add(context.toPoint());
		}
		T projected = sourceClass.isArray()
				? (T) Array.newInstance(sourceClass.getComponentType(),
						Array.getLength(source))
				: (T) ((source instanceof MemCacheProxy)
						? ((MemCacheProxy) source).nonProxy()
						: newInstance(sourceClass));
		boolean reachableBySinglePath = reachableBySinglePath(sourceClass);
		if (context == null || !reachableBySinglePath) {
			reached.put(source, projected == null ? NULL_MARKER : projected);
			if (alsoMapTo != null) {
				reached.put(alsoMapTo,
						projected == null ? NULL_MARKER : projected);
			}
		}
		if (dataFilter != null) {
			if (context == null) {
				context = new GraphProjectionContext();
				context.adopt(sourceClass, null, null, projected, source);
				contexts.add(context);
			}
			T replaceProjected = dataFilter.filterData(source, projected,
					context, this);
			if (replaceProjected != projected) {
				if (!reachableBySinglePath) {
					reached.put(source, replaceProjected == null ? NULL_MARKER
							: replaceProjected);
					if (alsoMapTo != null) {
						reached.put(alsoMapTo, replaceProjected == null
								? NULL_MARKER : replaceProjected);
					}
				}
				return replaceProjected;
			}
		}
		if (projected == null) {
			return projected;
		}
		if (context != null && context.depth >= maxDepth) {
			return projected;
		}
		if (source instanceof MemCacheProxy) {
			((MemCacheProxy) source).beforeProjection();
		}
		Field[] fields = getFieldsForClass(projected);
		Set<Field> checkFields = perObjectPermissionFields
				.get(projected.getClass().getName());
		for (Field field : fields) {
			if (checkFields.contains(field)) {
				if (!permitField(field, source)) {
					continue;
				}
			}
			Object value = field.get(source);
			if (value == null) {
				field.set(projected, null);
			} else {
				// the 10 or so lines are manual unwrapping trial
				Class fc = field.getType();
				if (fc == Timestamp.class && replaceTimestampsWithDates) {
					// actually breaks the (T) contract here - naughty
					// this is because the arithmetic involved in reconstructing
					// timestamps in a gwt js client
					// is expensive
					field.set(projected,
							(T) new Date(((Timestamp) value).getTime()));
					continue;
				}
				if (isPrimitiveOrDataClass(fc)) {
					field.set(projected, value);
					continue;
				}
				if (replaceMap != null && value instanceof HasIdAndLocalId
						&& replaceMap.containsKey(value)) {
					value = (T) replaceMap.get(value);
				}
				boolean fieldReachableBySinglePath = reachableBySinglePath(
						field.getType());
				if (!fieldReachableBySinglePath) {
					long nanoTime = System.nanoTime();
					Object projectedFieldValue = reached.get(value);
					if (projectedFieldValue != null) {
						if (projectedFieldValue == NULL_MARKER) {
							field.set(projected, null);
						} else {
							field.set(projected, projectedFieldValue);
						}
						continue;
					}
				}
				GraphProjectionContext childContext = null;
				if (context == null || context.depth() + 1 == contexts.size()) {
					childContext = new GraphProjectionContext();
					contexts.add(childContext);
				} else {
					childContext = contexts.get(context.depth() + 1);
				}
				childContext.adopt(sourceClass, field, context, projected,
						source);
				Object cv = project(value, null, childContext, true);
				field.set(projected, cv);
			}
		}
		return projected;
	}

	// TODO - shouldn't this be package-private?
	public Collection projectCollection(Collection coll,
			GraphProjectionContext context) throws Exception {
		Collection c = null;
		if (coll.getClass() == ArrayList.class) {
			c = new ArrayList();
		} else if (coll.getClass() == LinkedList.class) {
			c = new LinkedList();
			// no "persistentLists", at least
			// um...persistentBag??
		} else if (coll.getClass() == GArrayList.class) {
			c = new GArrayList();
			// no "persistentLists", at least
			// um...persistentBag??
		} else if (coll instanceof Vector) {
			c = new Vector();
		} else if (coll instanceof List) {
			c = new ArrayList();
		} else if (coll.getClass() == LiSet.class) {
			c = new LiSet();
		} else if (coll.getClass() == LightSet.class) {
			c = new LightSet();
		} else if (coll.getClass() == ConcurrentLinkedQueue.class) {
			c = new ConcurrentLinkedQueue();
		} else if (coll.getClass() == LinkedHashSet.class) {
			c = new LinkedHashSet();
		} else if (coll instanceof Set) {
			c = new LinkedHashSet();
		} else {
			throw new Exception(
					"Collection type not handled in projection path: "
							+ coll.getClass().getName());
		}
		// collections are assumed reachable by single path only
		// reached.put(coll, c == null ? NULL_MARKER : c);
		Iterator itr = coll.iterator();
		Object value;
		for (; itr.hasNext();) {
			value = itr.next();
			Object projected = project(value, context);
			if (value == null || projected != null) {
				if (dataFilter.projectIntoCollection(value, projected,
						context)) {
					c.add(projected);
				}
			}
		}
		return c;
	}

	public void setFilters(GraphProjectionFieldFilter fieldFilter,
			GraphProjectionDataFilter dataFilter) {
		this.fieldFilter = fieldFilter;
		this.dataFilter = dataFilter;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public void setReached(IdentityHashMap reached) {
		this.reached = reached;
	}

	public void setReplaceMap(
			LinkedHashMap<HasIdAndLocalId, HasIdAndLocalId> replaceMap) {
		this.replaceMap = replaceMap;
	}

	private List<Field> ensureDeclaredNonStaticFields(Class c) {
		if (!perClassDeclaredFields.containsKey(c)) {
			Field[] fields = c.getDeclaredFields();
			List<Field> nonStatic = new ArrayList<Field>();
			for (Field field : fields) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				} else {
					field.setAccessible(true);
					nonStatic.add(field);
				}
			}
			perClassDeclaredFields.put(c, nonStatic);
		}
		return perClassDeclaredFields.get(c);
	}

	private Permission
			ensurePerClassReadPermission(Class<? extends Object> sourceClass) {
		if (!perClassReadPermission.containsKey(sourceClass)) {
			ObjectPermissions annotation = sourceClass
					.getAnnotation(ObjectPermissions.class);
			perClassReadPermission.put(sourceClass,
					annotation == null ? null : annotation.read());
		}
		return perClassReadPermission.get(sourceClass);
	}

	private boolean permitField(Field field, Object source) throws Exception {
		PropertyPermissions pp = perFieldPermission.get(field);
		if (pp != null) {
			AnnotatedPermissible ap = new AnnotatedPermissible(pp.read());
			return PermissionsManager.get().isPermissible(source, ap);
		}
		return false;
	}

	protected <T> T newInstance(Class sourceClass) throws Exception {
		if (constructorMethodsLookup.containsKey(sourceClass)) {
			return (T) constructorMethodsLookup.get(sourceClass).newInstance();
		}
		if (!constructorLookup.containsKey(sourceClass)) {
			Constructor ctor = sourceClass.getConstructor(new Class[] {});
			ctor.setAccessible(true);
			constructorLookup.put(sourceClass, ctor);
		}
		if (dumpProjectionStats) {
			System.out.println("missing constructor - " + sourceClass);
		}
		return (T) constructorLookup.get(sourceClass)
				.newInstance(new Object[] {});
	}

	boolean checkObjectPermissions(Object source) {
		if (source instanceof HasReadPermission
				&& !dataFilter.ignoreObjectHasReadPermissionCheck()) {
			return ((HasReadPermission) source).canRead();
		}
		Class<? extends Object> sourceClass = source.getClass();
		if (!perObjectPermissionClasses.containsKey(sourceClass)) {
			Boolean result = fieldFilter == null ? new Boolean(true)
					: fieldFilter.permitClass(sourceClass);
			perObjectPermissionClasses.put(sourceClass, result);
		}
		if (disablePerObjectPermissions) {
			return true;
		}
		Boolean valid = perObjectPermissionClasses.get(sourceClass);
		if (valid == null) {// per-objected
			Permission permission = ensurePerClassReadPermission(sourceClass);
			if (permission == null) {
				return true;
			} else {
				AnnotatedPermissible ap = new AnnotatedPermissible(permission);
				return PermissionsManager.get().isPermissible(source, ap);
			}
		}
		return valid;
	}

	public static interface ConstructorMethod<T> {
		Class<T> getReturnClass();

		T newInstance();
	}

	public static class GraphProjectionContext {
		static int debugDepth = 200;

		public GraphProjectionContext parent;

		public Object projectedOwner;

		public String fieldName;

		public Class clazz;

		public Field field;

		public Object sourceOwner;

		private int depth;

		public GraphProjectionContext() {
		}

		public void adopt(Class clazz, Field field,
				GraphProjectionContext parent, Object projectedOwner,
				Object sourceOwner) {
			this.clazz = clazz;
			this.field = field;
			this.sourceOwner = sourceOwner;
			this.fieldName = field == null ? "" : field.getName();
			this.parent = parent;
			this.projectedOwner = projectedOwner;
			this.depth = parent == null ? 0 : parent.depth + 1;
			if (depth() > debugDepth) {
				int debug = 0;
			}
		}

		public int depth() {
			return depth;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof GraphProjectionContext) {
				GraphProjectionContext o2 = (GraphProjectionContext) obj;
				return o2.clazz == clazz && o2.fieldName.equals(fieldName);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return clazz.hashCode() ^ fieldName.hashCode();
		}

		public String toPoint() {
			String point = field == null ? clazz.getSimpleName()
					: field.getType().getSimpleName() + ": "
							+ clazz.getSimpleName() + "." + fieldName;
			return point;
		}

		@Override
		public String toString() {
			return (parent == null ? "" : parent.toString() + "::")
					+ clazz.getSimpleName() + "." + fieldName;
		}

		public String toPath(boolean withToString) {
			String string = "?";
			if (withToString) {
				sourceOwner.toString();
				if (string.contains("@")
						&& string.contains(sourceOwner.getClass().getName())) {
					string = "@" + sourceOwner.hashCode();
				}
			}
			return (parent == null ? "" : parent.toPath(withToString) + "::")
					+ clazz.getSimpleName() + "/" + string + "." + fieldName;
		}
	}

	public static interface GraphProjectionDataFilter {
		/*
		 * IMPORTANT - if filterdata changes the return value (i.e. doesn't
		 * return a value === projected) it must immediately (on new object
		 * instantiation) register the new value in graphProjection.reached
		 */
		<T> T filterData(T original, T projected,
				GraphProjectionContext context, GraphProjection graphProjection)
				throws Exception;

		default boolean ignoreObjectHasReadPermissionCheck() {
			return false;
		}

		<T> boolean projectIntoCollection(T value, T projected,
				GraphProjectionContext context);
	}

	public static interface GraphProjectionDualFilter
			extends GraphProjectionFieldFilter, GraphProjectionDataFilter {
	}

	public static interface GraphProjectionFieldFilter {
		public abstract Boolean permitClass(Class clazz);

		boolean permitField(Field field, Set<Field> perObjectPermissionFields,
				Class clazz);

		boolean permitTransient(Field field);
	}

	public interface InstantiateImplCallback<T> {
		boolean instantiateLazyInitializer(T initializer,
				GraphProjectionContext context);
	}

	public interface InstantiateImplCallbackWithShellObject<T>
			extends InstantiateImplCallback<T> {
		boolean instantiateLazyInitializer(T initializer,
				GraphProjectionContext context);

		Object instantiateShellObject(T initializer,
				GraphProjectionContext context);
	}

	public static String generateFieldwiseEqualString(Class clazz)
			throws Exception {
		List<String> fieldNames = new ArrayList<>();
		GraphProjection graphProjection = new GraphProjection(
				new AllFieldsFilter(), null);
		for (Field field : graphProjection.getFieldsForClass(clazz)) {
			String name = field.getName();
			if (DomainObjectCloner.IGNORE_FOR_DOMAIN_OBJECT_CLONING
					.contains(name)) {
				continue;
			}
			fieldNames.add(name);
		}
		String template = "@Override\npublic boolean equivalentTo(%s o) {\nreturn CommonUtils.equals(%s);\n}";
		return String.format(template, clazz.getSimpleName(),
				fieldNames.stream().map(n -> String.format("%s, o.%s ", n, n))
						.collect(Collectors.joining(", ")));
	}

	public static String generateFieldwiseEquivalenceHash(Class clazz)
			throws Exception {
		List<String> fieldNames = new ArrayList<>();
		GraphProjection graphProjection = new GraphProjection(
				new AllFieldsFilter(), null);
		for (Field field : graphProjection.getFieldsForClass(clazz)) {
			String name = field.getName();
			if (DomainObjectCloner.IGNORE_FOR_DOMAIN_OBJECT_CLONING
					.contains(name)) {
				continue;
			}
			fieldNames.add(name);
		}
		String template = "@Override\npublic int equivalenceHash() {\nreturn Objects.hash(%s);\n}";
		return String.format(template,
				fieldNames.stream().collect(Collectors.joining(", ")));
	}

	public static String generateFieldwiseToString(Class clazz)
			throws Exception {
		GraphProjection graphProjection = new GraphProjection(
				new AllFieldsFilter(), null);
		StringBuilder lineParts = new StringBuilder();
		for (Field field : graphProjection.getFieldsForClass(clazz)) {
			String name = field.getName();
			if (DomainObjectCloner.IGNORE_FOR_DOMAIN_OBJECT_CLONING
					.contains(name)) {
				continue;
			}
			lineParts.append(String.format("sb.append(\"%s\");\n", name));
			lineParts.append("sb.append(\":\");\n");
			lineParts.append(String.format("sb.append(%s);\n", name));
			lineParts.append("sb.append(\"\\n\");\n");
		}
		String template = "@Override\npublic String toString() {\n"
				+ "StringBuilder sb = new StringBuilder();\n%s\n"
				+ "return sb.toString();\n}";
		return String.format(template, lineParts);
	}

	public static String fieldwiseToString(Object obj) {
		return fieldwiseToString(obj, true, 999);
	}

	public static String fieldwiseToString(Object obj, boolean withTypes,
			int maxLen, String... excludeFields) {
		try {
			List<String> fieldNames = new ArrayList<>();
			GraphProjection graphProjection = new GraphProjection(
					new AllFieldsFilter(), null);
			StringBuilder sb = new StringBuilder();
			List<String> excludeList = Arrays.asList(excludeFields);
			for (Field field : graphProjection
					.getFieldsForClass(obj.getClass())) {
				String name = field.getName();
				if (excludeList.contains(name)) {
					continue;
				}
				if (withTypes) {
					sb.append(field.getType().getSimpleName());
					sb.append("/");
				}
				sb.append(name);
				if (maxLen < 100 && !withTypes) {
					sb.append(CommonUtils.padStringLeft("", 18 - name.length(),
							" "));
				}
				sb.append(": ");
				String str = CommonUtils.nullSafeToString(field.get(obj));
				str = CommonUtils.trimToWsChars(str, maxLen, true);
				sb.append(str);
				sb.append("\n");
			}
			return sb.toString();
		} catch (Exception e) {
			return "Exception: " + e.getMessage();
		}
	}
}
