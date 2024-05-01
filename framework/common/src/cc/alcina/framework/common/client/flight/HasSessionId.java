package cc.alcina.framework.common.client.flight;

import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.ReflectiveSerializable;

public interface HasSessionId extends ReflectiveSerializable {
	String getSessionId();
}
