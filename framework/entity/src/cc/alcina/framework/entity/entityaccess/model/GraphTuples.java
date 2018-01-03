package cc.alcina.framework.entity.entityaccess.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.projection.GraphProjection;

public class GraphTuples {
	transient Predicate<Field> fieldFilter = field -> true;

	public static class TObjectRef {
		public TObjectRef() {
		}

		public TObjectRef(TClassRef classRef) {
			this.classRef = classRef;
		}

		public TClassRef classRef;

		public Map<TFieldRef, String> values = new LinkedHashMap<>();

		@Override
		public String toString() {
			return Ax.format("%s:\n%s", classRef,
					CommonUtils.joinWithNewlineTab(values.entrySet()));
		}
	}

	public List<TObjectRef> objects = new ArrayList<>();

	public class TFieldRef {
		public TFieldRef(TClassRef classRef, Field field) {
			this.classRef = classRef;
			this.name = field.getName();
			this.type = ensureClassRef(field.getType());
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
	}

	public class TClassRef {
		public String name;

		public List<TFieldRef> fieldRefs = new ArrayList<>();

		public String simpleName() {
			return name.contains(".") ? name.replaceFirst(".+\\.(.+)", "$1")
					: name;
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
					ref.fieldRefs.add(new TFieldRef(ref, field));
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
