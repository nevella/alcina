package cc.alcina.framework.servlet.component.sequence.adapter;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.flight.FlightEventWrappable;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.service.InstanceOracle.Query;
import cc.alcina.framework.common.client.service.InstanceProvider;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasStringRepresentation;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Copy;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafTransforms;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.KeyValue;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorEvent;
import cc.alcina.framework.servlet.component.romcom.protocol.Mutations;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentEvent;
import cc.alcina.framework.servlet.flight.FlightEventStreamProviderZip;

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
	public SequenceSearchDefinition getDefaultSearchDefinition() {
		return new FlightEventSearchDefinition();
	}

	@Override
	public ModelTransform<FlightEvent, ? extends Model>
			getDetailTransformAdditional() {
		return new DetailTransformerAdditional();
	}

	@TypeSerialization("sequencepath")
	public static class SequencePathParameter
			extends InstanceQuery.Parameter<String> {
	}

	public static InstanceQuery createInstanceQuery(String sequencePath) {
		return new InstanceQuery().withType(FlightEventSequence.class)
				.addParameters(
						new SequencePathParameter().withValue(sequencePath));
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
	public static class DecoratorEventTransformer
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

	@Registration({ FlightEventSequence.DetailTransformer.class,
			RemoteComponentEvent.class })
	public static class RemoteComponentEventTransformer extends Model.All
			implements
			FlightEventSequence.DetailTransformer<RemoteComponentEvent> {
		static class RemoteComponentEventDetailArea extends Model.All
				implements ModelEvents.Copy.Handler {
			KeyValue mutationContainsStyleNode;

			KeyValue mutationContainsImgSrc;

			@Directed.Wrap("links")
			List<Link> actions = List.of(Link.of(ModelEvents.Copy.class));

			@Property.Not
			RemoteComponentEvent remoteComponentEvent;

			RemoteComponentEventDetailArea(
					RemoteComponentEvent remoteComponentEvent) {
				this.remoteComponentEvent = remoteComponentEvent;
				List<Mutations> list = (List) remoteComponentEvent.response.messageEnvelope.messages
						.stream().filter(m -> m instanceof Mutations).toList();
				List<DomNode> additions = list.stream()
						.flatMap(m -> m.domMutations.stream())
						.map(MutationRecord::additionAsDomNode)
						.filter(Objects::nonNull).toList();
				List<DomNode> largeStyles = additions.stream().filter(
						n -> n.tagIs("style") && n.toMarkup().length() > 1000)
						.toList();
				List<DomNode> largeImages = additions.stream().filter(
						n -> n.tagIs("img") && n.toMarkup().length() > 1000)
						.toList();
				mutationContainsStyleNode = KeyValue
						.stringValue("Large style nodes", largeStyles.size());
				mutationContainsImgSrc = KeyValue
						.stringValue("Large img  nodes", largeImages.size());
			}

			@Override
			public void onCopy(Copy event) {
				String value = Ax
						.utf8String(remoteComponentEvent.provideOutputBytes());
				event.reemitAs(this, ModelEvents.CopyToClipboard.class, value);
			}
		}

		RemoteComponentEventDetailArea detail;

		@Override
		public Model apply(RemoteComponentEvent t) {
			detail = new RemoteComponentEventDetailArea(t);
			return this;
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

		FlightEventRow() {
		}

		FlightEventRow(FlightEvent event) {
			this.event = event;
			index = (int) event.id;
			time = new Date(event.provideTime());
			duration = event.provideDuration();
			type = NestedName.get(event.event);
			subtype = event.provideSubtype();
			detail = Ax.blankToEmpty(event.provideDetail())
					.replace("[CLIENT_TO_SERVER]", "");
			in = event.provideInputBytes().length;
			out = event.provideOutputBytes().length;
			stringRepresentation = event.provideStringRepresentation();
		}

		@Override
		public String provideStringRepresentation() {
			return stringRepresentation;
		}
	}

	@InstanceProvider.Parameter(SequencePathParameter.class)
	public static class InstanceProviderImpl
			implements InstanceProvider<FlightEventSequence> {
		@Override
		public FlightEventSequence provide(Query<FlightEventSequence> query)
				throws Exception {
			FlightEventSequence sequence = new FlightEventSequence();
			String path = PathEncoder
					.decode(query.parameterValue(SequencePathParameter.class));
			FlightEventStreamProviderZip provider = new FlightEventStreamProviderZip(
					path, s -> s);
			provider.getReplayStream();
			sequence.elements = provider.events;
			return sequence;
		}
	}

	static class PathEncoder {
		static String encode(String path) {
			return UrlComponentEncoder.get().encode(path);
		}

		static String decode(String path) {
			return UrlComponentEncoder.get().decode(path);
		}
	}
}
