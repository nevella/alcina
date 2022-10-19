package cc.alcina.framework.entity.persistence.domain;

import java.util.function.Predicate;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.util.Ax;

public class NotCacheFilter extends DomainFilter {
	private DomainFilter filter;

	public NotCacheFilter(DomainFilter filter) {
		super(null);
		this.filter = filter;
		this.setPredicate(new NotPredicate<>(filter.asPredicate()));
	}

	@Override
	public boolean canFlatten() {
		return filter.canFlatten();
	}

	// preserves toString
	static class NotPredicate<T> implements Predicate<T> {
		private Predicate predicate;

		NotPredicate(Predicate predicate) {
			this.predicate = predicate;
		}

		@Override
		public boolean test(T t) {
			return !predicate.test(t);
		}

		@Override
		public String toString() {
			return Ax.format("NOT(%s)", predicate);
		}
	}
}