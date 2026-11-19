package cc.alcina.framework.gwt.client.search;

import java.util.Arrays;

import cc.alcina.framework.common.client.search.EnumMultipleCriterion;
import cc.alcina.framework.common.client.search.StandardSearchOperator;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.ui.AbstractBoundWidget;

public abstract class EnumMultipleSearchable<E extends Enum, C extends EnumMultipleCriterion<E>>
		extends FlatSearchable<C> {
	protected Class<E> enumClass;

	protected int maxSelectedItems = 999;

	public EnumMultipleSearchable(Class<C> clazz, Class<E> enumClass,
			String objectName, String criteriaName) {
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

	protected <T extends EnumMultipleSearchable> T
			maxSelectedItems(int maxSelectedItems) {
		this.maxSelectedItems = maxSelectedItems;
		return (T) this;
	}
}