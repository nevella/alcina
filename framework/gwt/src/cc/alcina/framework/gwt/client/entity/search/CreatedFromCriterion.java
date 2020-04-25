package cc.alcina.framework.gwt.client.entity.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;

@SearchDefinitionSerializationInfo("ctsc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class CreatedFromCriterion extends DateCriterion {
	public CreatedFromCriterion() {
		super("Created since", Direction.ASCENDING);
	}

	public boolean rangeControlledByDirection() {
		return true;
	}
}
