package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public abstract class AlcinaBeanSerializer {
	protected static final String PROPERTIES = "props";

	protected static final String PROPERTIES_SHORT = "p";

	public static final String CLASS_NAME = "cn";

	protected static final String LITERAL = "lit";

	protected Map<String, Class> abbrevLookup = new LinkedHashMap<>();

	protected Map<Class, String> reverseAbbrevLookup = new LinkedHashMap<>();

	protected String propertyFieldName;

	private boolean throwOnUnrecognisedProperty;

	public abstract <T> T deserialize(String jsonString);

	public boolean isThrowOnUnrecognisedProperty() {
		return this.throwOnUnrecognisedProperty;
	}

	public AlcinaBeanSerializer registerLookups(Map<String, Class> abbrevLookup,
			Map<Class, String> reverseAbbrevLookup) {
		this.abbrevLookup = abbrevLookup;
		this.reverseAbbrevLookup = reverseAbbrevLookup;
		propertyFieldName = PROPERTIES_SHORT;
		return this;
	}

	public abstract String serialize(Object bean);

	public AlcinaBeanSerializer throwOnUnrecognisedProperty() {
		throwOnUnrecognisedProperty = true;
		return this;
	}

	protected Class getClassMaybeAbbreviated(String cns) {
		Class clazz;
		if (abbrevLookup.containsKey(cns)) {
			clazz = abbrevLookup.get(cns);
		} else {
			clazz = Reflections.classLookup().getClassForName(cns);
		}
		return clazz;
	}

	protected String normaliseReverseAbbreviation(Class<? extends Object> type,
			String typeName) {
		if (reverseAbbrevLookup.containsKey(type)) {
			typeName = reverseAbbrevLookup.get(type);
		}
		return typeName;
	}

	public static class SerializationHolder {
		private Object value;

		public SerializationHolder() {
		}

		public SerializationHolder(Object value) {
			super();
			this.value = value;
		}

		public Object getValue() {
			return this.value;
		}

		public void setValue(Object value) {
			this.value = value;
		}
	}

	public static <V> V deserializeHolder(String serialized) {
		Object object = Registry.impl(AlcinaBeanSerializer.class)
				.deserialize(serialized);
		if (object instanceof AlcinaBeanSerializer.SerializationHolder) {
			return (V) ((AlcinaBeanSerializer.SerializationHolder) object)
					.getValue();
		} else {
			return (V) object;
		}
	}

	public static String serializeHolder(Object value) {
		return Registry.impl(AlcinaBeanSerializer.class)
				.serialize(new SerializationHolder(value));
	}
}
