package com.google.gwt.dom.client;

import java.util.Optional;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public interface LocalDomElement extends LocalDomNode {
	Element createOrReturnChild(String tagName);

	void setOuterHtml(String html);

	void setAttribute(String name, String value);

	void setInnerHTML(String html);

	int getEventBits();

	String getPendingInnerHtml();
}
