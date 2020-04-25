package cc.alcina.framework.gwt.client.entity.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.CriteriaGroup;

@SearchDefinitionSerializationInfo("dcg")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
public class EntityCriteriaGroup extends CriteriaGroup {
	public EntityCriteriaGroup() {
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
