package cc.alcina.framework.servlet.component.sequence;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser.Ui;

@TypedProperties
class Header extends Model.All {
	static PackageProperties._Header properties = PackageProperties.header;

	@TypedProperties
	static class Left extends Model.All {
		static PackageProperties._Header_Left properties = PackageProperties.header_left;

		@Directed.Transform(NameTransform.class)
		String name;

		static class NameTransform implements ModelTransform<String, Link> {
			@Override
			public Link apply(String t) {
				return new Link().withHref(Window.Location.getPath())
						.withText(t).withClassName("name");
			}
		}

		String filter;

		String highlight;

		Left(Header header) {
			Page page = header.page;
			bindings().from(page.ui.settings)
					.on(SequenceSettings.properties.sequenceKey)
					.map(key -> Ax.format("Sequence: %s", key)).to(this)
					.on(properties.name).oneWay();
			bindings().from(page.ui).on(Ui.properties.place)
					.map(place -> Ax.isBlank(place.filter) ? ""
							: Ax.format("Filter: '%s'", place.filter))
					.to(this).on(properties.filter).oneWay();
			bindings().from(page.ui).on(Ui.properties.place)
					.map(place -> Ax.isBlank(place.highlight) ? ""
							: Ax.format("Highlight: '%s' [%s/%s]",
									place.highlight,
									Math.max(0, place.highlightIdx),
									page.highlightModel.matches.size()))
					.to(this).on(properties.highlight).oneWay();
		}
	}

	class Mid extends Model.All {
		AppSuggestorSequence suggestor;

		Mid() {
			suggestor = new AppSuggestorSequence();
		}
	}

	class Right extends Model.All {
		Dotburger dotburger = new Dotburger();
	}

	Left left;

	Mid mid;

	Right right;

	@Property.Not
	Page page;

	Header(Page page) {
		this.page = page;
		mid = new Mid();
		right = new Right();
		bindings().from(page).on(Page.properties.sequence).nonNull().value(this)
				.map(Left::new).to(this).on(properties.left).oneWay();
	}
}