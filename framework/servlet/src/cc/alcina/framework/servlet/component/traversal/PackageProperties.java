package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.InputOutputDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.PropertyDisplayMode;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

class PackageProperties {
	static _Dotburger_Menu dotburger_Menu = new _Dotburger_Menu();

	static _TraversalSettings traversalSettings = new _TraversalSettings();

	static class _Dotburger_Menu implements TypedProperty.Container {
		TypedProperty<Dotburger.Menu, Heading> section1 = new TypedProperty<>(
				Dotburger.Menu.class, "section1");

		TypedProperty<Dotburger.Menu, SelectionType> selectionType = new TypedProperty<>(
				Dotburger.Menu.class, "selectionType");

		TypedProperty<Dotburger.Menu, Heading> section2 = new TypedProperty<>(
				Dotburger.Menu.class, "section2");

		TypedProperty<Dotburger.Menu, PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(
				Dotburger.Menu.class, "propertyDisplayMode");

		TypedProperty<Dotburger.Menu, Heading> section3 = new TypedProperty<>(
				Dotburger.Menu.class, "section3");

		TypedProperty<Dotburger.Menu, InputOutputDisplayMode> ioDisplayMode = new TypedProperty<>(
				Dotburger.Menu.class, "ioDisplayMode");
	}

	static class _TraversalSettings implements TypedProperty.Container {
		TypedProperty<TraversalSettings, Boolean> descentSelectionIncludesSecondaryRelations = new TypedProperty<>(
				TraversalSettings.class,
				"descentSelectionIncludesSecondaryRelations");

		TypedProperty<TraversalSettings, Boolean> showContainerLayers = new TypedProperty<>(
				TraversalSettings.class, "showContainerLayers");

		TypedProperty<TraversalSettings, PropertyDisplayMode> propertyDisplayMode = new TypedProperty<>(
				TraversalSettings.class, "propertyDisplayMode");

		TypedProperty<TraversalSettings, InputOutputDisplayMode> inputOutputDisplayMode = new TypedProperty<>(
				TraversalSettings.class, "inputOutputDisplayMode");
	}
}
