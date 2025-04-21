package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.ProcessingInstruction;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.regexp.shared.RegExp;
import com.sun.jna.platform.win32.DsGetDC;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FastLcProvider;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.gwt.client.util.DomUtils;

/**
 * <p>
 * This began more or less as a toy, but now is a decent dropin for well-formed
 * DOM docs compared to say Xerces
 */
public class HtmlParser {
	static final boolean debugCursor = false;

	public static Element parseMarkup(String markup) {
		try {
			LooseContext.push();
			Document.contextProvider.createFrame(RemoteType.NONE);
			Element element = Document.get().createDocumentElement(markup,
					true);
			return element;
		} finally {
			LooseContext.pop();
		}
	}

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
		int idx = 0;
		int length = string.length();
		int maxCharsPerTextNode = LocalDom.getMaxCharsPerTextNode();
		Text text = document.createTextNode(string);
		element.appendChild(text);
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
		return isSelfClosingTagLc(tag.toLowerCase());
	}

	private static boolean isSelfClosingTagLc(String lcTag) {
		switch (lcTag) {
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
	 * Also splits #text nodes based on observed text node (from setHtml) size
	 * (?)
	 *
	 * FIXME - LDM2 - live test in the browser
	 */
	private boolean emitBrowserCompatibleDom = true;

	StringBuilder markingBuilder2 = new StringBuilder();

	/*
	 * Similar API to stringbuilder
	 */
	class MarkingBuilder {
		String empty = "";

		int start = -1;

		int end;

		String toString;

		String toStringLowerCase;

		int length() {
			return start == -1 ? 0 : end - start;
		}

		void append(char c) {
			toString = null;
			int charIndex = idx - 1;
			if (start == -1) {
				start = charIndex;
				end = charIndex;
			} else {
				Preconditions.checkState(charIndex == end);
			}
			end++;
		}

		void setLength(int length) {
			toString = null;
			if (length == 0) {
				start = -1;
			} else {
				end = start + length;
			}
		}

		@Override
		public String toString() {
			if (toString == null) {
				toString = start == -1 ? empty : html.substring(start, end);
			}
			return toString;
		}

		public boolean textEquals(String string) {
			int length = length();
			if (length != string.length()) {
				return false;
			}
			for (int idx = 0; idx < length; idx++) {
				if (string.charAt(idx) != charAt(idx)) {
					return false;
				}
			}
			return true;
		}

		public String toStringLowerCase() {
			if (toString == null || toStringLowerCase == null) {
				toStringLowerCase = lc.lc(toString());
			}
			return toStringLowerCase;
		}

		public char charAt(int idx) {
			return html.charAt(start + idx);
		}
	}

	MarkingBuilder markingBuilder = new MarkingBuilder();

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

	private FastLcProvider lc = new FastLcProvider();

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
			tag = markingBuilder.toString().toLowerCase();
			if (tag.startsWith("/")) {
				tag = tag.substring(1);
				closeTag = true;
			}
		}
		if (!closeTag) {
			emitStartElement(tag);
		}
		selfCloseTag |= isSelfClosingTagLc(tag);
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
		case "noscript":
			Preconditions.checkState(!closeTag);
			int closeIdx0 = idx;
			while (closeIdx0 != -1) {
				closeIdx0 = html.indexOf("</", closeIdx0);
				int closeIdx1 = html.indexOf(">", closeIdx0);
				String endTag = html.substring(closeIdx0 + 2, closeIdx1);
				if (endTag.equalsIgnoreCase(tag)) {
					break;
				}
			}
			String textContent = html.substring(idx, closeIdx0);
			emitText(textContent);
			emitEndElement(tag);
			idx = closeIdx0 + 2 + tag.length() + 1;
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
			tag = string.toLowerCase();
			body = "";
		} else {
			tag = string.substring(0, idx).toLowerCase();
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

	/*
	 * Performance - markingbuilder should have a seenbuffer (char[]) which
	 * handles startsWtih etc (implementing CharSequence?)
	 */
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
			boolean emptyBuffer = markingBuilder.length() == 0;
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
					markingBuilder.append(c);
				}
				break;
			case TEXT:
				if (c == '<') {
					emitEscapedText(markingBuilder.toString());
					resetBuilder();
					tokenState = TokenState.EXPECTING_TAG;
				} else {
					markingBuilder.append(c);
				}
				break;
			case EXPECTING_TAG:
				selfCloseTag = false;
				if (markingBuilder.textEquals("!--")) {
					tag = markingBuilder.toString();
					resetBuilder();
					markingBuilder.append(c);
					tokenState = TokenState.EXPECTING_COMMENT;
				} else if (markingBuilder.textEquals("![CDATA[")) {
					tag = markingBuilder.toString();
					resetBuilder();
					markingBuilder.append(c);
					tokenState = TokenState.EXPECTING_CDATA;
				} else if (markingBuilder.textEquals("?")) {
					tag = markingBuilder.toString();
					resetBuilder();
					markingBuilder.append(c);
					tokenState = TokenState.EXPECTING_PROCESSING_INSTRUCTION;
				} else {
					boolean handled = false;
					if (isWhiteSpace) {
						if (isLookaheadValidTag()) {
							tag = markingBuilder.toStringLowerCase();
							handled = true;
							resetBuilder();
							tokenState = TokenState.EXPECTING_ATTRIBUTES;
						}
					}
					if (!handled) {
						switch (c) {
						case '/':
							if (markingBuilder.length() > 0) {
								selfCloseTag = true;
							} else {
								markingBuilder.append(c);
							}
							break;
						case '>':
							if (isLookaheadValidTag()) {
								emitElement();
							} else {
								logInvalidMarkup(markingBuilder.toString());
								tokenState = TokenState.EXPECTING_NODE;
							}
							resetBuilder();
							break;
						default:
							markingBuilder.append(c);
							break;
						}
					}
				}
				break;
			case EXPECTING_COMMENT:
				// FIXME - dirndl 1x1g - optimise end-of-builder checks (with
				// some sort of buffering builder) - lowish priority since the
				// node types that use this check are rare
				if (c == '>' && markingBuilder.toString().endsWith("--")) {
					markingBuilder.setLength(markingBuilder.length() - 2);
					emitComment(markingBuilder.toString());
					resetBuilder();
					tokenState = TokenState.EXPECTING_NODE;
				} else {
					markingBuilder.append(c);
				}
				break;
			case EXPECTING_CDATA:
				if (c == '>' && markingBuilder.toString().endsWith("]]")) {
					markingBuilder.setLength(markingBuilder.length() - 2);
					emitCData(markingBuilder.toString());
					resetBuilder();
					tokenState = TokenState.EXPECTING_NODE;
				} else {
					markingBuilder.append(c);
				}
				break;
			case EXPECTING_PROCESSING_INSTRUCTION:
				if (c == '>' && markingBuilder.toString().endsWith("?")) {
					markingBuilder.setLength(markingBuilder.length() - 1);
					emitProcessingInstruction(markingBuilder.toString());
					resetBuilder();
					tokenState = TokenState.EXPECTING_NODE;
				} else {
					markingBuilder.append(c);
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
					markingBuilder.append(c);
					tokenState = TokenState.EXPECTING_ATTR_SEP;
					break;
				}
				break;
			case EXPECTING_ATTR_SEP:
				switch (c) {
				case '=':
					attrName = markingBuilder.toString();
					attrValue = "";
					resetBuilder();
					tokenState = TokenState.EXPECTING_ATTR_VALUE_DELIM;
					break;
				default:
					markingBuilder.append(c);
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
					markingBuilder.append(c);
					tokenState = TokenState.EXPECTING_ATTR_VALUE;
					break;
				}
				break;
			case EXPECTING_ATTR_VALUE:
				boolean handled = false;
				if (attrDelim == ' ' && c == '>') {
					attrValue = markingBuilder.toString();
					emitAttribute();
					emitElement();
					resetBuilder();
					break;
				}
				if (c == attrDelim) {
					// fix for quotes in attributes
					attrValue = markingBuilder.toString();
					emitAttribute();
					resetBuilder();
					tokenState = TokenState.EXPECTING_ATTRIBUTES;
					break;
				}
				markingBuilder.append(c);
				break;
			}
		}
		if (hasSyntheticContainer) {
		}
		return rootResult;
	}

	void logInvalidMarkup(String tagLookahead) {
		if (debugCursor) {
			Ax.out("!! omit invalid markup: '%s' [%s]", tagLookahead,
					lineNumber);
		}
	}

	static RegExp validTag = RegExp.compile("^[a-z_][0-9a-z:_\\-.]*$");

	boolean isLookaheadValidTag() {
		int length = markingBuilder.length();
		boolean closing = markingBuilder.charAt(0) == '/';
		int start = closing ? 1 : 0;
		for (int idx = start; idx < length; idx++) {
			char c = markingBuilder.charAt(idx);
			if (idx == start) {
				if (c >= 'a' && c <= 'z') {
				} else if (c == '_') {
				} else if (c == '/') {
				} else {
					return false;
				}
			} else {
				if (c >= 'a' && c <= 'z') {
				} else if (c >= '0' && c <= '9') {
				} else {
					switch (c) {
					case '_':
					case '-':
					case ':':
					case '.':
						break;
					default:
						return false;
					}
				}
			}
		}
		String tag = markingBuilder.toStringLowerCase();
		if (tag.startsWith("xml")) {
			return false;
		}
		return true;
	}

	void resetBuilder() {
		markingBuilder.setLength(0);
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
