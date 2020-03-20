package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.domain.Entity;

class ObjectVersion<T extends Entity> {
	T object;

	Transaction transaction;

	boolean writeable;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ObjectVersion) {
			ObjectVersion other = (ObjectVersion) obj;
			/*
			 * will never be compared when object!=obj.object
			 */
			return Objects.equals(this.transaction, other.transaction);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return transaction.hashCode();
	}

	/*
	 * only incorrect if state is write and we're not a writeable version
	 */
	public boolean isCorrectWriteableState(boolean write) {
		return !write || writeable;
	}
}
