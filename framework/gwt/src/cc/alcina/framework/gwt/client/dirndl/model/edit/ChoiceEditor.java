package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Commit;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Selected;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorEvents.DecoratorsChanged;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor.SuggestOracleRouter;

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
 * 
 * <p>
 * This will emit a Commit event if enter is pressed with no choice selected
 * (i.e. reemitting a descendant CommitWithNoSelectedChoice) (instructing the
 * container that the user may be requesting commit of the choices)
 */
@Bean(PropertySource.FIELDS)
@TypedProperties
@Feature.Ref(Feature_Dirndl_ChoiceEditor_Impl.class)
@Directed(tag = "choice-editor")
public abstract class ChoiceEditor<T> extends Choices<T>
		implements HasDecorators, DecoratorEvents.DecoratorsChanged.Handler,
		ModelEvents.Commit.Handler, Choices.CommitWithNoSelectedChoice.Handler {
	@Directed(tag = "choice-node")
	@Bean(PropertySource.FIELDS)
	static class ChoiceNode extends DecoratorNode<Choice, Object>
			implements ModelEvents.Commit.Handler {
		static class Descriptor
				extends DecoratorNode.Descriptor<Choice, Object, ChoiceNode> {
			static transient Descriptor INSTANCE = new Descriptor();

			@Override
			public ChoiceNode createNode() {
				return new ChoiceNode();
			}

			@Override
			public Function<Choice, ?> itemRenderer() {
				return choice -> {
					Object value = choice.getValue();
					if (value instanceof Model) {
						return value;
					} else {
						return choiceToString(choice);
					}
				};
			}

			public void onCommit(Commit event) {
				// NOOP - ancestor handles changes
			}

			@Override
			public String triggerSequence() {
				return "";
			}

			@Override
			protected Object toStringRepresentable(Choice wrappedType) {
				return wrappedType.getValue();
				/*
				 * wip - decorator
				 */
				// if (wrappedType.getValue() instanceof StringRepresentable) {
				// }
				// return choiceToString(wrappedType);
			}
		}

		ChoiceNode() {
		}

		ChoiceNode(Choice choice) {
			putReferenced(choice);
		}

		@Override
		public DecoratorNode.Descriptor<Choice, Object, ?> getDescriptor() {
			return new ChoiceNode.Descriptor();
		}

		/**
		 * Reemit any commit events from inside - the container may position the
		 * cursor
		 */
		@Override
		public void onCommit(Commit event) {
			if (event.getModel() == this) {
				event.bubble();
				return;
			}
			event.reemitAs(this, Commit.class, this);
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

	SuggestOracleRouter suggestOracleRouter;

	@Override
	public void onDecoratorsChanged(DecoratorsChanged event) {
		List<DecoratorNode> model = event.getModel();
		List<T> decoratorChoiceValues = model.stream()
				.map(n -> (T) n.getStringRepresentable()).toList();
		event.reemitAs(this, ModelEvents.SelectionDirty.class,
				decoratorChoiceValues);
	}

	public ChoiceEditor() {
		editArea = new EditArea();
		editArea.provideFragmentModel().addModelled(ChoiceNode.class);
		keyboardNavigation = new KeyboardNavigation(this);
		bindings().from(editArea).on(EditArea.properties.value)
				.withSetOnInitialise(false).signal(this::onEditCommit);
		decorators.add(createChoiceDecorator());
	}

	/*
	 * this *may* be called twice - but this is interim, pending a think about
	 * how to resolve annotations along a transformation chain - and this is
	 * linked to annotation ResolutionHistory
	 * 
	 * 
	 * 
	 * wip - dirndl.transform
	 */
	@Override
	protected void populateFromNodeContext(Node node,
			Predicate<T> valueFilter) {
		Optional<Class<? extends SuggestOracleRouter>> routerTypeOptional = node
				.optional(RouterType.class).map(RouterType::value);
		SuggestOracleRouter suggestOracleRouter = routerTypeOptional
				.map(Reflections::newInstance).orElse(null);
		if (suggestOracleRouter != null) {
			this.suggestOracleRouter = suggestOracleRouter;
		}
		super.populateFromNodeContext(node, valueFilter);
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface RouterType {
		/**
		 * The answer type
		 */
		Class<? extends SuggestOracleRouter> value();
	}

	@Override
	public List<ContentDecorator> getDecorators() {
		return this.decorators;
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
	public List<Model> getElements() {
		return super.getElements();
	}

	@Override
	public FragmentModel provideFragmentModel() {
		return editArea.provideFragmentModel();
	}

	@Override
	public void validateDecorators() {
	}

	protected abstract void onSelectedValues(List<T> selectedValues);

	void onEditCommit() {
		List<T> selectedValues = getEditorValues();
		onSelectedValues(selectedValues);
	}

	@Property.Not
	List<T> getEditorValues() {
		return editArea.fragmentModel.byType(ChoiceNode.class)
				.map(ChoiceNode::getStringRepresentable).map(sr -> (T) sr)
				.collect(Collectors.toList());
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

	void updateAreaFromSelectedValue(T value) {
		List<T> values = value == null ? List.of() : List.of(value);
		Client.eventBus().queued()
				.lambda(() -> updateAreaFromSelectedValues0(values)).dispatch();
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

	@Override
	public void onCommit(Commit event) {
		Model model = event.getModel();
		if (model == this) {
			event.bubble();
			return;
		}
		ChoiceNode choiceNode = (ChoiceNode) model;
		DomNode choiceNodeNode = choiceNode.provideElement().asDomNode();
		DomNode target = choiceNodeNode.relative().nextSibling();
		Document.get().getSelection().collapse(target.asLocation());
	}

	@Override
	public void onCommitWithNoSelectedChoice(CommitWithNoSelectedChoice event) {
		event.reemitAs(this, Commit.class, this);
	}
}