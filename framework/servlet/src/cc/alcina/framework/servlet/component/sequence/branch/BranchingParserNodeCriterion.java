package cc.alcina.framework.servlet.component.sequence.branch;

import javax.xml.bind.annotation.XmlType;

import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.EntityCriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

public class BranchingParserNodeCriterion {
	@TypeSerialization(
		properties = @PropertySerialization(
			name = CriteriaGroup.PROPERTY_CRITERIA,
			defaultProperty = true,
			types = {
			//@formatter:off
					TextCriterion.class
				//@formatter:on
			}))
	@XmlType(name = "BranchingParserNodeSearchDefinition_CriteriaGroup")
	public static class CriteriaGroup extends EntityCriteriaGroup {
	}

	abstract static class CriterionHandler<SC extends SearchCriterion>
			extends DomainCriterionHandler<SC> {
		@Override
		public Class<? extends SearchDefinition> handlesSearchDefinition() {
			return BranchingParserNodeSearchDefinition.class;
		}
	}
}
