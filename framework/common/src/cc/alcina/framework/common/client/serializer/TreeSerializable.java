package cc.alcina.framework.common.client.serializer;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.StringMap;

/**
 *
 * <p>
 * That is used by app server code to generate the serialization signature of
 * the reachable serializables at startup time, and to validate the serializable
 * classes (most importantly that any non-property fields are transient)
 *
 * <p>
 * Note that - as per {@link FlatTreeSerializer}, Collection properties cannot
 * contain nulls, and cannot *be* nulls.
 *
 * @author nick@alcina.cc
 *
 */
@Registration(TreeSerializable.class)
public interface TreeSerializable extends Serializable {
	public static final String CONTEXT_IGNORE_CUSTOM_CHECKS = TreeSerializable.class
			.getName() + ".CONTEXT_IGNORE_CUSTOM_CHECKS";

	default <T extends TreeSerializable> T copy() {
		return (T) FlatTreeSerializer.clone(this);
	}

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

		public String mapKeys(String serialized, boolean to) {
			if (serializable == null) {
				return serialized;
			}
			TypeSerialization typeSerialization = Reflections.at(serializable)
					.annotation(TypeSerialization.class);
			if (typeSerialization != null
					&& typeSerialization.keyPrefixMappings().length > 0) {
				serialized = mapKeys(serialized, typeSerialization, to);
			}
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

		private String mapKeys(String serialized,
				TypeSerialization typeSerialization, boolean to) {
			Map<String, String> mappings = new LinkedHashMap<>();
			String[] prefixMappings = typeSerialization.keyPrefixMappings();
			for (int idx = 0; idx < prefixMappings.length; idx += 2) {
				if (to) {
					mappings.put(prefixMappings[idx], prefixMappings[idx + 1]);
				} else {
					mappings.put(prefixMappings[idx + 1], prefixMappings[idx]);
				}
			}
			StringMap keyValues = StringMap.fromPropertyString(serialized);
			StringMap replacements = new StringMap();
			boolean modified = false;
			for (String key : keyValues.keySet()) {
				for (String mapKey : mappings.keySet()) {
					if (key.startsWith(mapKey)) {
						if (key.length() == mapKey.length() || key
								.length() > mapKey.length()
								&& (key.charAt(mapKey.length()) == '.' || key
										.charAt(mapKey.length()) == '-')) {
							String replacementKey = mappings.get(mapKey)
									+ key.substring(mapKey.length());
							replacements.put(key, replacementKey);
							modified = true;
						}
					}
				}
			}
			if (modified) {
				replacements.entrySet().forEach(e -> {
					String value = keyValues.remove(e.getKey());
					keyValues.put(e.getValue(), value);
				});
				return keyValues.sorted(new FlatTreeSerializer.KeyComparator())
						.toPropertyString();
			} else {
				return serialized;
			}
		}
	}

	// marker for deserialization
	public interface NonMultiple {
	}
}
