package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.ElementJso.ContiguousTextNodes;
import com.google.gwt.dom.client.ElementJso.ElementJsoIndex;
import com.google.gwt.dom.client.mutations.LocalMutations;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.RemoteMutations;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.context.ContextFrame;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Topic;

/**
 * <p>
 * FIXME - dirndl 1x3 - Refactoring needs - there's a lot of semi-duplication in
 * the 'link remote to localdom' models - i.e. puts to remoteLookup
 *
 * <p>
 * Probably need just one true path
 *
 * <p>
 * Notes re gc => we maintain a map of remote (browser dom) nodes to local
 * nodes, but that's weak. Strong refs are via node.remote fields
 *
 * <p>
 * Notes re issues with ex-gwt mutations - FIXME - dirndl 1x1e -
 * https://github.com/nevella/alcina/issues/23
 *
 * <p>
 * FIXME - dirndl - 1x3 - parsing externally, modified HTML requires
 * verification of text node code, which may mean that it’s more
 * efficient/simpler to just walk the dom
 *
 * <p>
 * FIXME - dirndl - 1x3 - document split of text nodes for browser compatibility
 * in {@link ElementLocal#checkSplitTextNodesForBrowserCompatibility},
 * {@link HtmlParser#appendTextNodes}
 *
 * <p>
 * FIXME - dirndl - 1x3 -
 * <ul>
 * <li>next steps: cleanup localdom.nodefor0 (abandon fallback, basically -since
 * we're toast by then anyway)
 * <li>edgecase - setting a localdom unconnected Text node to >65536 chars will
 * desync
 *
 * <p>
 * WIP - the maps may all go away if DomIds turns up trumps. Note that all elt
 * id refs must be cleared on onDetach - which may make SyncMutations harder
 * (since it'll need to be able to track exactly which subtrees were mutated)
 *
 */
public class LocalDom implements ContextFrame {
	// FIXME - remcom - move to Document
	public int maxCharsPerTextNode = 65536;

	public Topic<Exception> topicPublishException;

	public Topic<String> topicUnableToParse;

	Topic<Exception> topicReportException;

	private static LocalDomCollections collections;

	private static Map<String, String> declarativeCssNames;

	private static boolean disableRemoteWrite;

	// FIXME - localdom - remove (once there's better general logging)
	private static boolean logParseAndMutationIssues;

	private static Map<String, Supplier<Element>> elementCreators;

	static LocalDomCollections collections() {
		return collections;
	}

	static void consoleLog(String message) {
		if (LocalDomDebugImpl.debugAll) {
			consoleLog0(message);
		}
	}

	private static native void consoleLog(String message, boolean error) /*-{
    if (error) {
      console.error(message);
    } else {
      console.log(message);
    }
	}-*/;;

	static void consoleLog(Supplier<String> messageSupplier) {
		if (LocalDomDebugImpl.debugAll) {
			consoleLog0(messageSupplier.get());
		}
	}

	native static void consoleLog0(String message) /*-{
    console.log(message);

	}-*/;

	static Element createElement(String tagName) {
		return get().createElement0(tagName);
	}

	public static void debug(ElementJso elementJso) {
		get().debug0(elementJso);
	}

	synchronized static String declarativeCssName(String key) {
		return declarativeCssNames.computeIfAbsent(key, k -> {
			String lcKey = k.toLowerCase();
			if (!lcKey.equals(k)) {
				StringBuilder sb = new StringBuilder();
				for (int idx = 0; idx < k.length(); idx++) {
					char c = k.charAt(idx);
					if (c >= 'A' && c <= 'Z') {
						sb.append("-");
						sb.append(String.valueOf(c).toLowerCase());
					} else {
						sb.append(c);
					}
				}
				return sb.toString();
			} else {
				return k;
			}
		});
	}

	public static void ensureRemote(Node node) {
		get().ensureRemote0(node);
	}

	static void ensureRemoteDocument() {
		nodeFor(Document.get().jsoRemote().getDocumentElement0());
	}

	static <C extends ClientDomNode> C
			ensureRemoteNodeMaybePendingSync(Node node) {
		return (C) get().ensureRemoteNodeMaybePendingSync0(node);
	}

	public static void eventMod(NativeEvent evt, String eventName) {
		get().eventMod0(evt, eventName);
	}

	/**
	 * Flush any pending sync (sync local subtree to remote) jobs and mark the
	 * subtrees as synced
	 */
	public static void flush() {
		get().flush0();
	}

	public static void flushLocalMutations() {
		get().localMutations.fireMutations();
	}

	private static LocalDom get() {
		return Document.get().localDom;
	}

	public static LocalMutations getLocalMutations() {
		return get().localMutations;
	}

	public static RemoteMutations getRemoteMutations() {
		return get().remoteMutations;
	}

	static boolean hasNode(JavaScriptObject remote) {
		return get().remoteLookup.containsKey(remote);
	}

	public static void initalize() {
		disableRemoteWrite = !GWT.isClient();
		if (GWT.isScript()) {
			JavascriptKeyableLookup.initJs();
			collections = new LocalDomCollections_Script();
		} else {
			collections = new LocalDomCollections();
		}
		declarativeCssNames = collections.createStringMap();
		elementCreators = collections.createIdentityEqualsMap(String.class);
		initElementCreators();
	}

	public static void initalizeDetachedSync() {
		get().initalizeDetachedSync0();
	}

