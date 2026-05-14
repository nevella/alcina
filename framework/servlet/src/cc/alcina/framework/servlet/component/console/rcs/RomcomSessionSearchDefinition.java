package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.criterion.PropertyCriterion;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionCriterion.ActiveCriterion;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionCriterion.ExceptionCriterion;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionCriterion.MarkedCriterion;

@TypeSerialization(
	value = "romcomsession",
	properties = { @PropertySerialization(
		name = SearchDefinition.PROPERTY_CRITERIA_GROUPS,
		types = RomcomSessionCriterion.CriteriaGroup.class,
		defaultProperty = true) })
public class RomcomSessionSearchDefinition extends SequenceSearchDefinition {
	enum Preset {
		Active {
			@Override
			RomcomSessionSearchDefinition getDefinition() {
				RomcomSessionSearchDefinition def = new RomcomSessionSearchDefinition();
				new ActiveCriterion().withValue(BooleanEnum.TRUE)
						.addToSoleCriteriaGroup(def);
				def.orderBy(RomcomSessionEntry.properties.start, false);
				return def;
			}
		},
		Marked {
			@Override
			RomcomSessionSearchDefinition getDefinition() {
				RomcomSessionSearchDefinition def = new RomcomSessionSearchDefinition();
				new MarkedCriterion().withValue(BooleanEnum.TRUE)
						.addToSoleCriteriaGroup(def);
				def.orderBy(RomcomSessionEntry.properties.start, false);
				return def;
			}
		},
		Exceptions {
			@Override
			RomcomSessionSearchDefinition getDefinition() {
				RomcomSessionSearchDefinition def = new RomcomSessionSearchDefinition();
				new ExceptionCriterion().withValue(BooleanEnum.TRUE)
						.addToSoleCriteriaGroup(def);
				def.orderBy(RomcomSessionEntry.properties.start, false);
				return def;
			}
		},
		Large {
			@Override
			RomcomSessionSearchDefinition getDefinition() {
				RomcomSessionSearchDefinition def = new RomcomSessionSearchDefinition();
				PropertyCriterion.of(
						RomcomSessionSequence.RomcomSessionView.properties.largestPacket,
						100000)
						.withOperator(StandardSearchOperator.GREATER_THAN)
						.addToSoleCriteriaGroup(def);
				def.orderBy(RomcomSessionEntry.properties.largestPacket, false);
				return def;
			}
		},
		Slow {
			@Override
			RomcomSessionSearchDefinition getDefinition() {
				RomcomSessionSearchDefinition def = new RomcomSessionSearchDefinition();
				PropertyCriterion.of(
						RomcomSessionSequence.RomcomSessionView.properties.slowestResponse,
						500).withOperator(StandardSearchOperator.GREATER_THAN)
						.addToSoleCriteriaGroup(def);
				def.orderBy(RomcomSessionEntry.properties.slowestResponse,
						false);
				return def;
			}
		},
		All {
			@Override
			RomcomSessionSearchDefinition getDefinition() {
				RomcomSessionSearchDefinition def = new RomcomSessionSearchDefinition();
				def.orderBy(RomcomSessionEntry.properties.start, false);
				return def;
			}
		};

		abstract RomcomSessionSearchDefinition getDefinition();

		String getName() {
			return Ax.friendly(this);
		}
	}

	@Override
	public Class<? extends Sequence> sequenceClass() {
		return RomcomSessionSequence.class;
	}

	@Override
	public Class<? extends Bindable> queriedBindableClass() {
		return RomcomSessionEntry.class;
	}

	public RomcomSessionSearchDefinition() {
		init();
	}

	@Override
	protected void init() {
		super.init();
	}
}
