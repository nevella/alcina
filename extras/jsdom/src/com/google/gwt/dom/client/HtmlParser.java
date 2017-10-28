package com.google.gwt.dom.client;

import java.util.Objects;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

public class HtmlParser {
	enum TokenState {
		EXPECTING_NODE, EXPECTING_TAG, TEXT, EXPECTING_COMMENT,
		EXPECTING_ATTRIBUTES, EXPECTING_ATTR_SEP, EXPECTING_ATTR_VALUE_DELIM,
		EXPECTING_ATTR_VALUE
	}

	StringBuilder builder = new StringBuilder();

	boolean builderIsWhiteSpace = true;

	boolean selfCloseTag = false;

	TokenState tokenState;

	void resetBuilder() {
		builder.setLength(0);
	}

	int idx = 0;

	private String html;

	private String tag;

	private String attrName;

	private char attrDelim;

	private String attrValue;

	// FIXME - optimise
	private StringMap attributes = new StringMap();

	private Element rootResult;

	private Element cursor;

	private Element replaceContents;

	private boolean emitHtmlHeadBodyTags;

	public Element parse(DomElement root, Element replaceContents,
			boolean emitHtmlHeadBodyTags) {
		return parse(root.getOuterHtml(), replaceContents,
				emitHtmlHeadBodyTags);
	}

	public Element parse(String html, Element replaceContents,
			boolean emitHtmlHeadBodyTags) {
		this.html = html;
		this.replaceContents = replaceContents;
		this.emitHtmlHeadBodyTags = emitHtmlHeadBodyTags;
		resetBuilder();
		tokenState = TokenState.EXPECTING_NODE;
		LocalDom.setDisableRemoteWrite(true);
		if (replaceContents != null) {
			replaceContents.clearResolved();
		}
		while (idx < html.length()) {
			char c = html.charAt(idx++);
			boolean isWhiteSpace = false;
			boolean emptyBuffer = builder.length() == 0;
			switch (c) {
			case ' ':
			case '\t':
			case '\n':
			case '\r':
				isWhiteSpace = true;
			}
			// ignoreable whitespace
			switch (tokenState) {
			case EXPECTING_NODE:
				if (c == '<') {
					resetBuilder();
					tokenState = TokenState.EXPECTING_TAG;
				} else {
					tokenState = TokenState.TEXT;
					builder.append(c);
				}
				break;
			case TEXT:
				if (c == '<') {
					emitEscapedText(builder.toString());
					resetBuilder();
					tokenState = TokenState.EXPECTING_TAG;
				} else {
					builder.append(c);
				}
				break;
			case EXPECTING_TAG:
				selfCloseTag = false;
				String tagLookahead = builder.toString();
				if (tagLookahead.equals("!--")) {
					tag = tagLookahead;
					resetBuilder();
					builder.append(c);
					tokenState = TokenState.EXPECTING_COMMENT;
				} else {
					if (isWhiteSpace) {
						tag = tagLookahead;
						resetBuilder();
						tokenState = TokenState.EXPECTING_ATTRIBUTES;
					} else {
						switch (c) {
						case '/':
							if (builder.length() > 0) {
								selfCloseTag = true;
							} else {
								builder.append(c);
							}
							break;
						case '>':
							emitElement();
							resetBuilder();
							break;
						default:
							builder.append(c);
							break;
						}
					}
				}
				break;
			case EXPECTING_COMMENT:
				if (c == '>' && builder.toString().endsWith("--")) {
					builder.setLength(builder.length() - 2);
					emitComment(builder.toString());
					resetBuilder();
					tokenState = TokenState.EXPECTING_NODE;
				} else {
					builder.append(c);
				}
				break;
			case EXPECTING_ATTRIBUTES:
				if (isWhiteSpace) {
					continue;
				}
				switch (c) {
				case '/':
					selfCloseTag = true;
					break;
				case '>':
					emitElement();
					resetBuilder();
					break;
				default:
					builder.append(c);
					tokenState = TokenState.EXPECTING_ATTR_SEP;
					break;
				}
				break;
			case EXPECTING_ATTR_SEP:
				switch (c) {
				case '=':
					attrName = builder.toString();
					attrValue = "";
					resetBuilder();
					tokenState = TokenState.EXPECTING_ATTR_VALUE_DELIM;
					break;
				default:
					builder.append(c);
					break;
				}
				break;
			case EXPECTING_ATTR_VALUE_DELIM:
				switch (c) {
				case '"':
				case '\'':
					attrDelim = c;
					tokenState = TokenState.EXPECTING_ATTR_VALUE;
					break;
				case ' ':
					attrValue = "";
					emitAttribute();
					resetBuilder();
					break;
				default:
					attrDelim = ' ';
					builder.append(c);
					tokenState = TokenState.EXPECTING_ATTR_VALUE;
					break;
				}
				break;
			case EXPECTING_ATTR_VALUE:
				boolean handled = false;
				if (attrDelim == ' ' && c == '>') {
					attrValue = builder.toString();
					emitAttribute();
					emitElement();
					resetBuilder();
					break;
				}
				if (c == attrDelim) {
					attrValue = builder.toString();
					emitAttribute();
					resetBuilder();
					tokenState = TokenState.EXPECTING_ATTRIBUTES;
					break;
				}
				builder.append(c);
				break;
			}
		}
		LocalDom.setDisableRemoteWrite(false);
		return rootResult;
	}

