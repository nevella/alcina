package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Arrays;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.search.EnumCriterion;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseEnumCriterionPack {
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
		public boolean hasValue(C sc) {
			return sc.getValue() != null;
		}

		@Override
		public String getCriterionPropertyName() {
			return "value";
		}

		protected <T extends BaseEnumCriterionSearchable> T
				maxSelectedItems(int maxSelectedItems) {
			this.maxSelectedItems = maxSelectedItems;
			return (T) this;
		}

		@Override
		public AbstractBoundWidget createEditor() {
			return new FlatSearchSelector(enumClass, maxSelectedItems,
					FriendlyEnumRenderer.INSTANCE,
					() -> Arrays.asList(enumClass.getEnumConstants()));
		}
	}

	public interface BaseEnumCriterionHandler<T, E extends Enum, C extends EnumCriterion<E>> {
		default CacheFilter getFilter0(C sc) {
			E e = sc.getValue();
			if (e == null) {
				return null;
			}
			return new CacheFilter(new CollectionFilter<T>() {
				@Override
				public boolean allow(T t) {
					return BaseEnumCriterionHandler.this.test(t, e);
				}
			}).invertIf(
					sc.getOperator() == StandardSearchOperator.DOES_NOT_EQUAL);
		}

		public boolean test(T t, E value);
	}
}