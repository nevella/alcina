package cc.alcina.framework.gwt.client.data.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;

@SearchDefinitionSerializationInfo("fctsc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class FinishedFromCriterion extends DateCriterion {
	public FinishedFromCriterion() {
		super("Finished since", Direction.ASCENDING);
	}

	public boolean rangeControlledByDirection() {
		return true;
	}
}
