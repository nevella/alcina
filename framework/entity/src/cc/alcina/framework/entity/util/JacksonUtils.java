package cc.alcina.framework.entity.util;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CommonUtils;

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

	public static String serializeNoTypes(Object object) {
		return new JacksonJsonObjectSerializer().withIdRefs()
				.withAllowUnknownProperties().serialize(object);
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

	public static String toNestedJsonList(List<List> cellList)
			throws JsonProcessingException {
		JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
		ArrayNode root = nodeFactory.arrayNode();
		List<String> headers = cellList.get(0);
		cellList.stream().skip(1).forEach(list -> {
			ObjectNode row = nodeFactory.objectNode();
			root.add(row);
			for (int idx = 0; idx < headers.size(); idx++) {
				String header = headers.get(idx);
				String value = CommonUtils.nullSafeToString(list.get(idx));
				row.put(header, value);
			}
		});
		String json = new ObjectMapper().writerWithDefaultPrettyPrinter()
				.writeValueAsString(root);
		return json;
	}
}
