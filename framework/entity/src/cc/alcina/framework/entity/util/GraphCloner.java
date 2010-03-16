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

 public class GraphCloner {
	private CloneFilter dataFilter;

	private CloneFilter fieldFilter;

	public GraphCloner() {
	}

	public GraphCloner(CloneFilter fieldFilter, CloneFilter dataFilter) {
		this.fieldFilter = fieldFilter;
		this.dataFilter = dataFilter;
	}

	private IdentityHashMap reached = new IdentityHashMap();

	public <T> T clone(T source, ClassFieldPair context) throws Exception {
		return clone(source, null, context);
	}

	public <T> T clone(T source, Object alsoMapTo, ClassFieldPair context)
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
		T cloned = (T) source.getClass().newInstance();
		reached.put(source, cloned);
		if (alsoMapTo != null) {
			reached.put(alsoMapTo, cloned);
		}
		if (dataFilter != null) {
			if (context == null) {
				context = new ClassFieldPair(c, "");
			}
			T replaceClone = dataFilter.filterData(source, cloned, context,
					this);
			if (replaceClone != cloned) {
				reached.put(source, replaceClone);
				if (alsoMapTo != null) {
					reached.put(alsoMapTo, replaceClone);
				}
				// System.out.println(context + ":"
				// + source.getClass().getSimpleName() + ":"
				// + System.identityHashCode(source) + ">>"
				// + System.identityHashCode(replaceClone));
				return replaceClone;
			}
		}
		if (cloned == null) {
			return cloned;
		}
		Field[] fields = getFieldsForClass(cloned);
		Set<Field> checkFields = perObjectPermissionFields.get(cloned
				.getClass());
		for (Field field : fields) {
			Object value = field.get(source);
			if (checkFields.contains(field) && !permitField(field, source)) {
				continue;
			}
			if (c.getSimpleName().equals("JadeGroup")
					&& "Developers".equals(source.toString())
					&& field.getName().equals("memberOfGroups")) {
				int k = 3;
			}
			Object cv = clone(value, new ClassFieldPair(c, field.getName()));
			field.set(cloned, cv);
		}
		return cloned;
	}

	// TODO - shouldn't this be package-private?
	public Collection cloneCollection(Collection coll, ClassFieldPair context)
			throws Exception {
		Collection c = null;
		if (coll instanceof ArrayList) {
			c = new ArrayList();
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
			Object clone = clone(value, context);
			if (clone == null) {
				int z = 3;
			}
			c.add(clone);
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

	Map<Class, Field[]> cloneableFields = new HashMap<Class, Field[]>();

	Map<Class, Set<Field>> perObjectPermissionFields = new HashMap<Class, Set<Field>>();

	private Field[] getFieldsForClass(Object cloned) {
		Class<? extends Object> clazz = cloned.getClass();
		if (!cloneableFields.containsKey(clazz)) {
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
			cloneableFields.put(clazz, (Field[]) allFields
					.toArray(new Field[allFields.size()]));
			perObjectPermissionFields.put(clazz, dynamicPermissionFields);
		}
		return cloneableFields.get(clazz);
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

	public static interface CloneFilter {
		/*
		 * IMPORTANT - if filterdata changes the return value (i.e. doesn't
		 * return value) it must immediately (on instantiation) register the new
		 * value in graphCloner.reached
		 */
		<T> T filterData(T value, T cloned, ClassFieldPair context,
				GraphCloner graphCloner) throws Exception;

		boolean permitField(Field field, Set<Field> perObjectPermissionFields);
	}

	public static class PermissibleFieldFilter implements CloneFilter {
		public <T> T filterData(T value, T cloned,
				ClassFieldPair context, GraphCloner graphCloner)
				throws Exception {
			return null;
		}

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

	public static class CollectionCloneFilter implements CloneFilter {
		@SuppressWarnings("unchecked")
		public <T> T filterData(T value, T cloned,
			ClassFieldPair context, GraphCloner graphCloner)
			throws Exception {
			if (value instanceof Collection) {
				return (T) graphCloner.cloneCollection((Collection) value, context);
			}
			if (value instanceof Map) {
				return (T) cloneMap((Map) value, context, graphCloner);
			}
			return cloned;
		}

		private Object cloneMap(Map map, ClassFieldPair context,
				GraphCloner graphCloner) throws Exception {
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
				m.put(graphCloner.clone(key, context), graphCloner.clone(value,
						context));
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
}
