package cc.alcina.framework.gwt.client.entity.search.searchable;

import java.util.Arrays;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.search.DateRange;
import cc.alcina.framework.common.client.search.DateRangeEnumCriterion;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class DatePeriodTypeSearchable
		extends FlatSearchable<DateRangeEnumCriterion> {
	public DatePeriodTypeSearchable() {
		super(DateRangeEnumCriterion.class, "Date", "Range",
				Arrays.asList(StandardSearchOperator.EQUALS));
	}

	@Override
	public AbstractBoundWidget createEditor() {
		return new FlatSearchSelector(DateRange.class, 1,
				FriendlyEnumRenderer.INSTANCE,
				() -> Arrays.asList(DateRange.values()));
	}

	@Override
	public String getCriterionPropertyName() {
		return "value";
	}

	@Override
	public boolean hasValue(DateRangeEnumCriterion sc) {
		return sc.getValue() != null;
	}
}