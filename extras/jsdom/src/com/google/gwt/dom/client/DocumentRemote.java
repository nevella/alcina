package com.google.gwt.dom.client;

import com.google.gwt.core.client.GWT;

@SuppressWarnings("deprecation")
public class DocumentRemote extends NodeRemote implements DomDocument {
	/**
	 * We cache Document.nativeGet() in DevMode, because crossing the JSNI
	 * boundary thousands of times just to read a constant value is slow.
	 */
	private static DocumentRemote doc;

	private static native DocumentRemote nativeGet() /*-{
    return $doc;
	}-*/;

	/**
	 * Gets the default document. This is the document in which the module is
	 * running.
	 * 
	 * @return the default document
	 */
	static DocumentRemote get() {
		if (GWT.isScript()) {
			return nativeGet();
		}
		// No need to be MT-safe. Single-threaded JS code.
		if (doc == null) {
			doc = nativeGet();
		}
		return doc;
	}

	protected DocumentRemote() {
	}

	@Override
	public final AnchorElement createAnchorElement() {
		return DomDocumentStatic.createAnchorElement(this);
	}

	@Override
	public final AreaElement createAreaElement() {
		return DomDocumentStatic.createAreaElement(this);
	}

	@Override
	public final AudioElement createAudioElement() {
		return DomDocumentStatic.createAudioElement(this);
	}

	@Override
	public final BaseElement createBaseElement() {
		return DomDocumentStatic.createBaseElement(this);
	}

	@Override
	public final QuoteElement createBlockQuoteElement() {
		return DomDocumentStatic.createBlockQuoteElement(this);
	}

	@Override
	public final NativeEvent createBlurEvent() {
		return DomDocumentStatic.createBlurEvent(this);
	}

	@Override
	public final BRElement createBRElement() {
		return DomDocumentStatic.createBRElement(this);
	}

	@Override
	public final ButtonElement createButtonElement() {
		return DomDocumentStatic.createButtonElement(this);
	}

	@Override
	public final InputElement createButtonInputElement() {
		return DomDocumentStatic.createButtonInputElement(this);
	}

	@Override
	public final CanvasElement createCanvasElement() {
		return DomDocumentStatic.createCanvasElement(this);
	}

	@Override
	public final TableCaptionElement createCaptionElement() {
		return DomDocumentStatic.createCaptionElement(this);
	}

	@Override
	public final NativeEvent createChangeEvent() {
		return DomDocumentStatic.createChangeEvent(this);
	}

	@Override
	public final InputElement createCheckInputElement() {
		return DomDocumentStatic.createCheckInputElement(this);
	}

