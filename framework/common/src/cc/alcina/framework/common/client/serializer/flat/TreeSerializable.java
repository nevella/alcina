package cc.alcina.framework.common.client.serializer.flat;

import java.io.Serializable;

/**
 * Important! (because hard to enforce) - any class that implements
 * TreeSerializable should also have (usually via a superclass)
 * 
 * @RegistryLocation(registryPoint = TreeSerializable.class)
 * @author nick@alcina.cc
 *
 */
public interface TreeSerializable extends Serializable {
	default void onAfterTreeDeserialize() {
	}

	default void onBeforeTreeDeserialize() {
	}

	default void onBeforeTreeSerialize() {
	}
}
