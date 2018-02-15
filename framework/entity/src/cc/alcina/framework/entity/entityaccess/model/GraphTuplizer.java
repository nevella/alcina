package cc.alcina.framework.entity.entityaccess.model;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor.IndividualPropertyAccessor;
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

		void notifyUnmapped(TFieldRef inField, TFieldRef outField);

		long getId(TObjectRef inObjRef);

		boolean ignore(TObjectRef inObjRef);

		void putRelational(TObjectRef inObjRef, HasIdAndLocalId t,
				TFieldRef inField);

		void prepareCustom(HasIdAndLocalId t);

		void doCustom(TObjectRef inObjRef, HasIdAndLocalId t);

		void prepare(TObjectRef inObjRef);

		default void translateNonRelational(NonRelationalTranslateToken token) {
			token.fieldName = token.inField.name;
			token.value = token.inValue;
		}

		default boolean ignore(TObjectRef inObjRef, String hint) {
			return ignore(inObjRef);
		}
	}

	public static enum DetupelizeInstructionType {
		IGNORE, HILI, FUNCTION, ID, LONG, ID_COMPRESS
	}

	public static class DetupelizeInstruction {
		public DetupelizeInstructionType type;

		public String path;

		public String outFieldName;

		public Consumer<NonRelationalTranslateToken> consumer;

		public DetupelizeInstruction() {
		}

		public DetupelizeInstruction(DetupelizeInstructionType type,
				String path) {
			this.type = type;
			this.path = path;
		}

		public DetupelizeInstruction(DetupelizeInstructionType type,
				String path, String outFieldName) {
			this.type = type;
			this.path = path;
			this.outFieldName = outFieldName;
		}

		public DetupelizeInstruction(DetupelizeInstructionType type,
				String path, String outFieldName,
				Consumer<NonRelationalTranslateToken> consumer) {
			this.type = type;
			this.path = path;
			this.outFieldName = outFieldName;
			this.consumer = consumer;
		}

		private transient String inFieldPart;

		private transient String classSimplePart;

		public String inFieldPart() {
			if (inFieldPart == null) {
				inFieldPart = path.replaceFirst(".+\\.", "");
			}
			return inFieldPart;
		}

		public String classSimplePart() {
			if (classSimplePart == null) {
				classSimplePart = path.replaceFirst("\\..+", "");
			}
			return classSimplePart;
		}

		IndividualPropertyAccessor accessor = null;

		public IndividualPropertyAccessor
				outAccessor(Class<? extends HasIdAndLocalId> clazz) {
			if (accessor == null) {
				accessor = Reflections.propertyAccessor().cachedAccessor(clazz,
						outFieldName);
			}
			return accessor;
		}
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
		tuples.objects.forEach(this::prepare);
		if ("".isEmpty()) {
			tuples.objects.forEach(this::create);
			tuples.objects.forEach(this::nonRelational);
			tuples.objects.forEach(this::relational);
			tuples.objects.forEach(this::prepareCustom);
		}
		tuples.objects.forEach(this::doCustom);
	}

	private void prepare(TObjectRef inObjRef) {
		if (mapper.ignore(inObjRef)) {
			return;
		}
		mapper.prepare(inObjRef);
	}

	private void create(TObjectRef inObjRef) {
		if (mapper.ignore(inObjRef)) {
			return;
		}
		Class clazz = mapper.classFor(inObjRef.classRef);
		long id = mapper.getId(inObjRef);
		if (id == -1) {
			return;
		}
		HasIdAndLocalId t = (HasIdAndLocalId) Reflections.classLookup()
				.newInstance(clazz, id, 0L);
		t.setId(id);
		DomainTransformEvent dte = new DomainTransformEvent();
		dte.setObjectClass(clazz);
		dte.setObjectId(id);
		dte.setTransformType(TransformType.CREATE_OBJECT);
		TransformManager.get().addTransform(dte);
		TransformManager.get().registerDomainObject(t);
		inObjRef.hili = t;
	}

	public static class NonRelationalTranslateToken {
		public String value;

		public String fieldName;

		public TFieldRef inField;

		public String inValue;

		public boolean fieldTranslated;

		public void in(TFieldRef inField, String inValue) {
			this.inField = inField;
			this.inValue = inValue;
			fieldTranslated = false;
		}
	}

	private void nonRelational(TObjectRef inObjRef) {
		if (mapper.ignore(inObjRef)) {
			return;
		}
		Class clazz = mapper.classFor(inObjRef.classRef);
		HasIdAndLocalId t = inObjRef.hili;
		TClassRef outRef = tuples.ensureClassRef(clazz);
		NonRelationalTranslateToken token = new NonRelationalTranslateToken();
		for (TFieldRef inField : inObjRef.classRef.fieldRefs) {
			String value = inObjRef.values.get(inField);
			token.in(inField, value);
			mapper.translateNonRelational(token);
			TFieldRef outField = outRef.fieldRefByName(token.fieldName);
			if (outField != null && (inField.equivalentTo(outField)
					|| token.fieldTranslated)) {
				Object newValue = getNewValue(inField, token.value);
				try {
					outField.accessor().setPropertyValue(t, newValue);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			} else {
				mapper.notifyUnmapped(inField, outField);
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
		Class clazz = mapper.classFor(inField.type);
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
			dte.setValueClass(clazz);
			dte.setObjectClass(mapper.classFor(inField.classRef));
			dte.setPropertyName(inField.name);
			try {
				out = TransformManager.get().getTargetObject(dte, false);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return out;
	}

	private void relational(TObjectRef inObjRef) {
		if (mapper.ignore(inObjRef)) {
			return;
		}
		Class clazz = mapper.classFor(inObjRef.classRef);
		HasIdAndLocalId t = inObjRef.hili;
		TClassRef outRef = tuples.ensureClassRef(clazz);
		for (TFieldRef inField : inObjRef.classRef.fieldRefs) {
			TFieldRef outField = outRef.fieldRefByName(inField.name);
			String value = inObjRef.values.get(inField);
			if (outField != null && inField.equivalentTo(outField)) {
			} else {
				mapper.putRelational(inObjRef, t, inField);
			}
		}
	}

	private void doCustom(TObjectRef inObjRef) {
		if (mapper.ignore(inObjRef, "doCustom")) {
			return;
		}
		HasIdAndLocalId t = inObjRef.hili;
		mapper.doCustom(inObjRef, t);
	}

	private void prepareCustom(TObjectRef inObjRef) {
		if (mapper.ignore(inObjRef)) {
			return;
		}
		HasIdAndLocalId t = inObjRef.hili;
		mapper.prepareCustom(t);
	}
}