	@Override
	public final NativeEvent createClickEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey) {
		return DomDocumentStatic.createClickEvent(this, detail, screenX,
				screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	@Override
	public final TableColElement createColElement() {
		return DomDocumentStatic.createColElement(this);
	}

	@Override
	public final TableColElement createColGroupElement() {
		return DomDocumentStatic.createColGroupElement(this);
	}

	@Override
	public final NativeEvent createContextMenuEvent() {
		return DomDocumentStatic.createContextMenuEvent(this);
	}

	@Override
	public final NativeEvent createDblClickEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey) {
		return DomDocumentStatic.createDblClickEvent(this, detail, screenX,
				screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	@Override
	public final ModElement createDelElement() {
		return DomDocumentStatic.createDelElement(this);
	}

	@Override
	public final DivElement createDivElement() {
		return DomDocumentStatic.createDivElement(this);
	}

	@Override
	public final DListElement createDLElement() {
		return DomDocumentStatic.createDLElement(this);
	}

	@Override
	public final Element createElement(String tagName) {
		return DomDocumentStatic.createElement(this, tagName);
	}

	@Override
	public final NativeEvent createErrorEvent() {
		return DomDocumentStatic.createErrorEvent(this);
	}

	@Override
	public final FieldSetElement createFieldSetElement() {
		return DomDocumentStatic.createFieldSetElement(this);
	}

	@Override
	public final InputElement createFileInputElement() {
		return DomDocumentStatic.createFileInputElement(this);
	}

	@Override
	public final NativeEvent createFocusEvent() {
		return DomDocumentStatic.createFocusEvent(this);
	}

	@Override
	public final FormElement createFormElement() {
		return DomDocumentStatic.createFormElement(this);
	}

	@Override
	public final FrameElement createFrameElement() {
		return DomDocumentStatic.createFrameElement(this);
	}

	@Override
	public final FrameSetElement createFrameSetElement() {
		return DomDocumentStatic.createFrameSetElement(this);
	}

	@Override
	public final HeadElement createHeadElement() {
		return DomDocumentStatic.createHeadElement(this);
	}

	@Override
	public final HeadingElement createHElement(int n) {
		return DomDocumentStatic.createHElement(this, n);
	}

	@Override
	public final InputElement createHiddenInputElement() {
		return DomDocumentStatic.createHiddenInputElement(this);
	}

	@Override
	public final HRElement createHRElement() {
		return DomDocumentStatic.createHRElement(this);
	}

	@Override
	public final NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		return DomDocumentStatic.createHtmlEvent(this, type, canBubble,
				cancelable);
	}

	@Override
	public final IFrameElement createIFrameElement() {
		return DomDocumentStatic.createIFrameElement(this);
	}

	@Override
	public final ImageElement createImageElement() {
		return DomDocumentStatic.createImageElement(this);
	}

	@Override
	public final InputElement createImageInputElement() {
		return DomDocumentStatic.createImageInputElement(this);
	}

	@Override
	public final NativeEvent createInputEvent() {
		return DomDocumentStatic.createInputEvent(this);
	}

	@Override
	public final ModElement createInsElement() {
		return DomDocumentStatic.createInsElement(this);
	}

	@Override
	public final NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return DomDocumentStatic.createKeyCodeEvent(this, type, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	@Override
	public final NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return DomDocumentStatic.createKeyDownEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	@Override
	public final NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return DomDocumentStatic.createKeyDownEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, keyCode, charCode);
	}

	@Override
	public final NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return DomDocumentStatic.createKeyEvent(this, type, canBubble,
				cancelable, ctrlKey, altKey, shiftKey, metaKey, keyCode,
				charCode);
	}

	@Override
	public final NativeEvent createKeyPressEvent(boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int charCode) {
		return DomDocumentStatic.createKeyPressEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, charCode);
	}

	@Override
	public final NativeEvent createKeyPressEvent(boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) {
		return DomDocumentStatic.createKeyPressEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, keyCode, charCode);
	}

	@Override
	public final NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return DomDocumentStatic.createKeyUpEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	@Override
	public final NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return DomDocumentStatic.createKeyUpEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, keyCode, charCode);
	}

	@Override
	public final LabelElement createLabelElement() {
		return DomDocumentStatic.createLabelElement(this);
	}

	@Override
	public final LegendElement createLegendElement() {
		return DomDocumentStatic.createLegendElement(this);
	}

	@Override
	public final LIElement createLIElement() {
		return DomDocumentStatic.createLIElement(this);
	}

	@Override
	public final LinkElement createLinkElement() {
		return DomDocumentStatic.createLinkElement(this);
	}

	@Override
	public final NativeEvent createLoadEvent() {
		return DomDocumentStatic.createLoadEvent(this);
	}

	@Override
	public final MapElement createMapElement() {
		return DomDocumentStatic.createMapElement(this);
	}

	@Override
	public final MetaElement createMetaElement() {
		return DomDocumentStatic.createMetaElement(this);
	}

	@Override
	public final NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return DomDocumentStatic.createMouseDownEvent(this, detail, screenX,
				screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey,
				button);
	}

	@Override
	public final NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return DomDocumentStatic.createMouseEvent(this, type, canBubble,
				cancelable, detail, screenX, screenY, clientX, clientY, ctrlKey,
				altKey, shiftKey, metaKey, button, relatedTarget);
	}

	@Override
	public final NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return DomDocumentStatic.createMouseMoveEvent(this, detail, screenX,
				screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey,
				button);
	}

	@Override
	public final NativeEvent createMouseOutEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return DomDocumentStatic.createMouseOutEvent(this, detail, screenX,
				screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey,
				button, relatedTarget);
	}

	@Override
	public final NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return DomDocumentStatic.createMouseOverEvent(this, detail, screenX,
				screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey,
				button, relatedTarget);
	}

	@Override
	public final NativeEvent createMouseUpEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return DomDocumentStatic.createMouseUpEvent(this, detail, screenX,
				screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey,
				button);
	}

	@Override
	public final ObjectElement createObjectElement() {
		return DomDocumentStatic.createObjectElement(this);
	}

	@Override
	public final OListElement createOLElement() {
		return DomDocumentStatic.createOLElement(this);
	}

	@Override
	public final OptGroupElement createOptGroupElement() {
		return DomDocumentStatic.createOptGroupElement(this);
	}

	@Override
	public final OptionElement createOptionElement() {
		return DomDocumentStatic.createOptionElement(this);
	}

	@Override
	public final ParamElement createParamElement() {
		return DomDocumentStatic.createParamElement(this);
	}

	@Override
	public final InputElement createPasswordInputElement() {
		return DomDocumentStatic.createPasswordInputElement(this);
	}

	@Override
	public final ParagraphElement createPElement() {
		return DomDocumentStatic.createPElement(this);
	}

	@Override
	public final PreElement createPreElement() {
		return DomDocumentStatic.createPreElement(this);
	}

	@Override
	public final ButtonElement createPushButtonElement() {
		return DomDocumentStatic.createPushButtonElement(this);
	}

	@Override
	public final QuoteElement createQElement() {
		return DomDocumentStatic.createQElement(this);
	}

	@Override
	public final InputElement createRadioInputElement(String name) {
		return DomDocumentStatic.createRadioInputElement(this, name);
	}

	@Override
	public final ButtonElement createResetButtonElement() {
		return DomDocumentStatic.createResetButtonElement(this);
	}

	@Override
	public final InputElement createResetInputElement() {
		return DomDocumentStatic.createResetInputElement(this);
	}

	@Override
	public final ScriptElement createScriptElement() {
		return DomDocumentStatic.createScriptElement(this);
	}

	@Override
	public final ScriptElement createScriptElement(String source) {
		return DomDocumentStatic.createScriptElement(this, source);
	}

	@Override
	public final NativeEvent createScrollEvent() {
		return DomDocumentStatic.createScrollEvent(this);
	}

	@Override
	public final SelectElement createSelectElement() {
		return DomDocumentStatic.createSelectElement(this);
	}

	@Override
	public final SelectElement createSelectElement(boolean multiple) {
		return DomDocumentStatic.createSelectElement(this, multiple);
	}

	@Override
	public final SourceElement createSourceElement() {
		return DomDocumentStatic.createSourceElement(this);
	}

	@Override
	public final SpanElement createSpanElement() {
		return DomDocumentStatic.createSpanElement(this);
	}

	@Override
	public final StyleElement createStyleElement() {
		return DomDocumentStatic.createStyleElement(this);
	}

	@Override
	public final ButtonElement createSubmitButtonElement() {
		return DomDocumentStatic.createSubmitButtonElement(this);
	}

	@Override
	public final InputElement createSubmitInputElement() {
		return DomDocumentStatic.createSubmitInputElement(this);
	}

	@Override
	public final TableElement createTableElement() {
		return DomDocumentStatic.createTableElement(this);
	}

	@Override
	public final TableSectionElement createTBodyElement() {
		return DomDocumentStatic.createTBodyElement(this);
	}

	@Override
	public final TableCellElement createTDElement() {
		return DomDocumentStatic.createTDElement(this);
	}

	@Override
	public final TextAreaElement createTextAreaElement() {
		return DomDocumentStatic.createTextAreaElement(this);
	}

	@Override
	public final InputElement createTextInputElement() {
		return DomDocumentStatic.createTextInputElement(this);
	}

	/**
	 * Creates a text node.
	 * 
	 * @param data
	 *            the text node's initial text
	 * @return the newly created element
	 */
	@Override
	public final Text createTextNode(String data) {
		TextRemote remote = createTextNode0(data);
		return LocalDom.nodeFor(remote);
	}

	@Override
	public final TableSectionElement createTFootElement() {
		return DomDocumentStatic.createTFootElement(this);
	}

	@Override
	public final TableSectionElement createTHeadElement() {
		return DomDocumentStatic.createTHeadElement(this);
	}

	@Override
	public final TableCellElement createTHElement() {
		return DomDocumentStatic.createTHElement(this);
	}

	@Override
	public final TitleElement createTitleElement() {
		return DomDocumentStatic.createTitleElement(this);
	}

	@Override
	public final TableRowElement createTRElement() {
		return DomDocumentStatic.createTRElement(this);
	}

	@Override
	public final UListElement createULElement() {
		return DomDocumentStatic.createULElement(this);
	}

	/**
	 * Creates an identifier guaranteed to be unique within this document.
	 * 
	 * This is useful for allocating element id's.
	 * 
	 * @return a unique identifier
	 */
	@Override
	public native final String createUniqueId() /*-{
    // In order to force uid's to be document-unique across multiple modules,
    // we hang a counter from the document.
    if (!this.gwt_uid) {
      this.gwt_uid = 1;
    }

    return "gwt-uid-" + this.gwt_uid++;
	}-*/;

	@Override
	public final VideoElement createVideoElement() {
		return DomDocumentStatic.createVideoElement(this);
	}

	@Override
	public final Document documentFor() {
		return (Document) node();
	}

	/**
	 * Enables or disables scrolling of the document.
	 * 
	 * @param enable
	 *            whether scrolling should be enabled or disabled
	 */
	@Override
	public final void enableScrolling(boolean enable) {
		getViewportElement().getStyle().setProperty("overflow",
				enable ? "auto" : "hidden");
	}

	@Override
	public final BodyElement getBody() {
		return nodeFor(getBody0());
	}

	/**
	 * Returns the left offset between the absolute coordinate system and the
	 * body's positioning context. This method is useful for positioning
	 * children of the body element in absolute coordinates.
	 * 
	 * <p>
	 * For example, to position an element directly under the mouse cursor
	 * (assuming you are handling a mouse event), do the following:
	 * </p>
	 * 
	 * <pre>
	 * Event event;
	 * Document doc;
	 * DivElement child; // assume absolutely-positioned child of the body
	 * // Get the event location in absolute coordinates.
	 * int absX = event.getClientX() + Window.getScrollLeft();
	 * int absY = event.getClientY() + Window.getScrollTop();
	 * // Position the child element, adjusting for the difference between the
	 * // absolute coordinate system and the body's positioning coordinates.
	 * child.getStyle().setPropertyPx("left", absX - doc.getBodyOffsetLeft());
	 * child.getStyle().setPropertyPx("top", absY - doc.getBodyOffsetTop());
	 * </pre>
	 * 
	 * @return the left offset of the body's positioning coordinate system
	 */
	@Override
	public final int getBodyOffsetLeft() {
		return DOMImpl.impl.getBodyOffsetLeft(documentFor());
	}

	/**
	 * Returns the top offset between the absolute coordinate system and the
	 * body's positioning context. This method is useful for positioning
	 * children of the body element in absolute coordinates.
	 * 
	 * @return the top offset of the body's positioning coordinate system
	 * @see #getBodyOffsetLeft()
	 */
	@Override
	public final int getBodyOffsetTop() {
		return DOMImpl.impl.getBodyOffsetTop(documentFor());
	}

	/**
	 * The height of the document's client area.
	 * 
	 * @return the document's client height
	 */
	@Override
	public final int getClientHeight() {
		return getViewportElement().getClientHeight();
	}

	/**
	 * The width of the document's client area.
	 * 
	 * @return the document's client width
	 */
	@Override
	public final int getClientWidth() {
		return getViewportElement().getClientWidth();
	}

	/**
	 * Gets the document's "compatibility mode", typically used for determining
	 * whether the document is in "quirks" or "strict" mode.
	 * 
	 * @return one of "BackCompat" or "CSS1Compat"
	 */
	@Override
	public final native String getCompatMode() /*-{
    return this.compatMode;
	}-*/;

	/**
	 * Gets the document's element. This is typically the &lt;html&gt; element.
	 * 
	 * @return the document element
	 */
	@Override
	public final Element getDocumentElement() {
		return LocalDom.nodeFor(getDocumentElement0());
	}

	/**
	 * The domain name of the server that served the document, or null if the
	 * server cannot be identified by a domain name.
	 * 
	 * @return the document's domain, or <code>null</code> if none exists
	 */
	@Override
	public final native String getDomain() /*-{
    return this.domain;
	}-*/;

	/**
	 * Returns the {@link Element} whose id is given by elementId. If no such
	 * element exists, returns null. Behavior is not defined if more than one
	 * element has this id.
	 * 
	 * @param elementId
	 *            the unique id value for an element
	 * @return the matching element
	 */
	@Override
	public final Element getElementById(String elementId) {
		return LocalDom.nodeFor(getElementById0(elementId));
	}

	@Override
	public final NodeList<Element> getElementsByTagName(String tagName) {
		return new NodeList(getElementsByTagName0(tagName));
	}

	/**
	 * The element that contains metadata about the document, including links to
	 * or definitions of scripts and style sheets.
	 *
	 * @return the document's head
	 */
	@Override
	public final native HeadElement getHead() /*-{
    // IE8 does not have document.head
    // when removing IE8 support we remove the second statement
    return this.head || this.getElementsByTagName('head')[0];
	}-*/;

	/**
	 * Returns the URI of the page that linked to this page. The value is an
	 * empty string if the user navigated to the page directly (not through a
	 * link, but, for example, via a bookmark).
	 * 
	 * @return the referrer URI
	 */
	@Override
	public final native String getReferrer() /*-{
    return this.referrer;
	}-*/;

	/**
	 * The height of the scrollable area of the document.
	 * 
	 * @return the height of the document's scrollable area
	 */
	@Override
	public final int getScrollHeight() {
		return getViewportElement().getScrollHeight();
	}

	/**
	 * The number of pixels that the document's content is scrolled from the
	 * left.
	 * 
	 * <p>
	 * If the document is in RTL mode, this method will return a negative value
	 * of the number of pixels scrolled from the right.
	 * </p>
	 * 
	 * @return the document's left scroll position
	 */
	@Override
	public final int getScrollLeft() {
		return DOMImpl.impl.getScrollLeft(documentFor());
	}

	/**
	 * The number of pixels that the document's content is scrolled from the
	 * top.
	 * 
	 * @return the document's top scroll position
	 */
	@Override
	public final int getScrollTop() {
		return DOMImpl.impl.getScrollTop(documentFor());
	}

	/**
	 * The width of the scrollable area of the document.
	 * 
	 * @return the width of the document's scrollable area
	 */
	@Override
	public final int getScrollWidth() {
		return getViewportElement().getScrollWidth();
	}

	/**
	 * Gets the title of a document as specified by the TITLE element in the
	 * head of the document.
	 * 
	 * @return the document's title
	 */
	@Override
	public final native String getTitle() /*-{
    return this.title;
	}-*/;

	/**
	 * Gets the absolute URI of this document.
	 * 
	 * @return the document URI
	 */
	@Override
	public final native String getURL() /*-{
    return this.URL;
	}-*/;

	/**
	 * Gets the document's viewport element. This is the element that should be
	 * used to for scrolling and client-area measurement. In quirks-mode it is
	 * the &lt;body&gt; element, while in standards-mode it is the &lt;html&gt;
	 * element.
	 * 
	 * This is package-protected because the viewport is
	 * 
	 * @return the document's viewport element
	 */
	@Override
	public final Element getViewportElement() {
		return isCSS1Compat() ? getDocumentElement() : getBody();
	}

	/**
	 * Imports a node from another document to this document.
	 * 
	 * The returned node has no parent; ({@link Node#getParentNode()} is null).
	 * The source node is not altered or removed from the original document;
	 * this method creates a new copy of the source node.
	 * 
	 * For all nodes, importing a node creates a node object owned by the
	 * importing document, with attribute values identical to the source node's
	 * nodeName and nodeType, plus the attributes related to namespaces (prefix,
	 * localName, and namespaceURI). As in the cloneNode operation on a Node,
	 * the source node is not altered. Additional information is copied as
	 * appropriate to the nodeType, attempting to mirror the behavior expected
	 * if a fragment of XML or HTML source was copied from one document to
	 * another, recognizing that the two documents may have different DTDs in
	 * the XML case.
	 * 
	 * @param node
	 *            the node to import
	 * @param deep
	 *            If <code>true</code>, recursively import the subtree under the
	 *            specified node; if <code>false</code>, import only the node
	 *            itself, as explained above
	 */
	@Override
	public final native void importNode(Node node, boolean deep) /*-{
    this.importNode(node, deep);
	}-*/;

	/**
	 * Determines whether the document's "compatMode" is "CSS1Compat". This is
	 * normally described as "strict" mode.
	 * 
	 * @return <code>true</code> if the document is in CSS1Compat mode
	 */
	@Override
	public final boolean isCSS1Compat() {
		return getCompatMode().equals("CSS1Compat");
	}

	/**
	 * Sets the number of pixels that the document's content is scrolled from
	 * the left.
	 * 
	 * @param left
	 *            the document's left scroll position
	 */
	@Override
	public final void setScrollLeft(int left) {
		DOMImpl.impl.setScrollLeft(documentFor(), left);
	}

	/**
	 * Sets the number of pixels that the document's content is scrolled from
	 * the top.
	 * 
	 * @param top
	 *            the document's top scroll position
	 */
	@Override
	public final void setScrollTop(int top) {
		DOMImpl.impl.setScrollTop(documentFor(), top);
	}

	/**
	 * Sets the title of a document as specified by the TITLE element in the
	 * head of the document.
	 * 
	 * @param title
	 *            the document's new title
	 */
	@Override
	public final native void setTitle(String title) /*-{
    this.title = title;
	}-*/;

	/**
	 * The element that contains the content for the document. In documents with
	 * BODY contents, returns the BODY element.
	 * 
	 * @return the document's body
	 */
	private final native NodeRemote getBody0() /*-{
    return this.body;
	}-*/;

	native final TextRemote createTextNode0(String data) /*-{
    return this.createTextNode(data);
	}-*/;

	final native ElementRemote generateFromOuterHtml(String outer) /*-{
    var div = this.createElement("div");
    div.innerHTML = outer;
    return div.childNodes[0];
	}-*/;

	final native ElementRemote getDocumentElement0() /*-{
    return this.documentElement;
	}-*/;

	final native ElementRemote getElementById0(String elementId) /*-{
    return this.getElementById(elementId);
	}-*/;

	/**
	 * Returns a {@link NodeList} of all the {@link Element Elements} with a
	 * given tag name in the order in which they are encountered in a preorder
	 * traversal of the document tree.
	 * 
	 * @param tagName
	 *            the name of the tag to match on (the special value
	 *            <code>"*"</code> matches all tags)
	 * @return a list containing all the matched elements
	 */
	final native NodeListRemote<Element>
			getElementsByTagName0(String tagName) /*-{
    return this.getElementsByTagName(tagName);
	}-*/;
}
