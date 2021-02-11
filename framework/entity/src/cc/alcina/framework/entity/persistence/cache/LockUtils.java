package cc.alcina.framework.entity.persistence.cache;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;

public class LockUtils {
	private static Map<ClassIdLock, WeakReference<ClassIdLock>> classIdLocks = new WeakHashMap<ClassIdLock, WeakReference<ClassIdLock>>();

	private static Map<ClassStringKeyLock, ClassStringKeyLock> classStringKeyLocks = new HashMap<ClassStringKeyLock, ClassStringKeyLock>();

	private static Logger logger = LoggerFactory.getLogger(LockUtils.class);

	public static synchronized ClassIdLock obtainClassIdLock(Class clazz,
			long id) {
		return obtainClassIdLock(clazz, id, true);
	}

	public static synchronized ClassIdLock obtainClassIdLock(Class clazz,
			long id, boolean log) {
		ClassIdLock key = new ClassIdLock(clazz, id);
		if (!classIdLocks.containsKey(key)) {
			classIdLocks.put(key, new WeakReference<ClassIdLock>(key));
		}
		if (log) {
			logger.info(
					"Obtained classIdLock - {} - hash {} - identity hash {}",
					key, key.hashCode(), System.identityHashCode(key));
		}
		return classIdLocks.get(key).get();
	}

	public static ClassIdLock obtainClassIdLock(Entity entity) {
		return obtainClassIdLock(entity.entityClass(), entity.getId());
	}

	public static synchronized ClassStringKeyLock
			obtainClassStringKeyLock(Class clazz, String key) {
		ClassStringKeyLock lock = new ClassStringKeyLock(clazz, key);
		if (!classStringKeyLocks.containsKey(lock)) {
			classStringKeyLocks.put(lock, lock);
		}
		return classStringKeyLocks.get(lock);
	}

	public static synchronized ClassStringKeyLock
			obtainStringKeyLock(String key) {
		return obtainClassStringKeyLock(LockUtils.class, key);
	}

	public static class ClassStringKeyLock {
		Class clazz;

		String key;

		private ReentrantLock lock;

		public ClassStringKeyLock(Class clazz, String key) {
			this.clazz = clazz;
			this.key = key;
			this.lock = new ReentrantLock();
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

		public void lock() {
			this.lock.lock();
		}

		public void unlock() {
			this.lock.unlock();
		}
	}
}
