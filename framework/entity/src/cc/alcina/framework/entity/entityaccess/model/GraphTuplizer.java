package cc.alcina.framework.entity.entityaccess.model;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.cache.Domain;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.entityaccess.model.GraphTuples.TClassRef;
import cc.alcina.framework.entity.entityaccess.model.GraphTuples.TFieldRef;
import cc.alcina.framework.entity.entityaccess.model.GraphTuples.TObjectRef;

public class GraphTuplizer {
	private GraphTuples tuples;

	public interface DetupleizeMapper {
		boolean ignore(TClassRef classRef);

		Class classFor(TClassRef classRef);

		Object resolve(Class clazz, String value);
	}

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

	DetachedEntityCache created = new DetachedEntityCache();

	private DetupleizeMapper mapper;

	public void detupleize(GraphTuples tuples,
			DetupleizeMapper detupelizeMapper) {
		this.tuples = tuples;
		this.mapper = detupelizeMapper;
		tuples.objects.forEach(this::nonRelationalCreate);
		tuples.objects.forEach(this::relationalCreate);
	}

	private void nonRelationalCreate(TObjectRef inObjRef) {
		if (mapper.ignore(inObjRef.classRef)) {
			return;
		}
		Class clazz = mapper.classFor(inObjRef.classRef);
		HasIdAndLocalId t = Domain.create(clazz);
		TClassRef outRef = tuples.ensureClassRef(clazz);
		for (TFieldRef inField : inObjRef.classRef.fieldRefs) {
			TFieldRef outField = outRef.fieldRefByName(inField.name);
			String value = inObjRef.values.get(inField);
			if (inField.equivalentTo(outField)) {
				Object newValue = getNewValue(inField, value);
				try {
					Reflections.propertyAccessor().setPropertyValue(t,
							inField.name, newValue);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
	}

	private Object getNewValue(TFieldRef inField, String in) {
		if (in == null) {
			return null;
		}
		if (in.equals("null")) {
			return null;
		}
		Object out = null;
		Class clazz = inField.classRef.getType();
		if (HasIdAndLocalId.class.isAssignableFrom(clazz)) {
			return mapper.resolve(clazz, in);
		}
		boolean dateValued = Date.class.isAssignableFrom(clazz);
		boolean longValued = dateValued || clazz == Long.class
				|| clazz == long.class;
		if (longValued) {
			long l = Long.parseLong(in);
			if (dateValued) {
				out = new Date(l);
			} else {
				out = l;
			}
		} else {
			DomainTransformEvent dte = new DomainTransformEvent();
			dte.setNewStringValue(in);
			dte.setObjectClass(clazz);
			dte.setPropertyName(inField.name);
			try {
				out = TransformManager.get().getTargetObject(dte, false);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return out;
	}

	private void relationalCreate(TObjectRef ref) {
		if (mapper.ignore(ref.classRef)) {
			return;
		}
	}
}
