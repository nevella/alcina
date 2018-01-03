package cc.alcina.framework.entity.entityaccess.model;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.entityaccess.model.GraphTuples.TClassRef;
import cc.alcina.framework.entity.entityaccess.model.GraphTuples.TFieldRef;
import cc.alcina.framework.entity.entityaccess.model.GraphTuples.TObjectRef;

public class GraphTuplizer {
	private GraphTuples tuples;

	public GraphTuples tupleize(Collection objects,
			Predicate<Field> fieldFilter) {
		tuples = new GraphTuples();
		tuples.fieldFilter = fieldFilter;
		for (Object object : objects) {
			tuple(object);
		}
		return tuples;
	}

	private void tuple(Object object) {
		TClassRef classRef = tuples.ensureClassRef(object.getClass());
		TObjectRef obj = new TObjectRef(classRef);
		tuples.objects.add(obj);
		for (TFieldRef field : classRef.fieldRefs) {
			obj.values.put(field, getValue(object, field));
		}
	}

	Set<String> hiliFields = new LinkedHashSet<>();

	private String getValue(Object object, TFieldRef field) {
		try {
			Object oValue = field.field.get(object);
			if (oValue instanceof HasIdAndLocalId) {
				String locator = object.getClass().getSimpleName() + "."
						+ field.name;
				if (hiliFields.add(locator)) {
					Ax.out(locator);
				}
				return String.valueOf(((HasIdAndLocalId) oValue).getId());
				// Ax.runtimeException("shouldn't persist: %s",
				// oValue.getClass());
			}
			if (oValue == null) {
				return null;
			}
			Class clazz = oValue.getClass();
			if (clazz == long.class || clazz == Long.class) {
				return oValue.toString();
			}
			if (Date.class.isAssignableFrom(clazz)) {
				return String.valueOf(((Date) oValue).getTime());
			}
			if(field.name.equals("activityDateTime")){
				int debug=3;
			}
			DomainTransformEvent dte = new DomainTransformEvent();
			dte.setNewValue(oValue);
			dte.setObjectClass(object.getClass());
			dte.setPropertyName(field.name);
			TransformManager.get().convertToTargetObject(dte);
			return dte.getNewStringValue();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
