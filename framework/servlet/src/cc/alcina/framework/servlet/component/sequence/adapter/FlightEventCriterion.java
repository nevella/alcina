package cc.alcina.framework.servlet.component.sequence.adapter;

import javax.xml.bind.annotation.XmlType;

import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.EntityCriteriaGroup;
import cc.alcina.framework.common.client.domain.search.criterion.CreatedFromCriterion;
import cc.alcina.framework.common.client.domain.search.criterion.CreatedToCriterion;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.BooleanEnumCriterion;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.SelfNamingCriterion;
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
					CreatedToCriterion.class,
					IsMutationsCriterion.class
				//@formatter:on
			}))
	@XmlType(name = "FlightEventCriterion_CriteriaGroup")
	public static class FlightEventCriteriaGroup extends EntityCriteriaGroup {
	}

	@TypeSerialization("mutations")
	@Registration(SearchDefinitionSerializationInfo.class)
	public static class IsMutationsCriterion extends BooleanEnumCriterion
			implements SelfNamingCriterion {
		@Override
		public String toString() {
			return toStringWithDisplayName(true);
		}
	}

	abstract static class CriterionHandler<SC extends SearchCriterion>
			extends DomainCriterionHandler<SC> {
		@Override
		public Class<? extends SearchDefinition> handlesSearchDefinition() {
			return FlightEventSearchDefinition.class;
		}
	}
}
