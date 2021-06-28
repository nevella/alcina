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
	default Customiser treeSerializationCustomiser() {
		return Customiser.INSTANCE;
	}

	public static class Customiser<T extends TreeSerializable> {
		public static final transient Customiser INSTANCE = new Customiser(
				null);

		protected T serializable;

		public Customiser(T treeSerializable) {
			this.serializable = treeSerializable;
		}

		public String filterTestSerialized(String serialized) {
			return serialized;
		}

		public void onAfterTreeDeserialize() {
		}

		public void onAfterTreeSerialize() {
		}

		public void onBeforeTreeDeserialize() {
		}

		public void onBeforeTreeSerialize() {
		}
	}
}
