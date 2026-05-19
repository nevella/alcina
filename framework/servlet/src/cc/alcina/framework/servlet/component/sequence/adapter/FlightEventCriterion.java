package cc.alcina.framework.servlet.component.sequence.adapter;

import java.util.List;
import java.util.function.Predicate;

import javax.xml.bind.annotation.XmlType;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.BindableCriteriaGroup;
import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.criterion.CreatedFromCriterion;
import cc.alcina.framework.common.client.domain.search.criterion.CreatedToCriterion;
import cc.alcina.framework.common.client.domain.search.criterion.PropertyCriterion;
import cc.alcina.framework.common.client.domain.search.criterion.PropertyOrderCriterion;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.SearchDefinitionSerializationInfo;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.common.client.search.BooleanEnumCriterion;
import cc.alcina.framework.common.client.search.CriteriaGroup;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.SelfNamingCriterion;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import cc.alcina.framework.gwt.client.objecttree.search.packs.BaseEnumCriterionPack.BaseEnumCriterionHandler;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

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
					IsMutationsCriterion.class,
					PropertyCriterion.class,
					PropertyOrderCriterion.class
				//@formatter:on
			}))
	@XmlType(name = "FlightEventCriterion_CriteriaGroup")
	public static class FlightEventCriteriaGroup extends BindableCriteriaGroup {
	}

	static class PropertyCriterionHandler
			extends CriterionHandler<PropertyCriterion>
			implements PropertyCriterion.Handler {
	}

	@TypeSerialization("mutations")
	@Registration(SearchDefinitionSerializationInfo.class)
	public static class IsMutationsCriterion extends BooleanEnumCriterion
			implements SelfNamingCriterion {
		@Override
		public String toString() {
			return toStringWithDisplayName(true);
		}

		@Override
		public List<StandardSearchOperator> getApplicableOperators() {
			return List.of(StandardSearchOperator.EQUALS,
					StandardSearchOperator.DOES_NOT_EQUAL);
		}

		public static class Handler
				extends CriterionHandler<IsMutationsCriterion> implements
				BaseEnumCriterionHandler<FlightEvent, BooleanEnum, IsMutationsCriterion> {
			@Override
			public boolean test(FlightEvent t, BooleanEnum value) {
				return t.provideIsMutation() == value.toBoolean();
			}
		}
	}

	public static class TextCriterionHandler
			extends CriterionHandler<TextCriterion> {
		@Override
		public DomainFilter getFilter(TextCriterion sc) {
			String text = TextUtils.normalisedLcKey(sc.getValue());
			if (text.isEmpty()) {
				return null;
			}
			return new DomainFilter(new Predicate<FlightEvent>() {
				@Override
				public boolean test(FlightEvent o) {
					return SearchUtils.matches(text,
							o.provideStringRepresentation());
				}
			}).invertIf(sc
					.getOperator() == StandardSearchOperator.DOES_NOT_CONTAIN);
		}
	}

	abstract static class CriterionHandler<SC extends SearchCriterion> extends
			DomainCriterionHandler<SC> implements Registration.AllSubtypes {
		@Override
		public Class<? extends SearchDefinition> handlesSearchDefinition() {
			return FlightEventSearchDefinition.class;
		}
	}
}
