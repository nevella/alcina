package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;

@SearchDefinitionSerializationInfo("mdtc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization("modifiedto")
public class ModifiedToCriterion extends DateCriterion {

    public ModifiedToCriterion() {
        super("Modified before", Direction.DESCENDING);
    }

    @Override
    public boolean rangeControlledByDirection() {
        return true;
    }
}