	private void emitAttribute() {
		attributes.put(attrName, decodeEntities(attrValue));
	}

	static String decodeEntities(String text) {
		return EntityDecoder.decode(text);
	}

	private void emitElement() {
		boolean closeTag = false;
		if (tag == null) {
			tag = builder.toString();
			if (tag.startsWith("/")) {
				tag = tag.substring(1);
				closeTag = true;
			}
		}
		if (!closeTag) {
			emitStartElement(tag);
		}
		selfCloseTag |= isSelfClosingTag(tag);
		if (closeTag && selfCloseTag) {
			// exclusive or really. we'll have already emitted the close here,
			// so ignore
			// e.g. is gwt celltable safehtml for input
		} else {
			if (closeTag || selfCloseTag) {
				emitEndElement(tag);
			}
		}
		switch (tag) {
		case "script":
		case "style":
			Preconditions.checkState(!closeTag);
			String close = Ax.format("</%s>", tag);
			String close2 = Ax.format("</%s>", tag.toUpperCase());
			int idx2 = html.indexOf(close, idx);
			int idx3 = html.indexOf(close2, idx);
			if (idx2 == -1 || (idx3 != -1 && idx3 < idx2)) {
				idx2 = idx3;
			}
			String textContent = html.substring(idx, idx2);
			emitText(textContent);
			emitEndElement(tag);
			idx = idx2 + close.length();
			break;
		}
		tokenState = TokenState.EXPECTING_NODE;
		tag = null;
		selfCloseTag = false;
	}

	// https://www.thoughtco.com/html-singleton-tags-3468620
	static boolean isSelfClosingTag(String tag) {
		switch (tag) {
		case "area":
		case "base":
		case "br":
		case "col":
		case "command":
		case "embed":
		case "hr":
		case "img":
		case "input":
		case "keygen":
		case "link":
		case "meta":
		case "param":
		case "source":
		case "track":
		case "wbr":
			return true;
		}
		return false;
	}

	private void emitStartElement(String tag) {
		if (!emitHtmlHeadBodyTags) {
			switch (tag) {
			case "html":
			case "head":
			case "body":
				return;
			}
		}
		Element element = null;
		if (rootResult == null && replaceContents != null) {
			element = replaceContents;
			// ignore outer element attributes here - but this will only be
			// called to recalculate innerHtml
		} else {
			element = Document.get().createElement(tag);
			element.local().attributes = attributes;
		}
		attributes = new StringMap();
		if (rootResult == null) {
			rootResult = element;
			setCursor(element, tag, 1);
		} else {
			cursor.appendChild(element);
			setCursor(element, tag, 1);
		}
	}

	boolean debugCursor = true;

	int debugCursorDepth = 0;

	private void setCursor(Element element, String tag, int delta) {
		if (debugCursor) {
			String fromTag = cursor == null ? "(null)" : cursor.getTagName();
			String toTag = element == null ? "(null)" : element.getTagName();
			Ax.out("%s -> %s: %s -> %s", debugCursorDepth,
					debugCursorDepth + delta, fromTag, toTag);
			if (delta == 1) {
				if (!Objects.equals(toTag, tag)) {
					Ax.err(">> %s, expected %s", toTag, tag);
				}
			} else {
				if (!Objects.equals(fromTag, tag)) {
					Ax.err("<< %s, expected %s", fromTag, tag);
				}
			}
			debugCursorDepth += delta;
		}
		cursor = element;
	}

	private void emitEndElement(String tag) {
		if (!emitHtmlHeadBodyTags) {
			switch (tag) {
			case "html":
			case "head":
			case "body":
				return;
			}
		}
		setCursor(cursor.getParentElement(), tag, -1);
	}

	private void emitComment(String string) {
		tag = null;
		if (string.matches("\\?.+\\?") || true) {
			// FIXME - make this a real PI
			// hmm...now chromium seems to want comments preserved. weird. but
			// wonderful
			emitText(string);
		}
		// FIXME - if ie<=9, hmm....panic?
	}

	private void emitEscapedText(String string) {
		emitText(decodeEntities(string));
	}

	private void emitText(String string) {
		if (string.isEmpty()) {
			return;
		}
		Text text = Document.get().createTextNode(string);
		cursor.appendChild(text);
		if (debugCursor) {
			Ax.out("  tx: %s", CommonUtils.trimToWsChars(string, 50, true));
		}
	}
}
