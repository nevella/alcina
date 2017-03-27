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
import com.google.gwt.core.client.JavascriptObjectEquivalent;

/**
 * A Document is the root of the HTML hierarchy and holds the entire content.
 * Besides providing access to the hierarchy, it also provides some convenience
 * methods for accessing certain sets of information from the document.
 */
public class Document extends Node implements DomDocument {
	private static Document doc;

	DomDocument typedImpl;

	Document_Jso typedDomImpl;

	Document_Jvm vmLocalImpl;

	public Document cast() {
		return this;
	}

	public String getNodeName() {
		return typedImpl.getNodeName();
	}

	public <T extends Node> T appendChild(T newChild) {
		return typedImpl.appendChild(newChild);
	}

	public short getNodeType() {
		return typedImpl.getNodeType();
	}

	public Node cloneNode(boolean deep) {
		return typedImpl.cloneNode(deep);
	}

	public Node getChild(int index) {
		return typedImpl.getChild(index);
	}

	public String getNodeValue() {
		return typedImpl.getNodeValue();
	}

	public void setNodeValue(String nodeValue) {
		typedImpl.setNodeValue(nodeValue);
	}

	public AnchorElement createAnchorElement() {
		return typedImpl.createAnchorElement();
	}

	public int getChildCount() {
		return typedImpl.getChildCount();
	}

	public AreaElement createAreaElement() {
		return typedImpl.createAreaElement();
	}

	public NodeList<Node> getChildNodes() {
		return typedImpl.getChildNodes();
	}

	public Node getFirstChild() {
		return typedImpl.getFirstChild();
	}

	public Node getLastChild() {
		return typedImpl.getLastChild();
	}

	public Node getNextSibling() {
		return typedImpl.getNextSibling();
	}

	public AudioElement createAudioElement() {
		return typedImpl.createAudioElement();
	}

	public Element getParentElement() {
		return typedImpl.getParentElement();
	}

	public BaseElement createBaseElement() {
		return typedImpl.createBaseElement();
	}

	public Document getOwnerDocument() {
		return typedImpl.getOwnerDocument();
	}

	public Node getParentNode() {
		return typedImpl.getParentNode();
	}

	public Node getPreviousSibling() {
		return typedImpl.getPreviousSibling();
	}

	public boolean hasChildNodes() {
		return typedImpl.hasChildNodes();
	}

	public boolean hasParentElement() {
		return typedImpl.hasParentElement();
	}

	public QuoteElement createBlockQuoteElement() {
		return typedImpl.createBlockQuoteElement();
	}

	public Node insertAfter(Node newChild, Node refChild) {
		return typedImpl.insertAfter(newChild, refChild);
	}

	public NativeEvent createBlurEvent() {
		return typedImpl.createBlurEvent();
	}

	public BRElement createBRElement() {
		return typedImpl.createBRElement();
	}

	public ButtonElement createButtonElement() {
		return typedImpl.createButtonElement();
	}

	public Node insertBefore(Node newChild, Node refChild) {
		return typedImpl.insertBefore(newChild, refChild);
	}

	public Node insertFirst(Node child) {
		return typedImpl.insertFirst(child);
	}

	public InputElement createButtonInputElement() {
		return typedImpl.createButtonInputElement();
	}

	public boolean isOrHasChild(Node child) {
		return typedImpl.isOrHasChild(child);
	}

	public void removeFromParent() {
		typedImpl.removeFromParent();
	}

	public CanvasElement createCanvasElement() {
		return typedImpl.createCanvasElement();
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		return typedImpl.replaceChild(newChild, oldChild);
	}

	public TableCaptionElement createCaptionElement() {
		return typedImpl.createCaptionElement();
	}

	public Node removeChild(Node oldChild) {
		return typedImpl.removeChild(oldChild);
	}

	public NativeEvent createChangeEvent() {
		return typedImpl.createChangeEvent();
	}

	public InputElement createCheckInputElement() {
		return typedImpl.createCheckInputElement();
	}

	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return typedImpl.createClickEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	public TableColElement createColElement() {
		return typedImpl.createColElement();
	}

	public TableColElement createColGroupElement() {
		return typedImpl.createColGroupElement();
	}

	public NativeEvent createContextMenuEvent() {
		return typedImpl.createContextMenuEvent();
	}

	public NativeEvent createDblClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return typedImpl.createDblClickEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	public ModElement createDelElement() {
		return typedImpl.createDelElement();
	}

	public DivElement createDivElement() {
		return typedImpl.createDivElement();
	}

	public DListElement createDLElement() {
		return typedImpl.createDLElement();
	}

	public Element createElement(String tagName) {
		return typedImpl.createElement(tagName);
	}