	private static void initElementCreators() {
		elementCreators.put(DivElement.TAG, () -> new DivElement());
		elementCreators.put(SpanElement.TAG, () -> new SpanElement());
		elementCreators.put(BodyElement.TAG, () -> new BodyElement());
		elementCreators.put(ButtonElement.TAG, () -> new ButtonElement());
		elementCreators.put(StyleElement.TAG, () -> new StyleElement());
		elementCreators.put(TableElement.TAG, () -> new TableElement());
		elementCreators.put(HeadElement.TAG, () -> new HeadElement());
		elementCreators.put(TableSectionElement.TAG_TBODY,
				() -> new TableSectionElement());
		elementCreators.put(TableSectionElement.TAG_TFOOT,
				() -> new TableSectionElement());
		elementCreators.put(TableSectionElement.TAG_THEAD,
				() -> new TableSectionElement());
		elementCreators.put(TableCaptionElement.TAG, () -> new HeadElement());
		elementCreators.put(TableCellElement.TAG_TD,
				() -> new TableCellElement());
		elementCreators.put(TableCellElement.TAG_TH,
				() -> new TableCellElement());
		elementCreators.put(TableColElement.TAG_COL,
				() -> new TableColElement());
		elementCreators.put(TableColElement.TAG_COLGROUP,
				() -> new TableColElement());
		elementCreators.put(TableRowElement.TAG, () -> new TableRowElement());
		elementCreators.put(InputElement.TAG, () -> new InputElement());
		elementCreators.put(TextAreaElement.TAG, () -> new TextAreaElement());
		elementCreators.put(HeadingElement.TAG_H1, () -> new HeadingElement());
		elementCreators.put(HeadingElement.TAG_H2, () -> new HeadingElement());
		elementCreators.put(HeadingElement.TAG_H3, () -> new HeadingElement());
		elementCreators.put(HeadingElement.TAG_H4, () -> new HeadingElement());
		elementCreators.put(HeadingElement.TAG_H5, () -> new HeadingElement());
		elementCreators.put(HeadingElement.TAG_H6, () -> new HeadingElement());
		elementCreators.put(AnchorElement.TAG, () -> new AnchorElement());
		elementCreators.put(ImageElement.TAG, () -> new ImageElement());
		elementCreators.put(LabelElement.TAG, () -> new LabelElement());
		elementCreators.put(ScriptElement.TAG, () -> new ScriptElement());
		elementCreators.put(SelectElement.TAG, () -> new SelectElement());
		elementCreators.put(OptionElement.TAG, () -> new OptionElement());
		elementCreators.put(IFrameElement.TAG, () -> new IFrameElement());
		elementCreators.put(UListElement.TAG, () -> new UListElement());
		elementCreators.put(OListElement.TAG, () -> new OListElement());
		elementCreators.put(LIElement.TAG, () -> new LIElement());
		elementCreators.put(PreElement.TAG, () -> new PreElement());
		elementCreators.put(ParagraphElement.TAG, () -> new ParagraphElement());
		elementCreators.put(BRElement.TAG, () -> new BRElement());
		elementCreators.put(HRElement.TAG, () -> new HRElement());
		elementCreators.put(FormElement.TAG, () -> new FormElement());
		// svg
		elementCreators.put(MapElement.TAG, () -> new MapElement());
		elementCreators.put(ParamElement.TAG, () -> new ParamElement());
		elementCreators.put(OptGroupElement.TAG, () -> new OptGroupElement());
		elementCreators.put(QuoteElement.TAG_BLOCKQUOTE,
				() -> new QuoteElement());
		elementCreators.put(QuoteElement.TAG_Q, () -> new QuoteElement());
		elementCreators.put(TableCaptionElement.TAG,
				() -> new TableCaptionElement());
		elementCreators.put(DListElement.TAG, () -> new DListElement());
		elementCreators.put(TitleElement.TAG, () -> new TitleElement());
		elementCreators.put(FieldSetElement.TAG, () -> new FieldSetElement());
		elementCreators.put(FrameSetElement.TAG, () -> new FrameSetElement());
		elementCreators.put(MetaElement.TAG, () -> new MetaElement());
		elementCreators.put(SourceElement.TAG, () -> new SourceElement());
		elementCreators.put(LinkElement.TAG, () -> new LinkElement());
		elementCreators.put(ObjectElement.TAG, () -> new ObjectElement());
		elementCreators.put(ModElement.TAG_INS, () -> new ModElement());
		elementCreators.put(ModElement.TAG_DEL, () -> new ModElement());
		elementCreators.put(BaseElement.TAG, () -> new BaseElement());
		elementCreators.put(FrameElement.TAG, () -> new FrameElement());
		elementCreators.put(AreaElement.TAG, () -> new AreaElement());
		elementCreators.put(LegendElement.TAG, () -> new LegendElement());
	}

	public static void invokeExternal(Runnable runnable) {
		get().invokeExternal0(runnable);
	}

	static boolean isApplyToRemote() {
		return get().isApplyToRemote0();
	}

	public static boolean isPending(NodeRemote nodeRemote) {
		return get().isPending0(nodeRemote);
	}

	public static boolean isStopPropagation(NativeEvent evt) {
		return get().isStopPropagation0(evt);
	}

	static boolean isUseRemoteDom() {
		return GWT.isClient();
	}

	public static void log(Level level, String template, Object... args) {
		boolean error = level.intValue() > Level.INFO.intValue();
		if (!error && !get().loggingConfiguration.logEvents) {
			return;
		}
		String message = Ax.format(template, args);
		if (error) {
			Ax.err(message);
		} else {
			Ax.out(message);
		}
		if (Al.isBrowser()) {
			consoleLog(message, error);
		}
	}

	public static <T extends Node> T nodeFor(JavaScriptObject jso) {
		return nodeFor((NodeJso) jso);
	}

	public static <T extends Node> T nodeFor(NodeJso remote) {
		return (T) get().nodeFor0(remote);
	}

	/**
	 * Get the node corresponding to the nodeJso if it already exists, but don't
	 * attempt to compute it by ascending the remote tree
	 */
	static Node nodeForNoResolve(NodeJso nodeJso) {
		return get().remoteLookup.get(nodeJso);
	}

	public static void notifyLocalMutations(Runnable runnable) {
		LocalMutations localMutations = get().localMutations;
		if (localMutations == null) {
			return;
		}
		localMutations.notify(runnable);
	}

