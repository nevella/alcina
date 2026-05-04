package cc.alcina.framework.servlet.component.console.home;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.console.ServerConsoleContents;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionPlace;

@Registration({ ServerConsoleContents.class, ServerConsoleHomePlace.class })
class ServerConsoleHomeArea extends ServerConsoleContents {
	Heading heading = new Heading("Server console");

	@Directed.Wrap("cards")
	List<Card> cards = new ArrayList<>();

	class Card extends Model.All {
		@Directed.Wrap("heading")
		Link link;

		String description;

		Card(ServerConsolePlace place) {
			link = new Link().withPlace(place).withText(place.toNameString());
			description = place.getDescription();
		}
	}

	ServerConsoleHomeArea() {
		add(RomcomSessionPlace.class);
	}

	void add(Class<? extends ServerConsolePlace> clazz) {
		ServerConsolePlace place = Reflections.newInstance(clazz);
		cards.add(new Card(place));
	}
}
