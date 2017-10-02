package com.google.gwt.dom.client;

public class DocumentLocal extends NodeLocal implements DomDocument {
	public Document document;

	private Element bodyElement;

	private Element headElement;

	private int gwtLuid = 1;

	@Override
	public String getNodeName() {
		return "#document";
	}

	public DocumentLocal() {
	}

	@Override
	public short getNodeType() {
		return Node.DOCUMENT_NODE;
	}

	@Override
	public Element getDocumentElement() {
		return null;
	}

	@Override
	public String getNodeValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Text createTextNode(String data) {
		TextLocal local = new TextLocal(data);
		Text text = new Text(local);
		local.registerNode(text);
		return text;
	}

	@Override
	public Document nodeFor() {
		return document;
	}

	@Override
	public String createUniqueId() {
		return "gwt-luid-" + this.gwtLuid++;
	}

	@Override
	public Document documentFor() {
		return document;
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
	public BodyElement getBody() {
		return (BodyElement) this.bodyElement;
	}

	@Override
	public HeadElement getHead() {
		return (HeadElement) this.headElement;
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
	public final VideoElement createVideoElement() {
		return DomDocumentStatic.createVideoElement(this);
	}
}
