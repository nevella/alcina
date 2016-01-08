package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.Reflections;

public abstract class AlcinaBeanSerializer {
	protected static final String PROPERTIES = "props";

	protected static final String PROPERTIES_SHORT = "p";

	public static final String CLASS_NAME = "cn";

	protected static final String LITERAL = "lit";

	protected Map<String, Class> abbrevLookup = new LinkedHashMap<>();

	protected Map<Class, String> reverseAbbrevLookup = new LinkedHashMap<>();

	protected String propertyFieldName;

	public AlcinaBeanSerializer registerLookups(Map<String, Class> abbrevLookup,
			Map<Class, String> reverseAbbrevLookup) {
		this.abbrevLookup = abbrevLookup;
		this.reverseAbbrevLookup = reverseAbbrevLookup;
		propertyFieldName = PROPERTIES_SHORT;
		return this;
	}

	public abstract <T> T deserialize(String jsonString);

	public abstract String serialize(Object bean);

	protected String normaliseReverseAbbreviation(Class<? extends Object> type,
			String typeName) {
		if (reverseAbbrevLookup.containsKey(type)) {
			typeName = reverseAbbrevLookup.get(type);
		}
		return typeName;
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
}
