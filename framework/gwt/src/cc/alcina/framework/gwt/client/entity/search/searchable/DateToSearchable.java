package cc.alcina.framework.gwt.client.entity.search.searchable;

import cc.alcina.framework.gwt.client.entity.search.CreatedToCriterion;
import cc.alcina.framework.gwt.client.objecttree.search.DateCriterionSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class DateToSearchable
		extends DateCriterionSearchable<CreatedToCriterion> {
	public DateToSearchable() {
		this("Date", "To");
	}

	public DateToSearchable(String category, String name) {
		super(CreatedToCriterion.class, category, name,
				StandardSearchOperator.EQUAL);
	}
}