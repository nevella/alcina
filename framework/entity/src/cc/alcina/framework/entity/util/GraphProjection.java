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
package cc.alcina.framework.entity.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.entity.SEUtilities;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = ClearOnAppRestart.class)
public class GraphProjection {
	public static boolean isPrimitiveOrDataClass(Class c) {
		return c.isPrimitive() || c == String.class || c == Boolean.class
				|| c == Character.class || c.isEnum() || c == Class.class
				|| Number.class.isAssignableFrom(c)
				|| Date.class.isAssignableFrom(c) || isEnumSubclass(c);
	}

	private static boolean isEnumSubclass(Class c) {
		return c.getSuperclass() != null && c.getSuperclass().isEnum();
	}

	private GraphProjectionFilter dataFilter;

	private GraphProjectionFilter fieldFilter;

	public static boolean replaceTimestampsWithDates = true;

	private IdentityHashMap reached = new IdentityHashMap();

	Map<Class, Field[]> projectableFields = new HashMap<Class, Field[]>();

	Map<Class, Set<Field>> perObjectPermissionFields = new HashMap<Class, Set<Field>>();

	Map<Field, PropertyPermissions> perFieldPermission = new HashMap<Field, PropertyPermissions>();

	@ClearOnAppRestart
	private static Map<Method, PropertyPermissions> propertyPermissionLookup = new LinkedHashMap<Method, PropertyPermissions>();

	public GraphProjection() {
	}

