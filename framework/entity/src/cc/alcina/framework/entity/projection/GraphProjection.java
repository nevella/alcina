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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.safehtml.shared.SafeHtml;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.GArrayList;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.GraphProjectionTransient;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.HasReadPermission;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.ProjectByValue;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
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
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccAccess;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccAccess.MvccAccessType;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccObject;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter.AllFieldsFilter;
import cc.alcina.framework.entity.util.CachingConcurrentMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class GraphProjection {
	public static final int LOOKUP_SIZE = 1000;

	public static boolean replaceTimestampsWithDates = true;

	static Map<Field, Type> genericTypeLookup = new NullWrappingMap<Field, Type>(
			new ConcurrentHashMap(LOOKUP_SIZE));

	static CachingConcurrentMap<Class, String> classSimpleName = new CachingConcurrentMap<>(
			clazz -> {
				synchronized (clazz) {
					// prevent concurrent simpleName get
					String name = clazz.getSimpleName();
					return name;
				}
			}, LOOKUP_SIZE);

	static Map<Field, Boolean> genericEntityTypeLookup = new NullWrappingMap<Field, Boolean>(
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

	private static final String CONTEXT_LAST_CONTEXT_LOOKUPS = GraphProjection.class
			.getName() + ".CONTEXT_LAST_CONTEXT_LOOKUPS";

	public static final String TOPIC_PROJECTION_COUNT_DELTA = GraphProjection.class
			.getName() + ".TOPIC_PROJECTION_COUNT_DELTA";

	protected static final Object NULL_MARKER = new Object();

	static GraphProjection fieldwiseEqualityProjection = new GraphProjection(
			new AllFieldsFilter(), null);

	public static String classSimpleName(Class clazz) {
		if (clazz == null) {
			return "(null)";
		}
		try {
			return classSimpleName.get(clazz);
		} catch (Exception e) {
			synchronized (classSimpleName) {
				return clazz.getSimpleName();
			}
			// strange concurrency issues have occurred...maybe reload?
		}
	}

	public static String fieldwiseToString(Object obj) {
		return fieldwiseToString(obj, true, false, 999);
	}

	public static String fieldwiseToString(Object obj, boolean withTypes,
			boolean oneLine, int maxLen, String... excludeFields) {
		try {
			List<String> fieldNames = new ArrayList<>();
			GraphProjection graphProjection = fieldwiseEqualityProjection;
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
				sb.append(oneLine ? " " : "\n");
			}
			return sb.toString();
		} catch (Exception e) {
			return "Exception: " + e.getMessage();
		}
	}

	public static String fieldwiseToStringOneLine(Object obj) {
		return fieldwiseToString(obj, false, true, 999);
	}

	public static String generateFieldwiseEqualString(Class clazz)
			throws Exception {
		List<String> fieldNames = new ArrayList<>();
		GraphProjection graphProjection = fieldwiseEqualityProjection;
		for (Field field : graphProjection.getFieldsForClass(clazz)) {
			String name = field.getName();
			if (DomainObjectCloner.IGNORE_FOR_DOMAIN_OBJECT_CLONING
					.contains(name)) {
				continue;
			}
			fieldNames.add(name);
		}
		String template = "@Override\npublic boolean equals(Object obj) {\n"
				+ "if(obj instanceof %s){%s o = (%s)obj;return CommonUtils.equals(%s);\n}else{return false;}}";
		return String.format(template, clazz.getSimpleName(),
				clazz.getSimpleName(), clazz.getSimpleName(),
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

	public static String generateFieldwiseEquivalentString(Class clazz)
			throws Exception {
		List<String> fieldNames = new ArrayList<>();
		GraphProjection graphProjection = fieldwiseEqualityProjection;
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

	public static String generateFieldwiseHashCode(Class clazz)
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
		String template = "@Override\npublic int hashCode() {\nreturn Objects.hash(%s);\n}";
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

	public static boolean isEnumOrEnumSubclass(Class c) {
		return c.isEnum() || isEnumSubclass(c);
	}

	public static boolean isEnumSubclass(Class c) {
		return c.getSuperclass() != null && c.getSuperclass().isEnum();
	}

	public static boolean isGenericEntityType(Field field) {
		if (!genericEntityTypeLookup.containsKey(field)) {
			Type pt = getGenericType(field);
			boolean isEntity = false;
			if (pt instanceof ParameterizedType) {
				Type genericType = ((ParameterizedType) pt)
						.getActualTypeArguments()[0];
				if (genericType instanceof Class) {
					Class type = (Class) genericType;
					isEntity = Entity.class.isAssignableFrom(type);
				}
			}
			genericEntityTypeLookup.put(field, isEntity);
		}
		return genericEntityTypeLookup.get(field);
	}

	public static boolean isPrimitiveOrDataClass(Class c) {
		return c.isPrimitive() || c == String.class || c == Boolean.class
				|| c == Character.class || c.isEnum() || c == Class.class
				|| Number.class.isAssignableFrom(c)
				|| Date.class.isAssignableFrom(c) || isEnumOrEnumSubclass(c)
				|| ProjectByValue.class.isAssignableFrom(c)
				|| SafeHtml.class.isAssignableFrom(c);
	}

	public static <T> T maxDepthProjection(T t, int depth,
			GraphProjectionFieldFilter fieldFilter) {
		CollectionProjectionFilterWithCache dataFilter = new CollectionProjectionFilterWithCache();
		GraphProjections projections = GraphProjections.defaultProjections()
				.dataFilter(dataFilter).maxDepth(depth);
		if (fieldFilter != null) {
			projections.fieldFilter(fieldFilter);
		}
		T result = projections.project(t);
		return result;
	}

	public static boolean nonTransientFieldwiseEqual(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.getClass() != o2.getClass()) {
			return false;
		}
		try {
			for (Field field : fieldwiseEqualityProjection
					.getFieldsForClass(o1.getClass())) {
				Object v1 = field.get(o1);
				Object v2 = field.get(o2);
				if (!Objects.equals(v1, v2)) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static int nonTransientFieldwiseHash(Object o1) {
		try {
			StringBuilder sb = new StringBuilder();
			int hash = 0;
			for (Field field : fieldwiseEqualityProjection
					.getFieldsForClass(o1.getClass())) {
				Object v1 = field.get(o1);
				hash ^= Objects.hashCode(v1);
			}
			return hash;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static synchronized void registerConstructorMethods(
			List<? extends ConstructorMethod> methods) {
		for (ConstructorMethod constructorMethod : methods) {
			constructorMethodsLookup.put(constructorMethod.getReturnClass(),
					constructorMethod);
		}
	}

	public static void reuseLookups(boolean reuse) {
		if (reuse) {
			LooseContext.set(CONTEXT_LAST_CONTEXT_LOOKUPS,
					new GraphProjection());
		} else {
			LooseContext.remove(CONTEXT_LAST_CONTEXT_LOOKUPS);
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

	private String contextDebugPath;

	protected Map reached = new ProjectionIdentityMap();

	Map<Class, Permission> perClassReadPermission = new HashMap<Class, Permission>(
			LOOKUP_SIZE);

	Map<Field, PropertyPermissions> perFieldPermission = new LinkedHashMap<Field, PropertyPermissions>(
			LOOKUP_SIZE);

	Map<Class, List<Field>> projectableFields = new HashMap<Class, List<Field>>(
			LOOKUP_SIZE);

	Map<Class, List<Field>> projectablePrimitiveOrDataFields = new HashMap<Class, List<Field>>(
			LOOKUP_SIZE);

	Map<Class, List<Field>> projectableNonPrimitiveOrDataFields = new HashMap<Class, List<Field>>(
			LOOKUP_SIZE);

	Map<String, Set<Field>> perObjectPermissionFields = new HashMap<String, Set<Field>>(
			LOOKUP_SIZE);

	Map<Class, Boolean> perObjectPermissionClasses = new HashMap<Class, Boolean>(
			LOOKUP_SIZE);

	private int maxDepth = Integer.MAX_VALUE;

	/*
	 * This denotes 'check reachability of maps and collection subclasses which
	 * do not return true for 'reachableBySinglePath()' - i.e. doesn't cause a
	 * check of Set/Multimap/Multikeymap - so is low-cost
	 */
	private boolean collectionReachedCheck = true;

	private LinkedHashMap<Entity, Entity> replaceMap = null;

	private List<GraphProjectionContext> contexts = new ArrayList<GraphProjectionContext>();

	private boolean dumpProjectionStats;

	private CountingMap<String> contextStats = new CountingMap<String>();

	private boolean disablePerObjectPermissions;

	private int traversalCount = 0;

	private int creationCount = 0;

	private long start = 0;

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

	public List<Field> getFieldsForClass(Class clazz) throws Exception {
		return getFieldsForClass(clazz, true);
	}

	public List<Field> getFieldsForClass(Class clazz, boolean forProjection)
			throws Exception {
		List<Field> result = projectableFields.get(clazz);
		if (result == null) {
			result = new ArrayList<Field>();
			Set<Field> dynamicPermissionFields = new HashSet<Field>();
			Class c = clazz;
			while (c != Object.class) {
				List<Field> fields = ensureDeclaredNonStaticFields(c);
				for (Field field : fields) {
					if (forProjection && field.getAnnotation(
							GraphProjectionTransient.class) != null) {
						continue;
					}
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
					// special-case mvcc fields
					PropertyDescriptor propertyDescriptor = SEUtilities
							.getPropertyDescriptorByName(c, field.getName());
					if (propertyDescriptor != null
							&& propertyDescriptor.getReadMethod() != null) {
						MvccAccess mvccAccess = propertyDescriptor
								.getReadMethod()
								.getAnnotation(MvccAccess.class);
						if (mvccAccess != null && mvccAccess
								.type() == MvccAccessType.TRANSACTIONAL_ACCESS_NOT_SUPPORTED) {
							continue;
						}
					}
					result.add(field);
				}
				c = c.getSuperclass();
			}
			projectableFields.put(clazz, result);
			// string for faster lookup, i think (class.hashcode slower?
			// native?)
			perObjectPermissionFields.put(clazz.getName(),
					dynamicPermissionFields);
			for (Field field : dynamicPermissionFields) {
				PropertyPermissions pp = getPropertyPermission(field);
				perFieldPermission.put(field, pp);
			}
		}
		return result;
	}

	/*
	 * if we have: a.b .equals c - but not a.b==c and we want to project c, not
	 * a.b - put c in this map
	 */
	public LinkedHashMap<Entity, Entity> getReplaceMap() {
		return this.replaceMap;
	}

	public boolean isCollectionReachedCheck() {
		return this.collectionReachedCheck;
	}

	public <T> T project(T source, GraphProjectionContext context)
			throws Exception {
		if (context != null) {
			return project(source, null, context, false);
		} else {
			GraphProjection last = LooseContext
					.get(CONTEXT_LAST_CONTEXT_LOOKUPS);
			try {
				if (last != null) {
					perClassReadPermission = last.perClassReadPermission;
					perFieldPermission = last.perFieldPermission;
					perObjectPermissionClasses = last.perObjectPermissionClasses;
					perObjectPermissionFields = last.perObjectPermissionFields;
					projectableFields = last.projectableFields;
				}
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
				if (last != null) {
					LooseContext.set(CONTEXT_LAST_CONTEXT_LOOKUPS, this);
				}
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

	public <T> T project(T source, T projected, GraphProjectionContext context,
			boolean easysChecked) throws Exception {
		traversalCount++;
		if (source == null) {
			return null;
		}
		if (context != null) {
			if (contextDebugPath != null) {
				if (context.toString().contains(contextDebugPath)) {
					int debug = 4;
				}
			}
		}
		Class sourceClass = source.getClass();
		boolean checkReachable = false;
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
			if (replaceMap != null && source instanceof Entity
					&& replaceMap.containsKey(source)) {
				source = (T) replaceMap.get(source);
				sourceClass = source.getClass();
			}
			checkReachable = checkReachable(source);
			// check here unlikely to matter
			// if (!reachableBySinglePath) {
			if (checkReachable) {
				Object reachedInstance = reached.get(source);
				if (reachedInstance != null) {
					if (projected != null && projected != reachedInstance) {
					} else {
						if (reachedInstance == NULL_MARKER) {
							return null;
						} else {
							return (T) reachedInstance;
						}
					}
				}
			}
			// }
		} else {
			checkReachable = checkReachable(source);
		}
		if (!checkObjectPermissions(source)) {
			return null;
		}
		creationCount++;
		if (dumpProjectionStats && context != null) {
			contextStats.add(context.toPoint());
		}
		if (projected == null) {
			if (sourceClass.isArray()) {
				projected = (T) Array.newInstance(
						sourceClass.getComponentType(),
						Array.getLength(source));
			} else if (source instanceof MvccObject) {
				projected = newInstance(((Entity) source).entityClass(),
						context);
			} else {
				projected = newInstance(sourceClass, context);
			}
		}
		boolean reachableBySinglePath = reachableBySinglePath(sourceClass);
		if ((context == null || !reachableBySinglePath) && checkReachable) {
			reached.put(source, projected == null ? NULL_MARKER : projected);
		}
		if (dataFilter != null) {
			if (context == null) {
				context = new GraphProjectionContext();
				context.adopt(sourceClass, null, null, projected, source);
				contexts.add(context);
			}
			// FIXME - mvcc.4 - why can't replaceprojected just sub for initial
			// projected? and what is 'alsomapto'? actually there's a reason -
			// may switch (A->B) to (A'->B')
			// so the dataFilter should handle its own projection
			T replaceProjected = dataFilter.filterData(source, projected,
					context, this);
			if (replaceProjected != projected) {
				if (!reachableBySinglePath && checkReachable) {
					reached.put(source, replaceProjected == null ? NULL_MARKER
							: replaceProjected);
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
		List<Field> primitiveOrDataFieldsForClass = getPrimitiveOrDataFieldsForClass(
				projected.getClass());
		/*
		 * Force previous statement to evaluate before getting "checkFields"
		 * value
		 */
		Preconditions.checkArgument(primitiveOrDataFieldsForClass.size() >= 0);
		Set<Field> checkFields = perObjectPermissionFields
				.get(projected.getClass().getName());
		// primitive/data before non - to ensure recursively reached collections
		// are ok
		for (Field field : primitiveOrDataFieldsForClass) {
			if (projected == source) {
				continue;
			}
			if (checkFields.contains(field)) {
				if (!permitField(field, source)) {
					continue;
				}
			}
			Object value = getFieldValue(field, source);
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
				field.set(projected, value);
			}
		}
		List<Field> nonPrimitiveOrDataFieldsForSource = getNonPrimitiveOrDataFieldsForClass(
				source.getClass());
		boolean checkFieldExistsInSource = !(source.getClass() == projected
				.getClass()
				|| source.getClass().getSuperclass() == projected.getClass());
		for (Field field : getNonPrimitiveOrDataFieldsForClass(
				projected.getClass())) {
			if (checkFields.contains(field)) {
				if (!permitField(field, source)) {
					continue;
				}
			}
			if (checkFieldExistsInSource) {
				if (!nonPrimitiveOrDataFieldsForSource.contains(field)) {
					continue;
				}
			}
			Object value = getFieldValue(field, source);
			if (value == null) {
				field.set(projected, null);
			} else {
				if (replaceMap != null && value instanceof Entity
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

	public <E> E registerProjected(E source, E projected) {
		return (E) reached.put(source, projected);
	}

	public void setCollectionReachedCheck(boolean collectionReachedCheck) {
		this.collectionReachedCheck = collectionReachedCheck;
	}

	public void setFilters(GraphProjectionFieldFilter fieldFilter,
			GraphProjectionDataFilter dataFilter) {
		this.fieldFilter = fieldFilter;
		this.dataFilter = dataFilter;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public void setReplaceMap(LinkedHashMap<Entity, Entity> replaceMap) {
		this.replaceMap = replaceMap;
	}

	private boolean checkReachable(Object source) {
		if (!collectionReachedCheck
				&& (source instanceof Collection || source instanceof Map)) {
			return false;
		}
		return true;
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

	private Object getFieldValue(Field field, Object source) throws Exception {
		// Trying hard to avoid the first case (it's very much not optimal)
		if (source instanceof MvccObject
				&& ((MvccObject) source).__getMvccVersions__() != null) {
			return SEUtilities.getPropertyValue(source, field.getName());
		} else {
			return field.get(source);
		}
	}

	private List<Field> getNonPrimitiveOrDataFieldsForClass(Class clazz)
			throws Exception {
		List<Field> result = projectableNonPrimitiveOrDataFields.get(clazz);
		if (result == null) {
			result = getFieldsForClass(clazz).stream()
					.filter(f -> !isPrimitiveOrDataClass(f.getType()))
					.collect(Collectors.toList());
			projectableNonPrimitiveOrDataFields.put(clazz, result);
		}
		return result;
	}

	private List<Field> getPrimitiveOrDataFieldsForClass(Class clazz)
			throws Exception {
		List<Field> result = projectablePrimitiveOrDataFields.get(clazz);
		if (result == null) {
			result = getFieldsForClass(clazz).stream()
					.filter(f -> isPrimitiveOrDataClass(f.getType()))
					.collect(Collectors.toList());
			projectablePrimitiveOrDataFields.put(clazz, result);
		}
		return result;
	}

	private boolean permitField(Field field, Object source) throws Exception {
		PropertyPermissions pp = perFieldPermission.get(field);
		if (pp != null) {
			AnnotatedPermissible ap = new AnnotatedPermissible(pp.read());
			return PermissionsManager.get().isPermissible(source, ap);
		}
		return false;
	}

	protected <T> T newInstance(Class sourceClass,
			GraphProjectionContext context) throws Exception {
		if (constructorMethodsLookup.containsKey(sourceClass)) {
			return (T) constructorMethodsLookup.get(sourceClass).newInstance();
		}
		if (!constructorLookup.containsKey(sourceClass)) {
			try {
				if (sourceClass.getName().equals(
						"java.util.Collections$UnmodifiableRandomAccessList")) {
					return (T) new ArrayList();
				}
				Constructor ctor = sourceClass.getConstructor(new Class[] {});
				ctor.setAccessible(true);
				constructorLookup.put(sourceClass, ctor);
				if (dumpProjectionStats) {
					System.out.println("missing constructor - " + sourceClass);
				}
			} catch (Exception e) {
				Ax.sysLogHigh("missing no-args constructor:\n\t%s\n\t%s  ",
						sourceClass, context);
				throw e;
			}
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

	boolean reachableBySinglePath(Class clazz) {
		if (clazz == Set.class || clazz == List.class
				|| clazz == ArrayList.class || clazz == Multimap.class
				|| clazz == UnsortedMultikeyMap.class) {
			return true;
		} else {
			return false;
		}
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
			this.clazz = Domain.resolveEntityClass(clazz);
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

		public Optional<Object> parent(Predicate predicate) {
			if (predicate.test(sourceOwner)) {
				return Optional.of(sourceOwner);
			}
			if (parent != null) {
				return parent.parent(predicate);
			}
			return Optional.empty();
		}

		public String toPath(boolean withToString) {
			String string = "?";
			if (withToString) {
				if (sourceOwner instanceof Entity) {
					string = ((Entity) sourceOwner).toStringEntity();
				} else if (sourceOwner instanceof Collection) {
					string = Ax.format("[%s]",
							((Collection) sourceOwner).size());
				} else {
					string = sourceOwner.toString();
				}
				if (string.contains("@")
						&& string.contains(sourceOwner.getClass().getName())) {
					string = "@" + sourceOwner.hashCode();
				}
			}
			return (parent == null ? "" : parent.toPath(withToString) + "::")
					+ GraphProjection.classSimpleName(clazz) + "/" + string
					+ "." + fieldName;
		}

		public String toPoint() {
			String point = field == null
					? GraphProjection.classSimpleName(clazz)
					: GraphProjection.classSimpleName(field.getType()) + ": "
							+ GraphProjection.classSimpleName(clazz) + "."
							+ fieldName;
			return point;
		}

		@Override
		public String toString() {
			return (parent == null ? "" : parent.toString() + "::")
					+ clazz.getSimpleName() + "." + fieldName;
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
		@Override
		boolean instantiateLazyInitializer(T initializer,
				GraphProjectionContext context);

		Object instantiateShellObject(T initializer,
				GraphProjectionContext context);
	}

	/*
	 * Trying to avoid System.identityHashCode - which doesn't always seem so
	 * fast. Also, the interplay with object monitor metadata in the object
	 * header (JVM-dependent) might be slowing us down.
	 */
	private static class ProjectionIdentityMap extends AbstractMap {
		ClassIdKey queryKey = new ClassIdKey();

		private Map<ClassIdKey, Object> entities = new Object2ObjectOpenHashMap<>(
				4 * LOOKUP_SIZE);

		private Map nonEntities = new Reference2ReferenceOpenHashMap<Object, Object>(
				100);

		JPAImplementation jpaImplementation = Registry
				.implOrNull(JPAImplementation.class);

		@Override
		public Set entrySet() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(Object key) {
			if (key instanceof Entity) {
				Entity entity = (Entity) key;
				if (useNonEntityMap(entity)) {
					return nonEntities.get(key);
				} else {
					queryKey.id = entity.getId();
					queryKey.clazzName = entity.entityClass().getName();
					return entities.get(queryKey);
				}
			} else {
				return nonEntities.get(key);
			}
		}

		@Override
		public Object put(Object key, Object value) {
			if (key instanceof Entity) {
				Entity entityKey = (Entity) key;
				if (useNonEntityMap(entityKey)) {
					nonEntities.put(key, value);
				} else {
					entities.put(new ClassIdKey(entityKey.getId(),
							entityKey.entityClass()), value);
				}
				return null;
			} else {
				nonEntities.put(key, value);
				return null;
			}
		}

		boolean useNonEntityMap(Entity entity) {
			return entity.getId() == 0 || (jpaImplementation != null
					&& jpaImplementation.isProxy(entity));
		}

		static class ClassIdKey {
			long id;

			String clazzName;

			ClassIdKey() {
			}

			ClassIdKey(long id, Class<? extends Entity> clazz) {
				this.id = id;
				this.clazzName = clazz.getName();
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof ClassIdKey) {
					ClassIdKey o = (ClassIdKey) obj;
					// reference equality for strings ok
					return o.id == id && o.clazzName == clazzName;
				} else {
					return false;
				}
			}

			@Override
			public int hashCode() {
				return ((int) id) ^ clazzName.hashCode();
			}
		}
	}
}
