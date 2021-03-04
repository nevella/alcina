package cc.alcina.framework.gwt.client.entity.search;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;

@SearchDefinitionSerializationInfo("stdc")
@RegistryLocation(registryPoint = SearchDefinitionSerializationInfo.class)
@TypeSerialization("startdate")
public class StartDateCriterion extends DateCriterion {

    public StartDateCriterion() {
        super("Start", Direction.ASCENDING);
    }
}