	public GraphProjection(GraphProjectionFilter fieldFilter,
			GraphProjectionFilter dataFilter) {
		this.fieldFilter = fieldFilter;
		this.dataFilter = dataFilter;
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

	public <T> T project(T source, GraphProjectionContext context)
			throws Exception {
		return project(source, null, context);
	}

	public <T> T project(T source, Object alsoMapTo,
			GraphProjectionContext context) throws Exception {
		if (source == null) {
			return null;
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
		T projected = sourceClass.isArray() ? (T) Array.newInstance(
				sourceClass.getComponentType(), Array.getLength(source))
				: (T) sourceClass.newInstance();
		reached.put(source, projected);
		if (alsoMapTo != null) {
			reached.put(alsoMapTo, projected);
		}
		if (dataFilter != null) {
			if (context == null) {
				context = new GraphProjectionContext(c, null, null, projected);
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
					field, context, projected);
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
				c.add(projected);
			}
		}
		return c;
	}

	public void setReached(IdentityHashMap reached) {
		this.reached = reached;
	}

	private Field[] getFieldsForClass(Object projected) throws Exception {
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
					if (fieldFilter != null) {
						if (!fieldFilter
								.permitField(f, dynamicPermissionFields)) {
							continue;
						}
					}
					f.setAccessible(true);
					allFields.add(f);
				}
				c = c.getSuperclass();
			}
			projectableFields.put(clazz,
					(Field[]) allFields.toArray(new Field[allFields.size()]));
			perObjectPermissionFields.put(clazz, dynamicPermissionFields);
			for (Field field : dynamicPermissionFields) {
				PropertyPermissions pp = getPropertyPermission(field
						.getDeclaringClass().getMethod(
								SEUtilities.getAccessorName(field),
								new Class[0]));
				perFieldPermission.put(field, pp);
			}
		}
		return projectableFields.get(clazz);
	}

	private PropertyPermissions getPropertyPermission(Method method) {
		if (!propertyPermissionLookup.containsKey(method)) {
			propertyPermissionLookup.put(method,
					method.getAnnotation(PropertyPermissions.class));
		}
		return propertyPermissionLookup.get(method);
	}

	private boolean permitField(Field field, Object source) throws Exception {
		PropertyPermissions pp = perFieldPermission.get(field);
		if (pp != null) {
			AnnotatedPermissible ap = new AnnotatedPermissible(pp.read());
			return PermissionsManager.get().isPermissible(source, ap);
		}
		return false;
	}

	public static class CollectionProjectionFilter implements
			GraphProjectionFilter {
		@SuppressWarnings("unchecked")
		public <T> T filterData(T original, T projected,
				GraphProjectionContext context, GraphProjection graphProjection)
				throws Exception {
			if (original.getClass().isArray()) {
				int n = Array.getLength(original);
				for (int i = 0; i < n; i++) {
					Object source = Array.get(original, i);
					Array.set(projected, i,
							graphProjection.project(source, context));
				}
			}
			if (original instanceof Collection) {
				return (T) graphProjection.projectCollection(
						(Collection) original, context);
			}
			if (original instanceof Map) {
				return (T) projectMap((Map) original, context, graphProjection);
			}
			return projected;
		}

		public boolean permitField(Field field,
				Set<Field> perObjectPermissionFields) {
			return false;
		}

		private Object projectMap(Map map, GraphProjectionContext context,
				GraphProjection graphProjection) throws Exception {
			Map m = null;
			if (map instanceof Multimap) {
				m = new Multimap();
			} else if (map instanceof LinkedHashMap) {
				m = new LinkedHashMap();
			} else {
				m = new HashMap();
			}
			Iterator itr = map.keySet().iterator();
			Object value, key;
			for (; itr.hasNext();) {
				key = itr.next();
				value = map.get(key);
				Object pKey = graphProjection.project(key, context);
				if (key == null || pKey != null) {
					m.put(pKey, graphProjection.project(value, context));
				}
			}
			return m;
		}
	}

	public static class GraphProjectionContext {
		public GraphProjectionContext parent;

		public Object ownerObject;

		public String fieldName;

		public Class clazz;

		public Field field;

		public GraphProjectionContext(Class clazz, Field field,
				GraphProjectionContext parent, Object ownerObject) {
			this.clazz = clazz;
			this.field = field;
			this.fieldName = field == null ? "" : field.getName();
			this.parent = parent;
			this.ownerObject = ownerObject;
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
			return (parent == null ? "" : parent.toString() + "::")
					+ clazz.getSimpleName() + "." + fieldName;
		}
	}

	public static interface GraphProjectionFilter {
		/*
		 * IMPORTANT - if filterdata changes the return value (i.e. doesn't
		 * return a value === projected) it must immediately (on new object
		 * instantiation) register the new value in graphProjection.reached
		 */
		<T> T filterData(T original, T projected,
				GraphProjectionContext context, GraphProjection graphProjection)
				throws Exception;

		boolean permitField(Field field, Set<Field> perObjectPermissionFields);
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

	public static class PermissibleFieldFilter implements GraphProjectionFilter {
		public static boolean disablePerObjectPermissions;

		public <T> T filterData(T original, T projected,
				GraphProjectionContext context, GraphProjection graphProjection)
				throws Exception {
			return null;
		}

		public boolean permitField(Field field,
				Set<Field> perObjectPermissionFields) {
			try {
				PropertyPermissions pp = field
						.getDeclaringClass()
						.getMethod(SEUtilities.getAccessorName(field),
								new Class[0])
						.getAnnotation(PropertyPermissions.class);
				if (pp != null) {
					AnnotatedPermissible ap = new AnnotatedPermissible(
							pp.read());
					if (ap.accessLevel() == AccessLevel.ADMIN_OR_OWNER) {
						if (ap.rule().isEmpty()
								&& !PermissionsManager.get().isLoggedIn()) {
							return false;
						}
						if (disablePerObjectPermissions) {
							return true;
							// only in app startup/warmup
						}
					}
					if (ap.accessLevel() == AccessLevel.ADMIN_OR_OWNER
							|| !ap.rule().isEmpty()) {
						perObjectPermissionFields.add(field);
						return true;
					}
					if (!PermissionsManager.get().isPermissible(ap)) {
						return false;
					}
				}
				return true;
				// TODO: 3.2 - replace with a call to tltm (should
				// really be tlpm) that checks obj read perms
				// that'll catch find-object stuff as well
			} catch (Exception e2) {
				return false;
			}
		}
	}
}
