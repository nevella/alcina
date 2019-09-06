package cc.alcina.framework.gwt.client.data.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;

@SearchDefinitionSerializationInfo("mdfc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class ModifiedFromCriterion extends DateCriterion {
	public ModifiedFromCriterion() {
		super("Modified from", Direction.ASCENDING);
	}

	@Override
	public boolean rangeControlledByDirection() {
		return true;
	}
}
