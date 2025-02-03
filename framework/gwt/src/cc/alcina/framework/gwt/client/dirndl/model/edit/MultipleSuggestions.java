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
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Selected;
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
 * The {@link #getSelectedValues()} property is modelled directly by the
 * {@link #selectedValues} field.
 * 
 * <p>
 * Note: this doesn't share much implementation with other {@link Choices}
 * subtypes, but very much shares behavior, so I'm happy with the type structure
 * (sure, Choices could be abstracted further to an interface, but...)
 */
@Bean(PropertySource.FIELDS)
@TypedProperties
@Feature.Ref(Feature_Dirndl_MultipleSuggestions.class)
@Directed(tag = "multiple-suggestions")
public class MultipleSuggestions<T> extends Multiple<T>
		implements HasDecorators {
	/*
	 * * Binds a collection property (in an editor) to a MultipleSuggestor
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	public static class ListSuggestions<T> extends Model.Value<List<T>>
			implements ModelEvents.SelectionChanged.Handler {
		public static class To
				implements ModelTransform<List, ListSuggestions> {
			@Override
			public ListSuggestions apply(List t) {
				ListSuggestions suggest = new ListSuggestions();
				suggest.value = t;
				return suggest;
			}
		}

		@Directed
		public MultipleSuggestions<T> suggest;

		private List<T> value;

		public ListSuggestions() {
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
		public void onSelectionChanged(ModelEvents.SelectionChanged event) {
			setValue(suggest.getSelectedValues());
		}
	}

	@Directed(tag = "choice-node")
	@Bean(PropertySource.FIELDS)
	static class ChoiceNode extends DecoratorNode<Choice, String> {
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
				return "";
			}

			@Override
			protected String toStringRepresentable(Choice wrappedType) {
				return choiceToString(wrappedType);
			}
		}

		ChoiceNode() {
		}

		ChoiceNode(Choice choice) {
			putReferenced(choice);
		}

		@Override
		public DecoratorNode.Descriptor<Choice, String, ?> getDescriptor() {
			return new ChoiceNode.Descriptor();
		}
	}

	static PackageProperties._MultipleSuggestions properties = PackageProperties.multipleSuggestions;

	static String choiceToString(Choice choice) {
		return CommonUtils.nullSafeToString(choice.getValue());
	}

	@Directed
	EditArea editArea;

	List<T> selectedValues;

	List<ContentDecorator> decorators = new ArrayList<>();

	transient KeyboardNavigation keyboardNavigation;

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

	@Override
	public List<T> getSelectedValues() {
		return selectedValues;
	}

	@Override
	public List<ContentDecorator> getDecorators() {
		return this.decorators;
	}

	/**
	 * FIXME - reflection - this shouldn't be needed (should be resolved from
	 * {@link HasDecorators} - that's possibly a gwt vs jdk typemodel
	 * inconsistency )
	 */
	@Binding(
		type = Type.PROPERTY,
		to = DecoratorBehavior.ExtendKeyboardNavigationAction.ATTR_NAME)
	@Override
	public boolean isMagicName() {
		return true;
	}

	@Override
	public void onSelected(Selected event) {
		/*
		 * this will be from the decorator Selected event (so unrelated to the
		 * selections of *this* area) and should be squelched
		 */
		/*
		 * NOOP
		 */
	}

	@Override
	public void onKeyDown(KeyDown event) {
		if (hasActiveDecorator()) {
			keyboardNavigation.onKeyDown(event);
		}
		HasDecorators.super.onKeyDown(event);
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

	@Override
	protected void updateSelectedValuesInternal(List<T> values) {
		this.selectedValues = values;
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
}