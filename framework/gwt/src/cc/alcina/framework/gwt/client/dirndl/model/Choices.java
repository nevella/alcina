package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Selected;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

@Directed(tag = "choices", receives = ModelEvents.Selected.class)
/*
 * I'm not entirely happy with 'Choices' firing 'Selection' events (could it not
 * be "chosen" events - or revert to "Selections/Selection") - even though
 * 'selected choice' is valid english and common parlance.
 *
 * But 'selection' is very heavily used in the codebase (including
 * SelectionTraversal) - so 'Choices+Selection' are the names we have.
 */
public abstract class Choices<T> extends Model.WithNode
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
	 */
	@Directed(
		bindings = @Binding(
			type = Type.PROPERTY,
			from = "selected",
			to = "_selected"),
		receives = DomEvents.Click.class,
		reemits = ModelEvents.Selected.class)
	public static class Choice<T> extends Model {
		private boolean selected;

		private final T value;

		public Choice(T value) {
			this.value = value;
		}

		@Directed
		public T getValue() {
			return this.value;
		}

		public boolean isSelected() {
			return this.selected;
		}

		public void setSelected(boolean selected) {
			boolean old_selected = this.selected;
			this.selected = selected;
			propertyChangeSupport().firePropertyChange("selected", old_selected,
					selected);
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
				NodeEvent.Context.newNodeContext(node)
						.fire(ModelEvents.SelectionChanged.class);
			}
		}
	}

	@TypeSerialization(reflectiveSerializable = false)
	@Directed(
		emits = { ModelEvents.SelectionChanged.class,
				ModelEvents.Selected.class })
	public static class Single<T> extends Choices<T> {
		protected boolean deselectIfSelectedClicked = false;

		/*
		 * set to false to allow more complex selection logic
		 */
		protected boolean changeOnSelectionEvent = true;

		private T provisionalValue;

		/*
		 * Use ModelEvents by preference - this allows ex-hierarchy observation
		 * of changes if required
		 */
		private Topic<Void> selectionChanged = Topic.create();

		/*
		 * Use ModelEvents by preference - this allows ex-hierarchy observation
		 * of changes if required
		 */
		private Topic<Void> valueSelected = Topic.create();

		public Single() {
		}

		public Single(List<T> values) {
			super(values);
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
		public void onSelected(Selected event) {
			if (event.wasReemitted(node)) {
				event.getContext().markCauseEventAsNotHandled();
				return;
			}
			Choices.Choice<T> choice = event == null ? null : event.getModel();
			T value = choice == null ? null : choice.getValue();
			if (deselectIfSelectedClicked && value == getSelectedValue()) {
				value = null;
			}
			provisionalValue = value;
			NodeEvent.Context.newModelContext(event.getContext(), node)
					.fire(Selected.class);
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
				NodeEvent.Context.newNodeContext(node)
						.fire(ModelEvents.SelectionChanged.class);
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

		@Directed.Delegating
		public static class Delegating<T> extends Single<T> {
			public Delegating(List<T> values) {
				super(values);
			}
		}
	}
}