package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@SearchDefinitionSerializationInfo("fctbc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization("finishedto")
public class FinishedToCriterion extends DateCriterion {

    public FinishedToCriterion() {
        super("Finished before", Direction.DESCENDING);
    }

    public boolean rangeControlledByDirection() {
        return true;
    }
}
