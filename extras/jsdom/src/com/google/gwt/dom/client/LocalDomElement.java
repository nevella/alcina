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

	enum AttrParseState {
		START, NAME, EQ, VALUE
	}

	default void setOuterHtml(String html) {
		RegExp tag = RegExp.compile("<([A-Za-z0-9_\\-.]+)( .+?)?>(.+)?</.+>",
				"m");
		RegExp tagNoContents = RegExp.compile("<([A-Za-z0-9_\\-.]+)( .+?)?/?>",
				"m");
		MatchResult matchResult = tag.exec(html);
		if (matchResult == null) {
			matchResult = tagNoContents.exec(html);
		}
		String attrString = matchResult.getGroup(2);
		if (attrString != null) {
			char valueDelimiter = '-';
			AttrParseState state = AttrParseState.START;
			StringBuilder nameBuilder = null;
			StringBuilder valueBuilder = null;
			for (int idx = 0; idx < attrString.length(); idx++) {
				char c = attrString.charAt(idx);
				if (c == ' ') {
					switch (state) {
					case VALUE:
						break;
					default:
						continue;
					}
				}
				switch (state) {
				case START:
					state = AttrParseState.NAME;
					nameBuilder = new StringBuilder();
					nameBuilder.append(c);
					break;
				case NAME:
					if (c == '=') {
						state = AttrParseState.EQ;
					} else {
						nameBuilder.append(c);
					}
					break;
				case EQ:
					if (c == '\'' || c == '"') {
						valueBuilder = new StringBuilder();
						state = AttrParseState.VALUE;
						valueDelimiter = c;
					}
					break;
				case VALUE:
					if (c == valueDelimiter) {
						setAttribute(nameBuilder.toString(),
								valueBuilder.toString());
						state = AttrParseState.START;
					} else {
						valueBuilder.append(c);
					}
					break;
				}
			}
		}
		if (matchResult.getGroupCount() == 4) {
			setInnerHTML(matchResult.getGroup(3));
		}
	}

	void setInnerHTML(String html);

	int getEventBits();

	String getPendingInnerHtml();
}
