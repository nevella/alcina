package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.csobjects.Bindable;

public class TraversalSettings extends Bindable.Fields {
	public static TraversalSettings get() {
		return TraversalProcessView.Ui.get().settings;
	}

	public boolean descentSelectionIncludesSecondaryRelations = true;

	public boolean showContainerLayers = false;

	public PropertyDisplayMode propertyDisplayMode = PropertyDisplayMode.QUARTER_WIDTH;

	public InputOutputDisplayMode inputOutputDisplayMode = InputOutputDisplayMode.INPUT_OUTPUT;

	public void setInputOutputDisplayMode(
			InputOutputDisplayMode inputOutputDisplayMode) {
		set("inputOutputDisplayMode", this.inputOutputDisplayMode,
				inputOutputDisplayMode,
				() -> this.inputOutputDisplayMode = inputOutputDisplayMode);
	}

	public void setDescentSelectionIncludesSecondaryRelations(
			boolean descentSelectionIncludesSecondaryRelations) {
		set("descentSelectionIncludesSecondaryRelations",
				this.descentSelectionIncludesSecondaryRelations,
				descentSelectionIncludesSecondaryRelations,
				() -> this.descentSelectionIncludesSecondaryRelations = descentSelectionIncludesSecondaryRelations);
	}

	public void setShowContainerLayers(boolean showContainerLayers) {
		set("showContainerLayers", this.showContainerLayers,
				showContainerLayers,
				() -> this.showContainerLayers = showContainerLayers);
	}

	public void
			setPropertyDisplayMode(PropertyDisplayMode propertyDisplayMode) {
		set("propertyDisplayMode", this.propertyDisplayMode,
				propertyDisplayMode,
				() -> this.propertyDisplayMode = propertyDisplayMode);
	}

	public enum PropertyDisplayMode {
		QUARTER_WIDTH, HALF_WIDTH, NONE
	}

	public enum InputOutputDisplayMode {
		INPUT_OUTPUT, INPUT, OUTPUT, NONE
	}

	public PropertyDisplayMode nextPropertyDisplayMode() {
		PropertyDisplayMode next = PropertyDisplayMode
				.values()[(propertyDisplayMode.ordinal() + 1)
						% PropertyDisplayMode.values().length];
		setPropertyDisplayMode(next);
		return next;
	}

	public InputOutputDisplayMode nextInputOutputDisplayMode() {
		InputOutputDisplayMode next = InputOutputDisplayMode
				.values()[(inputOutputDisplayMode.ordinal() + 1)
						% InputOutputDisplayMode.values().length];
		setInputOutputDisplayMode(next);
		return next;
	}
}
