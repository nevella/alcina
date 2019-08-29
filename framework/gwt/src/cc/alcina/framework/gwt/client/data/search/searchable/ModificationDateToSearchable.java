package cc.alcina.framework.gwt.client.data.search.searchable;

import cc.alcina.framework.gwt.client.data.search.ModifiedToCriterion;
import cc.alcina.framework.gwt.client.objecttree.search.DateCriterionSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class ModificationDateToSearchable
		extends DateCriterionSearchable<ModifiedToCriterion> {
	public ModificationDateToSearchable() {
		this("Modified", "To");
	}

	public ModificationDateToSearchable(String category, String name) {
		super(ModifiedToCriterion.class, category, name,
				StandardSearchOperator.EQUAL);
	}
}