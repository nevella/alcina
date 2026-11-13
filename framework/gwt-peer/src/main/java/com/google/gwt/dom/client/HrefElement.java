package com.google.gwt.dom.client;

public interface HrefElement {
	default boolean hasLinkHref() {
		String href = ((Element) this).getAttribute("href");
		return href.length() > 0 && !href.matches("#.*|javascript.*");
	}
}
