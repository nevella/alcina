package cc.alcina.framework.gwt.client.entity.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;

@SearchDefinitionSerializationInfo("ctbc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class CreatedToCriterion extends DateCriterion {
	public CreatedToCriterion() {
		super("Created before", Direction.DESCENDING);
	}

	public boolean rangeControlledByDirection() {
		return true;
	}
}
