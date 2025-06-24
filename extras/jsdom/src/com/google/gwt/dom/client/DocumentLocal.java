package com.google.gwt.dom.client;

import java.util.List;
import java.util.function.Supplier;

import org.w3c.dom.DOMException;

public class DocumentLocal extends NodeLocal implements ClientDomDocument {
	Document document;

	private Element bodyElement;

	private Element headElement;

	private int gwtLuid = 1;

	public SelectionLocal getSelection() {
		return document.getSelection().local();
	}

	public DocumentLocal(Document document) {
		this.document = document;
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
	public final AnchorElement createAnchorElement() {
		return ClientDomDocumentStatic.createAnchorElement(this);
	}

	@Override
	public final AreaElement createAreaElement() {
		return ClientDomDocumentStatic.createAreaElement(this);
	}

	@Override
	public final AudioElement createAudioElement() {
		return ClientDomDocumentStatic.createAudioElement(this);
	}

	@Override
	public final BaseElement createBaseElement() {
		return ClientDomDocumentStatic.createBaseElement(this);
	}

	@Override
	public final QuoteElement createBlockQuoteElement() {
		return ClientDomDocumentStatic.createBlockQuoteElement(this);
	}

	@Override
	public NativeEvent createBlurEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final BRElement createBRElement() {
		return ClientDomDocumentStatic.createBRElement(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final ButtonElement createButtonElement() {
		return ClientDomDocumentStatic.createButtonElement(this);
	}

	@Override
	public final InputElement createButtonInputElement() {
		return ClientDomDocumentStatic.createButtonInputElement(this);
	}

	@Override
	public final CanvasElement createCanvasElement() {
		return ClientDomDocumentStatic.createCanvasElement(this);
	}

	@Override
	public final TableCaptionElement createCaptionElement() {
		return ClientDomDocumentStatic.createCaptionElement(this);
	}

	@Override
	public CDATASection createCDATASection(String data) throws DOMException {
		CDATASectionLocal local = new CDATASectionLocal(this, data);
		CDATASection cdataSection = new CDATASection(local);
		local.putCDATASection(cdataSection);
		return cdataSection;
	}

	@Override
	public NativeEvent createChangeEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final InputElement createCheckInputElement() {
		return ClientDomDocumentStatic.createCheckInputElement(this);
	}

	@Override
	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final TableColElement createColElement() {
		return ClientDomDocumentStatic.createColElement(this);
	}

	@Override
	public final TableColElement createColGroupElement() {
		return ClientDomDocumentStatic.createColGroupElement(this);
	}

	@Override
	public Comment createComment(String data) {
		CommentLocal local = new CommentLocal(this, data);
		Comment comment = new Comment(local);
		local.putComment(comment);
		return comment;
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
	public final ModElement createDelElement() {
		return ClientDomDocumentStatic.createDelElement(this);
	}

	@Override
	public final DivElement createDivElement() {
		return ClientDomDocumentStatic.createDivElement(this);
	}

	@Override
	public final DListElement createDLElement() {
		return ClientDomDocumentStatic.createDLElement(this);
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
	public NativeEvent createErrorEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final FieldSetElement createFieldSetElement() {
		return ClientDomDocumentStatic.createFieldSetElement(this);
	}

	@Override
	public final InputElement createFileInputElement() {
		return ClientDomDocumentStatic.createFileInputElement(this);
	}

	@Override
	public NativeEvent createFocusEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final FormElement createFormElement() {
		return ClientDomDocumentStatic.createFormElement(this);
	}

	@Override
	public final FrameElement createFrameElement() {
		return ClientDomDocumentStatic.createFrameElement(this);
	}

	@Override
	public final FrameSetElement createFrameSetElement() {
		return ClientDomDocumentStatic.createFrameSetElement(this);
	}

	@Override
	public final HeadElement createHeadElement() {
		return ClientDomDocumentStatic.createHeadElement(this);
	}

	@Override
	public final HeadingElement createHElement(int n) {
		return ClientDomDocumentStatic.createHElement(this, n);
	}

	@Override
	public final InputElement createHiddenInputElement() {
		return ClientDomDocumentStatic.createHiddenInputElement(this);
	}

	@Override
	public final HRElement createHRElement() {
		return ClientDomDocumentStatic.createHRElement(this);
	}

	@Override
	public NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final IFrameElement createIFrameElement() {
		return ClientDomDocumentStatic.createIFrameElement(this);
	}

	@Override
	public final ImageElement createImageElement() {
		return ClientDomDocumentStatic.createImageElement(this);
	}

	@Override
	public final InputElement createImageInputElement() {
		return ClientDomDocumentStatic.createImageInputElement(this);
	}

	@Override
	public NativeEvent createInputEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final ModElement createInsElement() {
		return ClientDomDocumentStatic.createInsElement(this);
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
	public final LabelElement createLabelElement() {
		return ClientDomDocumentStatic.createLabelElement(this);
	}

	@Override
	public final LegendElement createLegendElement() {
		return ClientDomDocumentStatic.createLegendElement(this);
	}

	@Override
	public final LIElement createLIElement() {
		return ClientDomDocumentStatic.createLIElement(this);
	}

	@Override
	public final LinkElement createLinkElement() {
		return ClientDomDocumentStatic.createLinkElement(this);
	}

	@Override
	public final NativeEvent createLoadEvent() {
		return ClientDomDocumentStatic.createLoadEvent(this);
	}

	@Override
	public final MapElement createMapElement() {
		return ClientDomDocumentStatic.createMapElement(this);
	}

	@Override
	public final MetaElement createMetaElement() {
		return ClientDomDocumentStatic.createMetaElement(this);
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
	public final ObjectElement createObjectElement() {
		return ClientDomDocumentStatic.createObjectElement(this);
	}

	@Override
	public final OListElement createOLElement() {
		return ClientDomDocumentStatic.createOLElement(this);
	}

	@Override
	public final OptGroupElement createOptGroupElement() {
		return ClientDomDocumentStatic.createOptGroupElement(this);
	}

	@Override
	public final OptionElement createOptionElement() {
		return ClientDomDocumentStatic.createOptionElement(this);
	}

	@Override
	public final ParamElement createParamElement() {
		return ClientDomDocumentStatic.createParamElement(this);
	}

	@Override
	public final InputElement createPasswordInputElement() {
		return ClientDomDocumentStatic.createPasswordInputElement(this);
	}

	@Override
	public final ParagraphElement createPElement() {
		return ClientDomDocumentStatic.createPElement(this);
	}

	@Override
	public final PreElement createPreElement() {
		return ClientDomDocumentStatic.createPreElement(this);
	}

	@Override
	public ProcessingInstruction createProcessingInstruction(String target,
			String data) throws DOMException {
		ProcessingInstructionLocal local = new ProcessingInstructionLocal(this,
				target, data);
		ProcessingInstruction processingInstruction = new ProcessingInstruction(
				local);
		local.putProcessingInstruction(processingInstruction);
		return processingInstruction;
	}

	@Override
	public final ButtonElement createPushButtonElement() {
		return ClientDomDocumentStatic.createPushButtonElement(this);
	}

	@Override
	public final QuoteElement createQElement() {
		return ClientDomDocumentStatic.createQElement(this);
	}

	@Override
	public final InputElement createRadioInputElement(String name) {
		return ClientDomDocumentStatic.createRadioInputElement(this, name);
	}

	@Override
	public final ButtonElement createResetButtonElement() {
		return ClientDomDocumentStatic.createResetButtonElement(this);
	}

	@Override
	public final InputElement createResetInputElement() {
		return ClientDomDocumentStatic.createResetInputElement(this);
	}

	@Override
	public final ScriptElement createScriptElement() {
		return ClientDomDocumentStatic.createScriptElement(this);
	}

	@Override
	public final ScriptElement createScriptElement(String source) {
		return ClientDomDocumentStatic.createScriptElement(this, source);
	}

	@Override
	public NativeEvent createScrollEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final SelectElement createSelectElement() {
		return ClientDomDocumentStatic.createSelectElement(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final SelectElement createSelectElement(boolean multiple) {
		return ClientDomDocumentStatic.createSelectElement(this, multiple);
	}

	@Override
	public final SourceElement createSourceElement() {
		return ClientDomDocumentStatic.createSourceElement(this);
	}

	@Override
	public final SpanElement createSpanElement() {
		return ClientDomDocumentStatic.createSpanElement(this);
	}

	@Override
	public final StyleElement createStyleElement() {
		return ClientDomDocumentStatic.createStyleElement(this);
	}

	@Override
	public final ButtonElement createSubmitButtonElement() {
		return ClientDomDocumentStatic.createSubmitButtonElement(this);
	}

	@Override
	public final InputElement createSubmitInputElement() {
		return ClientDomDocumentStatic.createSubmitInputElement(this);
	}

	@Override
	public final TableElement createTableElement() {
		return ClientDomDocumentStatic.createTableElement(this);
	}

	@Override
	public final TableSectionElement createTBodyElement() {
		return ClientDomDocumentStatic.createTBodyElement(this);
	}

	@Override
	public final TableCellElement createTDElement() {
		return ClientDomDocumentStatic.createTDElement(this);
	}

	@Override
	public final TextAreaElement createTextAreaElement() {
		return ClientDomDocumentStatic.createTextAreaElement(this);
	}

	@Override
	public final InputElement createTextInputElement() {
		return ClientDomDocumentStatic.createTextInputElement(this);
	}

	@Override
	public Text createTextNode(String data) {
		TextLocal local = new TextLocal(this, data);
		Text text = new Text(local);
		local.putText(text);
		return text;
	}

	@Override
	public final TableSectionElement createTFootElement() {
		return ClientDomDocumentStatic.createTFootElement(this);
	}

	@Override
	public final TableSectionElement createTHeadElement() {
		return ClientDomDocumentStatic.createTHeadElement(this);
	}

	@Override
	public final TableCellElement createTHElement() {
		return ClientDomDocumentStatic.createTHElement(this);
	}

	@Override
	public final TitleElement createTitleElement() {
		return ClientDomDocumentStatic.createTitleElement(this);
	}

	@Override
	public final TableRowElement createTRElement() {
		return ClientDomDocumentStatic.createTRElement(this);
	}

	@Override
	public final UListElement createULElement() {
		return ClientDomDocumentStatic.createULElement(this);
	}

	@Override
	public String createUniqueId() {
		return "gwt-luid-" + this.gwtLuid++;
	}

	@Override
	public final VideoElement createVideoElement() {
		return ClientDomDocumentStatic.createVideoElement(this);
	}

	@Override
	public Document documentFor() {
		return document;
	}

	@Override
	public void enableScrolling(boolean enable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BodyElement getBody() {
		return (BodyElement) this.bodyElement;
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
	public String getCompatMode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getDocumentElement() {
		return null;
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
	public String getVisibilityState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasFocus() {
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
	public Document node() {
		return document;
	}

	@Override
	public void setNodeValue(String nodeValue) {
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

	@Override
	public <T> T invoke(Supplier<T> supplier, Class clazz, String methodName,
			List<Class> argumentTypes, List<?> arguments, boolean sync) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void invoke(Runnable runnable, Class clazz, String methodName,
			List<Class> argumentTypes, List<?> arguments, boolean sync) {
		ClientDomDocumentStatic.invoke(this, runnable, clazz, methodName,
				argumentTypes, arguments, sync);
	}

	@Override
	public Element getActiveElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Element> querySelectorAll(String selector) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ClientDomSelection ensureRemoteSelection(Selection selection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setActiveElement(Element elem) {
		throw new UnsupportedOperationException();
	}
}
