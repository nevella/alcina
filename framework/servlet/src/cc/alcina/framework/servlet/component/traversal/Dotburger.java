package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.Button;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Dropdown;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.PropertyDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.SecondaryAreaDisplayMode;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

/*
 * The main dropdown navigation/options menu
 */
@Directed.Delegating
class Dotburger extends Model.Fields {
	@Directed
	Dropdown dropdown;

	Menu menu;

	@TypedProperties
	static class Menu extends Model.All implements ValueChange.Container {
		static PackageProperties._Dotburger_Menu properties = PackageProperties.dotburger_menu;

		Heading section1 = new Heading("Selection type");

		@Directed.Transform(Choices.Single.To.class)
		@Choices.EnumValues(SelectionType.class)
		SelectionType selectionType = SelectionType.VIEW;

		Heading section2 = new Heading("Property display mode");

		@Directed.Transform(Choices.Single.To.class)
		@Choices.EnumValues(PropertyDisplayMode.class)
		PropertyDisplayMode propertyDisplayMode = PropertyDisplayMode.QUARTER_WIDTH;

		Heading section3 = new Heading("Secondary display mode");

		@Directed.Transform(Choices.Single.To.class)
		@Choices.Values(TraversalBrowser.ValidSecondaryAreaDisplayModes.class)
		SecondaryAreaDisplayMode secondaryAreaDisplayMode = SecondaryAreaDisplayMode.INPUT_OUTPUT;

		Heading section4 = new Heading("Actions");

		Link keyboardShortcuts = Link
				.of(TraversalCommand.ShowKeyboardShortcuts.class);

		Menu() {
			bindings().from(TraversalSettings.get())
					.on(TraversalSettings.properties.secondaryAreaDisplayMode)
					.to(this).on(properties.secondaryAreaDisplayMode).bidi();
			bindings().from(TraversalSettings.get())
					.on(TraversalSettings.properties.propertyDisplayMode)
					.to(this).on(properties.propertyDisplayMode).bidi();
		}
	}

	Dotburger() {
		menu = new Menu();
		Button button = new LeafModel.Button();
		button.className = "dotburger";
		dropdown = new Dropdown(button, menu)
				.withLogicalAncestor(Dotburger.class).withXalign(Position.END);
	}
}
