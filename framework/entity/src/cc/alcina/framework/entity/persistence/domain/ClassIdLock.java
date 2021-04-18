package cc.alcina.framework.entity.persistence.domain;

import cc.alcina.framework.common.client.util.Ax;

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
			return lock.clazz == clazz && lock.id.longValue() == id.longValue();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return clazz.hashCode() ^ id.hashCode();
	}

	@Override
	public String toString() {
		return Ax.format("%s::%s - %s", clazz, id, super.toString());
	}
}