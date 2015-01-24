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

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
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
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.NullWrappingMap;
import cc.alcina.framework.entity.SEUtilities;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class GraphProjection {
	public static Type getGenericType(Field field) {
		if (!genericTypeLookup.containsKey(field)) {
			genericTypeLookup.put(field, field.getGenericType());
		}
		return genericTypeLookup.get(field);
	}

	public static boolean isEnumSubclass(Class c) {
		return c.getSuperclass() != null && c.getSuperclass().isEnum();
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
				|| Date.class.isAssignableFrom(c) || isEnumSubclass(c);
	}

	public static synchronized void registerConstructorMethods(
			List<? extends ConstructorMethod> methods) {
		for (ConstructorMethod constructorMethod : methods) {
			constructorMethodsLookup.put(constructorMethod.getReturnClass(),
					constructorMethod);
		}
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

	public static boolean replaceTimestampsWithDates = true;

	protected IdentityHashMap reached = new IdentityHashMap();

	Map<Class, Field[]> projectableFields = new HashMap<Class, Field[]>();

	Map<Class, Set<Field>> perObjectPermissionFields = new HashMap<Class, Set<Field>>();

	Map<Class, Boolean> perObjectPermissionClasses = new HashMap<Class, Boolean>();

	static Map<Field, Type> genericTypeLookup = new NullWrappingMap<Field, Type>(
			new ConcurrentHashMap());

	static Map<Field, Boolean> genericHiliTypeLookup = new NullWrappingMap<Field, Boolean>(
			new ConcurrentHashMap());

	static Map<Class, Permission> perClassReadPermission = new NullWrappingMap<Class, Permission>(
			new ConcurrentHashMap());

	static Map<Field, PropertyPermissions> propertyPermissionLookup = new NullWrappingMap<Field, PropertyPermissions>(
			new ConcurrentHashMap());

	static Map<Class, ConstructorMethod> constructorMethodsLookup = new LinkedHashMap<Class, GraphProjection.ConstructorMethod>();

	static Map<Class, Constructor> constructorLookup = new ConcurrentHashMap<Class, Constructor>();

	public static final String CONTEXT_REPLACE_MAP = GraphProjection.class
			+ ".CONTEXT_REPLACE_MAP";

	Map<Field, PropertyPermissions> perFieldPermission = new LinkedHashMap<Field, PropertyPermissions>();

	private int maxDepth = Integer.MAX_VALUE;

	private LinkedHashMap<Object, Object> replaceMap = null;

	public GraphProjection() {
		replaceMap = LooseContext.get(CONTEXT_REPLACE_MAP);
	}

	public GraphProjection(GraphProjectionFieldFilter fieldFilter,
			GraphProjectionDataFilter dataFilter) {
		this();
		setFilters(fieldFilter, dataFilter);
	}

	public Field[] getFieldsForClass(Object projected) throws Exception {
		Class<? extends Object> clazz = projected.getClass();
		Field[] result = projectableFields.get(clazz);
		if (result == null) {
			List<Field> allFields = new ArrayList<Field>();
			Set<Field> dynamicPermissionFields = new HashSet<Field>();
			Class c = clazz;
			while (c != Object.class) {
				Field[] fields = c.getDeclaredFields();
				for (Field field : fields) {
					if (Modifier.isStatic(field.getModifiers())) {
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
					field.setAccessible(true);
					allFields.add(field);
				}
				c = c.getSuperclass();
			}
			result = (Field[]) allFields.toArray(new Field[allFields.size()]);
			projectableFields.put(clazz, result);
			perObjectPermissionFields.put(clazz, dynamicPermissionFields);
			for (Field field : dynamicPermissionFields) {
				PropertyPermissions pp = getPropertyPermission(field);
				perFieldPermission.put(field, pp);
			}
		}
		return result;
	}

	/**
	 * May want to pass the underlying field to filters, rather than accessor
	 * for ++performance
	 *
	 * @return the current portion of the source graph that has already been
	 *         reached in the traversal
	 */
	public IdentityHashMap getReached() {
		return reached;
	}

	/*
	 * if we have: a.b .equals c - but not a.b==c and we want to project c, not
	 * a.b - put c in this map
	 */
	public LinkedHashMap<Object, Object> getReplaceMap() {
		return this.replaceMap;
	}

	public <T> T project(T source, GraphProjectionContext context)
			throws Exception {
		return project(source, null, context);
	}

	public <T> T project(T source, Object alsoMapTo,
			GraphProjectionContext context) throws Exception {
		if (source == null) {
			return null;
		}
		if (replaceMap != null && replaceMap.containsKey(source)) {
			source = (T) replaceMap.get(source);
		}
		Class c = source.getClass();
		if (c == Timestamp.class && replaceTimestampsWithDates) {
			// actually breaks the (T) contract here - naughty
			// this is because the arithmetic involved in reconstructing
			// timestamps in a gwt js client
			// is expensive
			return (T) new Date(((Timestamp) source).getTime());
		}
		if (isPrimitiveOrDataClass(c)) {
			return source;
		}
		if (reached.containsKey(source)) {
			return (T) reached.get(source);
		}
		Class<? extends Object> sourceClass = source.getClass();
		if (!checkObjectPermissions(source)) {
			return null;
		}
		T projected = sourceClass.isArray() ? (T) Array.newInstance(
				sourceClass.getComponentType(), Array.getLength(source))
				: (T) newInstance(sourceClass);
		reached.put(source, projected);
		if (alsoMapTo != null) {
			reached.put(alsoMapTo, projected);
		}
		if (dataFilter != null) {
			if (context == null) {
				context = new GraphProjectionContext(c, null, null, projected,
						source);
			}
			T replaceProjected = dataFilter.filterData(source, projected,
					context, this);
			if (replaceProjected != projected) {
				reached.put(source, replaceProjected);
				if (alsoMapTo != null) {
					reached.put(alsoMapTo, replaceProjected);
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
		Field[] fields = getFieldsForClass(projected);
		Set<Field> checkFields = perObjectPermissionFields.get(projected
				.getClass());
		for (Field field : fields) {
			Object value = field.get(source);
			if (checkFields.contains(field)) {
				if (!permitField(field, source)) {
					continue;
				}
			}
			GraphProjectionContext childContext = new GraphProjectionContext(c,
					field, context, projected, source);
			Object cv = project(value, childContext);
			field.set(projected, cv);
		}
		return projected;
	}

	// TODO - shouldn't this be package-private?
	public Collection projectCollection(Collection coll,
			GraphProjectionContext context) throws Exception {
		Collection c = null;
		if (coll instanceof ArrayList || coll instanceof LinkedList) {
			c = coll.getClass().newInstance();
			// no "persistentLists", at least
			// um...persistentBag??
		} else if (coll instanceof List) {
			c = new ArrayList();
		} else if (coll instanceof LiSet) {
			c = new LiSet();
		} else if (coll instanceof Set) {
			c = new LinkedHashSet();
		}
		reached.put(coll, c);
		Iterator itr = coll.iterator();
		Object value;
		for (; itr.hasNext();) {
			value = itr.next();
			Object projected = project(value, context);
			if (value == null || projected != null) {
				if (dataFilter.projectIntoCollection(value, projected, context)) {
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

	public void setReplaceMap(LinkedHashMap<Object, Object> replaceMap) {
		this.replaceMap = replaceMap;
	}

	private Permission ensurePerClassReadPermission(
			Class<? extends Object> sourceClass) {
		if (!perClassReadPermission.containsKey(sourceClass)) {
			ObjectPermissions annotation = sourceClass
					.getAnnotation(ObjectPermissions.class);
			perClassReadPermission.put(sourceClass, annotation == null ? null
					: annotation.read());
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
		return (T) constructorLookup.get(sourceClass).newInstance(
				new Object[] {});
	}

	boolean checkObjectPermissions(Object source) {
		Class<? extends Object> sourceClass = source.getClass();
		if (!perObjectPermissionClasses.containsKey(sourceClass)) {
			Boolean result = fieldFilter == null ? new Boolean(true)
					: fieldFilter.permitClass(sourceClass);
			perObjectPermissionClasses.put(sourceClass, result);
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
		public WeakReference<GraphProjectionContext> parentRef;

		public Object projectedOwner;

		public String fieldName;

		public Class clazz;

		public Field field;

		static int debugDepth = 200;

		public Object sourceOwner;

		private int depth;

		public GraphProjectionContext(Class clazz, Field field,
				GraphProjectionContext parent, Object projectedOwner,
				Object sourceOwner) {
			this.clazz = clazz;
			this.field = field;
			this.sourceOwner = sourceOwner;
			this.fieldName = field == null ? "" : field.getName();
			this.parentRef = parent == null ? null
					: new WeakReference<GraphProjectionContext>(parent);
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

		@Override
		public String toString() {
			return (parentRef == null ? "" : parentRef.get().toString() + "::")
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

		<T> boolean projectIntoCollection(T value, T projected,
				GraphProjectionContext context);
	}

	public static interface GraphProjectionDualFilter extends
			GraphProjectionFieldFilter, GraphProjectionDataFilter {
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

	public interface InstantiateImplCallbackWithShellObject<T> extends
			InstantiateImplCallback<T> {
		boolean instantiateLazyInitializer(T initializer,
				GraphProjectionContext context);

		Object instantiateShellObject(T initializer,
				GraphProjectionContext context);
	}
}
