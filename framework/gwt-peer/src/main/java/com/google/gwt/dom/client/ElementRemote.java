package com.google.gwt.dom.client;

public interface ElementRemote extends ClientDomElement {
	DomRect getBoundingClientRect();

	void setSelectionRange(int pos, int length);

	ClientDomStyle getStyleRemote();
}
