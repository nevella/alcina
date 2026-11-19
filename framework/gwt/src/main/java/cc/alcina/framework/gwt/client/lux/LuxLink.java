package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.dirndl.StyleType;

public class LuxLink extends Widget implements HasClickHandlers {
	boolean addedCancelHandler = false;

	public LuxLink() {
		setElement(Document.get().createElement("A"));
	}

	@Override
	public HandlerRegistration addClickHandler(ClickHandler handler) {
		if (!addedCancelHandler) {
			addDomHandler(event -> event.preventDefault(),
					ClickEvent.getType());
			addedCancelHandler = true;
		}
		return addDomHandler(handler, ClickEvent.getType());
	}

	public LuxLink href(String href) {
		getElement().setAttribute("href", href);
		return this;
	}

	public LuxLink style(StyleType style) {
		style.set(this);
		return this;
	}

	public LuxLink text(String text) {
		getElement().setInnerText(text);
		return this;
	}
}
