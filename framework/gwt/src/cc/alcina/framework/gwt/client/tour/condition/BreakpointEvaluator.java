package cc.alcina.framework.gwt.client.tour.condition;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluationContext;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluator;

@ClientInstantiable
public class BreakpointEvaluator implements ConditionEvaluator {
	@Override
	public boolean evaluate(ConditionEvaluationContext context) {
		int breakpoint = 3;
		return true;
	}
}