package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Dropdown;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

/*
 * The main dropdown navigation/options menu
 */
@Directed.Delegating
class Dotburger extends Model.Fields {
	@Directed
	Dropdown dropdown;

	Menu menu;

	class Menu extends Model.All {
		@Directed.Transform(Choices.Single.To.class)
		@Choices.Values(
			value = Choices.Values.EnumSupplier.class,
			enumClass = SelectionType.class)
		SelectionType selectionType = SelectionType.VIEW;
	}

	Dotburger() {
		menu = new Menu();
		dropdown = new Dropdown(new LeafModel.Button("dotburger"), menu);
	}
}
