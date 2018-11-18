package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Arrays;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.search.EnumCriterion;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseEnumCriterionPack {
	public interface BaseEnumCriterionHandler<T, E extends Enum, C extends EnumCriterion<E>> {
		public boolean test(T t, E value);

		default DomainFilter getFilter0(C sc) {
			E e = sc.getValue();
			if (e == null) {
				return null;
			}
			return new DomainFilter(new CollectionFilter<T>() {
				@Override
				public boolean allow(T t) {
					return BaseEnumCriterionHandler.this.test(t, e);
				}
			}).invertIf(
					sc.getOperator() == StandardSearchOperator.DOES_NOT_EQUAL);
		}
	}

	public static abstract class BaseEnumCriterionSearchable<E extends Enum, C extends EnumCriterion<E>>
			extends FlatSearchable<C> {
		private Class<E> enumClass;

		private int maxSelectedItems = 1;

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