	public static void onRelatedException(RuntimeException e) {
		if (logParseAndMutationIssues) {
			throw e;
		} else {
			// devmode only
			Ax.simpleExceptionOut(e);
		}
	}

	public static PathRefRepresentations pathRefRepresentations() {
		return get().pathRefRepresentations;
	}

	static void putRemote(Element element, ElementJso remote) {
		get().putRemote0(element, remote);
	}

	public static void register(Document doc) {
		if (GWT.isClient()) {
			get().initalizeRemoteSync(doc);
			doc.getDocumentElement().setAttached(true);
		}
	}

	public static Node resolveExternal(NodeJso nodeJso) {
		return get().resolveExternal0(nodeJso);
	}

	static String safeParseByBrowser(String html) {
		ElementJso remote = Document.get().jsoRemote()
				.generateFromOuterHtml(html);
		return remote.buildOuterHtml();
	}

	public static void setDisableRemoteWrite(boolean disableRemoteWrite) {
		LocalDom.disableRemoteWrite = disableRemoteWrite;
	}

	public static void setSyncing(boolean syncing) {
		get().syncing = syncing;
	}

	public static void syncToRemote(Element element) {
		get().parseAndMarkSynced(element.jsoRemote(),
				element.jsoRemote().getOuterHtml(), element);
	}

	public static void triggerLocalDomException() {
		get().topicReportException
				.publish(new Exception("test exception trigger"));
	}

	public static String validateHtml(String html) {
		return get().validateHtml0(html);
	}

	public static void verifyDomEquivalence(boolean fromUserGesture) {
		boolean logEvents = get().loggingConfiguration.logEvents;
		try {
			if (fromUserGesture) {
				get().loggingConfiguration.logEvents = true;
			}
			get().remoteMutations.verifyDomEquivalence();
		} catch (Exception e) {
			e.printStackTrace();
			get().topicReportException.publish(e);
		} finally {
			if (fromUserGesture) {
				get().loggingConfiguration.logEvents = logEvents;
			}
		}
	}

	/**
	 * Check that it's valid to mutate (if non-localdom javascript is
	 * manipulating NodeJso instances, the MutationObserver must be connected -
	 * and the converse)
	 */
	static void verifyMutatingState() {
		get().verifyMutatingState0();
	}

	static void wasSynced(Element elem) {
		get().wasSynced0(elem);
	}

	private PathRefRepresentations pathRefRepresentations = new PathRefRepresentations();

	boolean applyToRemote = true;

	RemoteMutations remoteMutations;

	LocalMutations localMutations;

	private LoggingConfiguration loggingConfiguration;

	public BrowserBehaviour browserBehaviour;

	private DocumentJso docRemote;

	LocalDomDebugImpl debugImpl = new LocalDomDebugImpl();

	private Map<NodeJso, Node> remoteLookup;

	Map<NativeEvent, List<String>> eventMods = new LinkedHashMap<>();

	List<Node> pendingSync = new ArrayList<>();

	ScheduledCommand flushCommand = null;

	private int syncEventId = 1;

	private boolean syncEventIdDirty;

	boolean syncing;

	boolean markNonStructuralNodesAsSyncedOnSync;

	DomIds domIds;

	LocalDom() {
		if (GWT.isScript()) {
			remoteLookup = JsUniqueMap.createWeakMap();
		} else {
			remoteLookup = AlcinaCollections.newWeakMap();
		}
		topicPublishException = Topic.create();
		topicReportException = Topic.create();
		topicUnableToParse = Topic.create();
		topicReportException.add(this::handleReportedException);
		domIds = new DomIds();
	}

	void applyContiguousTextNodesToLocal(ContiguousTextNodes contiguous) {
		/*
		 * will be either text or comment, and will contain the full text
		 * content of [contiguous.previous,contiguousnode] + possibly content
		 * after [contiguous.node]
		 */
		NodeIndex previousIndex = NodeIndex.forNode(contiguous.previous);
		Node previousNode = previousIndex.getNode();
		Node parent = previousNode.getParentNode();
		NodeLocal contiguousLocal = null;
		Node created = createAndInsertAfter(parent, previousNode,
				contiguous.node.getNodeType(), contiguous.node.getNodeName(),
				contiguous.node.getNodeValue(), contiguous.node);
		String previousLocalText = previousNode.getTextContent();
		String remotePreviousTextContent = contiguous.previous.getNodeValue();
		previousNode.setTextContent(remotePreviousTextContent);
		created.setTextContent(previousLocalText
				.substring(remotePreviousTextContent.length()));
	}

