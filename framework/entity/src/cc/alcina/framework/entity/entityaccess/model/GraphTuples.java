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
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor.IndividualPropertyAccessor;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.entity.entityaccess.model.GraphTuples.TClassRef;
import cc.alcina.framework.entity.projection.GraphProjection;

public class GraphTuples {
	transient Predicate<Field> fieldFilter = field -> true;

	public static class TObjectRef {
		public TObjectRef() {
		}

		public transient HasIdAndLocalId hili;

		public TObjectRef(TClassRef classRef) {
			this.classRef = classRef;
		}

		public TClassRef classRef;

		public Map<TFieldRef, String> values = new LinkedHashMap<>();

		@Override
		public String toString() {
			return Ax.format("%s:\n\t%s", classRef,
					CommonUtils.joinWithNewlineTab(values.entrySet()));
		}

		public String value(String fieldName) {
			return values.get(classRef.fieldRefByName(fieldName));
		}
	}

	public List<TObjectRef> objects = new ArrayList<>();

	public static class TFieldRef implements HasEquivalence<TFieldRef> {
		public TFieldRef(TClassRef classRef, Field field, GraphTuples tuples) {
			this.classRef = classRef;
			this.name = field.getName();
			this.type = tuples.ensureClassRef(field.getType());
			this.field = field;
		}

		public TFieldRef() {
		}

		public transient Field field;

		public TClassRef classRef;

		public String name;

		public TClassRef type;

		@Override
		public String toString() {
			return name;
		}

		@Override
		public boolean equivalentTo(TFieldRef o) {
			return CommonUtils.equals(name, o.name, type.name, o.type.name);
		}

		private transient String path;

		public String path() {
			if (path == null) {
				path = Ax.format("%s.%s", classRef.simpleName(), name);
			}
			return path;
		}

		private transient IndividualPropertyAccessor accessor;

		public IndividualPropertyAccessor accessor() {
			if (accessor == null) {
				accessor = Reflections.propertyAccessor()
						.cachedAccessor(classRef.clazz, name);
			}
			return accessor;
		}

		// when to-class is different to from-class
		public void moveTo(TClassRef to) {
			accessor = null;
			classRef = to;
			field = null;
			to.fieldRefs.add(this);
		}
	}

	public static class TClassRef {
		public String name;

		public List<TFieldRef> fieldRefs = new ArrayList<>();

		private transient String simpleName;

		public String simpleName() {
			if (simpleName == null) {
				simpleName = name.contains(".")
						? name.replaceFirst(".+\\.(.+)", "$1") : name;
			}
			return simpleName;
		}

		private transient CachingMap<String, TFieldRef> fieldRefByName = new CachingMap<>(
				name -> fieldRefs.stream().filter(tfr -> tfr.name.equals(name))
						.findFirst().orElse(null));

		public TFieldRef fieldRefByName(String name) {
			return fieldRefByName.get(name);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TClassRef) {
				return ((TClassRef) obj).name.equals(name);
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		transient Class clazz;

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
		public String toString() {
			return simpleName();
		}
	}

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
			Field[] fields = new GraphProjection().getFieldsForClass(clazz);
			for (Field field : fields) {
				if (fieldFilter.test(field)) {
					ref.fieldRefs.add(new TFieldRef(ref, field, this));
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
