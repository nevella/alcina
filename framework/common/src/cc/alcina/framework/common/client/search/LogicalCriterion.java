package cc.alcina.framework.common.client.search;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.SearchHandlers;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.NestedName;

/**
 */
@Bean(PropertySource.FIELDS)
public abstract class LogicalCriterion extends SearchCriterion
		implements HasValue<LogicalCriterion.Criteria> {
	@TypedProperties
	public static class Criteria extends Bindable.Fields
			implements TreeSerializable {
		public List<SearchCriterion> criteria = new ArrayList<>();

		@Override
		public String toString() {
			return criteria.toString();
		}
	}

	public abstract static class Handler<LC extends LogicalCriterion>
			extends DomainCriterionHandler<LC> {
		@Override
		public Class<? extends SearchDefinition> handlesSearchDefinition() {
			return SearchDefinition.class;
		}

		public static class None extends Handler<LogicalCriterion.None> {
		}

		public static class Any extends Handler<LogicalCriterion.Any> {
		}

		public static class All extends Handler<LogicalCriterion.All> {
		}

		static class Sub {
			SearchCriterion criterion;

			DomainFilter filter;

			Predicate predicate;

			Sub(SearchCriterion criterion) {
				this.criterion = criterion;
				this.filter = SearchHandlers.getCriterionHandler(
						SearchHandlers.CONTEXT_DEFINITION.getTyped(), criterion)
						.getFilter(criterion);
				this.predicate = this.filter.asPredicate();
			}
		}

		List<Sub> subs;

		LogicalCriterion criterion;

		IntPair criteriaMatchCount;

		@Override
		public DomainFilter getFilter(LC criterion) {
			this.criterion = criterion;
			subs = criterion.getValue().criteria.stream().map(Sub::new)
					.toList();
			criteriaMatchCount = criterion.criteriaMatchCount();
			return new DomainFilter(new CountingPredicate());
		}

		class CountingPredicate implements Predicate {
			@Override
			public boolean test(Object t) {
				int passed = 0;
				for (int idx = 0; idx < subs.size(); idx++) {
					Sub sub = subs.get(idx);
					boolean test = sub.predicate.test(t);
					if (test) {
						passed++;
					}
					if (passed > criteriaMatchCount.i2) {
						return false;
					}
					int maxPossible = passed + subs.size() - idx - 1;
					if (maxPossible < criteriaMatchCount.i1) {
						return false;
					}
				}
				return true;
			}
		}
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", NestedName.get(this), value);
	}

	@XmlTransient
	public Criteria value = new Criteria();

	public static class None extends LogicalCriterion {
		@Override
		protected IntPair criteriaMatchCount() {
			return IntPair.ZERO;
		}
	}

	public static class Any extends LogicalCriterion {
		@Override
		protected IntPair criteriaMatchCount() {
			return IntPair.of(1, getValue().criteria.size());
		}
	}

	public static class All extends LogicalCriterion {
		@Override
		protected IntPair criteriaMatchCount() {
			return IntPair.point(getValue().criteria.size());
		}
	}

	protected abstract IntPair criteriaMatchCount();

	public LogicalCriterion() {
		setDisplayName("");
		setOperator(null);
	}

	public Criteria getValue() {
		return value;
	}

	public void setValue(Criteria value) {
		set("value", this.value, value, () -> this.value = value);
	}

	public void add(SearchCriterion searchCriterion) {
		getValue().criteria.add(searchCriterion);
	}
}
