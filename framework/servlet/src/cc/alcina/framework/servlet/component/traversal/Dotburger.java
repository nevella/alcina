package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Dropdown;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.InputOutputDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.PropertyDisplayMode;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

/*
 * The main dropdown navigation/options menu
 */
@Directed.Delegating
class Dotburger extends Model.Fields {
	@Directed
	Dropdown dropdown;

	Menu menu;

	static class Menu extends Model.All implements ValueChange.Container {
		static PackageProperties._Dotburger_Menu properties = PackageProperties.dotburger_Menu;

		Heading section1 = new Heading("Selection type");

		@Directed.Transform(Choices.Single.To.class)
		@Choices.EnumValues(SelectionType.class)
		SelectionType selectionType = SelectionType.VIEW;

		Heading section2 = new Heading("Property display mode");

		@Directed.Transform(Choices.Single.To.class)
		@Choices.EnumValues(PropertyDisplayMode.class)
		PropertyDisplayMode propertyDisplayMode = PropertyDisplayMode.QUARTER_WIDTH;

		Heading section3 = new Heading("I/O display mode");

		@Directed.Transform(Choices.Single.To.class)
		@Choices.EnumValues(InputOutputDisplayMode.class)
		InputOutputDisplayMode ioDisplayMode = InputOutputDisplayMode.INPUT_OUTPUT;

		Menu() {
			bindings().from(TraversalSettings.get())
					.on(TraversalSettings.properties.inputOutputDisplayMode)
					.to(this).on(properties.ioDisplayMode).bidi();
			bindings().from(TraversalSettings.get())
					.on(TraversalSettings.properties.propertyDisplayMode)
					.to(this).on(properties.propertyDisplayMode).bidi();
		}
	}

	Dotburger() {
		menu = new Menu();
		dropdown = new Dropdown(new LeafModel.Button("dotburger"), menu)
				.withLogicalAncestor(Dotburger.class).withXalign(Position.END);
	}
}
