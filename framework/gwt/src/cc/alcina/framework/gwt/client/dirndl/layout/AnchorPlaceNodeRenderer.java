package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Optional;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.entity.place.ActionRefPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;

public class AnchorPlaceNodeRenderer extends LeafNodeRenderer {
	@Override
	protected String getTag(Node node) {
		return "a";
	}

	@Override
	public Widget render(Node node) {
		Widget rendered = super.render(node);
		rendered.getElement().setInnerText(getText(node));
		BasePlace place = (BasePlace) node.model;
		rendered.getElement().setAttribute("href", place.toHrefString());
		if (place instanceof ActionRefPlace) {
			ActionRefPlace actionRefPlace = (ActionRefPlace) place;
			Optional<ActionHandler> actionHandler = actionRefPlace
					.getActionHandler();
			if (actionHandler.isPresent()) {
				rendered.getElement().setAttribute("href", "#");
				rendered.addDomHandler(evt -> actionHandler.get()
						.handleAction(evt, actionRefPlace),
						ClickEvent.getType());
			}
		}
		return rendered;
	}

	protected String getText(Node node) {
		return node.model == null ? "<null text>" : node.model.toString();
	}
}
