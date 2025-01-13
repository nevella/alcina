package cc.alcina.framework.servlet.component.gallery;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.Button;
import cc.alcina.framework.gwt.client.dirndl.model.Dropdown;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;

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
		Heading section4 = new Heading("Actions");

		Link keyboardShortcuts = Link
				.of(GalleryBrowserCommand.ShowKeyboardShortcuts.class);

		Menu() {
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
