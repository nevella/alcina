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

import com.google.gwt.core.client.GWT;

/**
 * A Document is the root of the HTML hierarchy and holds the entire content.
 * Besides providing access to the hierarchy, it also provides some convenience
 * methods for accessing certain sets of information from the document.
 */
public class Document extends Node<DomDocument, Document_Dom>
		implements DomDocument {
	private static Document doc;

	private Document_Jvm vmLocalImpl;

	public Document cast() {
		return this;
	}

	public String getNodeName() {
		return this.impl.getNodeName();
	}

	public <T extends Node> T appendChild(T newChild) {
		return this.impl.appendChild(newChild);
	}

	public short getNodeType() {
		return this.impl.getNodeType();
	}

	public Node cloneNode(boolean deep) {
		return this.impl.cloneNode(deep);
	}

	public Node getChild(int index) {
		return this.impl.getChild(index);
	}

	public String getNodeValue() {
		return this.impl.getNodeValue();
	}

	public void setNodeValue(String nodeValue) {
		this.impl.setNodeValue(nodeValue);
	}

	public AnchorElement createAnchorElement() {
		return this.impl.createAnchorElement();
	}

	public int getChildCount() {
		return this.impl.getChildCount();
	}

	public AreaElement createAreaElement() {
		return this.impl.createAreaElement();
	}

	public NodeList<Node> getChildNodes() {
		return this.impl.getChildNodes();
	}

	public Node getFirstChild() {
		return this.impl.getFirstChild();
	}

	public Node getLastChild() {
		return this.impl.getLastChild();
	}

	public Node getNextSibling() {
		return this.impl.getNextSibling();
	}

	public AudioElement createAudioElement() {
		return this.impl.createAudioElement();
	}

	public Element getParentElement() {
		return this.impl.getParentElement();
	}

	public BaseElement createBaseElement() {
		return this.impl.createBaseElement();
	}

	public Document getOwnerDocument() {
		return this.impl.getOwnerDocument();
	}

	public Node getParentNode() {
		return this.impl.getParentNode();
	}

	public Node getPreviousSibling() {
		return this.impl.getPreviousSibling();
	}

	public boolean hasChildNodes() {
		return this.impl.hasChildNodes();
	}

	public boolean hasParentElement() {
		return this.impl.hasParentElement();
	}

	public QuoteElement createBlockQuoteElement() {
		return this.impl.createBlockQuoteElement();
	}

	public Node insertAfter(Node newChild, Node refChild) {
		return this.impl.insertAfter(newChild, refChild);
	}

	public NativeEvent createBlurEvent() {
		return this.impl.createBlurEvent();
	}

	public BRElement createBRElement() {
		return this.impl.createBRElement();
	}

	public ButtonElement createButtonElement() {
		return this.impl.createButtonElement();
	}

	public Node insertBefore(Node newChild, Node refChild) {
		return this.impl.insertBefore(newChild, refChild);
	}

	public Node insertFirst(Node child) {
		return this.impl.insertFirst(child);
	}

	public InputElement createButtonInputElement() {
		return this.impl.createButtonInputElement();
	}

	public boolean isOrHasChild(Node child) {
		return this.impl.isOrHasChild(child);
	}

	public void removeFromParent() {
		this.impl.removeFromParent();
	}

	public CanvasElement createCanvasElement() {
		return this.impl.createCanvasElement();
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		return this.impl.replaceChild(newChild, oldChild);
	}

	public TableCaptionElement createCaptionElement() {
		return this.impl.createCaptionElement();
	}

	public Node removeChild(Node oldChild) {
		return this.impl.removeChild(oldChild);
	}

	public NativeEvent createChangeEvent() {
		return this.impl.createChangeEvent();
	}

	public InputElement createCheckInputElement() {
		return this.impl.createCheckInputElement();
	}

	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return this.impl.createClickEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	public TableColElement createColElement() {
		return this.impl.createColElement();
	}

	public TableColElement createColGroupElement() {
		return this.impl.createColGroupElement();
	}

	public NativeEvent createContextMenuEvent() {
		return this.impl.createContextMenuEvent();
	}

	public NativeEvent createDblClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return this.impl.createDblClickEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	public ModElement createDelElement() {
		return this.impl.createDelElement();
	}

	public DivElement createDivElement() {
		return this.impl.createDivElement();
	}

	public DListElement createDLElement() {
		return this.impl.createDLElement();
	}

	public Element createElement(String tagName) {
		return this.impl.createElement(tagName);
	}

	public NativeEvent createErrorEvent() {
		return this.impl.createErrorEvent();
	}

	public FieldSetElement createFieldSetElement() {
		return this.impl.createFieldSetElement();
	}

	public InputElement createFileInputElement() {
		return this.impl.createFileInputElement();
	}

	public NativeEvent createFocusEvent() {
		return this.impl.createFocusEvent();
	}

	public FormElement createFormElement() {
		return this.impl.createFormElement();
	}

	public FrameElement createFrameElement() {
		return this.impl.createFrameElement();
	}

	public FrameSetElement createFrameSetElement() {
		return this.impl.createFrameSetElement();
	}

	public HeadElement createHeadElement() {
		return this.impl.createHeadElement();
	}

	public HeadingElement createHElement(int n) {
		return this.impl.createHElement(n);
	}

	public InputElement createHiddenInputElement() {
		return this.impl.createHiddenInputElement();
	}

	public HRElement createHRElement() {
		return this.impl.createHRElement();
	}

	public NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		return this.impl.createHtmlEvent(type, canBubble, cancelable);
	}

	public IFrameElement createIFrameElement() {
		return this.impl.createIFrameElement();
	}

	public ImageElement createImageElement() {
		return this.impl.createImageElement();
	}

	public InputElement createImageInputElement() {
		return this.impl.createImageInputElement();
	}

	public NativeEvent createInputEvent() {
		return this.impl.createInputEvent();
	}

	public ModElement createInsElement() {
		return this.impl.createInsElement();
	}

	public NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return this.impl.createKeyCodeEvent(type, ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return this.impl.createKeyDownEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode);
	}

	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return this.impl.createKeyDownEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
	}

	public NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return this.impl.createKeyEvent(type, canBubble, cancelable, ctrlKey,
				altKey, shiftKey, metaKey, keyCode, charCode);
	}

	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode) {
		return this.impl.createKeyPressEvent(ctrlKey, altKey, shiftKey, metaKey,
				charCode);
	}

	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return this.impl.createKeyPressEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
	}

	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return this.impl.createKeyUpEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode);
	}

	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return this.impl.createKeyUpEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
	}

	public LabelElement createLabelElement() {
		return this.impl.createLabelElement();
	}

	public LegendElement createLegendElement() {
		return this.impl.createLegendElement();
	}

	public LIElement createLIElement() {
		return this.impl.createLIElement();
	}

	public LinkElement createLinkElement() {
		return this.impl.createLinkElement();
	}

	public NativeEvent createLoadEvent() {
		return this.impl.createLoadEvent();
	}

	public MapElement createMapElement() {
		return this.impl.createMapElement();
	}

	public MetaElement createMetaElement() {
		return this.impl.createMetaElement();
	}

	public NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return this.impl.createMouseDownEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return this.impl.createMouseEvent(type, canBubble, cancelable, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, relatedTarget);
	}

	public NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return this.impl.createMouseMoveEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public NativeEvent createMouseOutEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return this.impl.createMouseOutEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	public NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return this.impl.createMouseOverEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	public NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button) {
		return this.impl.createMouseUpEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public ObjectElement createObjectElement() {
		return this.impl.createObjectElement();
	}

	public OListElement createOLElement() {
		return this.impl.createOLElement();
	}

	public OptGroupElement createOptGroupElement() {
		return this.impl.createOptGroupElement();
	}

	public OptionElement createOptionElement() {
		return this.impl.createOptionElement();
	}

	public ParamElement createParamElement() {
		return this.impl.createParamElement();
	}

	public InputElement createPasswordInputElement() {
		return this.impl.createPasswordInputElement();
	}

	public ParagraphElement createPElement() {
		return this.impl.createPElement();
	}

	public PreElement createPreElement() {
		return this.impl.createPreElement();
	}

	public ButtonElement createPushButtonElement() {
		return this.impl.createPushButtonElement();
	}

	public QuoteElement createQElement() {
		return this.impl.createQElement();
	}

	public InputElement createRadioInputElement(String name) {
		return this.impl.createRadioInputElement(name);
	}

	public ButtonElement createResetButtonElement() {
		return this.impl.createResetButtonElement();
	}

	public InputElement createResetInputElement() {
		return this.impl.createResetInputElement();
	}

	public ScriptElement createScriptElement() {
		return this.impl.createScriptElement();
	}

	public ScriptElement createScriptElement(String source) {
		return this.impl.createScriptElement(source);
	}

	public NativeEvent createScrollEvent() {
		return this.impl.createScrollEvent();
	}

	public SelectElement createSelectElement() {
		return this.impl.createSelectElement();
	}

	public SelectElement createSelectElement(boolean multiple) {
		return this.impl.createSelectElement(multiple);
	}

	public SourceElement createSourceElement() {
		return this.impl.createSourceElement();
	}

	public SpanElement createSpanElement() {
		return this.impl.createSpanElement();
	}

	public StyleElement createStyleElement() {
		return this.impl.createStyleElement();
	}

	public ButtonElement createSubmitButtonElement() {
		return this.impl.createSubmitButtonElement();
	}

	public InputElement createSubmitInputElement() {
		return this.impl.createSubmitInputElement();
	}

	public TableElement createTableElement() {
		return this.impl.createTableElement();
	}

	public TableSectionElement createTBodyElement() {
		return this.impl.createTBodyElement();
	}

	public TableCellElement createTDElement() {
		return this.impl.createTDElement();
	}

	public TextAreaElement createTextAreaElement() {
		return this.impl.createTextAreaElement();
	}

	public InputElement createTextInputElement() {
		return this.impl.createTextInputElement();
	}

	public Text createTextNode(String data) {
		return this.impl.createTextNode(data);
	}

	public TableSectionElement createTFootElement() {
		return this.impl.createTFootElement();
	}

	public TableSectionElement createTHeadElement() {
		return this.impl.createTHeadElement();
	}

	public TableCellElement createTHElement() {
		return this.impl.createTHElement();
	}

	public TitleElement createTitleElement() {
		return this.impl.createTitleElement();
	}

	public TableRowElement createTRElement() {
		return this.impl.createTRElement();
	}

	public UListElement createULElement() {
		return this.impl.createULElement();
	}

	public Document nodeFor() {
		return this.impl.documentFor();
	}

	public VideoElement createVideoElement() {
		return this.impl.createVideoElement();
	}

	public String createUniqueId() {
		return this.impl.createUniqueId();
	}

	public void enableScrolling(boolean enable) {
		this.impl.enableScrolling(enable);
	}

	public BodyElement getBody() {
		return this.impl.getBody();
	}

	public int getBodyOffsetLeft() {
		return this.impl.getBodyOffsetLeft();
	}

	public int getBodyOffsetTop() {
		return this.impl.getBodyOffsetTop();
	}

	public int getClientHeight() {
		return this.impl.getClientHeight();
	}

	public int getClientWidth() {
		return this.impl.getClientWidth();
	}

	public String getCompatMode() {
		return this.impl.getCompatMode();
	}

	public Element getDocumentElement() {
		return this.impl.getDocumentElement();
	}

	public String getDomain() {
		return this.impl.getDomain();
	}

	public Element getElementById(String elementId) {
		return this.impl.getElementById(elementId);
	}

	public NodeList<Element> getElementsByTagName(String tagName) {
		return this.impl.getElementsByTagName(tagName);
	}

	public HeadElement getHead() {
		return this.impl.getHead();
	}

	public String getReferrer() {
		return this.impl.getReferrer();
	}

	public int getScrollHeight() {
		return this.impl.getScrollHeight();
	}

	public int getScrollLeft() {
		return this.impl.getScrollLeft();
	}

	public int getScrollTop() {
		return this.impl.getScrollTop();
	}

	public int getScrollWidth() {
		return this.impl.getScrollWidth();
	}

	public String getTitle() {
		return this.impl.getTitle();
	}

	public String getURL() {
		return this.impl.getURL();
	}

	public void importNode(Node node, boolean deep) {
		this.impl.importNode(node, deep);
	}

	public boolean isCSS1Compat() {
		return this.impl.isCSS1Compat();
	}

	public void setScrollLeft(int left) {
		this.impl.setScrollLeft(left);
	}

	public void setScrollTop(int top) {
		this.impl.setScrollTop(top);
	}

	public void setTitle(String title) {
		this.impl.setTitle(title);
	}

	public Element getViewportElement() {
		return this.impl.getViewportElement();
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
			doc = new Document();
			doc.domImpl = Document_Dom.get();
			// FIXME - could be document_js
			doc.vmLocalImpl = new Document_Jvm();
			doc.impl = doc.domImpl;
			VmLocalDomBridge.register(doc);
		}
		return doc;
	}

	protected Document() {
	}

	@Override
	public Document documentFor() {
		return nodeFor();
	}
}
