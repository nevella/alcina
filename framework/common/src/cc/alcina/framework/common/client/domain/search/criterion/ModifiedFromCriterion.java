package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@SearchDefinitionSerializationInfo("mdfc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization("modifiedfrom")
@Registration(SearchDefinitionSerializationInfo.class)
public class ModifiedFromCriterion extends DateCriterion {

    public ModifiedFromCriterion() {
        super("Modified from", Direction.ASCENDING);
    }

    @Override
    public boolean rangeControlledByDirection() {
        return true;
    }
}
