package cc.alcina.framework.gwt.client.data.search.searchable;

import cc.alcina.framework.gwt.client.data.search.FinishedToCriterion;
import cc.alcina.framework.gwt.client.objecttree.search.DateCriterionSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class DateFinishedToSearchable
		extends DateCriterionSearchable<FinishedToCriterion> {
	public DateFinishedToSearchable() {
		this("Date", "To");
	}

	public DateFinishedToSearchable(String category, String name) {
		super(FinishedToCriterion.class, category, name,
				StandardSearchOperator.EQUAL);
	}
}