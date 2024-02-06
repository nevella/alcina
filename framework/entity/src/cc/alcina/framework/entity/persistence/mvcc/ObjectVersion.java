package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;

class ObjectVersion<T> {
	T object;

	Transaction transaction;

	boolean writeable;

	public ObjectVersion() {
	}

	void debugObjectHash() {
		Ax.out("\t debug object hash: %s", System.identityHashCode(object));
	}

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

	@Override
	public String toString() {
		if (object instanceof Entity) {
			return Ax.format("(%s) - %s", writeable,
					((Entity) object).toLocator());
		} else {
			return Ax.format("(%s) - %s - %s", writeable,
					object.getClass().getSimpleName(), object);
		}
	}
}
