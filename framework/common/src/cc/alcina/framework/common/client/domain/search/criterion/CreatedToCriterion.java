package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;

@SearchDefinitionSerializationInfo("ctbc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization("createdto")
public class CreatedToCriterion extends DateCriterion {

    public CreatedToCriterion() {
        super("Created before", Direction.DESCENDING);
    }

    public boolean rangeControlledByDirection() {
        return true;
    }
}
