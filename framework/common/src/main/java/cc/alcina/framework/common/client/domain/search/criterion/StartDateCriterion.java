package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.DateCriterion;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@SearchDefinitionSerializationInfo("stdc")
@TypeSerialization("startdate")
@Registration(SearchDefinitionSerializationInfo.class)
public class StartDateCriterion extends DateCriterion {
	public StartDateCriterion() {
		super("Start", Direction.ASCENDING);
	}
}
