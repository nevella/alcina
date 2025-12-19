package cc.alcina.framework.servlet.component.sequence;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.cmp.help.Help;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser.Ui;

@TypedProperties
class Header extends Model.All {
	PackageProperties._Header.InstanceProperties properties() {
		return PackageProperties.header.instance(this);
	}

	@TypedProperties
	static class Left extends Model.All {
		PackageProperties._Header_Left.InstanceProperties properties() {
			return PackageProperties.header_left.instance(this);
		}

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
			from(page.ui.settings.properties().sequenceKey())
					.map(key -> Ax.format("Sequence: %s", key))
					.to(properties().name()).oneWay();
			from(page.ui.subtypeProperties().place())
					.map(place -> Ax.isBlank(place.filter) ? ""
							: Ax.format("Filter: '%s'", place.filter))
					.to(properties().filter()).oneWay();
			from(page.ui.subtypeProperties().place())
					.map(place -> Ax.isBlank(place.highlight) ? ""
							: Ax.format("Highlight: '%s' [%s/%s]",
									place.highlight,
									Math.max(0, place.highlightIdx),
									page.highlightModel.matches.size()))
					.to(properties().highlight()).oneWay();
		}
	}

	class Mid extends Model.All {
		AppSuggestorSequence suggestor;

		Mid() {
			suggestor = new AppSuggestorSequence();
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
		mid = new Mid();
		right = new Right();
		from(page.properties().sequence()).nonNull().value(this).map(Left::new)
				.to(properties().left()).oneWay();
	}
}