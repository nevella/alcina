package cc.alcina.framework.entity.persistence.cache;

public class ClassIdLock {
	Class clazz;

	Long id;

	public ClassIdLock(Class clazz, Long id) {
		super();
		this.clazz = clazz;
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassIdLock) {
			ClassIdLock lock = (ClassIdLock) obj;
			return lock.clazz == clazz && lock.id.longValue() == id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return clazz.hashCode() ^ id.hashCode();
	}
}