	Node createAndInsertAfter(Node parentNode, Node previousSibling,
			short nodeType, String nodeName, String nodeValue,
			NodeJso remoteNode) {
		ElementLocal parent = parentNode.local();
		Node newChild = null;
		switch (nodeType) {
		case Node.COMMENT_NODE:
			newChild = parent.ownerDocument.createComment(nodeValue);
			break;
		case Node.TEXT_NODE:
			newChild = parent.ownerDocument.createTextNode(nodeValue);
			break;
		case Node.ELEMENT_NODE:
			newChild = parent.ownerDocument.createElement(nodeName);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		// FIXME - remoteNode
		if (remoteNode != null) {
			newChild.putRemote(remoteNode, false);
		}
		parentNode.insertAfter(newChild, previousSibling);
		if (remoteNode != null) {
			linkRemote(remoteNode, newChild);
		}
		return newChild;
	}

	private Element createElement0(String tagName) {
		Supplier<Element> creator = elementCreators.get(tagName.toLowerCase());
		if (creator == null) {
			return new Element();
		} else {
			return creator.get();
		}
	}

	private void debug0(ElementJso elementJso) {
		int debug = 4;
	}

	private void ensureFlush() {
		if (flushCommand == null && GWT.isClient()) {
			flushCommand = () -> flush();
			Scheduler.get().scheduleFinally(flushCommand);
		}
	}

	public void ensurePendingSynced(Node node) {
		Preconditions.checkState(node.linkedToRemote());
		Element element = (Element) node;
		if (element.isPendingSync()) {
			ClientDomElement local = node.local();
			localToRemote(element, node.remote(), local);
		}
	}

	private void ensureRemote0(Node node) {
		if (isPathref()) {
			// FIXME - localdom - factor a bunch of this out into
			// LDjso/LDpathref classes
			ensureRemote0Pathref(node);
			return;
		}
		flush0(true);
		List<Node> ancestors = new ArrayList<>();
		Node cursor = node;
		Node withRemote = null;
		while (cursor != null) {
			if (cursor.linkedToRemote()) {
				withRemote = cursor;
				break;
			} else {
				ancestors.add(cursor);
				cursor = cursor.getParentElement();
			}
		}
		Collections.reverse(ancestors);
		if (withRemote == null) {
			// attaching child with-remote to without-remote (say, a popup)
			Node root = ancestors.get(0);
			ensureRemoteNodeMaybePendingSync(root);
			ensureRemote0(node);
			return;
		}
		for (Node needsRemote : ancestors) {
			int idx = needsRemote.indexInParentChildren();
			if (needsRemote instanceof Element
					&& withRemote instanceof Element) {
				debugImpl.debugPutRemote((Element) needsRemote, idx,
						(Element) withRemote);
			}
			NodeJso remote = withRemote.jsoRemote().getChildNodes0()
					.getItem0(idx);
			linkRemote(remote, needsRemote);
			needsRemote.putRemote(remote, true);
			withRemote = needsRemote;
		}
	}

	private void ensureRemote0Pathref(Node node) {
		Node cursor = node;
		while (cursor != null) {
			if (cursor.linkedToRemote()) {
				break;
			} else {
				cursor.putRemote(NodePathref.create(node), cursor.wasSynced());
			}
			cursor = cursor.getParentNode();
		}
	}

	private ClientDomNode ensureRemoteNodeMaybePendingSync0(Node node) {
		if (node.linkedToRemote()) {
			return node.remote();
		}
		ensureFlush();
		ClientDomNode remote = null;
		int nodeType = node.getNodeType();
		switch (nodeType) {
		case Node.ELEMENT_NODE:
			Element element = (Element) node;
			if (isPathref()) {
				remote = NodePathref.create(node);
			} else {
				remote = ((DomDispatchJso) DOMImpl.impl.remote())
						.createElement(element.getTagName());
			}
			element.pendingSync();
			pendingSync.add(node);
			break;
		case Node.TEXT_NODE:
			remote = Document.get().jsoRemote()
					.createTextNode0(((Text) node).getData());
			break;
		// case Node.DOCUMENT_NODE:
		// nodeDom = doc.domImpl();
		// break;
		default:
			throw new UnsupportedOperationException();
		}
		if (remote.isJso()) {
			linkRemote((NodeJso) remote, node);
		}
		node.putRemote(remote, false);
		return remote;
	}

	private void eventMod0(NativeEvent evt, String eventName) {
		if (!eventMods.keySet().contains(evt)) {
			eventMods.clear();
			eventMods.put(evt, new ArrayList<>());
		}
		eventMods.get(evt).add(eventName);
	}

	void flush0() {
		flush0(false);
	}

	void flush0(boolean force) {
		if (syncing) {
			return;
		}
		if (flushCommand == null && !force) {
			return;
		}
		flushCommand = null;
		try {
			syncing = true;
			if (syncEventIdDirty) {
				syncEventId++;
				syncEventIdDirty = false;
			}
			new ArrayList<>(pendingSync).stream()
					.forEach(this::ensurePendingSynced);
			if (syncEventIdDirty) {
				syncEventId++;
			}
		} catch (RuntimeException re) {
			topicReportException.publish(re);
			throw re;
		} finally {
			syncEventIdDirty = false;
			syncing = false;
		}
		/*
		 * REVISIT - there are a few considerations here:
		 * 
		 * The more general question is
		 * "when to group events, and flush their consequents, and when to flush one-by-one"
		 * . The latter is ... generally better, but does mean local/remote are
		 * out of sync.
		 * 
		 * That said, flush() is pretty rare (pre-tree sync is the non-obvious
		 * time it's called), so - what's below is an ok first approximation.
		 */
		if (get().applyToRemote) {
			localMutations.fireMutations();
		} else {
			localMutations.notify(() -> {
				// noop, just trigger a finally flush of mutations
			});
		}
	}

	void handleReportedException(Exception exception) {
		String message = null;
		if (loggingConfiguration.logHistoryOnEception) {
			try {
				message = remoteMutations.serializeHistory();
			} catch (Exception serializeException) {
				message = CommonUtils
						.toSimpleExceptionMessage(serializeException);
			}
		}
		log(Level.WARNING, "local dom :: %s",
				CommonUtils.toSimpleExceptionMessage(exception));
		topicPublishException
				.publish(new LocalDomException(exception, message));
	}

	private void initalizeDetachedSync0() {
		remoteMutations = new RemoteMutations(new MutationsAccess(),
				new RemoteMutations.LoggingConfiguration());
		localMutations = new LocalMutations(new MutationsAccess());
		loggingConfiguration = new LoggingConfiguration();
	}

	private void initalizeRemoteSync(Document doc) {
		docRemote = doc.jsoRemote();
		loggingConfiguration = new LoggingConfiguration();
		browserBehaviour = new BrowserBehaviour();
		browserBehaviour.test();
		linkRemote(docRemote, doc);
		localMutations = new LocalMutations(new MutationsAccess());
		nodeFor0(docRemote.getDocumentElement0());
		remoteMutations = new RemoteMutations(new MutationsAccess(),
				loggingConfiguration.asMutationsConfiguration());
	}

	private void invokeExternal0(Runnable runnable) {
		// DEV mode check (all script calls are guaranteed within Impl)
		// don't allow mutating ext js calls in the very first (onModuleLoad)
		// script cycle - since disconnect/connect cycle is not setup
		Preconditions.checkState(GWT.isScript() || !Impl.isFirstTimeClient());
		if (loggingConfiguration.mutationLogDoms) {
			verifyDomEquivalence(false);
		}
		flush();
		try {
			remoteMutations.startObserving();
			runnable.run();
		} finally {
			remoteMutations.syncMutationsAndStopObserving();
		}
	}

	private boolean isApplyToRemote0() {
		return applyToRemote;
	}

	boolean isPathref() {
		return Document.get().remoteType == RemoteType.PATHREF;
	}

	private boolean isPending0(NodeRemote nodeRemote) {
		return pendingSync.size() > 0
				&& pendingSync.stream().anyMatch(n -> n.remote() == nodeRemote);
	}

	private boolean isStopPropagation0(NativeEvent evt) {
		List<String> list = eventMods.get(evt);
		return list != null && (list.contains("eventStopPropagation")
				|| list.contains("eventCancelBubble"));
	}

	private void linkRemote(NodeJso remote, Node node) {
		Preconditions.checkState(!remoteLookup.containsKey(remote));
		remoteLookup.put(remote, node);
	}

	private void localToRemote(Element element, ElementRemote remote,
			ClientDomElement local) {
		pendingSync.remove(element);
		if (remote instanceof ElementJso) {
			String innerMarkup = local.getInnerHTML();
			remote.setInnerHTML(innerMarkup);
			// FIXME - dom-node-identity - setrefids. first with java, then an
			// optimised devmode version
			// that traversal will also coalesce text nodes
			ElementJso j_remote = (ElementJso) remote;
			// doesn't include style
			local.getAttributeMap().entrySet().forEach(e -> {
				String value = e.getValue();
				switch (e.getKey()) {
				case "text":
					j_remote.setPropertyString(e.getKey(), value);
					break;
				default:
					j_remote.setAttribute(e.getKey(), value);
					break;
				}
			});
			local.getStyle().getProperties().entrySet().forEach(e -> {
				StyleRemote remoteStyle = j_remote.getStyle0();
				remoteStyle.setProperty(e.getKey(), e.getValue());
			});
		} else {
			if (!element.isAttached()) {
				return;
			} else {
				remoteMutations.emitInnerMarkupMutation(element);
			}
		}
		int bits = ((ElementLocal) local).orSunkEventsOfAllChildren(0);
		bits |= DOM.getEventsSunk(element);
		DOM.sinkEvents(element, bits);
		Set<String> bitlessEventsSunk = new LinkedHashSet<>();
		((ElementLocal) local)
				.orSunkBitlessEventsOfAllChildren(bitlessEventsSunk);
		bitlessEventsSunk.forEach(eventTypeName -> {
			DOM.sinkBitlessEvent(element, eventTypeName);
		});
		element.resolvePendingSync();
		wasSynced0(element);
	}

	private <T extends Node> T nodeFor0(NodeJso remote) {
		return nodeFor0(remote, false);
	}

	private <T extends Node> T nodeFor0(NodeJso remote, boolean postReparse)
			throws LocalDomException {
		try {
			return nodeFor1(remote, postReparse);
		} catch (RuntimeException re) {
			topicReportException.publish(re);
			throw new LocalDomException(re);
		}
	}

	private <T extends Node> T nodeFor1(NodeJso remote, boolean postReparse) {
		if (remote == null) {
			return null;
		}
		T node = (T) remoteLookup.get(remote);
		if (node != null) {
			return node;
		}
		if (remote.provideIsNonStructural()) {
			// FIXME - dirndl 1x1e - non-performant, but rare (exception
			// for selectionish)
			ElementJso parentRemote = (ElementJso) remote.getParentNodeJso();
			Node parent = nodeFor0(parentRemote);
			int index = remote.indexInParentChildren();
			if (parent.getChildCount() == parentRemote.getChildCount()) {
				Node childNode = parent.getChild(index);
				linkRemote(remote, childNode);
				// FIXME - dirndl - localdom - syncmutations doesn't completely
				// handle this - e.g. bold/unbold (with keyboard) of
				// contenteditable - so added || true for now
				if ((markNonStructuralNodesAsSyncedOnSync || true)
						&& !childNode.wasSynced()) {
					childNode.onSync(syncEventId);
				}
				childNode.putRemote(remote, true);
				return (T) childNode;
			} else {
				/*
				 * TODO - LDM2 - this should never be the case (since structures
				 * should be in sync) - probably rework this method in line with
				 * "what are the whole-model preconditions'
				 *
				 */
				if (postReparse) {
					topicUnableToParse.publish(Ax.format(
							"Text node reparse - remote:\n%s\n\nlocal:\n%s\n",
							parentRemote.getOuterHtml(),
							((Element) parent).getOuterHtml()));
					throw new RuntimeException("Text node reparse");
				}
				ElementJsoIndex remoteIndex = parentRemote
						.provideRemoteIndex(false);
				ElementJso hasNodeJso = remoteIndex.hasNode();
				reparseFromRemote(hasNodeJso, (Element) parent, remoteIndex);
				return nodeFor0(remote, true);
			}
		}
		if (!remote.provideIsElement()) {
			return null;// say, shadowroot...
		}
		if (remote.getNodeName().equalsIgnoreCase("iframe")) {
			return null;// SEP
		}
		ElementJso elem = (ElementJso) remote;
		ElementJsoIndex remoteIndex = elem.provideRemoteIndex(false);
		ElementJso hasNodeJso = remoteIndex.hasNode();
		boolean hadNode = hasNodeJso != null;
		if (hasNodeJso == null) {
			ElementJso root = remoteIndex.root();
			Element hasNode = parseAndMarkSynced(root, root.getOuterHtml(),
					null);
			linkRemote(root, hasNode);
			hasNode.putRemote(root, true);
			hasNodeJso = root;
		}
		Element hasNode = (Element) remoteLookup.get(hasNodeJso);
		// if this returns true, we knew the remote element has DOM manipulated
		// outside GWT - parse the tree
		if (hasNode.resolveRemoteDefined()) {
			return nodeFor0(remote);
		}
		// htmlparser will sometimes fail to parse dodgy DOM - reparse from
		// browser DOM
		if (isApplyToRemote()
				&& shouldTryReparseFromRemote(elem, hasNode, remoteIndex)
				&& !postReparse) {
			/*
			 * FIXME - ldm2 - this clobbers event handlers. So is an exception
			 * (effectively) in generated DOM (but not in parsed HTML)
			 *
			 * FIXME - ldm2 - one cause of this is the following: <a
			 * type='outer'><a type='inner></a></a> - it might be best to throw
			 * during localdom generation with a construct like this
			 *
			 * Call mutations.verifyDomEquivalence(); if debugging needed
			 */
			Ax.err(">> Reparsing from remote - will remove event handlers");
			// FIXME - romcom - should throw (emit a topic message, handled by
			// client)
			if (!GWT.isScript()) {
				// remoteMutations.verifyDomEquivalence();
			}
			reparseFromRemote(hasNodeJso, hasNode, remoteIndex);
			return nodeFor0(remote, true);
		}
		List<Integer> indicies = remoteIndex.indicies();
		List<Boolean> remoteDefined = remoteIndex.remoteDefined();
		JsArray ancestors = remoteIndex.ancestors();
		debugImpl.debugNodeFor(elem, hasNode, remoteIndex, true);
		Element cursor = hasNode;
		for (int idx = indicies.size() - 1; idx >= 0; idx--) {
			int nodeIndex = indicies.get(idx);
			cursor.resolveRemoteDefined();
			Element child = (Element) cursor.getChild(nodeIndex);
			NodeJso childRemote = (NodeJso) ancestors.get(idx);
			linkRemote(childRemote, child);
			child.putRemote(childRemote, true);
			cursor = child;
		}
		debugImpl.debugNodeFor(elem, hasNode, remoteIndex, false);
		return (T) remoteLookup.get(remote);
	}

	private Element parseAndMarkSynced(ElementJso root, String outerHtml,
			Element replaceContents) {
		Element parsed = null;
		try {
			parsed = new HtmlParser().parse(outerHtml, replaceContents,
					root == Document.get().jsoRemote().getDocumentElement0());
		} catch (Exception e) {
			// TODO - possibly log. But maybe not - full support of dodgy dom wd
			// be truly hard
			// FIXME - dirndl 1x3 - DEVEX (or retire - IE legacy?)
			e.printStackTrace();
			parsed = new HtmlParser().parse(safeParseByBrowser(outerHtml),
					replaceContents,
					root == Document.get().jsoRemote().getDocumentElement0());
		}
		if (parsed != null) {
			if (replaceContents != null) {
				replaceContents.getAttributeMap().keySet().stream()
						.collect(Collectors.toList())
						.forEach(replaceContents::removeAttribute);
				root.getAttributeMap()
						.forEach((k, v) -> replaceContents.setAttribute(k, v));
			}
			wasSynced0(parsed);
			root.getContiguousTextContainers()
					.forEach(this::applyContiguousTextNodesToLocal);
		} else {
			topicUnableToParse.publish(outerHtml);
		}
		return parsed;
	}

	private void putRemote0(Element element, ElementJso remote) {
		flush();
		syncEventId++;
		wasSynced(element);
		remoteLookup.put(remote, element);
		element.putRemote(remote, true);
	}

	private void reparseFromRemote(ElementJso elem, Element hasNode,
			ElementJsoIndex remoteIndex) {
		List<Integer> sizes = remoteIndex.sizes();
		List<Integer> indicies = remoteIndex.indicies();
		boolean sizesMatch = true;
		Element cursor = hasNode;
		ElementJso remoteCursor = elem;
		for (int idx = sizes.size() - 1; idx >= 0; idx--) {
			int size = sizes.get(idx);
			boolean invalid = cursor.getChildCount() != size;
			Node node = null;
			NodeJso remoteNode = null;
			if (!invalid) {
				int nodeIndex = indicies.get(idx);
				node = cursor.getChild(nodeIndex);
				remoteNode = remoteCursor.getChildNodes0().getItem0(nodeIndex);
				invalid = node.getNodeType() != remoteNode.getNodeType();
			}
			if (invalid) {
				/*
				 * REVISIT - directedlayout.2. Optimised?
				 *
				 * check we have no widgets in the tree - if we do,
				 * we're...not..good. Also remove and remote refs below (albeit
				 * unlikely)
				 *
				 */
				int localIndex = (cursor.getParentElement() == null ? cursor
						: cursor.getParentElement()).getChildIndexLocal(cursor);
				cursor.local().clearChildrenAndAttributes0();
				String builtOuterHtml = remoteCursor.buildOuterHtml();
				String remoteOuterHtml = remoteCursor.getOuterHtml();
				parseAndMarkSynced(remoteCursor, builtOuterHtml, cursor);
				invalid = cursor.getChildCount() != size;
				if (!invalid) {
					int nodeIndex = indicies.get(idx);
					node = cursor.getChild(nodeIndex);
					remoteNode = remoteCursor.getChildNodes0()
							.getItem0(nodeIndex);
					invalid = node.getNodeType() != remoteNode.getNodeType();
				}
				if (invalid) {
					String preface = Ax.format(
							"sizes: %s\nsizeIdx:%s\nlocalIndex: %s\n"
									+ "(local) cursor.childCount: %s\nremoteSize:%s\n",
							sizes, idx, localIndex, cursor.getChildCount(),
							size);
					if (cursor.getChildCount() != size) {
						preface += "size mismatch\n";
					} else {
						int nodeIndex = indicies.get(idx);
						node = cursor.getChild(nodeIndex);
						remoteNode = remoteCursor.getChildNodes0()
								.getItem0(nodeIndex);
						preface += Ax.format("local node:%s\nremote node:%s\n",
								node, remoteNode);
					}
					try {
						preface += Ax.format("Remote index:\n%s\n",
								remoteIndex.getString());
					} catch (Exception e) {
						preface += Ax.format(
								"Exception getting remoteIndex:\n%s\n",
								e.toString());
					}
					// FIXME - localdom Exception logging in this fashion can
					// cause very
					// large strings and cause an OOM on the server
					// preface += Ax.format("Local dom tree:\n%s\n",
					// hasNode.local().provideLocalDomTree());
					// preface += Ax.format("(Local outer html):\n%s\n",
					// hasNode.local().getOuterHtml());
					// String message = Ax.format(
					// "%s\n(Built outer html):\n%s\n\n(Remote outer
					// html):\n%s",
					// preface, builtOuterHtml, remoteOuterHtml);
					// topicUnableToParse.publish(message);
					topicUnableToParse.publish(preface);
					Ax.out("Reparse unsuccessful");
					return;
				}
			}
			cursor = (Element) node;
			remoteCursor = (ElementJso) remoteNode;
		}
		Ax.out("Reparse successful");
	}

	public Node resolveExternal0(NodeJso nodeJso) {
		switch (nodeJso.getNodeType()) {
		case Node.ELEMENT_NODE:
			ElementJso elementJso = (ElementJso) nodeJso;
			Element element = Document.get().local()
					.createElement(elementJso.getTagNameRemote());
			element.putRemote(nodeJso, false);
			syncToRemote(element);
			linkRemote(elementJso, element);
			return element;
		case Node.TEXT_NODE:
			Text textNode = Document.get()
					.createTextNode(nodeJso.getNodeValue());
			textNode.putRemote(nodeJso, true);
			return textNode;
		default:
			throw new UnsupportedOperationException();
		}
	}

	private boolean shouldTryReparseFromRemote(ElementJso elem, Element hasNode,
			ElementJsoIndex remoteIndex) {
		if (remoteIndex.hasRemoteDefined()) {
			return false;
		}
		List<Integer> sizes = remoteIndex.sizes();
		List<Integer> indicies = remoteIndex.indicies();
		boolean sizesMatch = true;
		Element cursor = hasNode;
		for (int idx = sizes.size() - 1; idx >= 0; idx--) {
			int size = sizes.get(idx);
			if (cursor.getChildCount() != size) {
				sizesMatch = false;
				break;
			}
			int nodeIndex = indicies.get(idx);
			Node node = cursor.getChild(nodeIndex);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				return true;
			}
			cursor = (Element) node;
		}
		if (sizesMatch) {
			return false;
		}
		return true;
	}

