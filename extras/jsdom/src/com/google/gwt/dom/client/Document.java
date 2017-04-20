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

	public static Document create(DomDocument localImpl) {
		Document doc = new Document();
		Document_Jso jsoDoc = Document_Jso.get();
		doc.putDomImpl(jsoDoc);
		doc.putImpl(jsoDoc);
		doc.localImpl = localImpl;
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
			doc = create(new Document_Jvm());
			LocalDomBridge.register(doc);
		}
		return doc;
	}

	DomDocument impl;

	Document_Jso domImpl;

	DomDocument localImpl;

	protected Document() {
	}

	public <T extends Node> T appendChild(T newChild) {
		return impl.appendChild(newChild);
	}

	public Document cast() {
		return this;
	}

	public <T extends DomDocument> T castLocalImpl() {
		return (T) localImpl;
	}

	public Node cloneNode(boolean deep) {
		return impl.cloneNode(deep);
	}

	public AnchorElement createAnchorElement() {
		return impl.createAnchorElement();
	}

	public AreaElement createAreaElement() {
		return impl.createAreaElement();
	}

	public AudioElement createAudioElement() {
		return impl.createAudioElement();
	}

	public BaseElement createBaseElement() {
		return impl.createBaseElement();
	}

	public QuoteElement createBlockQuoteElement() {
		return impl.createBlockQuoteElement();
	}

	public NativeEvent createBlurEvent() {
		return impl.createBlurEvent();
	}

	public BRElement createBRElement() {
		return impl.createBRElement();
	}

	public ButtonElement createButtonElement() {
		return impl.createButtonElement();
	}

	public InputElement createButtonInputElement() {
		return impl.createButtonInputElement();
	}

	public CanvasElement createCanvasElement() {
		return impl.createCanvasElement();
	}

	public TableCaptionElement createCaptionElement() {
		return impl.createCaptionElement();
	}

	public NativeEvent createChangeEvent() {
		return impl.createChangeEvent();
	}

	public InputElement createCheckInputElement() {
		return impl.createCheckInputElement();
	}

	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return impl.createClickEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	public TableColElement createColElement() {
		return impl.createColElement();
	}

	public TableColElement createColGroupElement() {
		return impl.createColGroupElement();
	}

	public NativeEvent createContextMenuEvent() {
		return impl.createContextMenuEvent();
	}

	public NativeEvent createDblClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return impl.createDblClickEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	public ModElement createDelElement() {
		return impl.createDelElement();
	}

	public DivElement createDivElement() {
		return impl.createDivElement();
	}

	public DListElement createDLElement() {
		return impl.createDLElement();
	}

	public Element createElement(String tagName) {
		return impl.createElement(tagName);
	}

	public NativeEvent createErrorEvent() {
		return impl.createErrorEvent();
	}

	public FieldSetElement createFieldSetElement() {
		return impl.createFieldSetElement();
	}

	public InputElement createFileInputElement() {
		return impl.createFileInputElement();
	}

	public NativeEvent createFocusEvent() {
		return impl.createFocusEvent();
	}

	public FormElement createFormElement() {
		return impl.createFormElement();
	}

	public FrameElement createFrameElement() {
		return impl.createFrameElement();
	}

	public FrameSetElement createFrameSetElement() {
		return impl.createFrameSetElement();
	}

	public HeadElement createHeadElement() {
		return impl.createHeadElement();
	}

	public HeadingElement createHElement(int n) {
		return impl.createHElement(n);
	}

	public InputElement createHiddenInputElement() {
		return impl.createHiddenInputElement();
	}

	public HRElement createHRElement() {
		return impl.createHRElement();
	}

	public NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		return impl.createHtmlEvent(type, canBubble, cancelable);
	}

	public IFrameElement createIFrameElement() {
		return impl.createIFrameElement();
	}

	public ImageElement createImageElement() {
		return impl.createImageElement();
	}

	public InputElement createImageInputElement() {
		return impl.createImageInputElement();
	}

	public NativeEvent createInputEvent() {
		return impl.createInputEvent();
	}

	public ModElement createInsElement() {
		return impl.createInsElement();
	}

	public NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return impl.createKeyCodeEvent(type, ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return impl.createKeyDownEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode);
	}

	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return impl.createKeyDownEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
	}

	public NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return impl.createKeyEvent(type, canBubble, cancelable, ctrlKey,
				altKey, shiftKey, metaKey, keyCode, charCode);
	}

	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode) {
		return impl.createKeyPressEvent(ctrlKey, altKey, shiftKey, metaKey,
				charCode);
	}

	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return impl.createKeyPressEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
	}

	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return impl.createKeyUpEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode);
	}

	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return impl.createKeyUpEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
	}

	public LabelElement createLabelElement() {
		return impl.createLabelElement();
	}

	public LegendElement createLegendElement() {
		return impl.createLegendElement();
	}

	public LIElement createLIElement() {
		return impl.createLIElement();
	}

	public LinkElement createLinkElement() {
		return impl.createLinkElement();
	}

	public NativeEvent createLoadEvent() {
		return impl.createLoadEvent();
	}

	public MapElement createMapElement() {
		return impl.createMapElement();
	}

	public MetaElement createMetaElement() {
		return impl.createMetaElement();
	}

	public NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return impl.createMouseDownEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return impl.createMouseEvent(type, canBubble, cancelable, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, relatedTarget);
	}

	public NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return impl.createMouseMoveEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public NativeEvent createMouseOutEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return impl.createMouseOutEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	public NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return impl.createMouseOverEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	public NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button) {
		return impl.createMouseUpEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public ObjectElement createObjectElement() {
		return impl.createObjectElement();
	}

	public OListElement createOLElement() {
		return impl.createOLElement();
	}

	public OptGroupElement createOptGroupElement() {
		return impl.createOptGroupElement();
	}

	public OptionElement createOptionElement() {
		return impl.createOptionElement();
	}

	public ParamElement createParamElement() {
		return impl.createParamElement();
	}

	public InputElement createPasswordInputElement() {
		return impl.createPasswordInputElement();
	}

	public ParagraphElement createPElement() {
		return impl.createPElement();
	}

	public PreElement createPreElement() {
		return impl.createPreElement();
	}

	public ButtonElement createPushButtonElement() {
		return impl.createPushButtonElement();
	}

	public QuoteElement createQElement() {
		return impl.createQElement();
	}

	public InputElement createRadioInputElement(String name) {
		return impl.createRadioInputElement(name);
	}

	public ButtonElement createResetButtonElement() {
		return impl.createResetButtonElement();
	}

	public InputElement createResetInputElement() {
		return impl.createResetInputElement();
	}

	public ScriptElement createScriptElement() {
		return impl.createScriptElement();
	}

	public ScriptElement createScriptElement(String source) {
		return impl.createScriptElement(source);
	}

	public NativeEvent createScrollEvent() {
		return impl.createScrollEvent();
	}

	public SelectElement createSelectElement() {
		return impl.createSelectElement();
	}

	public SelectElement createSelectElement(boolean multiple) {
		return impl.createSelectElement(multiple);
	}

	public SourceElement createSourceElement() {
		return impl.createSourceElement();
	}

	public SpanElement createSpanElement() {
		return impl.createSpanElement();
	}

	public StyleElement createStyleElement() {
		return impl.createStyleElement();
	}

	public ButtonElement createSubmitButtonElement() {
		return impl.createSubmitButtonElement();
	}

	public InputElement createSubmitInputElement() {
		return impl.createSubmitInputElement();
	}

	public TableElement createTableElement() {
		return impl.createTableElement();
	}

	public TableSectionElement createTBodyElement() {
		return impl.createTBodyElement();
	}

	public TableCellElement createTDElement() {
		return impl.createTDElement();
	}

	public TextAreaElement createTextAreaElement() {
		return impl.createTextAreaElement();
	}

	public InputElement createTextInputElement() {
		return impl.createTextInputElement();
	}

	public Text createTextNode(String data) {
		return impl.createTextNode(data);
	}

	public TableSectionElement createTFootElement() {
		return impl.createTFootElement();
	}

	public TableSectionElement createTHeadElement() {
		return impl.createTHeadElement();
	}

	public TableCellElement createTHElement() {
		return impl.createTHElement();
	}

	public TitleElement createTitleElement() {
		return impl.createTitleElement();
	}

	public TableRowElement createTRElement() {
		return impl.createTRElement();
	}

	public UListElement createULElement() {
		return impl.createULElement();
	}

	public String createUniqueId() {
		return impl.createUniqueId();
	}

	public VideoElement createVideoElement() {
		return impl.createVideoElement();
	}

	@Override
	public Document documentFor() {
		return nodeFor();
	}

	public void enableScrolling(boolean enable) {
		impl.enableScrolling(enable);
	}

	public BodyElement getBody() {
		return impl.getBody();
	}

	public int getBodyOffsetLeft() {
		return impl.getBodyOffsetLeft();
	}

	public int getBodyOffsetTop() {
		return impl.getBodyOffsetTop();
	}

	public Node getChild(int index) {
		return impl.getChild(index);
	}

	public int getChildCount() {
		return impl.getChildCount();
	}

	public NodeList<Node> getChildNodes() {
		return impl.getChildNodes();
	}

	public int getClientHeight() {
		return impl.getClientHeight();
	}

	public int getClientWidth() {
		return impl.getClientWidth();
	}

	public String getCompatMode() {
		return impl.getCompatMode();
	}

	public Element getDocumentElement() {
		return impl.getDocumentElement();
	}

	public String getDomain() {
		return impl.getDomain();
	}

	public Element getElementById(String elementId) {
		return impl.getElementById(elementId);
	}

	public NodeList<Element> getElementsByTagName(String tagName) {
		return impl.getElementsByTagName(tagName);
	}

	public Node getFirstChild() {
		return impl.getFirstChild();
	}

	public HeadElement getHead() {
		return impl.getHead();
	}

	public Node getLastChild() {
		return impl.getLastChild();
	}

	public Node getNextSibling() {
		return impl.getNextSibling();
	}

	public String getNodeName() {
		return impl.getNodeName();
	}

	public short getNodeType() {
		return impl.getNodeType();
	}

	public String getNodeValue() {
		return impl.getNodeValue();
	}

	public Document getOwnerDocument() {
		return impl.getOwnerDocument();
	}

	public Element getParentElement() {
		return impl.getParentElement();
	}

	public Node getParentNode() {
		return impl.getParentNode();
	}

	public Node getPreviousSibling() {
		return impl.getPreviousSibling();
	}

	public String getReferrer() {
		return impl.getReferrer();
	}

	public int getScrollHeight() {
		return impl.getScrollHeight();
	}

	public int getScrollLeft() {
		return impl.getScrollLeft();
	}

	public int getScrollTop() {
		return impl.getScrollTop();
	}

	public int getScrollWidth() {
		return impl.getScrollWidth();
	}

	public String getTitle() {
		return impl.getTitle();
	}

	public String getURL() {
		return impl.getURL();
	}

	public Element getViewportElement() {
		return impl.getViewportElement();
	}

	public boolean hasChildNodes() {
		return impl.hasChildNodes();
	}

	public boolean hasParentElement() {
		return impl.hasParentElement();
	}

	public void importNode(Node node, boolean deep) {
		impl.importNode(node, deep);
	}

	public Node insertAfter(Node newChild, Node refChild) {
		return impl.insertAfter(newChild, refChild);
	}

	public Node insertBefore(Node newChild, Node refChild) {
		return impl.insertBefore(newChild, refChild);
	}

	public Node insertFirst(Node child) {
		return impl.insertFirst(child);
	}

	public boolean isCSS1Compat() {
		return impl.isCSS1Compat();
	}

	public boolean isOrHasChild(Node child) {
		return impl.isOrHasChild(child);
	}

	public Document nodeFor() {
		return impl.documentFor();
	}

	@Override
	public boolean provideIsLocal() {
		return false;
	}

	@Override
	public void putDomImpl(Node_Jso nodeDom) {
		domImpl = (Document_Jso) nodeDom;
	}

	@Override
	public void putImpl(DomNode impl) {
		this.impl = (Document_Jso) impl;
	}

	public Node removeChild(Node oldChild) {
		return impl.removeChild(oldChild);
	}

	public void removeFromParent() {
		impl.removeFromParent();
	}

	public Node replaceChild(Node newChild, Node oldChild) {
		return impl.replaceChild(newChild, oldChild);
	}

	public void setNodeValue(String nodeValue) {
		impl.setNodeValue(nodeValue);
	}

	public void setScrollLeft(int left) {
		impl.setScrollLeft(left);
	}

	public void setScrollTop(int top) {
		impl.setScrollTop(top);
	}

	public void setTitle(String title) {
		impl.setTitle(title);
	}

	@Override
	Document_Jso domImpl() {
		return domImpl;
	}

	@Override
	DomDocument impl() {
		return domImpl;
	}

	@Override
	DomDocument localImpl() {
		return localImpl;
	}


	@Override
	DomDocument implNoResolve() {
		return impl();
	}
}
