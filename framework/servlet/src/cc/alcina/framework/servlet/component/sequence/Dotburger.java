package cc.alcina.framework.servlet.component.sequence;

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
import cc.alcina.framework.servlet.component.sequence.SequenceSettings.DetailDisplayMode;
import cc.alcina.framework.servlet.component.traversal.TraversalCommand;

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

		Heading section2 = new Heading("Detail display mode");

		@Directed.Transform(Choices.Single.To.class)
		@Choices.EnumValues(DetailDisplayMode.class)
		DetailDisplayMode detailDisplayMode = DetailDisplayMode.QUARTER_WIDTH;

		Heading section4 = new Heading("Actions");

		Link keyboardShortcuts = Link
				.of(SequenceBrowserCommand.ShowKeyboardShortcuts.class);

		Menu() {
			bindings().from(SequenceSettings.get())
					.on(SequenceSettings.properties.detailDisplayMode).to(this)
					.on(properties.detailDisplayMode).bidi();
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
