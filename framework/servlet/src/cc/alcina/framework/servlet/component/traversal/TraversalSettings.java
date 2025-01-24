package cc.alcina.framework.servlet.component.traversal;

import java.util.Arrays;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;

@TypedProperties
@ReflectiveSerializer.Checks(ignore = false)
public class TraversalSettings extends Bindable.Fields {
	public static TraversalSettings get() {
		return TraversalBrowser.Ui.get().settings;
	}

	static PackageProperties._TraversalSettings properties = PackageProperties.traversalSettings;

	public boolean descentSelectionIncludesSecondaryRelations = true;

	public boolean showContainerLayers = false;

	public PropertyDisplayMode propertyDisplayMode = PropertyDisplayMode.QUARTER_WIDTH;

	public SecondaryAreaDisplayMode secondaryAreaDisplayMode = SecondaryAreaDisplayMode.INPUT_OUTPUT;

	public int tableRows = 50;

	public TraversalSettings() {
	}

	public enum PropertyDisplayMode {
		QUARTER_WIDTH, HALF_WIDTH, FULL_WIDTH, NONE
	}

	public enum SecondaryArea {
		INPUT, OUTPUT, TABLE;
	}

	public enum SecondaryAreaDisplayMode {
		INPUT_OUTPUT, INPUT, OUTPUT, TABLE, NONE;

		boolean isVisible(SecondaryArea area) {
			switch (area) {
			case INPUT:
				return this == INPUT || this == INPUT_OUTPUT;
			case OUTPUT:
				return this == OUTPUT || this == INPUT_OUTPUT;
			case TABLE:
				return this == TABLE;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	public PropertyDisplayMode nextPropertyDisplayMode() {
		PropertyDisplayMode next = PropertyDisplayMode
				.values()[(propertyDisplayMode.ordinal() + 1)
						% PropertyDisplayMode.values().length];
		properties.propertyDisplayMode.set(this, next);
		return next;
	}

	public SecondaryAreaDisplayMode nextSecondaryAreaDisplayMode() {
		SecondaryAreaDisplayMode[] values = TraversalBrowser.Ui.get()
				.getValidSecondaryAreadModes();
		SecondaryAreaDisplayMode next = values[(Arrays.asList(values)
				.indexOf(secondaryAreaDisplayMode) + 1) % values.length];
		properties.secondaryAreaDisplayMode.set(this, next);
		return next;
	}

	public void putTableRows(String tableRowsStr) {
		properties.tableRows.set(this, Integer.parseInt(tableRowsStr));
	}
}
