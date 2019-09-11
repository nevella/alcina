package com.google.gwt.dom.client;

public class DocumentLocal extends NodeLocal implements DomDocument {
	public Document document;

	private Element bodyElement;

	private Element headElement;

	private int gwtLuid = 1;

	public DocumentLocal() {
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
	public final BRElement createBRElement() {
		return DomDocumentStatic.createBRElement(this);
	}

	@SuppressWarnings("deprecation")
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
	public final InputElement createCheckInputElement() {
		return DomDocumentStatic.createCheckInputElement(this);
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
	public Element createElement(String tagName) {
		ElementLocal local = new ElementLocal(this, tagName);
		Element element = LocalDom.createElement(tagName).putLocal(local);
		switch (element.getTagName()) {
		case "head":
			headElement = element;
			break;
		case "body":
			bodyElement = element;
			break;
		}
		return element;
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
	public final ModElement createInsElement() {
		return DomDocumentStatic.createInsElement(this);
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
	public final SelectElement createSelectElement() {
		return DomDocumentStatic.createSelectElement(this);
	}

	@SuppressWarnings("deprecation")
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

	@Override
	public Text createTextNode(String data) {
		TextLocal local = new TextLocal(this, data);
		Text text = new Text(local);
		local.registerNode(text);
		return text;
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

	@Override
	public String createUniqueId() {
		return "gwt-luid-" + this.gwtLuid++;
	}

	@Override
	public final VideoElement createVideoElement() {
		return DomDocumentStatic.createVideoElement(this);
	}

	@Override
	public Document documentFor() {
		return document;
	}

	@Override
	public BodyElement getBody() {
		return (BodyElement) this.bodyElement;
	}

	@Override
	public String getCompatMode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getDocumentElement() {
		return null;
	}

	@Override
	public HeadElement getHead() {
		return (HeadElement) this.headElement;
	}

	@Override
	public String getNodeName() {
		return "#document";
	}

	@Override
	public short getNodeType() {
		return Node.DOCUMENT_NODE;
	}

	@Override
	public String getNodeValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Document nodeFor() {
		return document;
	}

	@Override
	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		throw new UnsupportedOperationException();
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createBlurEvent() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createChangeEvent() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createContextMenuEvent() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createDblClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createErrorEvent() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createFocusEvent() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createInputEvent() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createMouseOutEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NativeEvent createScrollEvent() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void enableScrolling(boolean enable) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getBodyOffsetLeft() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getBodyOffsetTop() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getClientHeight() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getClientWidth() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getDomain() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Element getElementById(String elementId) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NodeList<Element> getElementsByTagName(String tagName) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getReferrer() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getScrollHeight() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getScrollLeft() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getScrollTop() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getScrollWidth() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getTitle() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getURL() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Element getViewportElement() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void importNode(Node node, boolean deep) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isCSS1Compat() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setScrollLeft(int left) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void setScrollTop(int top) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void setTitle(String title) {
		throw new UnsupportedOperationException();
		
	}
}
