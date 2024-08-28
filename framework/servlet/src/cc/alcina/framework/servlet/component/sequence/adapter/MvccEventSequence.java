package cc.alcina.framework.servlet.component.sequence.adapter;

import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.DeserializerOptions;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.mvcc.MvccEvent;
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
public class MvccEventSequence extends Sequence.Abstract<MvccEvent> {
	public static final String LOCAL_PATH = "/tmp/sequence/mvcc-event";

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
			MvccEventSequence result = new MvccEventSequence();
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
	public ModelTransform<MvccEvent, Model> getRowTransform() {
		return MvccEventRow::new;
	}

	@Override
	public ModelTransform<MvccEvent, Model> getDetailTransform() {
		return MvccEventRow::new;
	}

	static class MvccEventRow extends Model.All
			implements HasStringRepresentation {
		@Directed.Exclude
		@Display.Exclude
		MvccEvent event;

		@Directed(className = "numeric")
		int index;

		@Directed(className = "numeric")
		int versionId;

		@ValueTransformer(LeafTransforms.Dates.TimestampNoDay.class)
		Date time;

		String type;

		@Directed.Exclude
		@Display.Exclude
		String stringRepresentation;

		MvccEventRow(MvccEvent event) {
			this.event = event;
			index = (int) event.id;
			time = event.date;
			type = event.type;
			versionId = event.versionId;
			stringRepresentation = event.toMultilineString();
		}

		@Override
		public String provideStringRepresentation() {
			return stringRepresentation;
		}
	}
}
