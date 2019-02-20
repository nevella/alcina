package cc.alcina.framework.entity.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class JacksonUtils {
    public static ObjectMapper defaultGraphMapper() {
        return new JacksonJsonObjectSerializer().withIdRefs().withTypeInfo()
                .getObjectMapper();
    }

    public static String serializeForLogging(Object object) {
        try {
            return defaultGraphMapper()
                    .setSerializationInclusion(Include.NON_DEFAULT)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
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
