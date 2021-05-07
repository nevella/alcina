package cc.alcina.framework.common.client.domain;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.domain.FilterCost.HasFilterCost;
import cc.alcina.framework.common.client.logic.domain.Entity;

public interface IndexedValueProvider<E extends Entity> extends HasFilterCost {
	public StreamOrSet<E> getKeyMayBeCollection(Object value);

	public static class StreamOrSet<E extends Entity> {
		private Set<E> set;

		private Stream<E> stream;

		public StreamOrSet(Set<E> set) {
			this.set = set;
		}

		public StreamOrSet(Stream<E> stream) {
			this.stream = stream;
		}

		public Set<E> provideSet() {
			return set == null ? stream.collect(Collectors.toSet()) : set;
		}

		public Stream<E> provideStream() {
			return stream == null ? set.stream() : stream;
		}
	}
}
