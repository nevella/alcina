package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.Map;

public class RegistryKeys {
	Map<String, RegistryKey> keys = Registry.delegateCreator
			.createDelegateMap(0, 0);

	final RegistryKey emptyLookupKey = get(Void.class);

	final RegistryKey undefinedTargetKey = get(void.class);

	public RegistryKeys() {
	}

	public RegistryKey emptyLookupKey() {
		return emptyLookupKey;
	}

	public RegistryKey get(Class<?> clazz) {
		String name = clazz.getName();
		RegistryKey key = keys.get(name);
		if (key == null) {
			synchronized (keys) {
				key = keys.get(name);
				if (key == null) {
					key = new RegistryKey(clazz);
					keys.put(name, key);
					key.ensureClazz(clazz);
				}
			}
		}
		return key;
	}

	public RegistryKey get(String name) {
		RegistryKey key = keys.get(name);
		if (key == null) {
			synchronized (keys) {
				key = keys.get(name);
				if (key == null) {
					key = new RegistryKey(name);
					keys.put(name, key);
				}
			}
		}
		return key;
	}

	public boolean isUndefinedTargetKey(RegistryKey key) {
		return key == undefinedTargetKey;
	}

	public RegistryKey undefinedTargetKey() {
		return undefinedTargetKey;
	}
}
