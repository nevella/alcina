package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Set;
import java.util.function.Function;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.search.TruncatedObjectCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSuggestorSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseTruncatedObjectCriterionPack {
	public interface BaseTruncatedObjectCriterionHandler<I extends HasId, O extends HasIdAndLocalId> {
		Function<I, O> getLinkedObjectMapper();

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
					HasIdAndLocalId linked = getLinkedObjectMapper().apply(i);
					return linked != null && linked.getId() == id;
				}
			}).invertIf(
					sc.getOperator() == StandardSearchOperator.DOES_NOT_EQUAL);
		}
	}

	public interface BaseTruncatedObjectMultipleCriterionHandler<I extends HasId, O extends HasIdAndLocalId> {
		Function<I, Set<O>> getLinkedObjectMapper();

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
		public boolean isNonDefaultValue(TC sc) {
			sc.ensurePlaceholderObject();
			return super.isNonDefaultValue(sc);
		}

		@Override
		public boolean hasValue(TC tc) {
			return tc.getId() != 0;
		}
	}
}