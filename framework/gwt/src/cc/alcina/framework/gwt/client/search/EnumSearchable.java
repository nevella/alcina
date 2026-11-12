package cc.alcina.framework.gwt.client.search;

import java.util.Arrays;

import cc.alcina.framework.common.client.search.EnumCriterion;
import cc.alcina.framework.common.client.search.StandardSearchOperator;
import cc.alcina.framework.common.client.util.HasDisplayName.PreferDisplayNameRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.ui.AbstractBoundWidget;

public abstract class EnumSearchable<E extends Enum, C extends EnumCriterion<E>>
		extends FlatSearchable<C> {
	protected Class<E> enumClass;

	protected int maxSelectedItems = 999;

	public EnumSearchable(Class<C> clazz, Class<E> enumClass, String objectName,
			String criteriaName) {
		super(clazz, objectName, criteriaName,
				Arrays.asList(StandardSearchOperator.EQUALS));
		this.enumClass = enumClass;
	}

	@Override
	public AbstractBoundWidget createEditor() {
		return new FlatSearchSelector(enumClass, 1,
				PreferDisplayNameRenderer.INSTANCE,
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
}