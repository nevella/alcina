package cc.alcina.framework.servlet.component.console;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.Help;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@TypedProperties
class Header extends Model.All {
	@TypedProperties
	static class Left extends Model.All {
		@Directed(
			tag = "a",
			bindings = @Binding(
				type = Binding.Type.PROPERTY,
				to = "href",
				literal = "#"))
		Object logo = "Alcina server console";

		Left() {
		}
	}

	class Mid extends Model.All {
		AppSuggestorServerConsole suggestor;

		Mid() {
			suggestor = new AppSuggestorServerConsole();
		}
	}

	class Right extends Model.All {
		Help.HeaderButton helpButton = new Help.HeaderButton();

		Dotburger dotburger = new Dotburger();
	}

	Left left;

	Mid mid;

	Right right;

	@Property.Not
	ServerConsolePage page;

	Header(ServerConsolePage page) {
		this.page = page;
		left = new Left();
		mid = new Mid();
		right = new Right();
	}
}