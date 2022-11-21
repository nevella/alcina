package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.ModelEvents.Selected;

@Directed(tag = "choices", receives = ModelEvents.Selected.class)
// FIXME - dirndl 1x1d - change to "Choices"
// also this should emit ModelEvents, not topic
public abstract class Choices<T> extends Model
		implements ModelEvents.Selected.Handler {
	protected List<Choices.Choice<T>> choices;

	private List<T> values;

	public Choices() {
		this(new ArrayList<>());
	}

	public Choices(List<T> values) {
		setValues(values);
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
	 * TODO - dirndl1.1 - is it possible to bind just to isSelected here? in
	 * which case no need to wrap wdiget/elements
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

	public static class Multiple<T> extends Choices<T> {
		public Topic<List<T>> selectionChanged = Topic.create();

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

		public void setSelectedValues(List<T> values) {
			List<T> oldValues = getSelectedValues();
			Set valuesSet = new HashSet(values);
			choices.forEach(c -> c.setSelected(valuesSet.contains(c.value)));
			List<T> newValues = getSelectedValues();
			if (!Objects.equals(oldValues, newValues)) {
				selectionChanged.publish(newValues);
			}
		}
	}

	@TypeSerialization(reflectiveSerializable = false)
	public static class Single<T> extends Choices<T> {
		public Topic<T> selectionChanged = Topic.create();

		public Topic<T> valueSelected = Topic.create();

		protected boolean deselectIfSelectedClicked = false;

		/*
		 * set to false to allow more complex selection logic
		 */
		protected boolean changeOnSelectionEvent = true;

		public Single() {
		}

		public Single(List<T> values) {
			super(values);
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
			Choices.Choice<T> choice = event == null ? null : event.getModel();
			T value = choice == null ? null : choice.getValue();
			if (deselectIfSelectedClicked && value == getSelectedValue()) {
				value = null;
			}
			valueSelected.publish(value);
			if (changeOnSelectionEvent) {
				setSelectedValue(value);
			}
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
				selectionChanged.publish(newValue);
			}
		}
	}
}