	public NativeEvent createErrorEvent() {
		return typedImpl.createErrorEvent();
	}

	public FieldSetElement createFieldSetElement() {
		return typedImpl.createFieldSetElement();
	}

	public InputElement createFileInputElement() {
		return typedImpl.createFileInputElement();
	}

	public NativeEvent createFocusEvent() {
		return typedImpl.createFocusEvent();
	}

	public FormElement createFormElement() {
		return typedImpl.createFormElement();
	}

	public FrameElement createFrameElement() {
		return typedImpl.createFrameElement();
	}

	public FrameSetElement createFrameSetElement() {
		return typedImpl.createFrameSetElement();
	}

	public HeadElement createHeadElement() {
		return typedImpl.createHeadElement();
	}

	public HeadingElement createHElement(int n) {
		return typedImpl.createHElement(n);
	}

	public InputElement createHiddenInputElement() {
		return typedImpl.createHiddenInputElement();
	}

	public HRElement createHRElement() {
		return typedImpl.createHRElement();
	}

	public NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		return typedImpl.createHtmlEvent(type, canBubble, cancelable);
	}

	public IFrameElement createIFrameElement() {
		return typedImpl.createIFrameElement();
	}

	public ImageElement createImageElement() {
		return typedImpl.createImageElement();
	}

	public InputElement createImageInputElement() {
		return typedImpl.createImageInputElement();
	}

	public NativeEvent createInputEvent() {
		return typedImpl.createInputEvent();
	}

	public ModElement createInsElement() {
		return typedImpl.createInsElement();
	}

	public NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return typedImpl.createKeyCodeEvent(type, ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return typedImpl.createKeyDownEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode);
	}

	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedImpl.createKeyDownEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
	}

	public NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedImpl.createKeyEvent(type, canBubble, cancelable, ctrlKey,
				altKey, shiftKey, metaKey, keyCode, charCode);
	}

	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode) {
		return typedImpl.createKeyPressEvent(ctrlKey, altKey, shiftKey, metaKey,
				charCode);
	}

	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedImpl.createKeyPressEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
	}

	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return typedImpl.createKeyUpEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode);
	}

	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedImpl.createKeyUpEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
	}

	public LabelElement createLabelElement() {
		return typedImpl.createLabelElement();
	}

	public LegendElement createLegendElement() {
		return typedImpl.createLegendElement();
	}

	public LIElement createLIElement() {
		return typedImpl.createLIElement();
	}

	public LinkElement createLinkElement() {
		return typedImpl.createLinkElement();
	}

	public NativeEvent createLoadEvent() {
		return typedImpl.createLoadEvent();
	}

	public MapElement createMapElement() {
		return typedImpl.createMapElement();
	}

	public MetaElement createMetaElement() {
		return typedImpl.createMetaElement();
	}

	public NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return typedImpl.createMouseDownEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return typedImpl.createMouseEvent(type, canBubble, cancelable, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, relatedTarget);
	}

	public NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return typedImpl.createMouseMoveEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public NativeEvent createMouseOutEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return typedImpl.createMouseOutEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	public NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return typedImpl.createMouseOverEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	public NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button) {
		return typedImpl.createMouseUpEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public ObjectElement createObjectElement() {
		return typedImpl.createObjectElement();
	}

	public OListElement createOLElement() {
		return typedImpl.createOLElement();
	}

	public OptGroupElement createOptGroupElement() {
		return typedImpl.createOptGroupElement();
	}

	public OptionElement createOptionElement() {
		return typedImpl.createOptionElement();
	}

	public ParamElement createParamElement() {
		return typedImpl.createParamElement();
	}

	public InputElement createPasswordInputElement() {
		return typedImpl.createPasswordInputElement();
	}

	public ParagraphElement createPElement() {
		return typedImpl.createPElement();
	}

	public PreElement createPreElement() {
		return typedImpl.createPreElement();
	}

	public ButtonElement createPushButtonElement() {
		return typedImpl.createPushButtonElement();
	}

	public QuoteElement createQElement() {
		return typedImpl.createQElement();
	}

	public InputElement createRadioInputElement(String name) {
		return typedImpl.createRadioInputElement(name);
	}

	public ButtonElement createResetButtonElement() {
		return typedImpl.createResetButtonElement();
	}

	public InputElement createResetInputElement() {
		return typedImpl.createResetInputElement();
	}

	public ScriptElement createScriptElement() {
		return typedImpl.createScriptElement();
	}

	public ScriptElement createScriptElement(String source) {
		return typedImpl.createScriptElement(source);
	}

	public NativeEvent createScrollEvent() {
		return typedImpl.createScrollEvent();
	}

	public SelectElement createSelectElement() {
		return typedImpl.createSelectElement();
	}

	public SelectElement createSelectElement(boolean multiple) {
		return typedImpl.createSelectElement(multiple);
	}

	public SourceElement createSourceElement() {
		return typedImpl.createSourceElement();
	}

	public SpanElement createSpanElement() {
		return typedImpl.createSpanElement();
	}

	public StyleElement createStyleElement() {
		return typedImpl.createStyleElement();
	}

	public ButtonElement createSubmitButtonElement() {
		return typedImpl.createSubmitButtonElement();
	}

	public InputElement createSubmitInputElement() {
		return typedImpl.createSubmitInputElement();
	}

	public TableElement createTableElement() {
		return typedImpl.createTableElement();
	}

	public TableSectionElement createTBodyElement() {
		return typedImpl.createTBodyElement();
	}

	public TableCellElement createTDElement() {
		return typedImpl.createTDElement();
	}

	public TextAreaElement createTextAreaElement() {
		return typedImpl.createTextAreaElement();
	}

	public InputElement createTextInputElement() {
		return typedImpl.createTextInputElement();
	}

	public Text createTextNode(String data) {
		return typedImpl.createTextNode(data);
	}

	public TableSectionElement createTFootElement() {
		return typedImpl.createTFootElement();
	}

	public TableSectionElement createTHeadElement() {
		return typedImpl.createTHeadElement();
	}

	public TableCellElement createTHElement() {
		return typedImpl.createTHElement();
	}

	public TitleElement createTitleElement() {
		return typedImpl.createTitleElement();
	}

	public TableRowElement createTRElement() {
		return typedImpl.createTRElement();
	}

	public UListElement createULElement() {
		return typedImpl.createULElement();
	}

	public Document nodeFor() {
		return typedImpl.documentFor();
	}

	public VideoElement createVideoElement() {
		return typedImpl.createVideoElement();
	}

	public String createUniqueId() {
		return typedImpl.createUniqueId();
	}

	public void enableScrolling(boolean enable) {
		typedImpl.enableScrolling(enable);
	}

	public BodyElement getBody() {
		return typedImpl.getBody();
	}

	public int getBodyOffsetLeft() {
		return typedImpl.getBodyOffsetLeft();
	}

	public int getBodyOffsetTop() {
		return typedImpl.getBodyOffsetTop();
	}

	public int getClientHeight() {
		return typedImpl.getClientHeight();
	}

	public int getClientWidth() {
		return typedImpl.getClientWidth();
	}

	public String getCompatMode() {
		return typedImpl.getCompatMode();
	}

	public Element getDocumentElement() {
		return typedImpl.getDocumentElement();
	}

	public String getDomain() {
		return typedImpl.getDomain();
	}

	public Element getElementById(String elementId) {
		return typedImpl.getElementById(elementId);
	}

	public NodeList<Element> getElementsByTagName(String tagName) {
		return typedImpl.getElementsByTagName(tagName);
	}

	public HeadElement getHead() {
		return typedImpl.getHead();
	}

	public String getReferrer() {
		return typedImpl.getReferrer();
	}

	public int getScrollHeight() {
		return typedImpl.getScrollHeight();
	}

	public int getScrollLeft() {
		return typedImpl.getScrollLeft();
	}

	public int getScrollTop() {
		return typedImpl.getScrollTop();
	}

	public int getScrollWidth() {
		return typedImpl.getScrollWidth();
	}

	public String getTitle() {
		return typedImpl.getTitle();
	}

	public String getURL() {
		return typedImpl.getURL();
	}

	public void importNode(Node node, boolean deep) {
		typedImpl.importNode(node, deep);
	}

	public boolean isCSS1Compat() {
		return typedImpl.isCSS1Compat();
	}

	public void setScrollLeft(int left) {
		typedImpl.setScrollLeft(left);
	}

	public void setScrollTop(int top) {
		typedImpl.setScrollTop(top);
	}

	public void setTitle(String title) {
		typedImpl.setTitle(title);
	}

	public Element getViewportElement() {
		return typedImpl.getViewportElement();
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
			Document_Jso jsoDoc = Document_Jso.get();
			doc.putDomImpl(jsoDoc);
			doc.putImpl(jsoDoc);
			// FIXME - could be document_js
			doc.vmLocalImpl = new Document_Jvm();
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

	@Override
	public void putDomImpl(Node_Jso nodeDom) {
		typedDomImpl = (Document_Jso) nodeDom;
		domImpl = nodeDom;
	}

	@Override
	public void putImpl(DomNode impl) {
		typedImpl = (Document_Jso) impl;
		this.impl = impl;
	}
}
