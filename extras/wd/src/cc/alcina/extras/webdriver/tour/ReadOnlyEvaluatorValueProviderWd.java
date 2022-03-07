package cc.alcina.extras.webdriver.tour;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.gwt.client.tour.condition.ReadOnlyEvaluator.ReadOnlyEvaluatorValueProvider;

@Registration(value = ReadOnlyEvaluatorValueProvider.class, priority = Registration.Priority.PREFERRED_LIBRARY)
public class ReadOnlyEvaluatorValueProviderWd
		extends ReadOnlyEvaluatorValueProvider {
	@Override
	public boolean isReadOnly() {
		return ResourceUtilities.is("readOnly");
	}
}
