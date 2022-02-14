package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@SearchDefinitionSerializationInfo("ctbc")
@TypeSerialization("createdto")
@Registration(SearchDefinitionSerializationInfo.class)
public class CreatedToCriterion extends DateCriterion {
	public CreatedToCriterion() {
		super("Created before", Direction.DESCENDING);
	}

	public boolean rangeControlledByDirection() {
		return true;
	}
}
