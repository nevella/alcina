package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

class ObjectVersion<T extends HasIdAndLocalId> {
	T object;

	Transaction transaction;

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
}
