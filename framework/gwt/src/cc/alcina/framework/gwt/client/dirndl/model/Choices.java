package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.MouseDownEvent;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.IndexedSelection;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.MouseDown;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Selected;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;

@Directed(tag = "choices", receives = { ModelEvents.Selected.class })
/*
 * I'm not entirely happy with 'Choices' firing 'Selection' events (could it not
 * be "chosen" events - or revert to "Selections/Selection") - even though
 * 'selected choice' is valid english and common parlance.
 *
 * But 'selection' is very heavily used in the codebase (including
 * SelectionTraversal) - so 'Choices+Selection' are the names we have.
 *
 * Also - possibly the wrapping with Choice could be done reflectively/more
 * elegantly
 *
 * FIXME - dirndl 1x1e - Now that overlay events are routed to logical parents,
 * it may be possible to cleanup event handling in this class
 */
public abstract class Choices<T> extends Model
		implements ModelEvents.Selected.Handler, HasSelectedValue {
	protected List<Choices.Choice<T>> choices;

	private List<T> values;

	public Choices() {
		this(new ArrayList<>());
	}

	public Choices(List<T> values) {
		setValues(values);
	}

	public Optional<Choice> find(Predicate<Choice> predicate) {
		return (Optional<Choice>) (Optional<?>) choices.stream()
				.filter(predicate::test).findFirst();
	}

	@Directed
	public List<Choices.Choice<T>> getChoices() {
		return this.choices;
	}

	public List<T> getValues() {
		return this.values;
	}

	public void setChoices(List<Choices.Choice<T>> choices) {
		var old_choices = this.choices;
		this.choices = choices;
		propertyChangeSupport().firePropertyChange("choices", old_choices,
				choices);
	}

	public void setValues(List<T> values) {
		this.values = values;
		setChoices(values.stream().map(Choices.Choice::new)
				.collect(Collectors.toList()));
	}

	/*
	 * It is possible to bind just to isSelected here (with a delegating
	 * renderer) - but the css for decoration of the choice becomes a lot
	 * simpler if there _is_ a choice > model structure, so Choice remains a
	 * container for the moment
	 *
	 * Note the preventDefault on mouseDown - which prevents (say) choices in a
	 * ReferenceDecorator moving document focus
	 */
	@Directed(
		bindings = {
				@Binding(
					type = Type.PROPERTY,
					from = "selected",
					to = "_selected"),
				@Binding(type = Type.PROPERTY, from = "indexSelected") },
		receives = { DomEvents.Click.class, DomEvents.MouseDown.class },
		emits = ModelEvents.Selected.class)
	public static class Choice<T> extends Model
			implements DomEvents.Click.Handler, DomEvents.MouseDown.Handler {
		private boolean selected;

		private boolean indexSelected;

		private final T value;

		public Choice(T value) {
			this.value = value;
		}

		@Directed
		public T getValue() {
			return this.value;
		}

		public boolean isIndexSelected() {
			return this.indexSelected;
		}

		public boolean isSelected() {
			return this.selected;
		}

		@Override
		public void onClick(Click event) {
			event.reemitAs(this, ModelEvents.Selected.class);
		}

		@Override
		public void onMouseDown(MouseDown event) {
			NativeEvent nativeEvent = ((MouseDownEvent) event.getContext()
					.getGwtEvent()).getNativeEvent();
			nativeEvent.preventDefault();
		}

		public void setIndexSelected(boolean indexSelected) {
			set("indexSelected", this.indexSelected, indexSelected,
					() -> this.indexSelected = indexSelected);
		}

		public void setSelected(boolean selected) {
			set("selected", this.selected, selected,
					() -> this.selected = selected);
		}
	}

	@Directed(emits = ModelEvents.SelectionChanged.class)
	public static class Multiple<T> extends Choices<T> {
		public Multiple() {
		}

		public Multiple(List<T> values) {
			super(values);
		}

		public List<T> getSelectedValues() {
			return choices.stream().filter(Choice::isSelected)
					.map(Choice::getValue).collect(Collectors.toList());
		}

		@Override
		public void onSelected(Selected event) {
			Choices.Choice<T> choice = event == null ? null : event.getModel();
			T value = choice == null ? null : choice.getValue();
			List<T> updatedValues = choices.stream().filter(c -> {
				T choiceValue = c.getValue();
				// toggle inclusion
				if (c.isSelected()) {
					return choiceValue != value;
				} else {
					return choiceValue == value;
				}
			}).map(Choice::getValue).collect(Collectors.toList());
			setSelectedValues(updatedValues);
		}

		@Override
		public Object provideSelectedValue() {
			return getSelectedValues();
		}

		public void setSelectedValues(List<T> values) {
			List<T> oldValues = getSelectedValues();
			Set valuesSet = new HashSet(values);
			choices.forEach(c -> c.setSelected(valuesSet.contains(c.value)));
			List<T> newValues = getSelectedValues();
			if (!Objects.equals(oldValues, newValues)) {
				NodeEvent.Context.fromNode(provideNode())
						.dispatch(ModelEvents.SelectionChanged.class, null);
			}
		}
	}

	@Directed(tag = "select", receives = DomEvents.Change.class)
	public static class Select<T> extends Single<T>
			implements DomEvents.Change.Handler {
		@Override
		public void onChange(Change event) {
			DirectedLayout.Node node = provideNode()
					.provideMostSpecificNodeForModel();
			SelectElement selectElement = (SelectElement) node.getRendered()
					.asElement();
			int index = selectElement.getSelectedIndex();
			T value = index >= 0 ? choices.get(index).getValue() : null;
			setSelectedValue(value);
			event.reemitAs(this, ModelEvents.Selected.class, value);
		}
	}

	/**
	 * Transforms a Choices model into an HTML Select. Note that the property of
	 * type Choices must have tag="select", since the resolver currently applies
	 * to child inputs, not the input on which it is declared
	 */
	public static class SelectResolver extends ContextResolver {
		@Override
		protected Property resolveDirectedProperty0(Property property) {
			if (property.getDeclaringType() == Choices.class
					&& property.getName().equals("choices")) {
				return Reflections.at(Select.class)
						.property(property.getName());
			} else {
				return super.resolveDirectedProperty0(property);
			}
		}

		/*
		 * Override to customize the default
		 */
		protected String transformOptionName(Choice choice) {
			return HasDisplayName.displayName(choice.getValue());
		}

		public static class Option extends Choices.Choice<String> {
			public Option(String displayName) {
				super(displayName);
			}

			public static class Transform extends
					AbstractContextSensitiveModelTransform<Choices.Choice, Option> {
				@Override
				public Option apply(Choice choice) {
					SelectResolver resolver = (SelectResolver) node
							.getResolver();
					return new Option(resolver.transformOptionName(choice));
				}
			}
		}

		/*
		 * Style template
		 */
		@Bean
		public static class Select {
			@Directed.Transform(Option.Transform.class)
			public List<Choices.Choice> getChoices() {
				return null;
			}
		}
	}

	@TypeSerialization(reflectiveSerializable = false)
	@Directed(
		emits = { ModelEvents.SelectionChanged.class,
				ModelEvents.Selected.class })
	public static class Single<T> extends Choices<T>
			implements KeyboardNavigation.Navigation.Handler {
		protected boolean deselectIfSelectedClicked = false;

		/*
		 * set to false to allow more complex selection logic
		 */
		protected boolean changeOnSelectionEvent = true;

		private T provisionalValue;

		/*
		 * Use ModelEvents by preference - this allows ex-hierarchy observation
		 * of changes if required (currently required for Overlay observation)
		 */
		private Topic<Void> selectionChanged = Topic.create();

		/*
		 * Use ModelEvents by preference - this allows ex-hierarchy observation
		 * of changes if required
		 */
		private Topic<Void> valueSelected = Topic.create();

		IndexedSelection indexedSelection;

		public Single() {
			this(new ArrayList<>());
		}

		public Single(List<T> values) {
			super(values);
			indexedSelection = new IndexedSelection(
					new IndexedSelectionHostImpl());
			indexedSelection.topicIndexChanged
					.add(this::onIndexedSelectionChange);
			updateIndexSelected(indexedSelection.getIndexSelected(), true);
		}

		public T getProvisionalValue() {
			return this.provisionalValue;
		}

		public T getSelectedValue() {
			return choices.stream().filter(Choice::isSelected).findFirst()
					.map(Choice::getValue).orElse(null);
		}

		public boolean isChangeOnSelectionEvent() {
			return this.changeOnSelectionEvent;
		}

		public boolean isDeselectIfSelectedClicked() {
			return this.deselectIfSelectedClicked;
		}

		@Override
		public void onNavigation(Navigation event) {
			switch (event.getModel()) {
			case COMMIT:
				int indexSelected = indexedSelection.getIndexSelected();
				if (indexSelected != -1) {
					event.consume();
					setSelectedValue(getValues().get(indexSelected));
					return;
				}
				break;
			}
			indexedSelection.onNavigation(event);
		}

		@Override
		public void onSelected(Selected event) {
			if (event.checkReemitted(this)) {
				return;
			}
			Choices.Choice<T> choice = event == null ? null : event.getModel();
			T value = choice == null ? null : choice.getValue();
			if (deselectIfSelectedClicked && value == getSelectedValue()) {
				value = null;
			}
			provisionalValue = value;
			event.reemit();
			valueSelected.signal();
			if (changeOnSelectionEvent) {
				setSelectedValue(value);
			}
		}

		@Override
		public Object provideSelectedValue() {
			return getSelectedValue();
		}

		public void setChangeOnSelectionEvent(boolean changeOnSelectionEvent) {
			this.changeOnSelectionEvent = changeOnSelectionEvent;
		}

		public void setDeselectIfSelectedClicked(
				boolean deselectIfSelectedClicked) {
			this.deselectIfSelectedClicked = deselectIfSelectedClicked;
		}

		public void setSelectedValue(T value) {
			T oldValue = getSelectedValue();
			choices.forEach(c -> c.setSelected(c.value == value));
			T newValue = getSelectedValue();
			if (!Objects.equals(oldValue, newValue)) {
				NodeEvent.Context.fromNode(provideNode())
						.dispatch(ModelEvents.SelectionChanged.class, null);
				selectionChanged.signal();
			}
		}

		public ListenerReference subscribeSelectionChanged(Runnable runnable) {
			return selectionChanged.add(runnable);
		}

		public ListenerReference subscribeValueSelected(Runnable runnable) {
			return valueSelected.add(runnable);
		}

		public void toggle(Choice<T> choice) {
			if (getSelectedValue() == choice.getValue()) {
				setSelectedValue(null);
			} else {
				setSelectedValue(choice.getValue());
			}
		}

		void onIndexedSelectionChange(IndexedSelection.Change change) {
			updateIndexSelected(change.oldIndexSelected, false);
			updateIndexSelected(change.newIndexSelected, true);
		}

		void updateIndexSelected(int index, boolean indexSelected) {
			if (index >= 0 && index < choices.size()) {
				choices.get(index).setIndexSelected(indexSelected);
			}
		}

		@Directed.Delegating
		public static class Delegating<T> extends Single<T> {
			public Delegating(List<T> values) {
				super(values);
			}
		}

		class IndexedSelectionHostImpl implements IndexedSelection.Host {
			@Override
			public List getItems() {
				return choices;
			}
		}
	}
}