	private String validateHtml0(String html) {
		DivElement div = Document.get().createDivElement();
		ensureRemote(div);
		ElementJso typedRemote = div.implAccess().jsoRemote();
		typedRemote.setInnerHTML(html);
		try {
			return typedRemote.getInnerHTML0();
		} catch (Exception e) {
			topicReportException.publish(e);
			return html;
		}
	}

	private void verifyMutatingState0() {
		Preconditions
				.checkArgument(syncing || (remoteMutations.isObserverConnected()
						|| !remoteMutations.isEnabled()));
	}

	private void wasSynced0(Element elem) {
		elem.local().walk(nl -> nl.node().onSync(syncEventId));
		syncEventIdDirty = true;
	}

	public class BrowserBehaviour {
		public int maxCharsPerTextNode = 65536;

		/*
		 * Try up to 2^20 chars
		 */
		public void test() {
			// 16 chars
			String seed = "0test1test2test3";
			String lengthTest = seed;
			int test = 1 << 20;
			while (lengthTest.length() < test) {
				lengthTest = lengthTest + lengthTest;
			}
			FormatBuilder format = new FormatBuilder();
			ElementJso div = docRemote.createElementNode0("div");
			TextJso text = docRemote.createTextNode0(lengthTest);
			div.appendChild0(text);
			int childNodesLengthNodeOperation = div.getChildNodes0()
					.getLength();
			div.setInnerHTML(lengthTest);
			int childNodesLengthHtmlOperation = div.getChildNodes0()
					.getLength();
			if (childNodesLengthHtmlOperation == 1) {
				maxCharsPerTextNode = Integer.MAX_VALUE;
			} else {
				maxCharsPerTextNode = div.getChildNodes0().getItem0(0)
						.getNodeValue().length();
			}
			log(Level.INFO,
					"test text length: %s\n\tchildNodesLengthNodeOperation: %s"
							+ "\n\tchildNodesLengthHtmlOperation: %s\n\tmaxCharsPerTextNode: %s",
					lengthTest.length(), childNodesLengthNodeOperation,
					childNodesLengthHtmlOperation, maxCharsPerTextNode);
			LocalDom.setMaxCharsPerTextNode(maxCharsPerTextNode);
		}
	}

