package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.List;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.gwt.client.gwittir.widget.DateBox;

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
		return "date";
	}

	@Override
	public boolean hasValue(DC sc) {
		return sc.getDate() != null;
	}

	@Override
	public SearchOperator getOperator(DC value) {
		return value.getOperator();
	}
}
