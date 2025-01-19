package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.Help;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;

class Header extends Model.All {
	static PackageProperties._Header_Left _Left_properties = PackageProperties.header_left;

	@TypedProperties
	class Left extends Model.All {
		String name;

		Left() {
			bindings().from(Header.this.page).on(Page.properties.history)
					.value(this::computeName).to(this).on(_Left_properties.name)
					.oneWay();
			bindings().from(Header.this.page.ui).on(Ui.properties.place)
					.value(this::computeName).to(this).on(_Left_properties.name)
					.oneWay();
		}

		String computeName() {
			FormatBuilder format = new FormatBuilder().separator(" - ");
			format.append(TraversalBrowser.Ui.get().getMainCaption());
			if (page.history != null) {
				format.append(page.history.displayName());
			}
			format.appendIfNonNull(page.place(), TraversalPlace::getTextFilter);
			return format.toString();
		}
	}

	class Mid extends Model.All {
		AppSuggestorTraversal suggestor;

		Mid() {
			suggestor = new AppSuggestorTraversal();
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
	Page page;

	Header(Page page) {
		this.page = page;
		left = new Left();
		mid = new Mid();
		right = new Right();
	}
}