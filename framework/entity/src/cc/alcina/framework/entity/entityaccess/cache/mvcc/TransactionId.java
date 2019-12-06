package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.Objects;

public class TransactionId {
	public long id;

	public TransactionId(long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TransactionId) {
			TransactionId other = (TransactionId) obj;
			return Objects.equals(id, other.id);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (int) id;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}
}
