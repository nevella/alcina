package cc.alcina.framework.entity.entityaccess.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.entity.projection.GraphProjection;

public class GraphTuples {
	transient Predicate<Field> fieldFilter = field -> true;

	public List<TObjectRef> objects = new ArrayList<>();

	transient Map<Class, TClassRef> classRefs = new LinkedHashMap<>();

	List<TClassRef> classRefList = new ArrayList<>();

	public TClassRef ensureClassRef(Class clazz) {
		if (classRefs.containsKey(clazz)) {
			return classRefs.get(clazz);
		}
		TClassRef ref = new TClassRef();
		ref.name = clazz.getName();
		classRefs.put(clazz, ref);
		classRefList.add(ref);
		ref.clazz = clazz;
		if (GraphProjection.isPrimitiveOrDataClass(clazz)
				|| Map.class.isAssignableFrom(clazz)
				|| Collection.class.isAssignableFrom(clazz)
				|| clazz.isInterface()) {
		} else {
			populateFields(ref, clazz);
		}
		return ref;
	}

	private void populateFields(TClassRef ref, Class clazz) {
		try {
			List<Field> fields = new GraphProjection().getFieldsForClass(clazz);
			for (Field field : fields) {
				if (fieldFilter.test(field)) {
					ref.fieldRefs.add(new TFieldRef(ref, field, this));
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static class TClassRef {
		public String name;

		public List<TFieldRef> fieldRefs = new ArrayList<>();

		private transient String simpleName;

		private transient CachingMap<String, TFieldRef> fieldRefByName = new CachingMap<>(
				name -> fieldRefs.stream().filter(tfr -> tfr.name.equals(name))
						.findFirst().orElse(null));

		transient Class clazz;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TClassRef) {
				return ((TClassRef) obj).name.equals(name);
			}
			return super.equals(obj);
		}

		public TFieldRef fieldRefByName(String name) {
			return fieldRefByName.get(name);
		}

		public Class getType() {
			if (clazz == null) {
				try {
					switch (name) {
					case "boolean":
						return boolean.class;
					case "int":
						return int.class;
					case "long":
						return long.class;
					case "double":
						return double.class;
					case "float":
						return float.class;
					default:
					}
					clazz = Class.forName(name);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
			return clazz;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		public String simpleName() {
			if (simpleName == null) {
				simpleName = name.contains(".")
						? name.replaceFirst(".+\\.(.+)", "$1")
						: name;
			}
			return simpleName;
		}

		@Override
		public String toString() {
			return simpleName();
		}
	}

	public static class TFieldRef implements HasEquivalence<TFieldRef> {
		public transient Field field;

		public TClassRef classRef;

		public String name;

		public TClassRef type;

		private transient String path;

		private transient PropertyReflector accessor;

		public TFieldRef() {
		}

		public TFieldRef(TClassRef classRef, Field field, GraphTuples tuples) {
			this.classRef = classRef;
			this.name = field.getName();
			this.type = tuples.ensureClassRef(field.getType());
			this.field = field;
		}

		public PropertyReflector accessor() {
			if (accessor == null) {
				accessor = Reflections.propertyAccessor()
						.property(classRef.clazz, name);
			}
			return accessor;
		}

		@Override
		public boolean equivalentTo(TFieldRef o) {
			return CommonUtils.equals(name, o.name, type.name, o.type.name);
		}

		// when to-class is different to from-class
		public void moveTo(TClassRef to) {
			accessor = null;
			classRef = to;
			field = null;
			to.fieldRefs.add(this);
		}

		public String path() {
			if (path == null) {
				path = Ax.format("%s.%s", classRef.simpleName(), name);
			}
			return path;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static class TObjectRef {
		public transient Entity entity;

		public TClassRef classRef;

		public Map<TFieldRef, String> values = new LinkedHashMap<>();

		public TObjectRef() {
		}

		public TObjectRef(TClassRef classRef) {
			this.classRef = classRef;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			values.entrySet().forEach(e -> {
				sb.append(e.getKey());
				sb.append("=");
				sb.append(CommonUtils.trimToWsChars(
						Ax.blankToEmpty(e.getValue()).replace("\n", "\\n"), 100,
						true));
				sb.append("\n");
			});
			return Ax.format("%s:\n\t%s", classRef, sb);
		}

		public String value(String fieldName) {
			return values.get(classRef.fieldRefByName(fieldName));
		}
	}
}
