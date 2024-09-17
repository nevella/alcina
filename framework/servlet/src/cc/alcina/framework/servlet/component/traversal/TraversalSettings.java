package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.TypedProperties;

@TypedProperties
public class TraversalSettings extends Bindable.Fields {
	public static TraversalSettings get() {
		return TraversalBrowser.Ui.get().settings;
	}

	static PackageProperties._TraversalSettings properties = PackageProperties.traversalSettings;

	public boolean descentSelectionIncludesSecondaryRelations = true;

	public boolean showContainerLayers = false;

	public PropertyDisplayMode propertyDisplayMode = PropertyDisplayMode.QUARTER_WIDTH;

	public InputOutputDisplayMode inputOutputDisplayMode = InputOutputDisplayMode.INPUT_OUTPUT;

	public int tableRows = 50;

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
		properties.propertyDisplayMode.set(this, next);
		return next;
	}

	public InputOutputDisplayMode nextInputOutputDisplayMode() {
		InputOutputDisplayMode next = InputOutputDisplayMode
				.values()[(inputOutputDisplayMode.ordinal() + 1)
						% InputOutputDisplayMode.values().length];
		properties.inputOutputDisplayMode.set(this, next);
		return next;
	}

	public void putTableRows(String tableRowsStr) {
		properties.tableRows.set(this, Integer.parseInt(tableRowsStr));
	}
}
