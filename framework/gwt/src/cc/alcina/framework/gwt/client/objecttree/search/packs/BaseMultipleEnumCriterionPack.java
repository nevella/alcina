package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.cache.CacheFilter;
import cc.alcina.framework.common.client.search.EnumMultipleCriterion;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseMultipleEnumCriterionPack {
	public static abstract class BaseEnumMultipleCriterionSearchable<E extends Enum, C extends EnumMultipleCriterion<E>>
			extends FlatSearchable<C> {
		private Class<E> enumClass;

		private int maxSelectedItems = 999;

		public BaseEnumMultipleCriterionSearchable(Class<C> clazz,
				Class<E> enumClass, String objectName, String criteriaName) {
			super(clazz, objectName, criteriaName,
					Arrays.asList(StandardSearchOperator.CONTAINS,
							StandardSearchOperator.DOES_NOT_CONTAIN));
			this.enumClass = enumClass;
		}

		@Override
		public boolean hasValue(C sc) {
			return sc.getValue() != null && sc.getValue().size() > 0;
		}

		@Override
		public String getCriterionPropertyName() {
			return "value";
		}

		protected <T extends BaseEnumMultipleCriterionSearchable> T
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

	public interface BaseMultipleEnumCriterionHandler<T, E extends Enum, SC extends EnumMultipleCriterion<E>> {
		default CacheFilter getFilter0(SC sc) {
			Set<E> values = sc.getValue();
			if (values.isEmpty()) {
				return null;
			}
			Predicate<T> pred = t -> test(t, values);
			return new CacheFilter(pred).invertIf(sc
					.getOperator() == StandardSearchOperator.DOES_NOT_CONTAIN);
		}

		public boolean test(T t, Set<E> value);
	}
}