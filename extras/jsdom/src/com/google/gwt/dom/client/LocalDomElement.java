package com.google.gwt.dom.client;

public interface LocalDomElement extends LocalDomNode {
	Element createOrReturnChild(String tagName);

	int getEventBits();

	void setAttribute(String name, String value);

	void setInnerHTML(String html);

	void setOuterHtml(String html);
}
