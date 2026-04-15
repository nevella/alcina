package cc.alcina.framework.servlet.component.sequence.adapter;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.EntitySearchDefinition;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;

@TypeSerialization(
	value = "flightevent",
	properties = { @PropertySerialization(
		name = SearchDefinition.PROPERTY_CRITERIA_GROUPS,
		types = FlightEventCriterion.FlightEventCriteriaGroup.class,
		defaultProperty = true) })
public class FlightEventSearchDefinition extends SequenceSearchDefinition {
	@TypeSerialization("flighteventsearchdefinition")
	public static class Parameter extends
			SequenceSearchDefinition.BaseParameter<FlightEventSearchDefinition> {
	}

	@Override
	public TreeSerializable.Customiser treeSerializationCustomiser() {
		return new CustomiserImpl(this);
	}

	protected static class CustomiserImpl extends
			EntitySearchDefinition.Customiser<FlightEventSearchDefinition> {
		public CustomiserImpl(FlightEventSearchDefinition serializable) {
			super(serializable);
		}

		@Override
		public void onBeforeTreeDeserialize() {
			serializable.soleCriteriaGroup().clearCriteria();
			super.onBeforeTreeDeserialize();
		}
	}

	@Override
	public Class<? extends Sequence> sequenceClass() {
		return FlightEventSequence.class;
	}

	@Override
	public Class<? extends Bindable> queriedBindableClass() {
		return FlightEvent.class;
	}

	public FlightEventSearchDefinition() {
		init();
	}

	@Override
	protected void init() {
		super.init();
	}
}
