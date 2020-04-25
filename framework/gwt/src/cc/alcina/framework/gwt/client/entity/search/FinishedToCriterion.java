package cc.alcina.framework.gwt.client.entity.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;

@SearchDefinitionSerializationInfo("fctbc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class FinishedToCriterion extends DateCriterion {
	public FinishedToCriterion() {
		super("Finished before", Direction.DESCENDING);
	}

	public boolean rangeControlledByDirection() {
		return true;
	}
}
