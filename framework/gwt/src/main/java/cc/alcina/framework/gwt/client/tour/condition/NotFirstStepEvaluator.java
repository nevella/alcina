package cc.alcina.framework.gwt.client.tour.condition;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluationContext;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluator;

@Reflected
public class NotFirstStepEvaluator implements ConditionEvaluator {
	@Override
	public boolean evaluate(ConditionEvaluationContext context) {
		return !context.provideIsFirstStep();
	}
}