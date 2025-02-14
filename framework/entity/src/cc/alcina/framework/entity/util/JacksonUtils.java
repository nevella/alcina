package cc.alcina.framework.entity.util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Io;

public class JacksonUtils {
	public static boolean isNullish(JsonNode node) {
		return node == NullNode.instance || node == MissingNode.getInstance();
	}

	public static ObjectMapper defaultGraphMapper() {
		return defaultSerializer().createObjectMapper();
	}

	public static JacksonJsonObjectSerializer defaultSerializer() {
		return new JacksonJsonObjectSerializer().withIdRefs().withTypeInfo()
				.withAllowUnknownProperties().withPrettyPrint();
	}

	public static <T> T deserialize(InputStream stream, Class<T> clazz) {
		try {
			return defaultSerializer().deserialize(
					new InputStreamReader(stream, StandardCharsets.UTF_8),
					clazz);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T deserialize(String json, Class<T> clazz) {
		try {
			return defaultSerializer().deserialize(json, clazz);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T deserializeFromFile(File file, Class<T> clazz) {
		return deserialize(Io.read().file(file).asString(), clazz);
	}

	public static <T> T deserializeNoTypes(String json, Class<T> clazz) {
		try {
			return new JacksonJsonObjectSerializer().withIdRefs()
					.withAllowUnknownProperties().deserialize(json, clazz);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String prettyJson(String json) {
		try {
			JsonNode tree = new ObjectMapper().readTree(json);
			return prettyJson(tree);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static String prettyJson(JsonNode node) {
		try {
			String pretty = new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(node);
			return pretty;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void prettyPrintJson(String json) {
		Ax.out(prettyJson(json));
	}

	public static void prettyPrintJson(JsonNode node) {
		Ax.out(prettyJson(node));
	}

	public static String serialize(Object object) {
		return defaultSerializer().serialize(object);
	}

	@SuppressWarnings("deprecation")
	// just informative, not intended for deserialization
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

	public static String serializeNoTypesElideDefaults(Object object) {
		return new JacksonJsonObjectSerializer().withIdRefs()
				.withDefaults(false).withAllowUnknownProperties()
				.serialize(object);
	}

	public static String serializeNoTypesInterchange(Object object) {
		return new JacksonJsonObjectSerializer().withPrettyPrint()
				.withWrapRootValue().withAllowUnknownProperties()
				.serialize(object);
	}

	public static byte[] serializeToByteArray(Object object) {
		return serialize(object).getBytes(StandardCharsets.UTF_8);
	}

	public static void serializeToFile(Object object, File file) {
		String json = serialize(object);
		if (file.exists()
				&& Objects.equals(Io.read().file(file).asString(), json)) {
			// noop
		} else {
			Io.write().string(json).toFile(file);
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

	public static ObjectMapper wsGraphMapper() {
		return wsSerializer().createObjectMapper();
	}

	// Special defaults for web services
	// Should be closer to a plain JSON
	public static JacksonJsonObjectSerializer wsSerializer() {
		return new JacksonJsonObjectSerializer();
	}
}
