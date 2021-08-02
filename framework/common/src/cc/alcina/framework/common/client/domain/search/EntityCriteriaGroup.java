package cc.alcina.framework.common.client.domain.search;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization(flatSerializable = false)
public abstract class EntityCriteriaGroup extends CriteriaGroup {
	public EntityCriteriaGroup() {
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		return "filters";
	}

	@Override
	public Class entityClass() {
		return null;
	}
}
