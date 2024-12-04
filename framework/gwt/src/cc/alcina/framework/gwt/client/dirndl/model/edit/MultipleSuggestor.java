package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Multiple;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * Renders a set of choices as a contenteditable - suitable for email
 * 'to/cc/bcc' fields, etc
 * 
 * <p>
 * Unlike the parent (and other {@link Choices} subtypes), this doesn't have a
 * fixed set of choices, so does not render the {@link #getChoices()} property
 * 
 * <p>
 * The {@link #getSelectedValues()} property is modelled directly
 */
@Bean(PropertySource.FIELDS)
@TypedProperties
public class MultipleSuggestor<T> extends Multiple<T> {
	static PackageProperties._MultipleSuggestor properties = PackageProperties.multipleSuggestor;

	@Directed
	EditArea area;

	List<T> selectedValues;

	@Override
	public List<T> getSelectedValues() {
		return selectedValues;
	}

	@Override
	protected void updateSelectedValuesInternal(List<T> values) {
		this.selectedValues = values;
	}

	public MultipleSuggestor() {
		area = new EditArea();
		area.provideFragmentModel().addModelled(ChoiceNode.class);
		bindings().from(this).on(properties.selectedValues)
				.accept(this::updateAreaFromSelectedValues);
	}

	// FIXME - fragmentNode - can this initial deferral be avoided?
	void updateAreaFromSelectedValues0(List<T> values) {
		values.stream().map(Choice::new).map(ChoiceNode::new)
				.forEach(area.fragmentModel.getFragmentRoot()::append);
	}

	void updateAreaFromSelectedValues(List<T> values) {
		Client.eventBus().queued()
				.lambda(() -> updateAreaFromSelectedValues0(values)).dispatch();
	}

	@Directed(tag = "choice-node")
	@Bean(PropertySource.FIELDS)
	static class ChoiceNode extends DecoratorNode<Choice, String> {
		@Override
		public DecoratorNode.Descriptor<Choice, String, ?> getDescriptor() {
			return new ChoiceNode.Descriptor();
		}

		ChoiceNode() {
		}

		ChoiceNode(Choice choice) {
			putReferenced(choice);
		}

		static class Descriptor
				extends DecoratorNode.Descriptor<Choice, String, ChoiceNode> {
			@Override
			public ChoiceNode createNode() {
				return new ChoiceNode();
			}

			@Override
			public Function<Choice, String> itemRenderer() {
				return MultipleSuggestor::choiceToString;
			}

			@Override
			public void onCommit(Commit event) {
				// TODO - fire changes
			}

			@Override
			public String triggerSequence() {
				return "";
			}

			@Override
			protected String toStringRepresentable(Choice wrappedType) {
				return choiceToString(wrappedType);
			}
		}
	}

	static String choiceToString(Choice choice) {
		return CommonUtils.nullSafeToString(choice.getValue());
	}

	void areaContentsFromChoices0(List<Choice<?>> choices) {
		choices.forEach(choice -> {
			ChoiceNode choiceNode = new ChoiceNode();
			choiceNode.putReferenced(choice);
			area.fragmentModel.getFragmentRoot().append(choiceNode);
		});
	}

	void areaContentsFromChoices(List<Choice<?>> choices) {
		Client.eventBus().queued()
				.lambda(() -> areaContentsFromChoices0(choices)).dispatch();
	}

	/*
	 * * Binds a collection property (in an editor) to a MultipleSuggestor
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	public static class ListSuggestor<T> extends Model.Value<List<T>>
			implements ModelEvents.SelectionChanged.Handler {
		@Directed
		public String captionish = "ish";

		@Directed
		public MultipleSuggestor<T> suggest;

		private List<T> value;

		public ListSuggestor() {
		}

		public static class To implements ModelTransform<List, ListSuggestor> {
			@Override
			public ListSuggestor apply(List t) {
				ListSuggestor suggest = new ListSuggestor();
				suggest.value = t;
				return suggest;
			}
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			suggest = new MultipleSuggestor<>();
			// populate the delegate values from this node's AnnotationLocation
			suggest.populateValuesFromNodeContext(event.node, null);
			value = suggest.getSelectedValues();
			super.onBeforeRender(event);
		}

		@Override
		public List<T> getValue() {
			return value;
		}

		@Override
		public void setValue(List<T> value) {
			set("value", this.value, value, () -> this.value = value);
			suggest.setSelectedValues(value);
		}

		@Override
		public void onSelectionChanged(SelectionChanged event) {
			setValue(suggest.getSelectedValues());
		}
	}

	@Override
	@Directed.Exclude
	public List<Choice<T>> getChoices() {
		return super.getChoices();
	}

	@Override
	protected void populateValuesFromNodeContext(Node node,
			Predicate<T> valueFilter) {
	}
}