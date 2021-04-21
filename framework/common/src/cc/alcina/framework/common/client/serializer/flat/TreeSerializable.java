package cc.alcina.framework.common.client.serializer.flat;

import java.io.Serializable;

public interface TreeSerializable extends Serializable {
	default void onAfterTreeDeserialize() {
	}

	default void onBeforeTreeSerialize() {
	}
}