	public static class LocalDomCollections {
		public <K, V> Map<K, V> createIdentityEqualsMap(Class<K> keyClass) {
			return new LinkedHashMap<>();
		}

		public Map<String, String> createStringMap() {
			return createIdentityEqualsMap(String.class);
		}
	}

	public static class LocalDomCollections_Script extends LocalDomCollections {
		@Override
		public <K, V> Map<K, V> createIdentityEqualsMap(Class<K> keyClass) {
			return JsUniqueMap.create();
		}
	}

	public static class LoggingConfiguration {
		public boolean mutationLogDoms;

		public boolean mutationLogEvents = !!GWT.isScript();

		public boolean logEvents = !GWT.isScript();

		public boolean logHistoryOnEception = true;

		public LoggingConfiguration() {
			// mutationLogDoms = ClientProperties.is(LocalDom.class,
			// "mutationLogDoms", false);
			// mutationLogEvents = ClientProperties.is(LocalDom.class,
			// "mutationLogEvents", !GWT.isScript());
			// logEvents = ClientProperties.is(LocalDom.class, "logEvents",
			// !GWT.isScript());
			// logHistoryOnEception = ClientProperties.is(LocalDom.class,
			// "logHistoryOnEception", true);
		}

		public RemoteMutations.LoggingConfiguration asMutationsConfiguration() {
			RemoteMutations.LoggingConfiguration result = new RemoteMutations.LoggingConfiguration();
			result.logDoms = mutationLogDoms;
			result.logEvents = mutationLogEvents;
			return result;
		}
	}

