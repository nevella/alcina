package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public abstract class AlcinaBeanSerializer {
    protected static final String PROPERTIES = "props";

    protected static final String PROPERTIES_SHORT = "p";

    public static final String CLASS_NAME = "cn";

    protected static final String LITERAL = "lit";

    public static <V> V deserializeHolder(String serialized) {
        if (serialized == null) {
            return null;
        }
        Object object = Registry.impl(AlcinaBeanSerializer.class)
                .deserialize(serialized);
        if (object instanceof AlcinaBeanSerializer.SerializationHolder) {
            return (V) ((AlcinaBeanSerializer.SerializationHolder) object)
                    .provideValue();
        } else {
            return (V) object;
        }
    }

    public static String serializeHolder(Object value) {
        if (value == null) {
            return null;
        }
        return Registry.impl(AlcinaBeanSerializer.class)
                .serialize(new SerializationHolder(value));
    }

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

    @Bean
    public static class SerializationHolder extends BaseBindable {
        private List listValue;

        private Map mapValue;

        private List valueHolder = new ArrayList();

        public SerializationHolder() {
        }

        public SerializationHolder(Object value) {
            super();
            if (value instanceof List) {
                listValue = (List) value;
            } else if (value instanceof Map) {
                mapValue = (Map) value;
            } else {
                valueHolder.add(value);
            }
        }

        public List getListValue() {
            return this.listValue;
        }

        public Map getMapValue() {
            return this.mapValue;
        }

        public List getValueHolder() {
            return this.valueHolder;
        }

        public Object provideValue() {
            if (mapValue != null) {
                return mapValue;
            }
            if (listValue != null) {
                return listValue;
            }
            return valueHolder.get(0);
        }

        public void setListValue(List listValue) {
            this.listValue = listValue;
        }

        public void setMapValue(Map mapValue) {
            this.mapValue = mapValue;
        }

        public void setValueHolder(List valueHolder) {
            this.valueHolder = valueHolder;
        }
    }
}
