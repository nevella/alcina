package cc.alcina.framework.servlet.component.sequence.adapter;

import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.flight.FlightEventWrappable;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafTransforms;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorEvent;

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

	@Override
	public ModelTransform<FlightEvent, ? extends Model>
			getDetailTransformAdditional() {
		return new DetailTransformerAdditional();
	}

	class DetailTransformerAdditional
			implements ModelTransform<FlightEvent, Model> {
		@Override
		public Model apply(FlightEvent event) {
			FlightEventWrappable wrappable = event.event;
			Optional<DetailTransformer> transformer = Registry
					.optional(DetailTransformer.class, wrappable.getClass());
			return (Model) transformer.map(tr -> tr.apply(wrappable))
					.orElse(null);
		}
	}

	public interface DetailTransformer<W extends FlightEventWrappable>
			extends Function<W, Model> {
	}

	@Registration({ FlightEventSequence.DetailTransformer.class,
			DecoratorEvent.class })
	public static class StringsTransformer
			implements FlightEventSequence.DetailTransformer<DecoratorEvent> {
		static class MutationStringsModel extends Model.All {
			@Binding(type = Binding.Type.PROPERTY)
			String style = "white-space: pre; display: flex; flex-direction: column; gap: 1rem; margin: 1rem; font-family: fixed-width";

			String mutationRecords;

			String editorDom;

			MutationStringsModel(DecoratorEvent t) {
				if (t.mutationStrings != null) {
					this.mutationRecords = t.mutationStrings.mutationRecords;
					this.editorDom = t.mutationStrings.editorDom;
				}
			}
		}

		@Override
		public Model apply(DecoratorEvent t) {
			return new MutationStringsModel(t);
		}
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

		String subtype;

		@Directed(className = "wide")
		String detail;

		@Directed.Exclude
		@Display.Exclude
		String stringRepresentation;

		FlightEventRow(FlightEvent event) {
			this.event = event;
			index = (int) event.id;
			time = new Date(event.provideTime());
			duration = event.provideDuration();
			type = NestedName.get(event.event);
			subtype = event.provideSubtype();
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
