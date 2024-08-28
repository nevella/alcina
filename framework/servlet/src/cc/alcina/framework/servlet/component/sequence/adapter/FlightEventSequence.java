package cc.alcina.framework.servlet.component.sequence.adapter;

import java.util.Date;

import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafTransforms;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;
import cc.alcina.framework.servlet.component.sequence.Sequence;

/**
 * A transformation class to render a sequence of FlightEvent instances in the
 * sequence viewer
 */
public class FlightEventSequence extends Sequence.Abstract<FlightEvent> {
	@Override
	public ModelTransform<FlightEvent, Model> getRowTransform() {
		return FlightEventRow::new;
	}

	@Override
	public ModelTransform<FlightEvent, Model> getDetailTransform() {
		return FlightEventRow::new;
	}

	static class FlightEventRow extends Model.All
			implements HasStringRepresentation {
		@Directed.Exclude
		@Display.Exclude
		FlightEvent event;

		@Directed(className = "numeric")
		int index;

		@ValueTransformer(LeafTransforms.Dates.TimestampNoDay.class)
		Date time;

		@Directed(className = "numeric")
		long duration;

		@Directed(className = "numeric")
		long in;

		@Directed(className = "numeric")
		long out;

		String type;

		@Directed(className = "wide")
		String detail;

		@Directed.Exclude
		@Display.Exclude
		String stringRepresentation;

		FlightEventRow(FlightEvent event) {
			this.event = event;
			index = (int) event.id;
			time = new Date(event.provideTime());
			type = NestedName.get(event.event);
			duration = event.provideDuration();
			detail = event.provideDetail();
			in = event.provideInputBytes().length;
			out = event.provideOutputBytes().length;
			stringRepresentation = event.provideStringRepresentation();
		}

		@Override
		public String provideStringRepresentation() {
			return stringRepresentation;
		}
	}
}
