package com.google.gwt.dom.client;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.ProcessingInstruction;

import com.google.gwt.core.client.GWT;

@SuppressWarnings("deprecation")
public final class DocumentJso extends NodeJso implements ClientDomDocument {
	/**
	 * We cache Document.nativeGet() in DevMode, because crossing the JSNI
	 * boundary thousands of times just to read a constant value is slow.
	 */
	private static DocumentJso doc;

	/**
	 * Gets the default document. This is the document in which the module is
	 * running.
	 *
	 * @return the default document
	 */
	static DocumentJso get() {
		if (GWT.isScript()) {
			return nativeGet();
		}
		// No need to be MT-safe. Single-threaded JS code.
		if (doc == null) {
			doc = nativeGet();
		}
		return doc;
	}

	private static native DocumentJso nativeGet() /*-{
    return $doc;
	}-*/;

	protected DocumentJso() {
	}

	@Override
	public AnchorElement createAnchorElement() {
		return ClientDomDocumentStatic.createAnchorElement(this);
	}

	@Override
	public AreaElement createAreaElement() {
		return ClientDomDocumentStatic.createAreaElement(this);
	}

	@Override
	public AudioElement createAudioElement() {
		return ClientDomDocumentStatic.createAudioElement(this);
	}

	@Override
	public BaseElement createBaseElement() {
		return ClientDomDocumentStatic.createBaseElement(this);
	}

	@Override
	public QuoteElement createBlockQuoteElement() {
		return ClientDomDocumentStatic.createBlockQuoteElement(this);
	}

	@Override
	public NativeEvent createBlurEvent() {
		return ClientDomDocumentStatic.createBlurEvent(this);
	}

	@Override
	public BRElement createBRElement() {
		return ClientDomDocumentStatic.createBRElement(this);
	}

	@Override
	public ButtonElement createButtonElement() {
		return ClientDomDocumentStatic.createButtonElement(this);
	}

	@Override
	public InputElement createButtonInputElement() {
		return ClientDomDocumentStatic.createButtonInputElement(this);
	}

	@Override
	public CanvasElement createCanvasElement() {
		return ClientDomDocumentStatic.createCanvasElement(this);
	}

	@Override
	public TableCaptionElement createCaptionElement() {
		return ClientDomDocumentStatic.createCaptionElement(this);
	}

	@Override
	public CDATASection createCDATASection(String data) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createChangeEvent() {
		return ClientDomDocumentStatic.createChangeEvent(this);
	}

	@Override
	public InputElement createCheckInputElement() {
		return ClientDomDocumentStatic.createCheckInputElement(this);
	}

	@Override
	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return ClientDomDocumentStatic.createClickEvent(this, detail, screenX,
				screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	@Override
	public TableColElement createColElement() {
		return ClientDomDocumentStatic.createColElement(this);
	}

	@Override
	public TableColElement createColGroupElement() {
		return ClientDomDocumentStatic.createColGroupElement(this);
	}

	@Override
	public Comment createComment(String data) {
		CommentJso remote = createCommentNode0(data);
		return LocalDom.nodeFor(remote);
	}

	native CommentJso createCommentNode0(String data) /*-{
    return this.createComment(data);
	}-*/;

	@Override
	public NativeEvent createContextMenuEvent() {
		return ClientDomDocumentStatic.createContextMenuEvent(this);
	}

	@Override
	public NativeEvent createDblClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return ClientDomDocumentStatic.createDblClickEvent(this, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey);
	}

	@Override
	public ModElement createDelElement() {
		return ClientDomDocumentStatic.createDelElement(this);
	}

	@Override
	public DivElement createDivElement() {
		return ClientDomDocumentStatic.createDivElement(this);
	}

	@Override
	public DListElement createDLElement() {
		return ClientDomDocumentStatic.createDLElement(this);
	}

	@Override
	public Element createElement(String tagName) {
		return ClientDomDocumentStatic.createElement(this, tagName);
	}

	native ElementJso createElementNode0(String tagName) /*-{
    return this.createElement(tagName);
	}-*/;

	@Override
	public NativeEvent createErrorEvent() {
		return ClientDomDocumentStatic.createErrorEvent(this);
	}

	@Override
	public FieldSetElement createFieldSetElement() {
		return ClientDomDocumentStatic.createFieldSetElement(this);
	}

