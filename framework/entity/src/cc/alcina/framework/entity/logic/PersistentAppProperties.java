package cc.alcina.framework.entity.logic;

import java.util.Optional;

import cc.alcina.framework.common.client.logic.domain.UserProperty;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

public class PersistentAppProperties {
	public static <T> T ensure(Class<T> clazz) {
		return Permissions.callWithPushedSystemUserIfNeededNoThrow(() -> {
			return UserProperty.ensure(clazz).deserialize();
		});
	}

	public static Optional<String> get(String key) {
		return Permissions.callWithPushedSystemUserIfNeededNoThrow(() -> {
			return UserProperty.byKey(key).map(UserProperty::getValue);
		});
	}

	public static boolean getBoolean(String key) {
		return get(key).map(Boolean::parseBoolean).orElse(false);
	}

	public static long getLong(String key) {
		return get(key).map(Long::parseLong).orElse(0L);
	}

	public static <T> Optional<T> getObject(Class<T> clazz, String key) {
		return Permissions.callWithPushedSystemUserIfNeededNoThrow(() -> {
			return UserProperty.byKey(key).map(UserProperty::deserialize);
		});
	}

	public static <T> void persistSingleton(T t) {
		Transaction.commit();
		Permissions.runWithPushedSystemUserIfNeeded(() -> {
			UserProperty.ensure(t.getClass()).serializeObject(t);
			Transaction.commit();
		});
	}

	public static void set(String key, String value) {
		Transaction.commit();
		Permissions.runWithPushedSystemUserIfNeeded(() -> {
			UserProperty.ensure(key).setValue(value);
			Transaction.commit();
		});
	}

	public static void setLong(String key, long l) {
		set(key, String.valueOf(l));
	}
}
