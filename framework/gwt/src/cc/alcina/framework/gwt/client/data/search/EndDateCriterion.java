package cc.alcina.framework.gwt.client.data.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;

@SearchDefinitionSerializationInfo("endc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class EndDateCriterion extends DateCriterion {
	public EndDateCriterion() {
		super("Start", Direction.ASCENDING);
	}
}
