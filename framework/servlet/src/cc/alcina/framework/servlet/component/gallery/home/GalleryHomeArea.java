package cc.alcina.framework.servlet.component.gallery.home;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.gallery.GalleryContents;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.gallery.model.multiplesuggestions.MultipleSuggestionsGalleryPlace;

@Registration({ GalleryContents.class, GalleryHomePlace.class })
class GalleryHomeArea extends GalleryContents {
	Heading heading = new Heading("Dirndl Gallery");

	@Directed.Wrap("cards")
	List<Card> cards = new ArrayList<>();

	class Card extends Model.All {
		@Directed.Wrap("heading")
		Link link;

		String description;

		Card(GalleryPlace place) {
			link = new Link().withPlace(place).withText(place.toNameString());
			description = place.getDescription();
		}
	}

	GalleryHomeArea() {
		add(MultipleSuggestionsGalleryPlace.class);
	}

	void add(Class<? extends GalleryPlace> clazz) {
		GalleryPlace place = Reflections.newInstance(clazz);
		cards.add(new Card(place));
	}
}
