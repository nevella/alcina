package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;

@SearchDefinitionSerializationInfo("ctsc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization("createdfrom")
public class CreatedFromCriterion extends DateCriterion {

    public CreatedFromCriterion() {
        super("Created since", Direction.ASCENDING);
    }

    public boolean rangeControlledByDirection() {
        return true;
    }
}
