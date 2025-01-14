package cc.alcina.framework.servlet.component.gallery;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.Help;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@TypedProperties
class Header extends Model.All {
	@TypedProperties
	static class Left extends Model.All {
		@Directed.Transform(NameTransform.class)
		String name;

		static class NameTransform implements ModelTransform<String, Link> {
			@Override
			public Link apply(String t) {
				return new Link().withHref(Window.Location.getPath())
						.withText(t).withClassName("name");
			}
		}

		Left() {
			name = "Gallery";
		}
	}

	class Mid extends Model.All {
		AppSuggestorGallery suggestor;

		Mid() {
			suggestor = new AppSuggestorGallery();
		}
	}

	class Right extends Model.All {
		Help.HeaderButton help = new Help.HeaderButton();

		Dotburger dotburger = new Dotburger();
	}

	Left left;

	Mid mid;

	Right right;

	@Property.Not
	GalleryPage page;

	Header(GalleryPage page) {
		this.page = page;
		left = new Left();
		mid = new Mid();
		right = new Right();
	}
}