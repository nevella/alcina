package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.List;

import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.search.SearchOperator;
import cc.alcina.framework.common.client.search.StandardSearchOperator;
import cc.alcina.framework.gwt.client.gwittir.widget.DateBox;
import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.ui.AbstractBoundWidget;

public class DateCriterionSearchable<DC extends DateCriterion>
		extends FlatSearchable<DC> {
	public DateCriterionSearchable(Class<DC> clazz, String category,
			String name) {
		this(clazz, category, name, StandardSearchOperator.LINEAR);
	}

	public DateCriterionSearchable(Class<DC> clazz, String category,
			String name, List<StandardSearchOperator> operators) {
		super(clazz, category, name, operators);
	}

	@Override
	public AbstractBoundWidget createEditor() {
		return new DateBox();
	}

	@Override
	public String getCriterionPropertyName() {
		return "value";
	}

	@Override
	public SearchOperator getOperator(DC value) {
		return value.getOperator();
	}

	@Override
	public boolean hasValue(DC sc) {
		return sc.getValue() != null;
	}
}
