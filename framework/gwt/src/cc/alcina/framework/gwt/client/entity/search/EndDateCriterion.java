package cc.alcina.framework.gwt.client.entity.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;

@SearchDefinitionSerializationInfo("endc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization("enddate")
public class EndDateCriterion extends DateCriterion {

    public EndDateCriterion() {
        super("Start", Direction.ASCENDING);
    }
}
