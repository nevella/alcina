package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Arrays;
import java.util.function.Predicate;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.DomainCriterionFilter;
import cc.alcina.framework.common.client.search.EnumCriterion;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseEnumCriterionPack {
	public interface BaseEnumCriterionHandler<T, E extends Enum, C extends EnumCriterion<E>>
			extends DomainCriterionFilter<C> {
		@Override
		default DomainFilter getFilter(C sc) {
			E e = sc.getValue();
			if (e == null) {
				return null;
			}
			return new DomainFilter(new Predicate<T>() {
				@Override
				public boolean test(T t) {
					return BaseEnumCriterionHandler.this.test(t, e);
				}
			}).invertIf(
					sc.getOperator() == StandardSearchOperator.DOES_NOT_EQUAL);
		}

		public boolean test(T t, E value);
	}

	public static abstract class BaseEnumCriterionSearchable<E extends Enum, C extends EnumCriterion<E>>
			extends FlatSearchable<C> {
		protected Class<E> enumClass;

		protected int maxSelectedItems = 1;

		public BaseEnumCriterionSearchable(Class<C> clazz, Class<E> enumClass,
				String objectName, String criteriaName) {
			super(clazz, objectName, criteriaName,
					Arrays.asList(StandardSearchOperator.EQUALS,
							StandardSearchOperator.DOES_NOT_EQUAL));
			this.enumClass = enumClass;
		}

		@Override
		public AbstractBoundWidget createEditor() {
			return new FlatSearchSelector(enumClass, maxSelectedItems,
					FriendlyEnumRenderer.INSTANCE,
					() -> Arrays.asList(enumClass.getEnumConstants()));
		}

		@Override
		public String getCriterionPropertyName() {
			return "value";
		}

		@Override
		public boolean hasValue(C sc) {
			return sc.getValue() != null;
		}

		protected <T extends BaseEnumCriterionSearchable> T
				maxSelectedItems(int maxSelectedItems) {
			this.maxSelectedItems = maxSelectedItems;
			return (T) this;
		}
	}
}