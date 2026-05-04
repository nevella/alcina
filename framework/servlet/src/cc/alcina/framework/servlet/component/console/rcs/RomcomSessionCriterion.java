package cc.alcina.framework.servlet.component.console.rcs;

import javax.xml.bind.annotation.XmlType;

import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.EntityCriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

public class RomcomSessionCriterion {
	@TypeSerialization(
		properties = @PropertySerialization(
			name = CriteriaGroup.PROPERTY_CRITERIA,
			defaultProperty = true,
			types = {
			//@formatter:off
					TextCriterion.class
				//@formatter:on
			}))
	@XmlType(name = "RomcomSessionSearchDefinition_CriteriaGroup")
	public static class CriteriaGroup extends EntityCriteriaGroup {
	}

	abstract static class CriterionHandler<SC extends SearchCriterion>
			extends DomainCriterionHandler<SC> {
		@Override
		public Class<? extends SearchDefinition> handlesSearchDefinition() {
			return RomcomSessionSearchDefinition.class;
		}
	}
}
