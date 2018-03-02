package cc.alcina.framework.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class JacksonUtil {
	public static String textOrNull(ObjectNode node, String fieldName) {
		if (node.hasNonNull(fieldName)) {
			return node.get(fieldName).asText();
		} else {
			return null;
		}
	}
}
