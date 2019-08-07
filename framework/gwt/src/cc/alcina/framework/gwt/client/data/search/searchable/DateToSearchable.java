package cc.alcina.framework.gwt.client.data.search.searchable;

import cc.alcina.framework.gwt.client.data.search.CreatedToCriterion;
import cc.alcina.framework.gwt.client.objecttree.search.DateCriterionSearchable;

public class DateToSearchable
		extends DateCriterionSearchable<CreatedToCriterion> {
	public DateToSearchable() {
		this("Date", "To");
	}

	public DateToSearchable(String category, String name) {
		super(CreatedToCriterion.class, category, name);
	}
}