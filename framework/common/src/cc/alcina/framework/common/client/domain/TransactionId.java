package cc.alcina.framework.common.client.domain;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

@Bean(PropertySource.FIELDS)
public class TransactionId implements Comparable<TransactionId> {
	public long id;

	TransactionId() {
	}

	public TransactionId(long id) {
		this.id = id;
	}

	@Override
	public int compareTo(TransactionId o) {
		return id < o.id ? -1 : id == o.id ? 0 : 1;
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
