package cc.alcina.framework.gwt.client.entity.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;

@SearchDefinitionSerializationInfo("mdtc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class ModifiedToCriterion extends DateCriterion {
	public ModifiedToCriterion() {
		super("Modified before", Direction.DESCENDING);
	}

	@Override
	public boolean rangeControlledByDirection() {
		return true;
	}
}
