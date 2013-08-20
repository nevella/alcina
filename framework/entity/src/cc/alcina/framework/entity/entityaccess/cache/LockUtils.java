package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class LockUtils {
	public static final String ARTICLE_DELETION_LOCK="ARTICLE_DELETION_LOCK";
	private static Map<ClassIdLock, WeakReference<ClassIdLock>> classIdLocks = new WeakHashMap<ClassIdLock, WeakReference<ClassIdLock>>();

	public static synchronized ClassIdLock obtainClassIdLock(Class clazz, long id) {
		ClassIdLock key = new ClassIdLock(clazz, id);
		if (!classIdLocks.containsKey(key)) {
			classIdLocks.put(key, new WeakReference<ClassIdLock>(key));
		}
		return classIdLocks.get(key).get();
	}

	static class ClassStringKeyLock {
		Class clazz;

		String key;

		public ClassStringKeyLock(Class clazz, String key) {
			this.clazz = clazz;
			this.key = key;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassStringKeyLock) {
				ClassStringKeyLock lock = (ClassStringKeyLock) obj;
				return lock.clazz == clazz && lock.key.equals(key);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return clazz.hashCode() ^ key.hashCode();
		}
	}

	private static Map<ClassStringKeyLock, ClassStringKeyLock> classStringKeyLocks = new HashMap<ClassStringKeyLock, ClassStringKeyLock>();
	public static synchronized Object obtainStringKeyLock(
			String key) {
		return obtainClassStringKeyLock(LockUtils.class, key);
	}
	public static synchronized Object obtainClassStringKeyLock(Class clazz,
			String key) {
		ClassStringKeyLock lock = new ClassStringKeyLock(clazz, key);
		if (!classStringKeyLocks.containsKey(lock)) {
			classStringKeyLocks.put(lock, lock);
		}
		return classStringKeyLocks.get(lock);
	}
}
