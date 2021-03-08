package cc.alcina.framework.common.client.serializer.flat;

import java.io.Serializable;

public interface TreeSerializable extends Serializable {
	/*
	 * Clear any default enum collections
	 */
	default void prepareForTreeDeserialization() {
	}
}
