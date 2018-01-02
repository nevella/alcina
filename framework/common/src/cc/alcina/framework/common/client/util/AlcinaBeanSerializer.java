package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.List;
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

	public static String serialize1(Object o) {
		return Registry.impl(AlcinaBeanSerializer.class).serialize(o);
	}
	public static <T> T deserialize1(String s) {
		return Registry.impl(AlcinaBeanSerializer.class).deserialize(s);
	}
}
