/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dom.client;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.EntityReference;
import org.w3c.dom.ProcessingInstruction;

/**
 * A Document is the root of the HTML hierarchy and holds the entire content.
 * Besides providing access to the hierarchy, it also provides some convenience
 * methods for accessing certain sets of information from the document.
 */
public class Document extends Node
		implements DomDocument, org.w3c.dom.Document {
	private static Document doc;

	public static Document create(DocumentLocal local) {
		Document doc = new Document();
		doc.local = local;
		if (LocalDom.isUseRemoteDom()) {
			doc.remote = DocumentRemote.get();
		}
		return doc;
	}

	/**
	 * Gets the default document. This is the document in which the module is
	 * running.
	 * 
	 * @return the default document
	 */
	public static Document get() {
		// No need to be MT-safe. Single-threaded JS code.
		if (doc == null) {
			DocumentLocal local = new DocumentLocal();
			doc = create(local);
			local.document = doc;
			LocalDom.register(doc);
		}
		return doc;
	}

	DocumentLocal local;

	DomDocument remote;

	Element documentElement;

	protected Document() {
	}

	@Override
	public org.w3c.dom.Node adoptNode(org.w3c.dom.Node arg0)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Document cast() {
		return this;
	}

	@Override
	public Node cloneNode(boolean deep) {
		return local.cloneNode(deep);
	}

	@Override
	public AnchorElement createAnchorElement() {
		return local.createAnchorElement();
	}

	@Override
	public AreaElement createAreaElement() {
		return local.createAreaElement();
	}

	@Override
	public Attr createAttribute(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Attr createAttributeNS(String arg0, String arg1)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public AudioElement createAudioElement() {
		return local.createAudioElement();
	}

	@Override
	public BaseElement createBaseElement() {
		return local.createBaseElement();
	}

	@Override
	public QuoteElement createBlockQuoteElement() {
		return local.createBlockQuoteElement();
	}

	@Override
	public NativeEvent createBlurEvent() {
		return typedRemote().createBlurEvent();
	}

	@Override
	public BRElement createBRElement() {
		return local.createBRElement();
	}

	@Override
	public ButtonElement createButtonElement() {
		return local.createButtonElement();
	}

	@Override
	public InputElement createButtonInputElement() {
		return local.createButtonInputElement();
	}

	@Override
	public CanvasElement createCanvasElement() {
		return local.createCanvasElement();
	}

	@Override
	public TableCaptionElement createCaptionElement() {
		return local.createCaptionElement();
	}

	@Override
	public CDATASection createCDATASection(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createChangeEvent() {
		return typedRemote().createChangeEvent();
	}

	@Override
	public InputElement createCheckInputElement() {
		return local.createCheckInputElement();
	}

	@Override
	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return typedRemote().createClickEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	@Override
	public TableColElement createColElement() {
		return local.createColElement();
	}

	@Override
	public TableColElement createColGroupElement() {
		return local.createColGroupElement();
	}

	@Override
	public Comment createComment(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createContextMenuEvent() {
		return typedRemote().createContextMenuEvent();
	}

	@Override
	public NativeEvent createDblClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return typedRemote().createDblClickEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	@Override
	public ModElement createDelElement() {
		return local.createDelElement();
	}

	@Override
	public DivElement createDivElement() {
		return DomDocumentStatic.createDivElement(this);
	}

	@Override
	public DListElement createDLElement() {
		return local.createDLElement();
	}

	@Override
	public DocumentFragment createDocumentFragment() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element createElement(String tagName) {
		return local.createElement(tagName);
	}

	@Override
	public org.w3c.dom.Element createElementNS(String arg0, String arg1)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityReference createEntityReference(String arg0)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createErrorEvent() {
		return typedRemote().createErrorEvent();
	}

	@Override
	public FieldSetElement createFieldSetElement() {
		return local.createFieldSetElement();
	}

	@Override
	public InputElement createFileInputElement() {
		return local.createFileInputElement();
	}

	@Override
	public NativeEvent createFocusEvent() {
		return typedRemote().createFocusEvent();
	}

	@Override
	public FormElement createFormElement() {
		return local.createFormElement();
	}

	@Override
	public FrameElement createFrameElement() {
		return local.createFrameElement();
	}

	@Override
	public FrameSetElement createFrameSetElement() {
		return local.createFrameSetElement();
	}

	@Override
	public HeadElement createHeadElement() {
		return local.createHeadElement();
	}

	@Override
	public HeadingElement createHElement(int n) {
		return local.createHElement(n);
	}

	@Override
	public InputElement createHiddenInputElement() {
		return local.createHiddenInputElement();
	}

	@Override
	public HRElement createHRElement() {
		return local.createHRElement();
	}

	@Override
	public NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		return typedRemote().createHtmlEvent(type, canBubble, cancelable);
	}

	@Override
	public IFrameElement createIFrameElement() {
		return local.createIFrameElement();
	}

	@Override
	public ImageElement createImageElement() {
		return local.createImageElement();
	}

	@Override
	public InputElement createImageInputElement() {
		return local.createImageInputElement();
	}

	@Override
	public NativeEvent createInputEvent() {
		return typedRemote().createInputEvent();
	}

	@Override
	public ModElement createInsElement() {
		return local.createInsElement();
	}

	@Override
	public NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return typedRemote().createKeyCodeEvent(type, ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	@Override
	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return typedRemote().createKeyDownEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	@Override
	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedRemote().createKeyDownEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	@Override
	public NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedRemote().createKeyEvent(type, canBubble, cancelable,
				ctrlKey, altKey, shiftKey, metaKey, keyCode, charCode);
	}

	@Override
	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode) {
		return typedRemote().createKeyPressEvent(ctrlKey, altKey, shiftKey,
				metaKey, charCode);
	}

	@Override
	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedRemote().createKeyPressEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	@Override
	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return typedRemote().createKeyUpEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	@Override
	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedRemote().createKeyUpEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	@Override
	public LabelElement createLabelElement() {
		return local.createLabelElement();
	}

	@Override
	public LegendElement createLegendElement() {
		return local.createLegendElement();
	}

	@Override
	public LIElement createLIElement() {
		return local.createLIElement();
	}

	@Override
	public LinkElement createLinkElement() {
		return local.createLinkElement();
	}

	@Override
	public NativeEvent createLoadEvent() {
		return typedRemote().createLoadEvent();
	}

	@Override
	public MapElement createMapElement() {
		return local.createMapElement();
	}

	@Override
	public MetaElement createMetaElement() {
		return local.createMetaElement();
	}

	@Override
	public NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return typedRemote().createMouseDownEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	@Override
	public NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return typedRemote().createMouseEvent(type, canBubble, cancelable,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, button, relatedTarget);
	}

	@Override
	public NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return typedRemote().createMouseMoveEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	@Override
	public NativeEvent createMouseOutEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return typedRemote().createMouseOutEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	@Override
	public NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return typedRemote().createMouseOverEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	@Override
	public NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button) {
		return typedRemote().createMouseUpEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	@Override
	public ObjectElement createObjectElement() {
		return local.createObjectElement();
	}

	@Override
	public OListElement createOLElement() {
		return local.createOLElement();
	}

	@Override
	public OptGroupElement createOptGroupElement() {
		return local.createOptGroupElement();
	}

	@Override
	public OptionElement createOptionElement() {
		return local.createOptionElement();
	}

	@Override
	public ParamElement createParamElement() {
		return local.createParamElement();
	}

	@Override
	public InputElement createPasswordInputElement() {
		return local.createPasswordInputElement();
	}

	@Override
	public ParagraphElement createPElement() {
		return local.createPElement();
	}

	@Override
	public PreElement createPreElement() {
		return local.createPreElement();
	}

	@Override
	public ProcessingInstruction createProcessingInstruction(String arg0,
			String arg1) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ButtonElement createPushButtonElement() {
		return local.createPushButtonElement();
	}

	@Override
	public QuoteElement createQElement() {
		return local.createQElement();
	}

	@Override
	public InputElement createRadioInputElement(String name) {
		return local.createRadioInputElement(name);
	}

	@Override
	public ButtonElement createResetButtonElement() {
		return local.createResetButtonElement();
	}

	@Override
	public InputElement createResetInputElement() {
		return local.createResetInputElement();
	}

	@Override
	public ScriptElement createScriptElement() {
		return local.createScriptElement();
	}

	@Override
	public ScriptElement createScriptElement(String source) {
		return local.createScriptElement(source);
	}

	@Override
	public NativeEvent createScrollEvent() {
		return typedRemote().createScrollEvent();
	}

	@Override
	public SelectElement createSelectElement() {
		return local.createSelectElement();
	}

	@Override
	public SelectElement createSelectElement(boolean multiple) {
		return local.createSelectElement(multiple);
	}

	@Override
	public SourceElement createSourceElement() {
		return local.createSourceElement();
	}

	@Override
	public SpanElement createSpanElement() {
		return local.createSpanElement();
	}

	@Override
	public StyleElement createStyleElement() {
		return local.createStyleElement();
	}

	@Override
	public ButtonElement createSubmitButtonElement() {
		return local.createSubmitButtonElement();
	}

	@Override
	public InputElement createSubmitInputElement() {
		return local.createSubmitInputElement();
	}

	@Override
	public TableElement createTableElement() {
		return local.createTableElement();
	}

	@Override
	public TableSectionElement createTBodyElement() {
		return local.createTBodyElement();
	}

	@Override
	public TableCellElement createTDElement() {
		return local.createTDElement();
	}

	@Override
	public TextAreaElement createTextAreaElement() {
		return local.createTextAreaElement();
	}

	@Override
	public InputElement createTextInputElement() {
		return local.createTextInputElement();
	}

	@Override
	public Text createTextNode(String data) {
		return local.createTextNode(data);
	}

	@Override
	public TableSectionElement createTFootElement() {
		return local.createTFootElement();
	}

	@Override
	public TableSectionElement createTHeadElement() {
		return local.createTHeadElement();
	}

	@Override
	public TableCellElement createTHElement() {
		return local.createTHElement();
	}

	@Override
	public TitleElement createTitleElement() {
		return local.createTitleElement();
	}

	@Override
	public TableRowElement createTRElement() {
		return local.createTRElement();
	}

	@Override
	public UListElement createULElement() {
		return local.createULElement();
	}

	@Override
	public String createUniqueId() {
		return local.createUniqueId();
	}

	@Override
	public VideoElement createVideoElement() {
		return local.createVideoElement();
	}

	@Override
	public Document documentFor() {
		return this;
	}

	@Override
	public void enableScrolling(boolean enable) {
		remote.enableScrolling(enable);
	}

	@Override
	public BodyElement getBody() {
		return local.getBody();
	}

	@Override
	public int getBodyOffsetLeft() {
		return remote.getBodyOffsetLeft();
	}

	@Override
	public int getBodyOffsetTop() {
		return remote.getBodyOffsetTop();
	}

	@Override
	public Node getChild(int index) {
		return local.getChild(index);
	}

	@Override
	public int getChildCount() {
		return local.getChildCount();
	}

	@Override
	public NodeList<Node> getChildNodes() {
		return local.getChildNodes();
	}

	@Override
	public int getClientHeight() {
		return remote.getClientHeight();
	}

	@Override
	public int getClientWidth() {
		return remote.getClientWidth();
	}

	@Override
	public String getCompatMode() {
		return remote.getCompatMode();
	}

	@Override
	public DocumentType getDoctype() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getDocumentElement() {
		if (documentElement == null) {
			documentElement = local.getDocumentElement();
			if (documentElement == null) {
				documentElement = remote().getDocumentElement();
			}
		}
		return documentElement;
	}

	@Override
	public String getDocumentURI() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDomain() {
		return remote.getDomain();
	}

	@Override
	public DOMConfiguration getDomConfig() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getElementById(String elementId) {
		return remote.getElementById(elementId);
	}

	@Override
	public NodeList<Element> getElementsByTagName(String tagName) {
		return remote.getElementsByTagName(tagName);
	}

	@Override
	public org.w3c.dom.NodeList getElementsByTagNameNS(String arg0,
			String arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getFirstChild() {
		return local.getFirstChild();
	}

	@Override
	public HeadElement getHead() {
		return local.getHead();
	}

	@Override
	public DOMImplementation getImplementation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getInputEncoding() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getLastChild() {
		return local.getLastChild();
	}

	@Override
	public Node getNextSibling() {
		return local.getNextSibling();
	}

	@Override
	public String getNodeName() {
		return local.getNodeName();
	}

	@Override
	public short getNodeType() {
		return local.getNodeType();
	}

	@Override
	public String getNodeValue() {
		return local.getNodeValue();
	}

	@Override
	public Document getOwnerDocument() {
		return null;
	}

	@Override
	public Element getParentElement() {
		return null;
	}

	@Override
	public Node getParentNode() {
		return null;
	}

	@Override
	public Node getPreviousSibling() {
		return null;
	}

	@Override
	public String getReferrer() {
		return remote.getReferrer();
	}

	@Override
	public int getScrollHeight() {
		return remote.getScrollHeight();
	}

	@Override
	public int getScrollLeft() {
		return remote.getScrollLeft();
	}

	@Override
	public int getScrollTop() {
		return remote.getScrollTop();
	}

	@Override
	public int getScrollWidth() {
		return remote.getScrollWidth();
	}

	@Override
	public boolean getStrictErrorChecking() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTextContent() throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTitle() {
		return remote.getTitle();
	}

	@Override
	public String getURL() {
		return remote.getURL();
	}

	@Override
	public Element getViewportElement() {
		return remote.getViewportElement();
	}

	@Override
	public String getXmlEncoding() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getXmlStandalone() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getXmlVersion() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasChildNodes() {
		return local.hasChildNodes();
	}

	@Override
	public boolean hasParentElement() {
		return false;
	}

	@Override
	public void importNode(Node node, boolean deep) {
		local.importNode(node, deep);
		remote.importNode(node, deep);
	}

	@Override
	public org.w3c.dom.Node importNode(org.w3c.dom.Node arg0, boolean arg1)
			throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCSS1Compat() {
		return remote.isCSS1Compat();
	}

	@Override
	public Node node() {
		return this;
	}

	@Override
	public void normalizeDocument() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeFromParent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public org.w3c.dom.Node renameNode(org.w3c.dom.Node arg0, String arg1,
			String arg2) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDocumentURI(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setScrollLeft(int left) {
		remote.setScrollLeft(left);
	}

	@Override
	public void setScrollTop(int top) {
		remote.setScrollTop(top);
	}

	@Override
	public void setStrictErrorChecking(boolean arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTitle(String title) {
		remote.setTitle(title);
	}

	@Override
	public void setXmlStandalone(boolean arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setXmlVersion(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public DocumentRemote typedRemote() {
		return (DocumentRemote) remote;
	}

	@Override
	protected boolean linkedToRemote() {
		return true;
	}

	@Override
	protected DocumentLocal local() {
		return local;
	}

	@Override
	protected void putRemote(NodeRemote remote, boolean resolved) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected DomDocument remote() {
		return remote;
	}

	@Override
	protected void resetRemote0() {
		throw new UnsupportedOperationException();
	}
}
