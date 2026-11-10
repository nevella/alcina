package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@SearchDefinitionSerializationInfo("mdfc")
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
