package cc.alcina.framework.gwt.client.data.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.CriteriaGroup;

@SearchDefinitionSerializationInfo("dcg")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class DataCriteriaGroup extends CriteriaGroup {
	public DataCriteriaGroup() {
	}

	@Override
	public String getDisplayName() {
		return "filters";
	}

	@Override
	public Class getEntityClass() {
		return null;
	}
}
