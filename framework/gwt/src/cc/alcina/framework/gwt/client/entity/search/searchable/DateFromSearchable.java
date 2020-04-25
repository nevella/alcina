package cc.alcina.framework.gwt.client.entity.search.searchable;

import cc.alcina.framework.gwt.client.entity.search.CreatedFromCriterion;
import cc.alcina.framework.gwt.client.objecttree.search.DateCriterionSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class DateFromSearchable
		extends DateCriterionSearchable<CreatedFromCriterion> {
	public DateFromSearchable() {
		this("Date", "From");
	}

	public DateFromSearchable(String category, String name) {
		super(CreatedFromCriterion.class, category, name,
				StandardSearchOperator.EQUAL);
	}
}