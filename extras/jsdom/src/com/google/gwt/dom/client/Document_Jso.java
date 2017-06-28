package com.google.gwt.dom.client;

import com.google.gwt.core.client.GWT;

public class Document_Jso extends Node_Jso implements DomDocument {
	/**
	 * We cache Document.nativeGet() in DevMode, because crossing the JSNI
	 * boundary thousands of times just to read a constant value is slow.
	 */
	private static Document_Jso doc;

	/**
	 * Gets the default document. This is the document in which the module is
	 * running.
	 * 
	 * @return the default document
	 */
	static Document_Jso get() {
		if (GWT.isScript()) {
			return nativeGet();
		}
		// No need to be MT-safe. Single-threaded JS code.
		if (doc == null) {
			doc = nativeGet();
		}
		return doc;
	}

	private static native Document_Jso nativeGet() /*-{
        return $doc;
	}-*/;

	protected Document_Jso() {
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

	/**
	 * Creates a text node.
	 * 
	 * @param data
	 *            the text node's initial text
	 * @return the newly created element
	 */
	@Override
	public native final Text createTextNode(String data) /*-{
		var text_jso = this.createTextNode(data);
		var textOut=@com.google.gwt.dom.client.LocalDomBridge::nodeFor(Lcom/google/gwt/core/client/JavaScriptObject;)(text_jso);
		return textOut;
	}-*/;

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

	/**
	 * The element that contains the content for the document. In documents with
	 * BODY contents, returns the BODY element.
	 * 
	 * @return the document's body
	 */
	private final native Node_Jso getBody0() /*-{
        return this.body;
	}-*/;

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

	@Override
	public final Document documentFor() {
		return (Document) nodeFor();
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
	public  final  Element getDocumentElement() {
		return LocalDomBridge.nodeFor(getDocumentElement0());
	}
	
	 final native Element_Jso getDocumentElement0() /*-{
        return this.documentElement;
	}-*/;

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
	public final  Element getElementById(String elementId) {
        return LocalDomBridge.nodeFor(getElementById0(elementId));
	}
	 final native Element_Jso getElementById0(String elementId) /*-{
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
	 final native NodeList_Jso<Element>
			getElementsByTagName0(String tagName) /*-{
        return this.getElementsByTagName(tagName);
	}-*/;
	@Override
	public final  NodeList<Element>
			getElementsByTagName(String tagName) {
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

	@Override
	public final AnchorElement createAnchorElement() {
		return DomDocument_Static.createAnchorElement(this);
	}

	@Override
	public final AreaElement createAreaElement() {
		return DomDocument_Static.createAreaElement(this);
	}

	@Override
	public final AudioElement createAudioElement() {
		return DomDocument_Static.createAudioElement(this);
	}

	@Override
	public final BaseElement createBaseElement() {
		return DomDocument_Static.createBaseElement(this);
	}

	@Override
	public final QuoteElement createBlockQuoteElement() {
		return DomDocument_Static.createBlockQuoteElement(this);
	}

	@Override
	public final NativeEvent createBlurEvent() {
		return DomDocument_Static.createBlurEvent(this);
	}

	@Override
	public final BRElement createBRElement() {
		return DomDocument_Static.createBRElement(this);
	}

	@Override
	public final ButtonElement createButtonElement() {
		return DomDocument_Static.createButtonElement(this);
	}

	@Override
	public final InputElement createButtonInputElement() {
		return DomDocument_Static.createButtonInputElement(this);
	}

	@Override
	public final CanvasElement createCanvasElement() {
		return DomDocument_Static.createCanvasElement(this);
	}

	@Override
	public final TableCaptionElement createCaptionElement() {
		return DomDocument_Static.createCaptionElement(this);
	}

	@Override
	public final NativeEvent createChangeEvent() {
		return DomDocument_Static.createChangeEvent(this);
	}

	@Override
	public final InputElement createCheckInputElement() {
		return DomDocument_Static.createCheckInputElement(this);
	}

	@Override
	public final NativeEvent createClickEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey) {
		return DomDocument_Static.createClickEvent(this, detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	@Override
	public final TableColElement createColElement() {
		return DomDocument_Static.createColElement(this);
	}

	@Override
	public final TableColElement createColGroupElement() {
		return DomDocument_Static.createColGroupElement(this);
	}

	@Override
	public final NativeEvent createContextMenuEvent() {
		return DomDocument_Static.createContextMenuEvent(this);
	}

	@Override
	public final NativeEvent createDblClickEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey) {
		return DomDocument_Static.createDblClickEvent(this, detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	@Override
	public final ModElement createDelElement() {
		return DomDocument_Static.createDelElement(this);
	}

	@Override
	public final DivElement createDivElement() {
		return DomDocument_Static.createDivElement(this);
	}

	@Override
	public final DListElement createDLElement() {
		return DomDocument_Static.createDLElement(this);
	}

	@Override
	public final Element createElement(String tagName) {
		return DomDocument_Static.createElement(this, tagName);
	}

	@Override
	public final NativeEvent createErrorEvent() {
		return DomDocument_Static.createErrorEvent(this);
	}

	@Override
	public final FieldSetElement createFieldSetElement() {
		return DomDocument_Static.createFieldSetElement(this);
	}

	@Override
	public final InputElement createFileInputElement() {
		return DomDocument_Static.createFileInputElement(this);
	}

	@Override
	public final NativeEvent createFocusEvent() {
		return DomDocument_Static.createFocusEvent(this);
	}

	@Override
	public final FormElement createFormElement() {
		return DomDocument_Static.createFormElement(this);
	}

	@Override
	public final FrameElement createFrameElement() {
		return DomDocument_Static.createFrameElement(this);
	}

	@Override
	public final FrameSetElement createFrameSetElement() {
		return DomDocument_Static.createFrameSetElement(this);
	}

	@Override
	public final HeadElement createHeadElement() {
		return DomDocument_Static.createHeadElement(this);
	}

	@Override
	public final HeadingElement createHElement(int n) {
		return DomDocument_Static.createHElement(this, n);
	}

	@Override
	public final InputElement createHiddenInputElement() {
		return DomDocument_Static.createHiddenInputElement(this);
	}

	@Override
	public final HRElement createHRElement() {
		return DomDocument_Static.createHRElement(this);
	}

	@Override
	public final NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		return DomDocument_Static.createHtmlEvent(this, type, canBubble, cancelable);
	}

	@Override
	public final IFrameElement createIFrameElement() {
		return DomDocument_Static.createIFrameElement(this);
	}

	@Override
	public final ImageElement createImageElement() {
		return DomDocument_Static.createImageElement(this);
	}

	@Override
	public final InputElement createImageInputElement() {
		return DomDocument_Static.createImageInputElement(this);
	}

	@Override
	public final NativeEvent createInputEvent() {
		return DomDocument_Static.createInputEvent(this);
	}

	@Override
	public final ModElement createInsElement() {
		return DomDocument_Static.createInsElement(this);
	}

	@Override
	public final NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return DomDocument_Static.createKeyCodeEvent(this, type, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	@Override
	public final NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return DomDocument_Static.createKeyDownEvent(this, ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	@Override
	public final NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return DomDocument_Static.createKeyDownEvent(this, ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	@Override
	public final NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return DomDocument_Static.createKeyEvent(this, type, canBubble, cancelable,
				ctrlKey, altKey, shiftKey, metaKey, keyCode, charCode);
	}

	@Override
	public final NativeEvent createKeyPressEvent(boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int charCode) {
		return DomDocument_Static.createKeyPressEvent(this, ctrlKey, altKey, shiftKey,
				metaKey, charCode);
	}

	@Override
	public final NativeEvent createKeyPressEvent(boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) {
		return DomDocument_Static.createKeyPressEvent(this, ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	@Override
	public final NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return DomDocument_Static.createKeyUpEvent(this, ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	@Override
	public final NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return DomDocument_Static.createKeyUpEvent(this, ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	@Override
	public final LabelElement createLabelElement() {
		return DomDocument_Static.createLabelElement(this);
	}

	@Override
	public final LegendElement createLegendElement() {
		return DomDocument_Static.createLegendElement(this);
	}

	@Override
	public final LIElement createLIElement() {
		return DomDocument_Static.createLIElement(this);
	}

	@Override
	public final LinkElement createLinkElement() {
		return DomDocument_Static.createLinkElement(this);
	}

	@Override
	public final NativeEvent createLoadEvent() {
		return DomDocument_Static.createLoadEvent(this);
	}

	@Override
	public final MapElement createMapElement() {
		return DomDocument_Static.createMapElement(this);
	}

	@Override
	public final MetaElement createMetaElement() {
		return DomDocument_Static.createMetaElement(this);
	}

	@Override
	public final NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return DomDocument_Static.createMouseDownEvent(this, detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	@Override
	public final NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return DomDocument_Static.createMouseEvent(this, type, canBubble, cancelable,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, button, relatedTarget);
	}

	@Override
	public final NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return DomDocument_Static.createMouseMoveEvent(this, detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	@Override
	public final NativeEvent createMouseOutEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return DomDocument_Static.createMouseOutEvent(this, detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	@Override
	public final NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return DomDocument_Static.createMouseOverEvent(this, detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	@Override
	public final NativeEvent createMouseUpEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return DomDocument_Static.createMouseUpEvent(this, detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	@Override
	public final ObjectElement createObjectElement() {
		return DomDocument_Static.createObjectElement(this);
	}

	@Override
	public final OListElement createOLElement() {
		return DomDocument_Static.createOLElement(this);
	}

	@Override
	public final OptGroupElement createOptGroupElement() {
		return DomDocument_Static.createOptGroupElement(this);
	}

	@Override
	public final OptionElement createOptionElement() {
		return DomDocument_Static.createOptionElement(this);
	}

	@Override
	public final ParamElement createParamElement() {
		return DomDocument_Static.createParamElement(this);
	}

	@Override
	public final InputElement createPasswordInputElement() {
		return DomDocument_Static.createPasswordInputElement(this);
	}

	@Override
	public final ParagraphElement createPElement() {
		return DomDocument_Static.createPElement(this);
	}

	@Override
	public final PreElement createPreElement() {
		return DomDocument_Static.createPreElement(this);
	}

	@Override
	public final ButtonElement createPushButtonElement() {
		return DomDocument_Static.createPushButtonElement(this);
	}

	@Override
	public final QuoteElement createQElement() {
		return DomDocument_Static.createQElement(this);
	}

	@Override
	public final InputElement createRadioInputElement(String name) {
		return DomDocument_Static.createRadioInputElement(this, name);
	}

	@Override
	public final ButtonElement createResetButtonElement() {
		return DomDocument_Static.createResetButtonElement(this);
	}

	@Override
	public final InputElement createResetInputElement() {
		return DomDocument_Static.createResetInputElement(this);
	}

	@Override
	public final ScriptElement createScriptElement() {
		return DomDocument_Static.createScriptElement(this);
	}

	@Override
	public final ScriptElement createScriptElement(String source) {
		return DomDocument_Static.createScriptElement(this, source);
	}

	@Override
	public final NativeEvent createScrollEvent() {
		return DomDocument_Static.createScrollEvent(this);
	}

	@Override
	public final SelectElement createSelectElement() {
		return DomDocument_Static.createSelectElement(this);
	}

	@Override
	public final SelectElement createSelectElement(boolean multiple) {
		return DomDocument_Static.createSelectElement(this, multiple);
	}

	@Override
	public final SourceElement createSourceElement() {
		return DomDocument_Static.createSourceElement(this);
	}

	@Override
	public final SpanElement createSpanElement() {
		return DomDocument_Static.createSpanElement(this);
	}

	@Override
	public final StyleElement createStyleElement() {
		return DomDocument_Static.createStyleElement(this);
	}

	@Override
	public final ButtonElement createSubmitButtonElement() {
		return DomDocument_Static.createSubmitButtonElement(this);
	}

	@Override
	public final InputElement createSubmitInputElement() {
		return DomDocument_Static.createSubmitInputElement(this);
	}

	@Override
	public final TableElement createTableElement() {
		return DomDocument_Static.createTableElement(this);
	}

	@Override
	public final TableSectionElement createTBodyElement() {
		return DomDocument_Static.createTBodyElement(this);
	}

	@Override
	public final TableCellElement createTDElement() {
		return DomDocument_Static.createTDElement(this);
	}

	@Override
	public final TextAreaElement createTextAreaElement() {
		return DomDocument_Static.createTextAreaElement(this);
	}

	@Override
	public final InputElement createTextInputElement() {
		return DomDocument_Static.createTextInputElement(this);
	}

	@Override
	public final TableSectionElement createTFootElement() {
		return DomDocument_Static.createTFootElement(this);
	}

	@Override
	public final TableSectionElement createTHeadElement() {
		return DomDocument_Static.createTHeadElement(this);
	}

	@Override
	public final TableCellElement createTHElement() {
		return DomDocument_Static.createTHElement(this);
	}

	@Override
	public final TitleElement createTitleElement() {
		return DomDocument_Static.createTitleElement(this);
	}

	@Override
	public final TableRowElement createTRElement() {
		return DomDocument_Static.createTRElement(this);
	}

	@Override
	public final UListElement createULElement() {
		return DomDocument_Static.createULElement(this);
	}

	@Override
	public final VideoElement createVideoElement() {
		return DomDocument_Static.createVideoElement(this);
	}

	
}
