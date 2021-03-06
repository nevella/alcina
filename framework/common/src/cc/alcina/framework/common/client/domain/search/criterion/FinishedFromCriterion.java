package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;

@SearchDefinitionSerializationInfo("fctsc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization("finishedfrom")
public class FinishedFromCriterion extends DateCriterion {

    public FinishedFromCriterion() {
        super("Finished since", Direction.ASCENDING);
    }

    public boolean rangeControlledByDirection() {
        return true;
    }
}
