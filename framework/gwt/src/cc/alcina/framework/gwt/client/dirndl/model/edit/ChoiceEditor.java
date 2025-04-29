package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Selected;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
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
@Feature.Ref(Feature_Dirndl_ChoiceSuggestions.class)
@Directed(tag = "choice-editor")
public abstract class ChoiceEditor<T> extends Choices<T>
		implements HasDecorators {
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
				return ChoiceEditor::choiceToString;
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

	static PackageProperties._ChoiceEditor properties = PackageProperties.choiceEditor;

	static String choiceToString(Choice choice) {
		return CommonUtils.nullSafeToString(choice.getValue());
	}

	@Directed
	EditArea editArea;

	List<ContentDecorator> decorators = new ArrayList<>();

	transient KeyboardNavigation keyboardNavigation;

	public ChoiceEditor() {
		editArea = new EditArea();
		editArea.provideFragmentModel().addModelled(ChoiceNode.class);
		provideFragmentModel().addModelled(ZeroWidthCursorTarget.class);
		keyboardNavigation = new KeyboardNavigation(this);
		bindings().from(editArea).on(EditArea.properties.value)
				.withSetOnInitialise(false).signal(this::onEditCommit);
		decorators.add(createChoiceDecorator());
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

	void onEditCommit() {
		List<T> selectedValues = getEditorValues();
		onSelectedValues(selectedValues);
	}

	@Property.Not
	List<T> getEditorValues() {
		return editArea.fragmentModel.byType(ChoiceNode.class)
				.map(ChoiceNode::getStringRepresentable)
				.filter(Objects::nonNull).map(this::selectedValueFromString)
				.collect(Collectors.toList());
	}

	protected abstract void onSelectedValues(List<T> selectedValues);

	T selectedValueFromString(String uid) {
		return getValues().stream().filter(
				t -> Objects.equals(CommonUtils.nullSafeToString(t), uid))
				.findFirst().orElse(null);
	}

	ContentDecorator createChoiceDecorator() {
		ContentDecorator.Builder<Choice> builder = ContentDecorator.builder();
		builder.setSuggestorProvider(
				(decorator, decoratorNode) -> new ChoiceSuggestor(this,
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