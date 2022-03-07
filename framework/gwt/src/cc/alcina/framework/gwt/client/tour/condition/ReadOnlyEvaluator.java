package cc.alcina.framework.gwt.client.tour.condition;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluationContext;
import cc.alcina.framework.gwt.client.tour.Tour.ConditionEvaluator;

@Reflected
public class ReadOnlyEvaluator implements ConditionEvaluator {
	@Override
	public boolean evaluate(ConditionEvaluationContext context) {
		return Registry.impl(ReadOnlyEvaluatorValueProvider.class).isReadOnly();
	}

	@Registration(ReadOnlyEvaluatorValueProvider.class)
	public static class ReadOnlyEvaluatorValueProvider {
		public boolean isReadOnly() {
			return GWT.isClient();
		}
	}
}
