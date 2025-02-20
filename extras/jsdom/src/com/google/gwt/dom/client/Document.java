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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.EntityReference;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.ContextFrame;
import cc.alcina.framework.common.client.context.ContextProvider;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.logic.reflection.Registration;

/**
 * <p>
 * A Document is the root of the HTML hierarchy and holds the entire content.
 * Besides providing access to the hierarchy, it also provides some convenience
 * methods for accessing certain sets of information from the document.
 *
 * <p>
 * Server-side, {@code Document.get()} returns a context-dependent instance
 *
 * <p>
 * This construct varies from the org.w3c dom in that a document object can be
 * created without corresponding markup (and documentElement) - this makes
 * context document construction easier, but it may be better to add the
 * complexity to doc creation to match the w3c model
 */
public class Document extends Node
		implements ClientDomDocument, org.w3c.dom.Document,
		org.w3c.dom.traversal.DocumentTraversal, ContextFrame {
	public static ContextProvider<RemoteType, Document> contextProvider;

	public static Document get() {
		return contextProvider.contextFrame();
	}

	// remoteType null :: multiiple context (remote type specified in frame
	// construction)
	public static void initialiseContextProvider(RemoteType remoteType) {
		Function<RemoteType, Document> frameConstructor = Document::new;
		Runnable onPostRegisterCreated = Document::registerWithLocalDom;
		contextProvider = ContextProvider.createProvider(frameConstructor,
				onPostRegisterCreated, remoteType, Document.class,
				remoteType == null);
	}

	private static void registerWithLocalDom() {
		Document doc = Document.get();
		if (doc.remoteType == RemoteType.NONE || doc.remoteType == null) {
			return;
		}
		LocalDom.register(doc);
	}

	RemoteType remoteType;

	LocalDom localDom;

	DocumentLocal local;

	ClientDomDocument remote;

	Element documentElement;

	public final DomDocument domDocument;

	public boolean resolveSvgStyles = false;

	/*
	 * Guides how inner/outerhtml is generated (doesn't affect parsing)
	 */
	public boolean htmlTags = true;

	Selection selection;

	protected Document(RemoteType remoteType) {
		this.remoteType = remoteType;
		domDocument = DomDocument.from(this, true);
		switch (remoteType) {
		case JSO:
			remote = DocumentJso.get();
			break;
		case REF_ID:
			remote = new DocumentAttachId(this);
			break;
		case NONE:
			break;
		default:
			throw new UnsupportedOperationException();
		}
		this.local = new DocumentLocal(this);
		this.selection = new Selection(this);
		localDom = new LocalDom();
	}

	@Override
	protected void onDetach() {
		throw new UnsupportedOperationException();
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
		return jsoRemote().createBlurEvent();
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
		return local.createCDATASection(arg0);
	}

	@Override
	public NativeEvent createChangeEvent() {
		return jsoRemote().createChangeEvent();
	}

	@Override
	public InputElement createCheckInputElement() {
		return local.createCheckInputElement();
	}

	@Override
	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return jsoRemote().createClickEvent(detail, screenX, screenY, clientX,
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
	public Comment createComment(String data) {
		return local.createComment(data);
	}

	@Override
	public NativeEvent createContextMenuEvent() {
		return jsoRemote().createContextMenuEvent();
	}

	@Override
	public NativeEvent createDblClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		return jsoRemote().createDblClickEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey);
	}

	@Override
	public ModElement createDelElement() {
		return local.createDelElement();
	}

	@Override
	public DivElement createDivElement() {
		return ClientDomDocumentStatic.createDivElement(this);
	}

	@Override
	public DListElement createDLElement() {
		return local.createDLElement();
	}

	public Element createDocumentElement(String markup) {
		return createDocumentElement(markup, false);
	}

	public Element createDocumentElement(String markup,
			boolean attachToParent) {
		documentElement = new HtmlParser().parse(markup, null, true);
		if (attachToParent) {
			local().appendChild(documentElement);
			documentElement.setAttached(true, true);
		}
		return documentElement;
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
		return jsoRemote().createErrorEvent();
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
		return jsoRemote().createFocusEvent();
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
		return jsoRemote().createHtmlEvent(type, canBubble, cancelable);
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
		return jsoRemote().createInputEvent();
	}

	@Override
	public ModElement createInsElement() {
		return local.createInsElement();
	}

	@Override
	public NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
		return jsoRemote().createKeyCodeEvent(type, ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	@Override
	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return jsoRemote().createKeyDownEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode);
	}

	@Override
	public NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return jsoRemote().createKeyDownEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	@Override
	public NativeEvent createKeyEvent(String type, boolean canBubble,
			boolean cancelable, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return jsoRemote().createKeyEvent(type, canBubble, cancelable, ctrlKey,
				altKey, shiftKey, metaKey, keyCode, charCode);
	}

	@Override
	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int charCode) {
		return jsoRemote().createKeyPressEvent(ctrlKey, altKey, shiftKey,
				metaKey, charCode);
	}

	@Override
	public NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return jsoRemote().createKeyPressEvent(ctrlKey, altKey, shiftKey,
				metaKey, keyCode, charCode);
	}

	@Override
	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode) {
		return jsoRemote().createKeyUpEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode);
	}

	@Override
	public NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
		return jsoRemote().createKeyUpEvent(ctrlKey, altKey, shiftKey, metaKey,
				keyCode, charCode);
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
		return jsoRemote().createLoadEvent();
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
		return jsoRemote().createMouseDownEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	@Override
	public NativeEvent createMouseEvent(String type, boolean canBubble,
			boolean cancelable, int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return jsoRemote().createMouseEvent(type, canBubble, cancelable, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, relatedTarget);
	}

	@Override
	public NativeEvent createMouseMoveEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button) {
		return jsoRemote().createMouseMoveEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	@Override
	public NativeEvent createMouseOutEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return jsoRemote().createMouseOutEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	@Override
	public NativeEvent createMouseOverEvent(int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return jsoRemote().createMouseOverEvent(detail, screenX, screenY,
				clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
				relatedTarget);
	}

	@Override
	public NativeEvent createMouseUpEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey, int button) {
		return jsoRemote().createMouseUpEvent(detail, screenX, screenY, clientX,
				clientY, ctrlKey, altKey, shiftKey, metaKey, button);
	}

	@Override
	public NodeIterator createNodeIterator(org.w3c.dom.Node root,
			int whatToShow, NodeFilter filter, boolean entityReferenceExpansion)
			throws DOMException {
		throw new UnsupportedOperationException();
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
	public ProcessingInstruction createProcessingInstruction(String target,
			String data) throws DOMException {
		return local.createProcessingInstruction(target, data);
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
		return jsoRemote().createScrollEvent();
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
	public TreeWalker createTreeWalker(org.w3c.dom.Node root, int whatToShow,
			NodeFilter filter, boolean entityReferenceExpansion)
			throws DOMException {
		return new SimpleTreeWalkerImpl(root);
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
		return this;
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
	public <T> T invoke(Supplier<T> supplier, Class clazz, String methodName,
			List<Class> argumentTypes, List<?> arguments, boolean sync) {
		return remote().invoke(supplier, clazz, methodName, argumentTypes,
				arguments, sync);
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
	public String getVisibilityState() {
		return remote.getVisibilityState();
	}

	public boolean isVisibilityStateVisible() {
		return Objects.equals(getVisibilityState(), "visible");
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
	public boolean hasFocus() {
		return remote.hasFocus();
	}

	@Override
	public boolean hasParentElement() {
		return false;
	}

	public DocumentImplAccess implAccess() {
		return new DocumentImplAccess();
	}

	@Override
	public void importNode(Node node, boolean deep) {
		local.importNode(node, deep);
		remote.importNode(node, deep);
	}

	@Override
	public Node importNode(org.w3c.dom.Node w3cNode, boolean deep) {
		Preconditions.checkState(remote instanceof NodeAttachId);
		if (deep) {
			return new NodeImport(w3cNode).run();
		} else {
			switch (w3cNode.getNodeType()) {
			case Node.ATTRIBUTE_NODE:
			case Node.DOCUMENT_FRAGMENT_NODE:
			case Node.DOCUMENT_NODE:
			case Node.DOCUMENT_TYPE_NODE:
			case Node.ENTITY_REFERENCE_NODE:
			case Node.ENTITY_NODE:
			case Node.NOTATION_NODE:
				throw new UnsupportedOperationException();
			case Node.CDATA_SECTION_NODE: {
				org.w3c.dom.CDATASection w3cTyped = (org.w3c.dom.CDATASection) w3cNode;
				CDATASection cdataSection = new CDATASection(
						new CDATASectionLocal(local, w3cTyped));
				cdataSection.local().putCDATASection(cdataSection);
				return cdataSection;
			}
			case Node.COMMENT_NODE: {
				org.w3c.dom.Comment w3cTyped = (org.w3c.dom.Comment) w3cNode;
				Comment comment = new Comment(
						new CommentLocal(local, w3cTyped));
				comment.local().putComment(comment);
				return comment;
			}
			case Node.ELEMENT_NODE: {
				org.w3c.dom.Element w3cTyped = (org.w3c.dom.Element) w3cNode;
				Element element = new Element(
						new ElementLocal(local, w3cTyped));
				element.local().putElement(element);
				return element;
			}
			case Node.PROCESSING_INSTRUCTION_NODE: {
				org.w3c.dom.ProcessingInstruction w3cTyped = (org.w3c.dom.ProcessingInstruction) w3cNode;
				ProcessingInstruction processingInstruction = new ProcessingInstruction(
						new ProcessingInstructionLocal(local, w3cTyped));
				processingInstruction.local()
						.putProcessingInstruction(processingInstruction);
				return processingInstruction;
			}
			case Node.TEXT_NODE: {
				org.w3c.dom.Text w3cTyped = (org.w3c.dom.Text) w3cNode;
				Text text = new Text(new TextLocal(local, w3cTyped));
				text.local().putText(text);
				return text;
			}
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	class NodeImport {
		class InOut {
			org.w3c.dom.Node in;

			Node outParent;

			public InOut(org.w3c.dom.Node in, Node outParent) {
				this.in = in;
				this.outParent = outParent;
			}
		}

		LinkedList<InOut> stack = new LinkedList<>();

		NodeImport(org.w3c.dom.Node in) {
			stack.add(new InOut(in, null));
		}

		Node run() {
			Node result = null;
			while (stack.size() > 0) {
				// don't pop, append in insert order
				InOut cursor = stack.remove(0);
				Node imported = importNode(cursor.in, false);
				if (cursor.outParent != null) {
					cursor.outParent.appendChild(imported);
				} else {
					result = imported;
				}
				org.w3c.dom.NodeList childNodes = cursor.in.getChildNodes();
				for (int idx = 0; idx < childNodes.getLength(); idx++) {
					stack.add(new InOut(childNodes.item(idx), imported));
				}
			}
			return result;
		}
	}

	@Override
	public boolean isCSS1Compat() {
		return remote.isCSS1Compat();
	}

	@Override
	public DocumentJso jsoRemote() {
		return (DocumentJso) remote;
	}

	@Override
	protected DocumentLocal local() {
		return local;
	}

	@Override
	public Node node() {
		return this;
	}

	@Override
	public void normalizeDocument() {
		throw new UnsupportedOperationException();
	}

	public DocumentAttachId attachIdRemote() {
		return (DocumentAttachId) remote;
	}

	@Override
	protected void putRemote(ClientDomNode remote) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected ClientDomDocument remote() {
		return remote;
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
	protected void resetRemote0() {
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

	public interface DocumentContextProvider {
		Document contextDocument();

		void registerCreatedDocument(Document document);
	}

	public class DocumentImplAccess {
		public DocumentAttachId attachIdRemote() {
			return (DocumentAttachId) remote();
		}

		public Node getNode(AttachId attachId) {
			return localDom.attachIds.getNode(attachId);
		}
	}

	@Registration.Singleton(DomDocument.PerDocumentSupplier.class)
	public static class PerDocumentSupplierGwtImpl
			implements DomDocument.PerDocumentSupplier {
		@Override
		public DomDocument get(org.w3c.dom.Document document) {
			return ((Document) document).domDocument;
		}
	}

	public enum RemoteType {
		NONE, JSO, REF_ID;

		boolean hasRemote() {
			switch (this) {
			case NONE:
				return false;
			default:
				return true;
			}
		}
	}

	/*
	 * For communication between different doms, to ensure creates apply the
	 * same ids
	 */
	public void setNextAttachId(int id) {
		localDom.attachIds.setNextAttachId(id);
	}

	@Override
	public void invoke(Runnable runnable, Class clazz, String methodName,
			List<Class> argumentTypes, List<?> arguments, boolean sync) {
		ClientDomDocumentStatic.invoke(this, runnable, clazz, methodName,
				argumentTypes, arguments, sync);
	}

	@Override
	public Element getActiveElement() {
		return remote.getActiveElement();
	}

	public List<Element> querySelectorAll(String selector) {
		return remote.querySelectorAll(selector);
	}

	public Selection getSelection() {
		return selection;
	}

	public void onDocumentEventSystemInit() {
		selection.onDocumentEventSystemInit();
	}

	@Override
	public ClientDomSelection ensureRemoteSelection(Selection selection) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'ensureRemoteSelection'");
	}
}
