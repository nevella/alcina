package cc.alcina.framework.gwt.client.data.search.searchable;

import cc.alcina.framework.gwt.client.data.search.ModifiedFromCriterion;
import cc.alcina.framework.gwt.client.objecttree.search.DateCriterionSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class ModificationDateFromSearchable
		extends DateCriterionSearchable<ModifiedFromCriterion> {
	public ModificationDateFromSearchable() {
		this("Modified", "From");
	}

	public ModificationDateFromSearchable(String category, String name) {
		super(ModifiedFromCriterion.class, category, name,
				StandardSearchOperator.EQUAL);
	}
}