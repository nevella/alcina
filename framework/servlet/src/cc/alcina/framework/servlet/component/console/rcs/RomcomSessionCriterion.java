package cc.alcina.framework.servlet.component.console.rcs;

import javax.xml.bind.annotation.XmlType;

import cc.alcina.framework.common.client.domain.search.DomainCriterionHandler;
import cc.alcina.framework.common.client.domain.search.EntityCriteriaGroup;
import cc.alcina.framework.common.client.domain.search.criterion.PropertyOrderCriterion;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.common.client.search.BooleanEnumCriterion;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.TextCriterion;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.objecttree.search.packs.BaseEnumCriterionPack.BaseEnumCriterionHandler;

public class RomcomSessionCriterion {
	@TypeSerialization(
		properties = @PropertySerialization(
			name = CriteriaGroup.PROPERTY_CRITERIA,
			defaultProperty = true,
			types = {
			//@formatter:off
					TextCriterion.class,
					ActiveCriterion.class,
					MarkedCriterion.class,
					ExceptionCriterion.class,
					PropertyOrderCriterion.class
				//@formatter:on
			}))
	@XmlType(name = "RomcomSessionSearchDefinition_CriteriaGroup")
	public static class CriteriaGroup extends EntityCriteriaGroup {
	}

	abstract static class CriterionHandler<SC extends SearchCriterion> extends
			DomainCriterionHandler<SC> implements Registration.AllSubtypes {
		@Override
		public Class<? extends SearchDefinition> handlesSearchDefinition() {
			return RomcomSessionSearchDefinition.class;
		}
	}

	@TypeSerialization("active")
	static class ActiveCriterion extends BooleanEnumCriterion {
		static class Handler extends CriterionHandler<ActiveCriterion>
				implements
				BaseEnumCriterionHandler<RomcomSessionEntry, BooleanEnum, ActiveCriterion> {
			@Override
			public boolean test(RomcomSessionEntry session, BooleanEnum value) {
				if (value == null) {
					return true;
				}
				return value.toBoolean() == session.active;
			}
		}
	}

	@TypeSerialization("marked")
	static class MarkedCriterion extends BooleanEnumCriterion {
		static class Handler extends CriterionHandler<MarkedCriterion>
				implements
				BaseEnumCriterionHandler<RomcomSessionEntry, BooleanEnum, MarkedCriterion> {
			@Override
			public boolean test(RomcomSessionEntry session, BooleanEnum value) {
				if (value == null) {
					return true;
				}
				return value.toBoolean() == session.marked;
			}
		}
	}

	@TypeSerialization("exception")
	static class ExceptionCriterion extends BooleanEnumCriterion {
		static class Handler extends CriterionHandler<ExceptionCriterion>
				implements
				BaseEnumCriterionHandler<RomcomSessionEntry, BooleanEnum, ExceptionCriterion> {
			@Override
			public boolean test(RomcomSessionEntry session, BooleanEnum value) {
				if (value == null) {
					return true;
				}
				return value.toBoolean() == session.exception;
			}
		}
	}
}
