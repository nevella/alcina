package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.ProcessingInstruction;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document.RemoteType;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.gwt.client.util.DomUtils;

public class HtmlParser {
	public static boolean debugCursor = false;

	public static void appendTextNodes(ClientDomDocument document,
			ClientDomElement element, String string) {
		// will not emit a zero-length text node (but that won't be parseable
		// anyway, although can be programatically created)
		if (string.isEmpty()) {
			return;
		}
		if (string.contains("\r\n")) {
			string = string.replace("\r\n", "\n");
		}
		boolean singleNode = string.length() == 76700;
		if (string.length() > 70000 & string.length() < 80000) {
			Ax.err("strlen:%s - %s", string.length(), element.getTagName());
		}
		int idx = 0;
		int length = string.length();
		if (length <= LocalDom.maxCharsPerTextNode || singleNode) {
			Text text = document.createTextNode(string);
			element.appendChild(text);
		} else {
			while (idx < length) {
				int segmentLength = length - idx;
				segmentLength = Math.min(segmentLength,
						LocalDom.maxCharsPerTextNode);
				String segment = string.substring(idx, idx + segmentLength);
				Text text = document.createTextNode(segment);
				element.appendChild(text);
				idx += segmentLength;
			}
		}
		if (debugCursor) {
			Ax.out("  tx: %s", CommonUtils.trimToWsChars(
					TextUtils.normalizeWhitespaceAndTrim(string), 50, true));
		}
	}

	static String decodeEntities(String text) {
		return EntityDecoder.decode(text);
	}