	@Override
	public InputElement createFileInputElement() {
		return ClientDomDocumentStatic.createFileInputElement(this);
	}

	@Override
	public NativeEvent createFocusEvent() {
		return ClientDomDocumentStatic.createFocusEvent(this);
	}

	@Override
	public FormElement createFormElement() {
		return ClientDomDocumentStatic.createFormElement(this);
	}

	@Override
	public FrameElement createFrameElement() {
		return ClientDomDocumentStatic.createFrameElement(this);
	}

	@Override
	public FrameSetElement createFrameSetElement() {
		return ClientDomDocumentStatic.createFrameSetElement(this);
	}

	@Override
	public HeadElement createHeadElement() {
		return ClientDomDocumentStatic.createHeadElement(this);
	}

	@Override
	public HeadingElement createHElement(int n) {
		return ClientDomDocumentStatic.createHElement(this, n);
	}

	@Override
	public InputElement createHiddenInputElement() {
		return ClientDomDocumentStatic.createHiddenInputElement(this);
	}

	@Override
	public HRElement createHRElement() {
		return ClientDomDocumentStatic.createHRElement(this);
	}

	@Override
	public NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		return ClientDomDocumentStatic.createHtmlEvent(this, type, canBubble,
				cancelable);
	}

	@Override
	public IFrameElement createIFrameElement() {
		return ClientDomDocumentStatic.createIFrameElement(this);
	}

	@Override
	public ImageElement createImageElement() {
		return ClientDomDocumentStatic.createImageElement(this);
	}

	@Override
	public InputElement createImageInputElement() {
		return ClientDomDocumentStatic.createImageInputElement(this);
	}

	@Override
	public NativeEvent createInputEvent() {
		return ClientDomDocumentStatic.createInputEvent(this);
	}

	@Override
	public ModElement createInsElement() {
		return ClientDomDocumentStatic.createInsElement(this);
	}

	@Override
	public NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return ClientDomDocumentStatic.createKeyCodeEvent(this, type, ctrlKey,
				altKey, shiftKey, metaKey, keyCode);
	}

	@Override
	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return ClientDomDocumentStatic.createKeyDownEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	@Override
	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return ClientDomDocumentStatic.createKeyDownEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, keyCode, charCode);
	}

	@Override
	public NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return ClientDomDocumentStatic.createKeyEvent(this, type, canBubble,
				cancelable, ctrlKey, altKey, shiftKey, metaKey, keyCode,
				charCode);
	}

	@Override
	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode) {
		return ClientDomDocumentStatic.createKeyPressEvent(this, ctrlKey,
				altKey, shiftKey, metaKey, charCode);
	}

	@Override
	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return ClientDomDocumentStatic.createKeyPressEvent(this, ctrlKey,
				altKey, shiftKey, metaKey, keyCode, charCode);
	}

	@Override
	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return ClientDomDocumentStatic.createKeyUpEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	@Override
	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return ClientDomDocumentStatic.createKeyUpEvent(this, ctrlKey, altKey,
				shiftKey, metaKey, keyCode, charCode);
	}

	@Override
	public LabelElement createLabelElement() {
		return ClientDomDocumentStatic.createLabelElement(this);
	}

	@Override
	public LegendElement createLegendElement() {
		return ClientDomDocumentStatic.createLegendElement(this);
	}

	@Override
	public LIElement createLIElement() {
		return ClientDomDocumentStatic.createLIElement(this);
	}

	@Override
	public LinkElement createLinkElement() {
		return ClientDomDocumentStatic.createLinkElement(this);
	}

	@Override
	public NativeEvent createLoadEvent() {
		return ClientDomDocumentStatic.createLoadEvent(this);
	}

	@Override
	public MapElement createMapElement() {
		return ClientDomDocumentStatic.createMapElement(this);
	}

	@Override
	public MetaElement createMetaElement() {
		return ClientDomDocumentStatic.createMetaElement(this);
	}

	@Override
	public NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return ClientDomDocumentStatic.createMouseDownEvent(this, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button);
	}

	@Override
	public NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return ClientDomDocumentStatic.createMouseEvent(this, type, canBubble,
				cancelable, detail, screenX, screenY, clientX, clientY, ctrlKey,
				altKey, shiftKey, metaKey, button, relatedTarget);
	}

	@Override
	public NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return ClientDomDocumentStatic.createMouseMoveEvent(this, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button);
	}

	@Override
	public NativeEvent createMouseOutEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return ClientDomDocumentStatic.createMouseOutEvent(this, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, relatedTarget);
	}

	@Override
	public NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return ClientDomDocumentStatic.createMouseOverEvent(this, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, relatedTarget);
	}

	@Override
	public NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button) {
		return ClientDomDocumentStatic.createMouseUpEvent(this, detail, screenX,
				screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey,
				button);
	}

	@Override
	public ObjectElement createObjectElement() {
		return ClientDomDocumentStatic.createObjectElement(this);
	}

	@Override
	public OListElement createOLElement() {
		return ClientDomDocumentStatic.createOLElement(this);
	}

	@Override
	public OptGroupElement createOptGroupElement() {
		return ClientDomDocumentStatic.createOptGroupElement(this);
	}

	@Override
	public OptionElement createOptionElement() {
		return ClientDomDocumentStatic.createOptionElement(this);
	}

	@Override
	public ParamElement createParamElement() {
		return ClientDomDocumentStatic.createParamElement(this);
	}

	@Override
	public InputElement createPasswordInputElement() {
		return ClientDomDocumentStatic.createPasswordInputElement(this);
	}

	@Override
	public ParagraphElement createPElement() {
		return ClientDomDocumentStatic.createPElement(this);
	}

	@Override
	public PreElement createPreElement() {
		return ClientDomDocumentStatic.createPreElement(this);
	}

	@Override
	public ProcessingInstruction createProcessingInstruction(String target,
			String data) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ButtonElement createPushButtonElement() {
		return ClientDomDocumentStatic.createPushButtonElement(this);
	}

	@Override
	public QuoteElement createQElement() {
		return ClientDomDocumentStatic.createQElement(this);
	}

	@Override
	public InputElement createRadioInputElement(String name) {
		return ClientDomDocumentStatic.createRadioInputElement(this, name);
	}

	@Override
	public ButtonElement createResetButtonElement() {
		return ClientDomDocumentStatic.createResetButtonElement(this);
	}

	@Override
	public InputElement createResetInputElement() {
		return ClientDomDocumentStatic.createResetInputElement(this);
	}

	@Override
	public ScriptElement createScriptElement() {
		return ClientDomDocumentStatic.createScriptElement(this);
	}

	@Override
	public ScriptElement createScriptElement(String source) {
		return ClientDomDocumentStatic.createScriptElement(this, source);
	}

	@Override
	public NativeEvent createScrollEvent() {
		return ClientDomDocumentStatic.createScrollEvent(this);
	}

	@Override
	public SelectElement createSelectElement() {
		return ClientDomDocumentStatic.createSelectElement(this);
	}

	@Override
	public SelectElement createSelectElement(boolean multiple) {
		return ClientDomDocumentStatic.createSelectElement(this, multiple);
	}

	@Override
	public SourceElement createSourceElement() {
		return ClientDomDocumentStatic.createSourceElement(this);
	}

	@Override
	public SpanElement createSpanElement() {
		return ClientDomDocumentStatic.createSpanElement(this);
	}

	@Override
	public StyleElement createStyleElement() {
		return ClientDomDocumentStatic.createStyleElement(this);
	}

	@Override
	public ButtonElement createSubmitButtonElement() {
		return ClientDomDocumentStatic.createSubmitButtonElement(this);
	}

	@Override
	public InputElement createSubmitInputElement() {
		return ClientDomDocumentStatic.createSubmitInputElement(this);
	}

	@Override
	public TableElement createTableElement() {
		return ClientDomDocumentStatic.createTableElement(this);
	}

	@Override
	public TableSectionElement createTBodyElement() {
		return ClientDomDocumentStatic.createTBodyElement(this);
	}

	@Override
	public TableCellElement createTDElement() {
		return ClientDomDocumentStatic.createTDElement(this);
	}

	@Override
	public TextAreaElement createTextAreaElement() {
		return ClientDomDocumentStatic.createTextAreaElement(this);
	}

	@Override
	public InputElement createTextInputElement() {
		return ClientDomDocumentStatic.createTextInputElement(this);
	}

	/**
	 * Creates a text node.
	 *
	 * @param data
	 *            the text node's initial text
	 * @return the newly created element
	 */
	@Override
	public Text createTextNode(String data) {
		TextJso remote = createTextNode0(data);
		return LocalDom.nodeFor(remote);
	}

	native TextJso createTextNode0(String data) /*-{
    return this.createTextNode(data);
	}-*/;

	@Override
	public TableSectionElement createTFootElement() {
		return ClientDomDocumentStatic.createTFootElement(this);
	}

	@Override
	public TableSectionElement createTHeadElement() {
		return ClientDomDocumentStatic.createTHeadElement(this);
	}

	@Override
	public TableCellElement createTHElement() {
		return ClientDomDocumentStatic.createTHElement(this);
	}

	@Override
	public TitleElement createTitleElement() {
		return ClientDomDocumentStatic.createTitleElement(this);
	}

	@Override
	public TableRowElement createTRElement() {
		return ClientDomDocumentStatic.createTRElement(this);
	}

	@Override
	public UListElement createULElement() {
		return ClientDomDocumentStatic.createULElement(this);
	}

	/**
	 * Creates an identifier guaranteed to be unique within this document.
	 *
	 * This is useful for allocating element id's.
	 *
	 * @return a unique identifier
	 */
	@Override
	public native String createUniqueId() /*-{
    // In order to force uid's to be document-unique across multiple modules,
    // we hang a counter from the document.
    if (!this.gwt_uid) {
      this.gwt_uid = 1;
    }

    return "gwt-uid-" + this.gwt_uid++;
	}-*/;

	@Override
	public VideoElement createVideoElement() {
		return ClientDomDocumentStatic.createVideoElement(this);
	}

	@Override
	public Document documentFor() {
		return (Document) node();
	}

	/**
	 * Enables or disables scrolling of the document.
	 *
	 * @param enable
	 *            whether scrolling should be enabled or disabled
	 */
	@Override
	public void enableScrolling(boolean enable) {
		getViewportElement().getStyle().setProperty("overflow",
				enable ? "auto" : "hidden");
	}

	native ElementJso generateFromOuterHtml(String outer) /*-{
    var div = this.createElement("div");
    div.innerHTML = outer;
    return div.childNodes[0];
	}-*/;

	@Override
	public BodyElement getBody() {
		return nodeFor(getBody0());
	}

	/**
	 * The element that contains the content for the document. In documents with
	 * BODY contents, returns the BODY element.
	 *
	 * @return the document's body
	 */
	private native NodeJso getBody0() /*-{
    return this.body;
	}-*/;

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
	public int getBodyOffsetLeft() {
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
	public int getBodyOffsetTop() {
		return DOMImpl.impl.getBodyOffsetTop(documentFor());
	}

	/**
	 * The height of the document's client area.
	 *
	 * @return the document's client height
	 */
	@Override
	public int getClientHeight() {
		return getViewportElement().getClientHeight();
	}

	/**
	 * The width of the document's client area.
	 *
	 * @return the document's client width
	 */
	@Override
	public int getClientWidth() {
		return getViewportElement().getClientWidth();
	}

	/**
	 * Gets the document's "compatibility mode", typically used for determining
	 * whether the document is in "quirks" or "strict" mode.
	 *
	 * @return one of "BackCompat" or "CSS1Compat"
	 */
	@Override
	public native String getCompatMode() /*-{
    return this.compatMode;
	}-*/;

	/**
	 * Gets the document's element. This is typically the &lt;html&gt; element.
	 *
	 * @return the document element
	 */
	@Override
	public Element getDocumentElement() {
		return LocalDom.nodeFor(getDocumentElement0());
	}

	native ElementJso getDocumentElement0() /*-{
    return this.documentElement;
	}-*/;

	/**
	 * The domain name of the server that served the document, or null if the
	 * server cannot be identified by a domain name.
	 *
	 * @return the document's domain, or <code>null</code> if none exists
	 */
	@Override
	public native String getDomain() /*-{
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
	public Element getElementById(String elementId) {
		return LocalDom.nodeFor(getElementById0(elementId));
	}

	native ElementJso getElementById0(String elementId) /*-{
    return this.getElementById(elementId);
	}-*/;

	@Override
	public NodeList<Element> getElementsByTagName(String tagName) {
		return new NodeList(getElementsByTagName0(tagName));
	}

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
	native NodeListJso<Element> getElementsByTagName0(String tagName) /*-{
    return this.getElementsByTagName(tagName);
	}-*/;

	/**
	 * The element that contains metadata about the document, including links to
	 * or definitions of scripts and style sheets.
	 *
	 * @return the document's head
	 */
	@Override
	public native HeadElement getHead() /*-{
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
	public native String getReferrer() /*-{
    return this.referrer;
	}-*/;

	/**
	 * The height of the scrollable area of the document.
	 *
	 * @return the height of the document's scrollable area
	 */
	@Override
	public int getScrollHeight() {
		// TODO(dramaix): Use document.scrollingElement when its available. See
		// getScrollLeft().
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
	public int getScrollLeft() {
		return DOMImpl.impl.getScrollLeft(documentFor());
	}

	/**
	 * The number of pixels that the document's content is scrolled from the
	 * top.
	 *
	 * @return the document's top scroll position
	 */
	@Override
	public int getScrollTop() {
		return DOMImpl.impl.getScrollTop(documentFor());
	}

	/**
	 * The width of the scrollable area of the document.
	 *
	 * @return the width of the document's scrollable area
	 */
	@Override
	public int getScrollWidth() {
		// TODO(dramaix): Use document.scrollingElement when its available. See
		// getScrollLeft().
		return getViewportElement().getScrollWidth();
	}

	public native SelectionJso getSelection()/*-{
    return this.getSelection();
	}-*/;

	/**
	 * Gets the title of a document as specified by the TITLE element in the
	 * head of the document.
	 *
	 * @return the document's title
	 */
	@Override
	public native String getTitle() /*-{
    return this.title;
	}-*/;

	/**
	 * Gets the absolute URI of this document.
	 *
	 * @return the document URI
	 */
	@Override
	public native String getURL() /*-{
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
	public Element getViewportElement() {
		return isCSS1Compat() ? getDocumentElement() : getBody();
	}

	@Override
	public native String getVisibilityState() /*-{
    return this.visibilityState;
	}-*/;

	@Override
	public native boolean hasFocus() /*-{
    return this.hasFocus();
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
	public native void importNode(Node node, boolean deep) /*-{
    this.importNode(node, deep);
	}-*/;

	/**
	 * Determines whether the document's "compatMode" is "CSS1Compat". This is
	 * normally described as "strict" mode.
	 *
	 * @return <code>true</code> if the document is in CSS1Compat mode
	 */
	@Override
	public boolean isCSS1Compat() {
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
	public void setScrollLeft(int left) {
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
	public void setScrollTop(int top) {
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
	public native void setTitle(String title) /*-{
    this.title = title;
	}-*/;

	String validateHtml(String html) {
		ElementJso elementJso = createElementNode0("div");
		return elementJso.sanitizeHTML(html);
	}

	@Override
	public <T> T invoke(Supplier<T> supplier, Class clazz, String methodName,
			List<Class> argumentTypes, List<?> arguments, boolean sync) {
		// this supplier will always just be a lambda(-esque) wrapping a JSNI
		// call
		return supplier.get();
	}

	@Override
	public void invoke(Runnable runnable, Class clazz, String methodName,
			List<Class> argumentTypes, List<?> arguments, boolean sync) {
		ClientDomDocumentStatic.invoke(this, runnable, clazz, methodName,
				argumentTypes, arguments, sync);
	}

	@Override
	public Element getActiveElement() {
		ElementJso activeElementJso = getActiveElement0();
		return activeElementJso == null ? null : activeElementJso.elementFor();
	}

	native ElementJso getActiveElement0()/*-{
		return this.activeElement;
		}-*/;

	@Override
	public List<Element> querySelectorAll(String selector) {
		return querySelectorAll0(selector).stream()
				.collect(Collectors.toList());
	}

	native NodeList<Element> querySelectorAll0(String selector) /*-{
    var nodeList = this.querySelectorAll(selector);
    return @com.google.gwt.dom.client.NodeList::new(Lcom/google/gwt/dom/client/ClientDomNodeList;)(nodeList);
	}-*/;

	@Override
	public ClientDomSelection ensureRemoteSelection(Selection selection) {
		return getSelection();
	}

	public native Element getFocussedDocumentElement()/*-{
		if (this.activeElement) {
		  var tagName = this.activeElement.tagName.toLowerCase();
		  return tagName != "body" && tagName != "html" ? @com.google.gwt.dom.client.LocalDom::nodeFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.activeElement)
			  : null;
		}
		return null;
		}-*/;
}
