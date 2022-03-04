package cc.alcina.framework.gwt.client.tour.condition;

import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluationContext;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluator;

@Reflected
public class BreakpointEvaluator implements ConditionEvaluator {
	@Override
	public boolean evaluate(ConditionEvaluationContext context) {
		int breakpoint = 3;
		return true;
	}
}