package cc.alcina.framework.gwt.client.entity.search.searchable;

import cc.alcina.framework.common.client.domain.search.criterion.FinishedFromCriterion;
import cc.alcina.framework.common.client.search.StandardSearchOperator;
import cc.alcina.framework.gwt.client.objecttree.search.DateCriterionSearchable;

public class DateFinishedFromSearchable
		extends DateCriterionSearchable<FinishedFromCriterion> {
	public DateFinishedFromSearchable() {
		this("Date", "From");
	}

	public DateFinishedFromSearchable(String category, String name) {
		super(FinishedFromCriterion.class, category, name,
				StandardSearchOperator.EQUAL);
	}
}