package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.DomainCriterionFilter;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.EntityCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSuggestorSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class EntityCriterionPack {
	public interface EntityCriterionHandler<I, O extends Entity, SC extends EntityCriterion<O>>
			extends DomainCriterionFilter<SC> {
		default Comparator getComparator() {
			return null;
		}

		@Override
		default DomainFilter getFilter(SC sc) {
			long id = sc.getId();
			if (id == 0) {
				return null;
			}
			if (getPropertyPath() != null
					&& sc.getOperator() == StandardSearchOperator.EQUALS) {
				return new DomainFilter(getPropertyPath(),
						Domain.find(sc.getObjectClass(), sc.getId()));
			}
			return new DomainFilter(new Predicate<I>() {
				@Override
				public boolean test(I i) {
					if (i == null) {
						return false;
					}
					Entity linked = getLinkedObjectMapper().apply(i);
					if (linked == null) {
						return false;
					}
					if (sc.getOperator().isOrdered()) {
						return sc.getOperator().evaluateComparatorResult(
								getComparator().compare(linked, sc.getValue()));
					}
					return linked.getId() == id;
				}
			}).invertIf(
					sc.getOperator() == StandardSearchOperator.DOES_NOT_EQUAL);
		}

		Function<I, O> getLinkedObjectMapper();

		default String getPropertyPath() {
			return null;
		}
	}

	public static abstract class BaseEntityCriterionSearchable<TC extends EntityCriterion>
			extends FlatSuggestorSearchable<TC> {
		public BaseEntityCriterionSearchable(String category, Class<TC> clazz) {
			super(clazz, category, "", StandardSearchOperator.EQUAL_OR_NOT);
			this.name = Ax.friendly(Reflections.at(getCriterionClass())
					.templateInstance().getObjectClass().getSimpleName());
		}

		@Override
		public boolean hasValue(TC tc) {
			return tc.getId() != 0;
		}

		@Override
		public boolean isNonDefaultValue(TC sc) {
			sc.ensurePlaceholderObject();
			return super.isNonDefaultValue(sc);
		}

		public <S extends BaseEntityCriterionSearchable> S
				withOverrideName(String name) {
			this.name = name;
			this.category = "";
			return (S) this;
		}
	}

	public interface BaseTruncatedObjectMultipleCriterionHandler<I extends HasId, O extends Entity, SC extends EntityCriterion<O>>
			extends DomainCriterionFilter<SC> {
		@Override
		default DomainFilter getFilter(SC sc) {
			long id = sc.getId();
			if (id == 0) {
				return null;
			}
			return new DomainFilter(new Predicate<I>() {
				@Override
				public boolean test(I i) {
					if (i == null) {
						return false;
					}
					Set<O> linked = getLinkedObjectMapper().apply(i);
					return linked.stream().anyMatch(li -> li.getId() == id);
				}
			}).invertIf(
					sc.getOperator() == StandardSearchOperator.DOES_NOT_EQUAL);
		}

		Function<I, Set<O>> getLinkedObjectMapper();
	}
}