package cc.alcina.framework.servlet.component.sequence.adapter;

import javax.xml.bind.annotation.XmlType;

import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.EntityCriteriaGroup;
import cc.alcina.framework.common.client.domain.search.criterion.CreatedFromCriterion;
import cc.alcina.framework.common.client.domain.search.criterion.CreatedToCriterion;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

public class FlightEventCriterion {
	@TypeSerialization(
		properties = @PropertySerialization(
			name = CriteriaGroup.PROPERTY_CRITERIA,
			defaultProperty = true,
			types = {
			//@formatter:off
					TextCriterion.class,
					CreatedFromCriterion.class,
					CreatedToCriterion.class
				//@formatter:on
			}))
	@XmlType(name = "FlightEventCriterion_CriteriaGroup")
	public static class FlightEventCriteriaGroup extends EntityCriteriaGroup {
	}

	abstract static class CriterionHandler<SC extends SearchCriterion>
			extends DomainCriterionHandler<SC> {
		@Override
		public Class<? extends SearchDefinition> handlesSearchDefinition() {
			return FlightEventSearchDefinition.class;
		}
	}
}
