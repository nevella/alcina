package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.CollectionCreators;

public class RegistryKeys {
	Map<String, RegistryKey> keys = CollectionCreators.Bootstrap
			.createConcurrentStringMap();

	final RegistryKey emptyLookupKey = get(Void.class);

	final RegistryKey undefinedTargetKey = get(void.class);

	public RegistryKeys() {
	}

	public RegistryKey emptyLookupKey() {
		return emptyLookupKey;
	}

	public RegistryKey get(Class<?>... classes) {
		String name = RegistryKey.nameFor(classes);
		return keys.computeIfAbsent(name,
				k -> new RegistryKey(k).ensureClases(classes));
	}

	public RegistryKey get(String name) {
		return keys.computeIfAbsent(name, RegistryKey::new);
	}

	public RegistryKey getFirst(Class[] value) {
		return get(new Class[] { value[0] });
	}

	public RegistryKey getNonFirst(Class[] value) {
		Preconditions.checkArgument(value.length <= 2);
		return value.length == 1 ? undefinedTargetKey
				: get(new Class[] { value[1] });
	}

	public boolean isUndefinedTargetKey(RegistryKey key) {
		return key == undefinedTargetKey;
	}

	public RegistryKey undefinedTargetKey() {
		return undefinedTargetKey;
	}
}