	// https://www.thoughtco.com/html-singleton-tags-3468620
	static boolean isSelfClosingTag(String tag) {
		switch (tag.toLowerCase()) {
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

	/**
	 * See https://developer.mozilla.org/en-US/docs/Web/API/CDATASection
	 *
	 * Emit processing instruction, cdata nodes as comments
	 *
	 * Also splits #text nodes based on observed
	 *
	 * FIXME - LDM2 - live test in the browser
	 */
	private boolean emitBrowserCompatibleDom = true;

	StringBuilder builder = new StringBuilder();

	boolean builderIsWhiteSpace = true;

	boolean selfCloseTag = false;

	TokenState tokenState;

	int idx = 0;

	private String html;

	private String tag;

	private String attrName;

	private char attrDelim;

	private String attrValue;

	// FIXME - dirndl 1x1g - optimise (a jsmap would probably be faster in
	// script land)
	private LightMap<String, String> attributes = new LightMap();

	private Element rootResult;

	private Element cursor;

	private Element replaceContents;

	private boolean emitHtmlHeadBodyTags;

	private List<Element> syntheticElements = new ArrayList<>();

	int debugCursorDepth = 0;

	private int lineNumber;

	private void emitAttribute() {
		attributes.put(attrName, decodeEntities(attrValue));
	}

	private void emitCData(String string) {
		tag = null;
		if (emitBrowserCompatibleDom) {
			emitComment(string);
			return;
		}
		CDATASection cdataSection = Document.get().createCDATASection(string);
		cursor.appendChild(cdataSection);
	}

	private void emitComment(String string) {
		tag = null;
		Comment comment = Document.get().createComment(string);
		cursor.appendChild(comment);
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

	private void emitEscapedText(String string) {
		emitText(decodeEntities(string));
	}

	private void emitProcessingInstruction(String string) {
		tag = null;
		if (emitBrowserCompatibleDom) {
			emitComment(string);
			return;
		}
		int idx = string.indexOf(" ");
		String tag, body;
		if (idx == -1) {
			tag = string;
			body = "";
		} else {
			tag = string.substring(0, idx);
			body = string.substring(idx + 1);
		}
		ProcessingInstruction processingInstruction = Document.get()
				.createProcessingInstruction(tag, body);
		cursor.appendChild(processingInstruction);
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
			if (Ax.notBlank(attributes.get("style"))) {
				element.local().hasUnparsedStyle = true;
			}
		}
		attributes = new LightMap<>();
		if (rootResult == null) {
			rootResult = element;
			setCursor(element, tag, 1);
		} else {
			if (tag.equals("tr") && cursor.getTagName().equals("table")) {
				Element tbody = Document.get().createElement("tbody");
				cursor.appendChild(tbody);
				syntheticElements.add(tbody);
				setCursor(tbody, "tbody", 1);
			}
			cursor.appendChild(element);
			setCursor(element, tag, 1);
		}
	}

	private void emitText(String string) {
		appendTextNodes(Document.get(), cursor, string);
	}

	public boolean isEmitBrowserCompatibleDom() {
		return this.emitBrowserCompatibleDom;
	}

	public Element parse(ClientDomElement root, Element replaceContents,
			boolean emitHtmlHeadBodyTags) {
		return parse(root.getOuterHtml(), replaceContents,
				emitHtmlHeadBodyTags);
	}

	public Element parse(String html, Element replaceContents,
			boolean emitHtmlHeadBodyTags) {
		RemoteType preParse = Document.get().remoteType;
		try {
			Document.get().remoteType = RemoteType.NONE;
			return parse0(html, replaceContents, emitHtmlHeadBodyTags);
		} finally {
			Document.get().remoteType = preParse;
		}
	}

	private Element parse0(String html, Element replaceContents,
			boolean emitHtmlHeadBodyTags) {
		if (html.contains("\uFEFF")) {
			html = html.replace("\uFEFF", "");
		}
		if (html.contains("/>")) {
			html = DomUtils.expandEmptyElements(html);
		}
		this.html = html;
		this.replaceContents = replaceContents;
		this.lineNumber = 1;
		this.emitHtmlHeadBodyTags = emitHtmlHeadBodyTags;
		resetBuilder();
		tokenState = TokenState.EXPECTING_NODE;
		if (replaceContents != null) {
			replaceContents.clearSynced();
		}
		int length = html.length();
		// gwt compiler hack - force string class init outside loop
		boolean hasSyntheticContainer = !emitHtmlHeadBodyTags
				&& (html.startsWith("<html>") || html.startsWith("<HTML>"));
		if (hasSyntheticContainer) {
			html = Ax.format("<div>%s</div>", html);
		}
		char c = html.charAt(idx);
		while (idx < length) {
			c = html.charAt(idx++);
			boolean isWhiteSpace = false;
			boolean emptyBuffer = builder.length() == 0;
			switch (c) {
			case ' ':
			case '\t':
			case '\n':
			case '\r':
				isWhiteSpace = true;
			}
			switch (c) {
			case '\n':
			case '\r':
				lineNumber++;
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
				} else if (tagLookahead.equals("![CDATA[")) {
					tag = tagLookahead;
					resetBuilder();
					builder.append(c);
					tokenState = TokenState.EXPECTING_CDATA;
				} else if (tagLookahead.startsWith("?")) {
					tag = tagLookahead;
					resetBuilder();
					builder.append(c);
					tokenState = TokenState.EXPECTING_PROCESSING_INSTRUCTION;
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
				// FIXME - dirndl 1x1g - optimise end-of-builder checks (with
				// some sort of buffering builder) - lowish priority since the
				// node types that use this check are rare
				if (c == '>' && builder.toString().endsWith("--")) {
					builder.setLength(builder.length() - 2);
					emitComment(builder.toString());
					resetBuilder();
					tokenState = TokenState.EXPECTING_NODE;
				} else {
					builder.append(c);
				}
				break;
			case EXPECTING_CDATA:
				if (c == '>' && builder.toString().endsWith("]]")) {
					builder.setLength(builder.length() - 2);
					emitCData(builder.toString());
					resetBuilder();
					tokenState = TokenState.EXPECTING_NODE;
				} else {
					builder.append(c);
				}
				break;
			case EXPECTING_PROCESSING_INSTRUCTION:
				if (c == '>' && builder.toString().endsWith("?")) {
					builder.setLength(builder.length() - 1);
					emitProcessingInstruction(builder.toString());
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
					// fix for quotes in attributes
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
		if (hasSyntheticContainer) {
		}
		return rootResult;
	}

	void resetBuilder() {
		builder.setLength(0);
	}

	private void setCursor(Element element, String tag, int delta) {
		if (syntheticElements.contains(cursor) && delta == -1) {
			syntheticElements.remove(cursor);
			cursor = cursor.getParentElement();
			element = element.getParentElement();
			debugCursorDepth += delta;
		}
		if (debugCursor) {
			String fromTag = cursor == null ? "(null)" : cursor.getTagName();
			String toTag = element == null ? "(null)" : element.getTagName();
			Ax.out("%s%s -> %s: %s -> %s [%s]",
					CommonUtils.padStringLeft("", debugCursorDepth, ' '),
					debugCursorDepth, debugCursorDepth + delta, fromTag, toTag,
					lineNumber);
			if (delta == 1) {
				if (!CommonUtils.equalsIgnoreCase(toTag, tag)) {
					Ax.err(">> %s, expected %s [%s]", toTag, tag, lineNumber);
				}
			} else {
				if (!CommonUtils.equalsIgnoreCase(fromTag, tag)) {
					Ax.err("<< %s, expected %s [%s]", fromTag, tag, lineNumber);
				}
			}
			debugCursorDepth += delta;
		}
		cursor = element;
	}

	public void setEmitBrowserCompatibleDom(boolean emitBrowserCompatibleDom) {
		this.emitBrowserCompatibleDom = emitBrowserCompatibleDom;
	}

	enum TokenState {
		EXPECTING_NODE, EXPECTING_TAG, TEXT, EXPECTING_COMMENT,
		EXPECTING_PROCESSING_INSTRUCTION, EXPECTING_ATTRIBUTES,
		EXPECTING_ATTR_SEP, EXPECTING_ATTR_VALUE_DELIM, EXPECTING_ATTR_VALUE,
		EXPECTING_CDATA
	}
}
