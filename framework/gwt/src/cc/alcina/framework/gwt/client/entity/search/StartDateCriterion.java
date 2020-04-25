package cc.alcina.framework.gwt.client.entity.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;

@SearchDefinitionSerializationInfo("stdc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class StartDateCriterion extends DateCriterion {
	public StartDateCriterion() {
		super("Start", Direction.ASCENDING);
	}
}
