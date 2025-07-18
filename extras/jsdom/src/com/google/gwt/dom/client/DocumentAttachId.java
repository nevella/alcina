package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.ProcessingInstruction;

import com.google.gwt.dom.client.mutations.LocationMutation;
import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.SelectionRecord;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;

public class DocumentAttachId extends NodeAttachId
		implements ClientDomDocument {
	Document document;

	public MutationProxy mutationProxy;

	RemoteWindowState invokeProxy;

	// hack-ish - the element path is not necessarily determined at sink events
	// time
	List<Runnable> sinkEventsQueue = new ArrayList<>();

	private SelectionAttachId selection;

	public Topic<Element> topicAttachingElement = Topic.create();

	public DocumentAttachId(Document document) {
		super(document);
		this.document = document;
		this.invokeProxy = new RemoteWindowState();
	}

	public void registerToRemoteInvokeProxy(InvokeProxy invokeProxy) {
		this.invokeProxy.remoteDelegate = invokeProxy;
	}

	@Override
	public AnchorElement createAnchorElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public AreaElement createAreaElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public AudioElement createAudioElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BaseElement createBaseElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public QuoteElement createBlockQuoteElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createBlurEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BRElement createBRElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ButtonElement createButtonElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createButtonInputElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CanvasElement createCanvasElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableCaptionElement createCaptionElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CDATASection createCDATASection(String data) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createChangeEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createCheckInputElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createClickEvent(int detail, int screenX, int screenY,
			int clientX, int clientY, boolean ctrlKey, boolean altKey,
			boolean shiftKey, boolean metaKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableColElement createColElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableColElement createColGroupElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Comment createComment(String data) {
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
	public ModElement createDelElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public DivElement createDivElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public DListElement createDLElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element createElement(String tagName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createErrorEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FieldSetElement createFieldSetElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createFileInputElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createFocusEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FormElement createFormElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FrameElement createFrameElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FrameSetElement createFrameSetElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public HeadElement createHeadElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public HeadingElement createHElement(int n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createHiddenInputElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public HRElement createHRElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createHtmlEvent(String type, boolean canBubble,
			boolean cancelable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IFrameElement createIFrameElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ImageElement createImageElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createImageInputElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createInputEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ModElement createInsElement() {
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
	public LabelElement createLabelElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LegendElement createLegendElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LIElement createLIElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LinkElement createLinkElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createLoadEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MapElement createMapElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MetaElement createMetaElement() {
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
	public ObjectElement createObjectElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public OListElement createOLElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public OptGroupElement createOptGroupElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public OptionElement createOptionElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ParamElement createParamElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createPasswordInputElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ParagraphElement createPElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PreElement createPreElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ProcessingInstruction createProcessingInstruction(String target,
			String data) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ButtonElement createPushButtonElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public QuoteElement createQElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createRadioInputElement(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ButtonElement createResetButtonElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createResetInputElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScriptElement createScriptElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScriptElement createScriptElement(String source) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeEvent createScrollEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SelectElement createSelectElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SelectElement createSelectElement(boolean multiple) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SourceElement createSourceElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SpanElement createSpanElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public StyleElement createStyleElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ButtonElement createSubmitButtonElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createSubmitInputElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableElement createTableElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableSectionElement createTBodyElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableCellElement createTDElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TextAreaElement createTextAreaElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createTextInputElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Text createTextNode(String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableSectionElement createTFootElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableSectionElement createTHeadElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableCellElement createTHElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TitleElement createTitleElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TableRowElement createTRElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public UListElement createULElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String createUniqueId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public VideoElement createVideoElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Document documentFor() {
		throw new UnsupportedOperationException();
	}

	/*
	 * Doesn't attempt removal on node removal, since AttachIds are cheap
	 */
	Map<String, AttachId> elementsById = AlcinaCollections.newLinkedHashMap();

	/*
	 * this needs to populate the elementsById lookup
	 */
	@Override
	public void emitMutation(MutationRecord mutation) {
		switch (mutation.type) {
		case attributes:
			switch (mutation.attributeName) {
			case "id":
				elementsById.put(mutation.newValue, mutation.target.attachId);
				break;
			}
			break;
		case innerMarkup:
			mutation.target.node.stream().filter(Node::provideIsElement)
					.forEach(n -> {
						Element elem = (Element) n;
						String id = ((Element) n).getId();
						if (Ax.notBlank(id)) {
							elementsById.put(id, AttachId.forNode(elem));
						}
						ensureBehaviors(mutation, elem);
					});
			break;
		case childList:
			mutation.addedNodes.stream().map(MutationNode::node)
					.filter(Node::provideIsElement).forEach(n -> {
						Element elem = (Element) n;
						ensureBehaviors(mutation, elem);
					});
		}
		mutationProxy.onMutation(mutation);
	}

	void ensureBehaviors(MutationRecord mutation, Element elem) {
		topicAttachingElement.publish(elem);
		mutation.registerBehaviors(elem);
	}

	void addSunkEvent(Runnable runnable) {
		sinkEventsQueue.add(runnable);
	}

	public void emitSinkBitlessEvent(Element elem, String eventTypeName) {
		addSunkEvent(() -> {
			if (elem.isAttached()) {
				mutationProxy.onSinkBitlessEvent(AttachId.forNode(elem),
						eventTypeName);
			}
		});
	}

	public void emitSinkEvents(Element elem, int eventBits) {
		addSunkEvent(() -> {
			if (elem.isAttached()) {
				mutationProxy.onSinkEvents(AttachId.forNode(elem), eventBits);
			}
		});
	}

	@Override
	public void enableScrolling(boolean enable) {
		throw new UnsupportedOperationException();
	}

	public void flushSinkEventsQueue() {
		List<Runnable> list = sinkEventsQueue.stream()
				.collect(Collectors.toList());
		sinkEventsQueue.clear();
		list.forEach(Runnable::run);
	}

	@Override
	public BodyElement getBody() {
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
		return invokeSync("getClientHeight");
	}

	@Override
	public int getClientWidth() {
		return invokeSync("getClientWidth");
	}

	@Override
	public String getCompatMode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getDocumentElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDomain() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getElementById(String elementId) {
		AttachId attachId = elementsById.get(elementId);
		return attachId == null ? null : (Element) attachId.node();
	}

	@Override
	public NodeList<Element> getElementsByTagName(String tagName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public HeadElement getHead() {
		throw new UnsupportedOperationException();
	}

	@Override
	public short getNodeType() {
		throw new UnsupportedOperationException();
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
		// FIXME - romcom - optimise
		return invokeSync("getScrollLeft");
	}

	@Override
	public int getScrollTop() {
		// FIXME - romcom - optimise
		return invokeSync("getScrollTop");
	}

	@Override
	public int getScrollWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTitle() {
		// FIXME - romcom - optimise
		return invokeSync("getTitle");
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
		return invokeSync("hasFocus");
	}

	@Override
	public void importNode(Node node, boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexInParentChildren() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCSS1Compat() {
		throw new UnsupportedOperationException();
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
		invokeAsync("setTitle", List.of(String.class), List.of(title));
	}

	@Override
	public Document getOwnerDocument() {
		return document;
	}

	public interface InvokeProxy {
		@Reflected
		public enum Flag {
			invoke_on_element_style
		}

		void invoke(NodeAttachId node, String methodName,
				List<Class> argumentTypes, List<?> arguments, List<Flag> flags,
				AsyncCallback<?> callback);

		<T> T invokeSync(NodeAttachId node, String methodName,
				List<Class> argumentTypes, List<?> arguments,
				List<InvokeProxy.Flag> flags);

		default <T> T invokeSync(NodeAttachId node, String methodName) {
			return invokeSync(node, methodName, null, null, null);
		}

		/** this will be executed async, with no callback */
		<T> T invokeScript(Class clazz, String methodName,
				List<Class> argumentTypes, List<?> arguments);
	}

	public interface MutationProxy {
		void onLocationMutation(LocationMutation locationMutation);

		void onMutation(MutationRecord mutationRecord);

		void onSinkBitlessEvent(AttachId from, String eventTypeName);

		void onSinkEvents(AttachId from, int eventBits);
	}

	@Override
	public <T> T invoke(Supplier<T> supplier, Class clazz, String methodName,
			List<Class> argumentTypes, List<?> arguments, boolean sync) {
		return invokeProxy.invokeScript(clazz, methodName, argumentTypes,
				arguments);
	}

	@Override
	public void invoke(Runnable runnable, Class clazz, String methodName,
			List<Class> argumentTypes, List<?> arguments, boolean sync) {
		ClientDomDocumentStatic.invoke(this, runnable, clazz, methodName,
				argumentTypes, arguments, sync);
	}

	@Override
	public Element getActiveElement() {
		return invokeSync("getActiveElement");
	}

	@Override
	public List<Element> querySelectorAll(String selector) {
		throw new UnsupportedOperationException();
	}

	static Logger logger = LoggerFactory.getLogger(DocumentAttachId.class);

	/*
	 * Thread-safety - this can be accessed outsdie the environment thread (it's
	 * a swap)
	 */
	public void onRemoteUiContextReceived(WindowState windowState) {
		logger.debug("received window.state [observed offsets] {}",
				windowState.nodeUiStates.size());
		invokeProxy.windowState = windowState;
	}

	@Override
	public SelectionAttachId getSelection() {
		return selection;
	}

	@Override
	public ClientDomSelection ensureRemoteSelection(Selection selection) {
		this.selection = new SelectionAttachId(selection);
		return this.selection;
	}

	public void onSelectionMutationReceived(SelectionRecord selectionMutation) {
		getSelection().setSelectionRecord(selectionMutation);
	}

	public SelectionRecord getPendingSelectionMutationAndClear() {
		return getSelection().getPendingSelectionMutationAndClear();
	}

	@Override
	public void setActiveElement(Element elem) {
		invokeAsync("setActiveElement", List.of(Element.class), List.of(elem));
	}
}
