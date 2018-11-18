package cc.alcina.framework.gwt.client.objecttree.search.packs;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.search.EnumMultipleCriterion;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class BaseMultipleEnumCriterionPack {
	public static abstract class BaseEnumMultipleCriterion<E extends Enum>
			extends EnumMultipleCriterion<E> {
		private Set<E> value = new LinkedHashSet<>();

		public BaseEnumMultipleCriterion<E> add(E e) {
			getValue().add(e);
			return this;
		}

		public BaseEnumMultipleCriterion<E> add(Set<E> e) {
			getValue().addAll(e);
			return this;
		}

		public Set<E> getValue() {
			return this.value;
		}

		public void setValue(Set<E> value) {
			Set<E> old_value = this.value;
			this.value = value;
			propertyChangeSupport().firePropertyChange("value", old_value,
					value);
		}
	}

	public static abstract class BaseEnumMultipleCriterionSearchable<E extends Enum, C extends EnumMultipleCriterion<E>>
			extends FlatSearchable<C> {
		protected Class<E> enumClass;

		protected int maxSelectedItems = 999;

		public BaseEnumMultipleCriterionSearchable(Class<C> clazz,
				Class<E> enumClass, String objectName, String criteriaName) {
			super(clazz, objectName, criteriaName,
					Arrays.asList(StandardSearchOperator.CONTAINS,
							StandardSearchOperator.DOES_NOT_CONTAIN));
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
			return sc.getValue() != null && sc.getValue().size() > 0;
		}

		protected <T extends BaseEnumMultipleCriterionSearchable> T
				maxSelectedItems(int maxSelectedItems) {
			this.maxSelectedItems = maxSelectedItems;
			return (T) this;
		}
	}

	public interface BaseMultipleEnumCriterionHandler<T, E extends Enum, SC extends EnumMultipleCriterion<E>> {
		public boolean test(T t, Set<E> value);

		default DomainFilter getFilter0(SC sc) {
			Set<E> values = sc.getValue();
			if (values.isEmpty()) {
				return null;
			}
			Predicate<T> pred = t -> test(t, values);
			return new DomainFilter(pred).invertIf(sc
					.getOperator() == StandardSearchOperator.DOES_NOT_CONTAIN);
		}
	}
}