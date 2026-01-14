package cc.alcina.framework.servlet.component.sequence.adapter;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;

@TypeSerialization(
	value = "flightevent",
	properties = { @PropertySerialization(
		name = "criteriaGroups",
		types = FlightEventCriterion.CriteriaGroup.class,
		defaultProperty = true) })
public class FlightEventSearchDefinition extends SequenceSearchDefinition {
	@TypeSerialization("FlightEventSearchDefinition")
	public static class Parameter
			extends InstanceQuery.Parameter<FlightEventSearchDefinition> {
		public Parameter() {
			setValue(new FlightEventSearchDefinition());
		}
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
