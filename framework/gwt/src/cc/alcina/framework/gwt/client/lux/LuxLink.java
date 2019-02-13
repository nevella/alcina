package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;

public class LuxLink extends Widget {
	public LuxLink() {
		setElement(Document.get().createElement("A"));
	}

	public LuxLink href(String href) {
		getElement().setAttribute("href", href);
		return this;
	}

	public LuxLink style(LuxStyleType style) {
		style.set(this);
		return this;
	}

	public LuxLink text(String text) {
		getElement().setInnerText(text);
		return this;
	}
}
