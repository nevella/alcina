package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@SearchDefinitionSerializationInfo("endc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization("enddate")
@Registration(SearchDefinitionSerializationInfo.class)
public class EndDateCriterion extends DateCriterion {

    public EndDateCriterion() {
        super("Start", Direction.ASCENDING);
    }
}
