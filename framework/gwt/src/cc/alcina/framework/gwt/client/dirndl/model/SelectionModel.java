package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvents.Selected;

@Directed(receives = NodeEvents.Selected.class)
public class SelectionModel<T> extends Model
		implements NodeEvents.Selected.Handler {
	private List<SelectionModel.Choice> choices;

	public Topic<T> valueSelected = Topic.create();

	public Topic<T> selectionChanged = Topic.create();

	/*
	 * set to false to allow more complex selection logic
	 */
	private boolean changeOnSelectionEvent = true;

	private boolean deselectIfSelectedClicked = false;

	public SelectionModel(List<T> values) {
		this.choices = values.stream().map(SelectionModel.Choice::new)
				.collect(Collectors.toList());
	}

	@Directed
	public List<SelectionModel.Choice> getChoices() {
		return this.choices;
	}

	public T getSelectedValue() {
		return (T) choices.stream().filter(Choice::isSelected).findFirst()
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
		SelectionModel.Choice choice = event == null ? null : event.getModel();
		T value = choice == null ? null : (T) choice.getValue();
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

	public void
			setDeselectIfSelectedClicked(boolean deselectIfSelectedClicked) {
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

	/*
	 * TODO - dirndl1.1 - is it possible to bind just to isSelected here? in
	 * which case no need to wrap wdiget/elements
	 */
	@Directed(bindings = @Binding(type = Type.PROPERTY, from = "selected", to = "_selected"), receives = DomEvents.Click.class, reemits = NodeEvents.Selected.class)
	public static class Choice extends Model {
		private boolean selected;

		private final Object value;

		public Choice(Object value) {
			this.value = value;
		}

		@Directed
		public Object getValue() {
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
}