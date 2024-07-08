package cc.alcina.framework.servlet.component.sequence.flight;

import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.DeserializerOptions;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafTransforms;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;
import cc.alcina.framework.servlet.component.sequence.Sequence;

public class FlightEventSequence extends Sequence.Abstract<FlightEvent> {
	/*
	 * Children must have a no-args constructor that populates the fields
	 */
	public static abstract class Loader implements Sequence.Loader {
		String path;

		String name;

		Function<String, String> serializationRefactoringHandler;

		public Loader(String path, String name,
				Function<String, String> serializationRefactoringHandler) {
			this.path = path;
			this.name = name;
			this.serializationRefactoringHandler = serializationRefactoringHandler;
		}

		@Override
		public Sequence<?> load(String location) {
			FlightEventSequence result = new FlightEventSequence();
			result.name = name;
			result.elements = (List) SEUtilities.listFilesRecursive(path, null)
					.stream().filter(f -> f.isFile())
					.map(f -> Io.read().file(f).asString())
					.map(serializationRefactoringHandler::apply)
					.<FlightEvent> map(s -> ReflectiveSerializer.deserialize(s,
							new DeserializerOptions()
									.withContinueOnException(true)))
					.sorted().collect(Collectors.toList());
			return result;
		}
	}

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
			index = (int) event.eventId;
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
