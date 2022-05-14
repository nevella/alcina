package cc.alcina.framework.common.client.serializer;

import cc.alcina.framework.common.client.util.LooseContext;

public class Serializers {
	public static final String CONTEXT_DESERIALIZING = FlatTreeSerializer.class
			.getName() + ".CONTEXT_DESERIALIZING";

	public static final String CONTEXT_SERIALIZING = FlatTreeSerializer.class
			.getName() + ".CONTEXT_SERIALIZING";

	public static boolean isDeserializing() {
		return LooseContext.is(CONTEXT_DESERIALIZING);
	}

	public static boolean isSerializing() {
		return LooseContext.is(CONTEXT_SERIALIZING);
	}
}