	public class MutationsAccess {
		public Node createAndInsertAfter(Node target, Node previousSibling,
				short nodeType, String nodeName, String nodeValue,
				NodeJso remoteNode) {
			return LocalDom.this.createAndInsertAfter(target, previousSibling,
					nodeType, nodeName, nodeValue, remoteNode);
		}

		public Element elementForNoResolve(ElementJso remote) {
			return (Element) nodeForNoResolve(remote);
		}

		public void markAsSynced(NodeJso ancestor) {
			try {
				markNonStructuralNodesAsSyncedOnSync = true;
				LocalDom.nodeFor(ancestor);
			} finally {
				markNonStructuralNodesAsSyncedOnSync = false;
			}
		}

		public Node nodeForNoResolve(NodeJso remote) {
			return LocalDom.nodeForNoResolve(remote);
		}

		public NodeJso parentNoResolve(NodeJso cursor) {
			return cursor.getParentNodeJso();
		}

		public void putRemoteChildren(Element elem,
				List<NodeJso> remoteChildrenS0) {
			NodeList<Node> childNodes = elem.getChildNodes();
			for (int idx = 0; idx < remoteChildrenS0.size(); idx++) {
				Node child = childNodes.getItem(idx);
				NodeJso remote = remoteChildrenS0.get(idx);
				// not sure about synced here...
				child.putRemote(remote, child.wasSynced());
				if (!remoteLookup.containsKey(remote)) {
					linkRemote(remote, child);
				}
			}
		}

