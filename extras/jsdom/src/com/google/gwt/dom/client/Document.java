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

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavascriptObjectEquivalent;

/**
 * A Document is the root of the HTML hierarchy and holds the entire content.
 * Besides providing access to the hierarchy, it also provides some convenience
 * methods for accessing certain sets of information from the document.
 */
public class Document extends Node implements DomDocument {
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

	protected Document() {
	}

	public <T extends Node> T appendChild(T newChild) {
		throw new UnsupportedOperationException();
	}

	public Document cast() {
		return this;
	}

	public Node cloneNode(boolean deep) {
		return local.cloneNode(deep);
	}

	public AnchorElement createAnchorElement() {
		return local.createAnchorElement();
	}

	public AreaElement createAreaElement() {
		return local.createAreaElement();
	}

	public AudioElement createAudioElement() {
		return local.createAudioElement();
	}

	public BaseElement createBaseElement() {
		return local.createBaseElement();
	}

	public QuoteElement createBlockQuoteElement() {
		return local.createBlockQuoteElement();
	}

	public NativeEvent createBlurEvent() {
		return typedRemote().createBlurEvent();
	}

	public BRElement createBRElement() {
		return local.createBRElement();
	}

	public ButtonElement createButtonElement() {
		return local.createButtonElement();
	}

	public InputElement createButtonInputElement() {
		return local.createButtonInputElement();
	}

	public CanvasElement createCanvasElement() {
		return local.createCanvasElement();
	}

	public TableCaptionElement createCaptionElement() {
		return local.createCaptionElement();
	}

	public NativeEvent createChangeEvent() {
		return typedRemote().createChangeEvent();
	}

	public InputElement createCheckInputElement() {
		return local.createCheckInputElement();
	}

	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return typedRemote().createClickEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	public TableColElement createColElement() {
		return local.createColElement();
	}

	public TableColElement createColGroupElement() {
		return local.createColGroupElement();
	}

	public NativeEvent createContextMenuEvent() {
		return typedRemote().createContextMenuEvent();
	}

	public NativeEvent createDblClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return typedRemote().createDblClickEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	public ModElement createDelElement() {
		return local.createDelElement();
	}

	public DivElement createDivElement() {
		return DomDocumentStatic.createDivElement(this);
	}

	public DListElement createDLElement() {
		return local.createDLElement();
	}

	public Element createElement(String tagName) {
		return local.createElement(tagName);
	}

	public NativeEvent createErrorEvent() {
		return typedRemote().createErrorEvent();
	}

	public FieldSetElement createFieldSetElement() {
		return local.createFieldSetElement();
	}

	public InputElement createFileInputElement() {
		return local.createFileInputElement();
	}

	public NativeEvent createFocusEvent() {
		return typedRemote().createFocusEvent();
	}

	public FormElement createFormElement() {
		return local.createFormElement();
	}

	public FrameElement createFrameElement() {
		return local.createFrameElement();
	}

	public FrameSetElement createFrameSetElement() {
		return local.createFrameSetElement();
	}

	public HeadElement createHeadElement() {
		return local.createHeadElement();
	}

	public HeadingElement createHElement(int n) {
		return local.createHElement(n);
	}

	public InputElement createHiddenInputElement() {
		return local.createHiddenInputElement();
	}

	public HRElement createHRElement() {
		return local.createHRElement();
	}

