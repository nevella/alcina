package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Set;
import java.util.function.Function;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSuggestorSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseTruncatedObjectCriterionPack {
	public interface BaseTruncatedObjectCriterionHandler<I extends HasId, O extends Entity> {
		default DomainFilter getFilter0(TruncatedObjectCriterion<O> sc) {
			long id = sc.getId();
			if (id == 0) {
				return null;
			}
			if (getPropertyPath() != null
					&& sc.getOperator() == StandardSearchOperator.EQUALS) {
				return new DomainFilter(getPropertyPath(),
						Domain.find(sc.getObjectClass(), sc.getId()));
			}
			return new DomainFilter(new CollectionFilter<I>() {
				@Override
				public boolean allow(I i) {
					if (i == null) {
						return false;
					}
					Entity linked = getLinkedObjectMapper().apply(i);
					return linked != null && linked.getId() == id;
				}
			}).invertIf(
					sc.getOperator() == StandardSearchOperator.DOES_NOT_EQUAL);
		}

		Function<I, O> getLinkedObjectMapper();

		default String getPropertyPath() {
			return null;
		}
	}

	public static abstract class BaseTruncatedObjectCriterionSearchable<TC extends TruncatedObjectCriterion>
			extends FlatSuggestorSearchable<TC> {
		public BaseTruncatedObjectCriterionSearchable(String category,
				Class<TC> clazz) {
			super(clazz, category, "", StandardSearchOperator.EQUAL_OR_NOT);
			this.name = Ax.friendly(Reflections.classLookup()
					.getTemplateInstance(getCriterionClass()).getObjectClass()
					.getSimpleName());
		}

		@Override
		public boolean hasValue(TC tc) {
			return tc.getId() != 0;
		}

		public <S extends BaseTruncatedObjectCriterionSearchable> S
				withOverrideName(String name) {
			this.name = name;
			this.category = "";
			return (S) this;
		}

		@Override
		public boolean isNonDefaultValue(TC sc) {
			sc.ensurePlaceholderObject();
			return super.isNonDefaultValue(sc);
		}
	}

	public interface BaseTruncatedObjectMultipleCriterionHandler<I extends HasId, O extends Entity> {
		default DomainFilter getFilter0(TruncatedObjectCriterion<O> sc) {
			long id = sc.getId();
			if (id == 0) {
				return null;
			}
			return new DomainFilter(new CollectionFilter<I>() {
				@Override
				public boolean allow(I i) {
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