		public void removeFromRemoteLookup(Node delta) {
			Stack<Node> stack = new Stack<>();
			stack.push(delta);
			do {
				Node node = stack.pop();
				remoteLookup.remove(node.remote());
				node.streamChildren().forEach(stack::push);
			} while (stack.size() > 0);
		}

		public void reportException(Exception exception) {
			topicReportException.publish(exception);
		}

		public void setApplyToRemote(boolean applyToRemote) {
			get().applyToRemote = applyToRemote;
		}

		public Stream<NodeJso> streamChildren(NodeJso node) {
			return node.getChildNodes0().streamRemote();
		}

		public Stream<NodeJso> streamRemote(NodeListJso<Node> nodeListRemote) {
			return nodeListRemote.streamRemote();
		}

		public NodeJso typedRemote(Node n) {
			return n.jsoRemote();
		}
	}

	/*
	 * Bridging class between the server-side DetachedDom and the client-side
	 * LocalDom
	 */
	public class PathRefRepresentations {
		boolean warnedEventException;

		public void applyEvent(DomEventData eventData) {
			try {
				if (eventData.preview) {
					DOM.previewEvent(eventData.event);
				} else {
					if (eventData.window) {
						Event event = eventData.event;
						switch (event.getType()) {
						case BrowserEvents.PAGEHIDE:
							Window.onPageHide();
							break;
						default:
							throw new UnsupportedOperationException();
						}
					} else {
						Element firstReceiver = (Element) eventData.firstReceiver
								.node();
						if (eventData.eventValue() != null) {
							firstReceiver.implAccess()
									.pathrefRemote().value = eventData
											.eventValue();
						}
						// FIXME - romcom - attach probably not being called
						if (firstReceiver.eventListener == null) {
							firstReceiver.eventListener = firstReceiver;
						}
						// um, is it that easy?
						DOM.dispatchEvent(eventData.event, firstReceiver,
								firstReceiver.eventListener);
					}
				}
			} catch (RuntimeException e) {
				if (e instanceof IndexOutOfBoundsException) {
					/*
					 * One unfinished business is event dispatch (client)
					 * reaching server post dom-change (server)
					 * 
					 * For the moment, report - eventually there should be some
					 * sort of transactional view of the dom (but even so, this
					 * is a problem that can occur in a pure-client system and
					 * isn't that easy to solve)
					 * 
					 * Note also that pathref is -not- as good as element/node
					 * UID here
					 */
					maybeWarn(e);
				} else {
					// handler exception
					e.printStackTrace();
				}
			}
		}

		// FIXME - romcom - switch from pathref to ref_id - (mostly), and then
		// throw (or at least improve logging)
		private void maybeWarn(Exception e) {
			Ax.simpleExceptionOut(e);
			if (warnedEventException) {
				return;
			}
			e.printStackTrace();
			warnedEventException = true;
		}

		public void applyMutations(List<MutationRecord> mutations,
				boolean applyToRemote) {
			LocalDom.this.remoteMutations.applyDetachedMutations(mutations,
					applyToRemote);
		}

		public MutationRecord asRemoveMutation(Node parent, Node oldChild) {
			return remoteMutations.nodeAsRemoveMutation(parent, oldChild);
		}

		public List<MutationRecord> domAsMutations() {
			return nodeAsMutations(Document.get().getDocumentElement(), true);
		}

		public List<MutationRecord> nodeAsMutations(Node node, boolean deep) {
			return remoteMutations.nodeAsMutations(node, deep);
		}
	}

	public static int getMaxCharsPerTextNode() {
		return get().maxCharsPerTextNode;
	}

	public static void setMaxCharsPerTextNode(int maxCharsPerTextNode) {
		get().maxCharsPerTextNode = maxCharsPerTextNode;
	}

	public static Topic<Exception> topicPublishException() {
		return get().topicPublishException;
	}

	public static Topic<String> topicUnableToParse() {
		return get().topicUnableToParse;
	}

	void onAttach(Node node) {
		domIds.onAttach(node);
	}

	void onDetach(Node node) {
		domIds.onDetach(node);
	}
}
