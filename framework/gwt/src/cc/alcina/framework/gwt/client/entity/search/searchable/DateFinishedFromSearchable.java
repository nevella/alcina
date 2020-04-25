package cc.alcina.framework.gwt.client.entity.search.searchable;

import cc.alcina.framework.gwt.client.entity.search.FinishedFromCriterion;
import cc.alcina.framework.gwt.client.objecttree.search.DateCriterionSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

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