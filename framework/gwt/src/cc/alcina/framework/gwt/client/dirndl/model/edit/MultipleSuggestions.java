package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Multiple;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;

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
@Feature.Ref(Feature_Dirndl_MultipleSuggestions.class)
@Directed(tag = "multiple-suggestions")
public class MultipleSuggestions<T> extends Multiple<T>
		implements HasDecorators {
	static PackageProperties._MultipleSuggestions properties = PackageProperties.multipleSuggestions;

	@Directed
	EditArea editArea;

	List<T> selectedValues;

	List<ContentDecorator> decorators = new ArrayList<>();

	@Override
	public List<T> getSelectedValues() {
		return selectedValues;
	}

	@Override
	public List<ContentDecorator> getDecorators() {
		return this.decorators;
	}

	transient KeyboardNavigation keyboardNavigation;

	@Override
	public void onKeyDown(KeyDown event) {
		if (hasActiveDecorator()) {
			keyboardNavigation.onKeyDown(event);
		}
		HasDecorators.super.onKeyDown(event);
	}

	@Override
	protected void updateSelectedValuesInternal(List<T> values) {
		this.selectedValues = values;
	}

	public MultipleSuggestions() {
		editArea = new EditArea();
		editArea.provideFragmentModel().addModelled(ChoiceNode.class);
		provideFragmentModel()
				.addModelled(DecoratorNode.ZeroWidthCursorTarget.class);
		keyboardNavigation = new KeyboardNavigation(this);
		bindings().from(this).on(properties.selectedValues)
				.accept(this::updateAreaFromSelectedValues);
		bindings().from(editArea).on(EditArea.properties.value)
				.withSetOnInitialise(false).signal(this::onEditCommit);
		decorators.add(createChoiceDecorator());
	}

	void onEditCommit() {
		List<T> selectedValues = editArea.fragmentModel.byType(ChoiceNode.class)
				.map(cn -> cn.getStringRepresentable()).filter(Objects::nonNull)
				.map(this::selectedValueFromString)
				.collect(Collectors.toList());
		setSelectedValues(selectedValues);
	}

	T selectedValueFromString(String uid) {
		return getValues().stream().filter(
				t -> Objects.equals(CommonUtils.nullSafeToString(t), uid))
				.findFirst().orElse(null);
	}

	ContentDecorator createChoiceDecorator() {
		ContentDecorator.Builder<Choice> builder = ContentDecorator.builder();
		builder.setChooserProvider(
				(decorator, decoratorNode) -> new ChoiceSuggestions(this,
						decorator, decoratorNode));
		builder.setDescriptor(ChoiceNode.Descriptor.INSTANCE);
		builder.setDecoratorParent(this);
		return builder.build();
	}

	// FIXME - fragmentNode - can this initial deferral be avoided?
	void updateAreaFromSelectedValues0(List<T> values) {
		// FIXME - FN - this should be a sync
		if (editArea.fragmentModel.byType(ChoiceNode.class).count() > 0) {
			return;
		}
		values.stream().map(Choice::new).map(ChoiceNode::new)
				.forEach(editArea.fragmentModel.getFragmentRoot()::append);
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
			static transient Descriptor INSTANCE = new Descriptor();

			@Override
			public ChoiceNode createNode() {
				return new ChoiceNode();
			}

			@Override
			public Function<Choice, String> itemRenderer() {
				return MultipleSuggestions::choiceToString;
			}

			public void onCommit(Commit event) {
				// NOOP - ancestor handles changes
			}

			@Override
			public String triggerSequence() {
				return "@";
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
			editArea.fragmentModel.getFragmentRoot().append(choiceNode);
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
	public static class ListSuggestions<T> extends Model.Value<List<T>>
			implements ModelEvents.SelectionChanged.Handler {
		@Directed
		public String captionish = "ish";

		@Directed
		public MultipleSuggestions<T> suggest;

		private List<T> value;

		public ListSuggestions() {
		}

		public static class To
				implements ModelTransform<List, ListSuggestions> {
			@Override
			public ListSuggestions apply(List t) {
				ListSuggestions suggest = new ListSuggestions();
				suggest.value = t;
				return suggest;
			}
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			suggest = new MultipleSuggestions<>();
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
	public FragmentModel provideFragmentModel() {
		return editArea.provideFragmentModel();
	}

	@Override
	public void validateDecorators() {
	}
}