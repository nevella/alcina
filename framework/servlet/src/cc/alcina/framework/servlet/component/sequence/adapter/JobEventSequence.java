package cc.alcina.framework.servlet.component.sequence.adapter;

import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.DeserializerOptions;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobDomain;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable;
import cc.alcina.framework.entity.persistence.domain.descriptor.JobObservable.AllocationEvent;
import cc.alcina.framework.entity.persistence.mvcc.MvccEvent;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafTransforms;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;
import cc.alcina.framework.servlet.component.sequence.adapter.MvccEventSequence.MvccEventRow;

/**
 * A transformation class to render a sequence of [JobObservable, MvccEvent]
 * instances in the sequence viewer
 */
public class JobEventSequence extends Sequence.Abstract<IdOrdered> {
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
			JobEventSequence result = new JobEventSequence();
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
	public ModelTransform<IdOrdered, Model> getRowTransform() {
		return JobOrMvccObservableRow::new;
	}

	@Override
	public ModelTransform<IdOrdered, Model> getDetailTransform() {
		return JobOrMvccObservableRow::new;
	}

	/**
	 * the union of the observable properties of JobObservableRow::MvccEventRow
	 */
	static class JobOrMvccObservableRow extends Model.All
			implements HasStringRepresentation {
		@Directed(className = "numeric")
		int index;

		@Directed(className = "numeric")
		int versionId;

		String name;

		@ValueTransformer(LeafTransforms.Dates.TimestampNoDay.class)
		Date time;

		String type;

		String subType;

		@Directed.Exclude
		@Display.Exclude
		String stringRepresentation;

		JobOrMvccObservableRow(IdOrdered event) {
			if (event instanceof JobObservable) {
				fromJobObservable((JobObservable) event);
			} else {
				fromMvccEvent((MvccEvent) event);
			}
		}

		void fromMvccEvent(MvccEvent event) {
			MvccEventRow row = new MvccEventRow(event);
			index = row.index;
			versionId = row.versionId;
			time = row.time;
			type = event.getClass().getSimpleName();
			subType = row.type;
			stringRepresentation = row.stringRepresentation;
		}

		void fromJobObservable(JobObservable event) {
			JobObservableRow row = new JobObservableRow(event);
			index = row.index;
			name = row.name;
			time = row.time;
			if (event instanceof JobObservable.AllocationEvent) {
				type = event.getClass().getSimpleName();
				subType = Ax.friendly(row.allocationEventType);
			} else {
				type = JobObservable.class.getSimpleName();
				subType = event.getClass().getSimpleName();
			}
			stringRepresentation = row.stringRepresentation;
		}

		@Override
		public String provideStringRepresentation() {
			return stringRepresentation;
		}
	}

	static class JobObservableRow extends Model.All
			implements HasStringRepresentation {
		@Directed.Exclude
		@Display.Exclude
		JobObservable event;

		@Directed(className = "numeric")
		int index;

		String name;

		@ValueTransformer(LeafTransforms.Dates.TimestampNoDay.class)
		Date time;

		JobDomain.EventType allocationEventType;

		@Directed.Exclude
		@Display.Exclude
		String stringRepresentation;

		JobObservableRow(JobObservable observable) {
			this.event = observable;
			index = (int) observable.id;
			time = observable.date;
			if (observable instanceof AllocationEvent) {
				allocationEventType = ((AllocationEvent) observable).eventType;
			}
			stringRepresentation = observable.toString();
		}

		@Override
		public String provideStringRepresentation() {
			return stringRepresentation;
		}
	}
}
