package com.google.gwt.dom.client;

import java.util.List;
import java.util.Optional;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public interface LocalDomElement extends LocalDomNode {
	default Element createOrReturnChild(String tagName) {
		Optional<LocalDomNode> optional = localDomChildren().stream()
				.filter(n -> n.getNodeName().equals(tagName)).findFirst();
		if (optional.isPresent()) {
			return LocalDomBridge.nodeFor((Node_Jvm) optional.get());
		}
		LocalDomElement newElement = create(tagName);
		appendChild(LocalDomBridge.nodeFor((Node_Jvm) newElement));
		return LocalDomBridge.nodeFor((Node_Jvm) newElement);
	}

	LocalDomElement create(String tagName);

	void setAttribute(String name, String value);

	default void setOuterHtml(String html) {
		RegExp tag = RegExp.compile("<\\S+( .+?)?>(.+)</.+>", "m");
		RegExp attr = RegExp.compile("(\\S+?)=[\"'](.+?)[\"']", "mg");
		MatchResult matchResult = tag.exec(html);
		String attrString = matchResult.getGroup(1);
		if (attrString != null) {
			MatchResult attrMatch = null;
			while ((attrMatch = attr.exec(attrString)) != null) {
				setAttribute(attrMatch.getGroup(1), attrMatch.getGroup(2));
			}
		}
		setInnerHTML(matchResult.getGroup(2));
	}

	void setInnerHTML(String html);
	int getEventBits();

	 String getPendingInnerHtml();
}
