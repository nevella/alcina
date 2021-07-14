package cc.alcina.extras.webdriver.tour;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.gwt.client.tour.condition.ReadOnlyEvaluator.ReadOnlyEvaluatorValueProvider;

@RegistryLocation(registryPoint = ReadOnlyEvaluatorValueProvider.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
public class ReadOnlyEvaluatorValueProviderWd
		extends ReadOnlyEvaluatorValueProvider {
	@Override
	public boolean isReadOnly() {
		return ResourceUtilities.is("readOnly");
	}
}
