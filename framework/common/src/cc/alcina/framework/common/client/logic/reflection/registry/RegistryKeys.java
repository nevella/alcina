package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.LinkedHashMap;
import java.util.Map;

public class RegistryKeys {
	Map<String, RegistryKey> keys = new LinkedHashMap<>();

	final RegistryKey emptyLookupKey = get(Void.class);

	final RegistryKey undefinedTargetKey = get(void.class);

	public RegistryKeys() {
	}

	public RegistryKey get(Class<?> clazz) {
		String name = clazz.getName();
		RegistryKey key = keys.get(name);
		if (key == null) {
			key = new RegistryKey(clazz);
			keys.put(name, key);
		}
		key.ensureClazz(clazz);
		return key;
	}

	public RegistryKey get(String name) {
		RegistryKey key = keys.get(name);
		if (key == null) {
			key = new RegistryKey(name);
			keys.put(name, key);
		}
		return key;
	}

	public RegistryKey emptyLookupKey() {
		return emptyLookupKey;
	}

	public RegistryKey undefinedTargetKey() {
		return undefinedTargetKey;
	}

	public boolean isUndefinedTargetKey(RegistryKey key) {
		return key == undefinedTargetKey;
	}
}
