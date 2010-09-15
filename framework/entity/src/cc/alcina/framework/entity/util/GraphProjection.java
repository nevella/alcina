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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.entity.SEUtilities;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class GraphProjection {
	private GraphProjectionFilter dataFilter;

	private GraphProjectionFilter fieldFilter;

	public GraphProjection() {
	}

	public GraphProjection(GraphProjectionFilter fieldFilter,
			GraphProjectionFilter dataFilter) {
		this.fieldFilter = fieldFilter;
		this.dataFilter = dataFilter;
	}

	private IdentityHashMap reached = new IdentityHashMap();

	public <T> T project(T source, ClassFieldPair context) throws Exception {
		return project(source, null, context);
	}

	public <T> T project(T source, Object alsoMapTo, ClassFieldPair context)
			throws Exception {
		if (source == null) {
			return null;
		}
		Class c = source.getClass();
		if (c.isPrimitive()
				|| c == String.class
				|| c == Boolean.class
				|| c == Character.class
				|| c.isEnum()
				|| c == Class.class
				|| (source instanceof Number)
				|| (source instanceof Date)
				|| (c.getEnclosingClass() != null && c.getEnclosingClass()
						.isEnum())) {
			return source;
		}
		if (reached.containsKey(source)) {
			return (T) reached.get(source);
		}
		// String dbg = context + ":" + source.getClass().getSimpleName() + ":"
		// + System.identityHashCode(source);
		// if (source instanceof HasIdAndLocalId) {
		// dbg += "-id:" + ((HasIdAndLocalId) source).getId();
		// }
		// System.out.println(dbg);
		Class<? extends Object> sourceClass = source.getClass();
		T projected = sourceClass.isArray() ? (T) Array.newInstance(sourceClass
				.getComponentType(), Array.getLength(source)) : (T) sourceClass
				.newInstance();
		reached.put(source, projected);
		if (alsoMapTo != null) {
			reached.put(alsoMapTo, projected);
		}
		if (dataFilter != null) {
			if (context == null) {
				context = new ClassFieldPair(c, "");
			}
			T replaceProjected = dataFilter.filterData(source, projected,
					context, this);
			if (replaceProjected != projected) {
				reached.put(source, replaceProjected);
				if (alsoMapTo != null) {
					reached.put(alsoMapTo, replaceProjected);
				}
				// System.out.println(context + ":"
				// + source.getClass().getSimpleName() + ":"
				// + System.identityHashCode(source) + ">>"
				// + System.identityHashCode(replaceProjected));
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
			Object cv = project(value, new ClassFieldPair(c, field.getName()));
			field.set(projected, cv);
		}
		return projected;
	}

	// TODO - shouldn't this be package-private?
	public Collection projectCollection(Collection coll, ClassFieldPair context)
			throws Exception {
		Collection c = null;
		if (coll instanceof ArrayList) {
			c = coll.getClass().newInstance();
			// no "persistentLists", at least
		} else if (coll instanceof LinkedHashSet) {
			c = new LinkedHashSet();
		} else if (coll instanceof Set) {
			c = new HashSet();
		}
		reached.put(coll, c);
		Iterator itr = coll.iterator();
		Object value;
		for (; itr.hasNext();) {
			value = itr.next();
			Object projected = project(value, context);
			c.add(projected);
		}
		return c;
	}

	private boolean permitField(Field field, Object source) throws Exception {
		PropertyPermissions pp = field.getDeclaringClass().getMethod(
				SEUtilities.getAccessorName(field), new Class[0])
				.getAnnotation(PropertyPermissions.class);
		if (pp != null) {
			AnnotatedPermissible ap = new AnnotatedPermissible(pp.read());
			return PermissionsManager.get().isPermissible(source, ap);
		}
		return false;
	}

	Map<Class, Field[]> projectableFields = new HashMap<Class, Field[]>();

	Map<Class, Set<Field>> perObjectPermissionFields = new HashMap<Class, Set<Field>>();

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
			projectableFields.put(clazz, (Field[]) allFields
					.toArray(new Field[allFields.size()]));
			perObjectPermissionFields.put(clazz, dynamicPermissionFields);
		}
		return projectableFields.get(clazz);
	}

	public void setReached(IdentityHashMap reached) {
		this.reached = reached;
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

	public static interface GraphProjectionFilter {
		/*
		 * IMPORTANT - if filterdata changes the return value (i.e. doesn't
		 * return a value === projected) it must immediately (on new object
		 * instantiation) register the new value in graphProjection.reached
		 */
		<T> T filterData(T original, T projected, ClassFieldPair context,
				GraphProjection graphProjection) throws Exception;

		boolean permitField(Field field, Set<Field> perObjectPermissionFields);
	}

	public static class PermissibleFieldFilter implements GraphProjectionFilter {
		public <T> T filterData(T original, T projected,
				ClassFieldPair context, GraphProjection graphProjection)
				throws Exception {
			return null;
		}

		public static boolean disablePerObjectPermissions;

		public boolean permitField(Field field,
				Set<Field> perObjectPermissionFields) {
			try {
				PropertyPermissions pp = field.getDeclaringClass().getMethod(
						SEUtilities.getAccessorName(field), new Class[0])
						.getAnnotation(PropertyPermissions.class);
				if (pp != null) {
					AnnotatedPermissible ap = new AnnotatedPermissible(pp
							.read());
					if (ap.accessLevel() == AccessLevel.ADMIN_OR_OWNER) {
						if (!PermissionsManager.get().isLoggedIn()){
							return false;
						}
						if (!disablePerObjectPermissions) {
							perObjectPermissionFields.add(field);
						}
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

	public static class CollectionProjectionFilter implements
			GraphProjectionFilter {
		@SuppressWarnings("unchecked")
		public <T> T filterData(T original, T projected,
				ClassFieldPair context, GraphProjection graphProjection)
				throws Exception {
			if (original.getClass().isArray()) {
				int n = Array.getLength(original);
				for (int i = 0; i < n; i++) {
					Array.set(projected, i, graphProjection.project(Array.get(
							original, i), context));
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

		private Object projectMap(Map map, ClassFieldPair context,
				GraphProjection graphProjection) throws Exception {
			Map m = null;
			if (map instanceof LinkedHashMap) {
				m = new LinkedHashMap();
			} else {
				m = new HashMap();
			}
			Iterator itr = map.keySet().iterator();
			Object value, key;
			for (; itr.hasNext();) {
				key = itr.next();
				value = map.get(key);
				m.put(graphProjection.project(key, context), graphProjection
						.project(value, context));
			}
			return m;
		}

		public boolean permitField(Field field,
				Set<Field> perObjectPermissionFields) {
			return false;
		}
	}

	public static class ClassFieldPair {
		public ClassFieldPair(Class clazz, String fieldName) {
			this.clazz = clazz;
			this.fieldName = fieldName;
		}

		public Class clazz;

		public String fieldName;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassFieldPair) {
				ClassFieldPair o2 = (ClassFieldPair) obj;
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
			return clazz.getSimpleName() + "." + fieldName;
		}
	}

	public interface InstantiateImplCallback<T> {
		boolean instantiateLazyInitializer(T initializer, ClassFieldPair context);
	}

	public interface InstantiateImplCallbackWithShellObject<T> extends
			InstantiateImplCallback<T> {
		boolean instantiateLazyInitializer(T initializer, ClassFieldPair context);

		Object instantiateShellObject(T initializer, ClassFieldPair context);
	}
}
