package cc.alcina.framework.gwt.client.data.search.searchable;

import cc.alcina.framework.gwt.client.data.search.FinishedFromCriterion;
import cc.alcina.framework.gwt.client.objecttree.search.DateCriterionSearchable;

public class DateFinishedFromSearchable
		extends DateCriterionSearchable<FinishedFromCriterion> {
	public DateFinishedFromSearchable() {
		this("Date", "From");
	}

	public DateFinishedFromSearchable(String category, String name) {
		super(FinishedFromCriterion.class, category, name);
	}
}