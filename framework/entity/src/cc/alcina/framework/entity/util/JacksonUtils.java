package cc.alcina.framework.entity.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class JacksonUtils {
    public static ObjectMapper defaultGraphMapper() {
        return defaultSerializer().createObjectMapper();
    }

    public static JacksonJsonObjectSerializer defaultSerializer() {
        return new JacksonJsonObjectSerializer().withIdRefs().withTypeInfo()
                .withDefaults(true).withAllowUnknownProperties()
                .withPrettyPrint();
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            return defaultSerializer().deserialize(json, clazz);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public static <T> T deserializeNoTypes(String json, Class<T> clazz) {
        try {
            return new JacksonJsonObjectSerializer().withIdRefs()
                    .withAllowUnknownProperties().deserialize(json, clazz);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public static String serialize(Object object) {
        return defaultSerializer().serialize(object);
    }

    public static String serializeForLogging(Object object) {
        try {
            return defaultSerializer().withDefaults(false).serialize(object);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public static String serializeForLoggingWithDefaultsNoTypes(Object object) {
        try {
            return new JacksonJsonObjectSerializer().withIdRefs()
                    .withAllowUnknownProperties().withPrettyPrint()
                    .serialize(object);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public static String serializeWithDefaultsAndTypes(Object object) {
        try {
            return serialize(object);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public static String textOrNull(ObjectNode node, String fieldName) {
        if (node.hasNonNull(fieldName)) {
            return node.get(fieldName).asText();
        } else {
            return null;
        }
    }
}