	public NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		return typedRemote().createHtmlEvent(type, canBubble, cancelable);
	}

	public IFrameElement createIFrameElement() {
		return local.createIFrameElement();
	}

	public ImageElement createImageElement() {
		return local.createImageElement();
	}

	public InputElement createImageInputElement() {
		return local.createImageInputElement();
	}

	public NativeEvent createInputEvent() {
		return typedRemote().createInputEvent();
	}

	public ModElement createInsElement() {
		return local.createInsElement();
	}

	public NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return typedRemote().createKeyCodeEvent(type, ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return typedRemote().createKeyDownEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedRemote().createKeyDownEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	public NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedRemote().createKeyEvent(type, canBubble, cancelable,
				ctrlKey, altKey, shiftKey, metaKey, keyCode, charCode);
	}

	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode) {
		return typedRemote().createKeyPressEvent(ctrlKey, altKey, shiftKey,
				metaKey, charCode);
	}

	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedRemote().createKeyPressEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return typedRemote().createKeyUpEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return typedRemote().createKeyUpEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	public LabelElement createLabelElement() {
		return local.createLabelElement();
	}

	public LegendElement createLegendElement() {
		return local.createLegendElement();
	}

	public LIElement createLIElement() {
		return local.createLIElement();
	}

	public LinkElement createLinkElement() {
		return local.createLinkElement();
	}

	public NativeEvent createLoadEvent() {
		return typedRemote().createLoadEvent();
	}

	public MapElement createMapElement() {
		return local.createMapElement();
	}

	public MetaElement createMetaElement() {
		return local.createMetaElement();
	}

	public NativeEvent createMouseDownEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return typedRemote().createMouseDownEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return typedRemote().createMouseEvent(type, canBubble, cancelable,
				detail, screenX, screenY, clientX, clientY, ctrlKey, altKey,
				shiftKey, metaKey, button, relatedTarget);
	}

	public NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return typedRemote().createMouseMoveEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public NativeEvent createMouseOutEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return typedRemote().createMouseOutEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	public NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return typedRemote().createMouseOverEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	public NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button) {
		return typedRemote().createMouseUpEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	public ObjectElement createObjectElement() {
		return local.createObjectElement();
	}

	public OListElement createOLElement() {
		return local.createOLElement();
	}

	public OptGroupElement createOptGroupElement() {
		return local.createOptGroupElement();
	}

	public OptionElement createOptionElement() {
		return local.createOptionElement();
	}

	public ParamElement createParamElement() {
		return local.createParamElement();
	}

	public InputElement createPasswordInputElement() {
		return local.createPasswordInputElement();
	}

	public ParagraphElement createPElement() {
		return local.createPElement();
	}

	public PreElement createPreElement() {
		return local.createPreElement();
	}

	public ButtonElement createPushButtonElement() {
		return local.createPushButtonElement();
	}

	public QuoteElement createQElement() {
		return local.createQElement();
	}

	public InputElement createRadioInputElement(String name) {
		return local.createRadioInputElement(name);
	}

	public ButtonElement createResetButtonElement() {
		return local.createResetButtonElement();
	}

	public InputElement createResetInputElement() {
		return local.createResetInputElement();
	}

	public ScriptElement createScriptElement() {
		return local.createScriptElement();
	}

	public ScriptElement createScriptElement(String source) {
		return local.createScriptElement(source);
	}

	public NativeEvent createScrollEvent() {
		return typedRemote().createScrollEvent();
	}

	public SelectElement createSelectElement() {
		return local.createSelectElement();
	}

	public SelectElement createSelectElement(boolean multiple) {
		return local.createSelectElement(multiple);
	}

	public SourceElement createSourceElement() {
		return local.createSourceElement();
	}

	public SpanElement createSpanElement() {
		return local.createSpanElement();
	}

	public StyleElement createStyleElement() {
		return local.createStyleElement();
	}

	public ButtonElement createSubmitButtonElement() {
		return local.createSubmitButtonElement();
	}

	public InputElement createSubmitInputElement() {
		return local.createSubmitInputElement();
	}

	public TableElement createTableElement() {
		return local.createTableElement();
	}

	public TableSectionElement createTBodyElement() {
		return local.createTBodyElement();
	}

	public TableCellElement createTDElement() {
		return local.createTDElement();
	}

	public TextAreaElement createTextAreaElement() {
		return local.createTextAreaElement();
	}

	public InputElement createTextInputElement() {
		return local.createTextInputElement();
	}

	public Text createTextNode(String data) {
		return local.createTextNode(data);
	}

	public TableSectionElement createTFootElement() {
		return local.createTFootElement();
	}

	public TableSectionElement createTHeadElement() {
		return local.createTHeadElement();
	}

	public TableCellElement createTHElement() {
		return local.createTHElement();
	}

	public TitleElement createTitleElement() {
		return local.createTitleElement();
	}

	public TableRowElement createTRElement() {
		return local.createTRElement();
	}

	public UListElement createULElement() {
		return local.createULElement();
	}

	public String createUniqueId() {
		return local.createUniqueId();
	}

	public VideoElement createVideoElement() {
		return local.createVideoElement();
	}

	public void enableScrolling(boolean enable) {
		remote.enableScrolling(enable);
	}

	public BodyElement getBody() {
		return local.getBody();
	}

	public int getBodyOffsetLeft() {
		return remote.getBodyOffsetLeft();
	}

	public int getBodyOffsetTop() {
		return remote.getBodyOffsetTop();
	}

	public Node getChild(int index) {
		return local.getChild(index);
	}

	public int getChildCount() {
		return local.getChildCount();
	}

	public NodeList<Node> getChildNodes() {
		return local.getChildNodes();
	}

	public int getClientHeight() {
		return remote.getClientHeight();
	}

	public int getClientWidth() {
		return remote.getClientWidth();
	}

	public String getCompatMode() {
		return remote.getCompatMode();
	}

	Element documentElement;

	public Element getDocumentElement() {
		if (documentElement == null) {
			documentElement = local.getDocumentElement();
			if (documentElement == null) {
				documentElement = remote().getDocumentElement();
			}
		}
		return documentElement;
	}

	public String getDomain() {
		return remote.getDomain();
	}

	public Element getElementById(String elementId) {
		return remote.getElementById(elementId);
	}

	public NodeList<Element> getElementsByTagName(String tagName) {
		return remote.getElementsByTagName(tagName);
	}

	public Node getFirstChild() {
		return local.getFirstChild();
	}

	public HeadElement getHead() {
		return local.getHead();
	}

	public Node getLastChild() {
		return local.getLastChild();
	}

	public Node getNextSibling() {
		return local.getNextSibling();
	}

	public String getNodeName() {
		return local.getNodeName();
	}

	public short getNodeType() {
		return local.getNodeType();
	}

	public String getNodeValue() {
		return local.getNodeValue();
	}

	public Document getOwnerDocument() {
		return null;
	}

	public Element getParentElement() {
		return null;
	}

	public Node getParentNode() {
		return null;
	}

	public Node getPreviousSibling() {
		return null;
	}

	public String getReferrer() {
		return remote.getReferrer();
	}

	public int getScrollHeight() {
		return remote.getScrollHeight();
	}

	public int getScrollLeft() {
		return remote.getScrollLeft();
	}

	public int getScrollTop() {
		return remote.getScrollTop();
	}

	public int getScrollWidth() {
		return remote.getScrollWidth();
	}

	public String getTitle() {
		return remote.getTitle();
	}

	public String getURL() {
		return remote.getURL();
	}

	public Element getViewportElement() {
		return remote.getViewportElement();
	}

	public boolean hasChildNodes() {
		return local.hasChildNodes();
	}

	public boolean hasParentElement() {
		return false;
	}

	public void importNode(Node node, boolean deep) {
		local.importNode(node, deep);
		remote.importNode(node, deep);
	}

	public boolean isCSS1Compat() {
		return remote.isCSS1Compat();
	}

	@Override
	protected void putRemote(NodeRemote remote,boolean resolved) {
		throw new UnsupportedOperationException();
	}

	public void removeFromParent() {
		throw new UnsupportedOperationException();
	}

	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	public void setScrollLeft(int left) {
		remote.setScrollLeft(left);
	}

	public void setScrollTop(int top) {
		remote.setScrollTop(top);
	}

	public void setTitle(String title) {
		remote.setTitle(title);
	}

	@Override
	public Node nodeFor() {
		return this;
	}

	@Override
	public Document documentFor() {
		return this;
	}

	@Override
	protected DocumentLocal local() {
		return local;
	}

	@Override
	protected DomDocument remote() {
		return remote;
	}

	@Override
	protected boolean linkedToRemote() {
		return true;
	}

	public DocumentRemote typedRemote() {
		return (DocumentRemote) remote;
	}

}
