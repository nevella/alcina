package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.DomIds.IdList;
import com.google.gwt.dom.client.ElementJso.ElementJsoIndex;
import com.google.gwt.dom.client.mutations.LocalMutations;
import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.RemoteMutations;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.context.ContextFrame;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

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

	static native void consoleLog(String message, boolean error) /*-{
    if (error) {
      console.error(message);
    } else {
      console.log(message);
    }
	}-*/;;

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

	public static RefidRepresentations refIdRepresentations() {
		return get().refIdRepresentations;
	}

	public static void register(Document doc) {
		if (GWT.isClient()) {
			get().initalizeRemoteSync(doc);
			doc.getDocumentElement().setAttached(true);
		}
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
		// FIXME - refid
		throw new UnsupportedOperationException();
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

	RefidRepresentations refIdRepresentations = new RefidRepresentations();

	boolean applyToRemote = true;

	RemoteMutations remoteMutations;

	LocalMutations localMutations;

	LoggingConfiguration loggingConfiguration;

	DocumentJso docRemote;

	Map<NativeEvent, List<String>> eventMods = new LinkedHashMap<>();

	List<Node> pendingSync = new ArrayList<>();

	ScheduledCommand flushCommand = null;

	int syncEventId = 1;

	boolean syncEventIdDirty;

	boolean syncing;

	boolean markNonStructuralNodesAsSyncedOnSync;

	DomIds domIds;

	LocalDom() {
		topicPublishException = Topic.create();
		topicReportException = Topic.create();
		topicUnableToParse = Topic.create();
		topicReportException.add(this::handleReportedException);
		domIds = new DomIds();
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

	/*
	 * This method links node and all ancestors to the corresponding remote
	 * nodes, by finding the unklinked nodes and walking the stack, linking to
	 * the nth remote node (where n is the index of the stack elt)
	 * 
	 * FIXME - refid - only permit a call to this in jso devmode, all other dom
	 * linking (local <-> remote) should occur as the ships come in - i.e.
	 * markupjso will
	 */
	private void ensureRemote0(Node node) {
		if (isRefid()) {
			// FIXME - refid - factor a bunch of this out into
			// LDjso/LDrefId classes
			ensureRemote0Refid(node);
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
			NodeJso remote = withRemote.jsoRemote().getChildNodes0()
					.getItem0(idx);
			linkRemote(remote, needsRemote);
			needsRemote.putRemote(remote, true);
			withRemote = needsRemote;
		}
	}

	private void ensureRemote0Refid(Node node) {
		Node cursor = node;
		while (cursor != null) {
			if (cursor.linkedToRemote()) {
				break;
			} else {
				cursor.putRemote(NodeRefid.create(node), cursor.wasSynced());
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
			if (isRefid()) {
				remote = NodeRefid.create(node);
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
			// clear nodes enqueued for sync, then removed
			pendingSync.removeIf(n -> n.getParentNode() == null);
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
		 * . The former is ... generally better, but does mean local/remote are
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
		domIds.releaseRemoved();
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
		linkRemote(docRemote, doc);
		localMutations = new LocalMutations(new MutationsAccess());
		ElementJso documentElementJso = docRemote.getDocumentElement0();
		Element documentElement = parse(documentElementJso, true);
		walkPutRemote(documentElementJso, documentElement);
		// create
		documentElement.setAttached(true);
		nodeFor0(documentElementJso);
		remoteMutations = new RemoteMutations(new MutationsAccess(),
				loggingConfiguration.asMutationsConfiguration());
	}

	void walkPutRemote(ElementJso elemJso, Element elem) {
		elem.putRemote(elemJso, true);
		DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<>(
				(Node) elem,
				e -> e.streamChildren().collect(Collectors.toList()));
		traversal.forEach(node -> {
			if (node.implAccess().isJsoRemote()) {
				return;
			}
			int idx = traversal.getIndexInParent();
			NodeJso parentRemote = node.getParentNode().implAccess()
					.jsoRemote();
			NodeJso remote = parentRemote.getChildNodes0().getItem0(idx);
			node.putRemote(remote, true);
		});
	}

	/**
	 * 
	 * @param elemJso
	 * @param emitHtmlStructuralTags
	 *            called only on init (to avoid an attempted emission of say two
	 *            &lt;title&gt; tags, which will break things)
	 * @return
	 */
	Element parse(ElementJso elemJso, boolean emitHtmlStructuralTags) {
		Element parsed = new HtmlParser().parse(elemJso.getOuterHtml(), null,
				emitHtmlStructuralTags);
		wasSynced0(parsed);
		return parsed;
	}

	private void invokeExternal0(Runnable runnable) {
		// DEV mode check (all script calls are guaranteed within Impl)
		// don't allow mutating ext js calls in the very first (onModuleLoad)
		// script cycle - since the disconnect/connect cycle is not setup
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

	boolean isRefid() {
		return Document.get().remoteType == RemoteType.REF_ID;
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

	// FIXME - refid - maybe remove? simplify call sites?
	private void linkRemote(NodeJso remote, Node node) {
		remote.setRefId(node.getRefId());
	}

	static void localToRemoteInner(Element element, String markup) {
		get().localToRemoteInner0(element, markup);
	}

	void localToRemoteInner0(Element element, String markup) {
		IdList subtreeIds = domIds.getSubtreeIds(element);
		pendingSync.remove(element);
		new MarkupJso().markup(element, markup, subtreeIds.ids);
		element.resolvePendingSync();
		wasSynced0(element);
	}

	void localToRemote(Element element, ElementRemote remote,
			ClientDomElement local) {
		pendingSync.remove(element);
		if (remote instanceof ElementJso) {
			String innerMarkup = local.getInnerHTML();
			IdList subtreeIds = domIds.getSubtreeIds(element);
			new MarkupJso().markup(element, innerMarkup, subtreeIds.ids);
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
			// ElementRefId
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
		if (remote.getNodeType() == Node.DOCUMENT_NODE) {
			return (T) Document.get();
		} else {
			int refId = remote.getRefId();
			Node node = domIds.getNode(new Refid(refId));
			if (node == null) {
				throw new IllegalStateException(
						"Remote should always be registered");
			} else {
				return (T) node;
			}
		}
	}

	private void putRemote0(Element element, ElementJso remote) {
		flush();
		syncEventId++;
		wasSynced(element);
		element.putRemote(remote, true);
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

	// refid - fix
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
			// FIXME - refid
			throw new UnsupportedOperationException();
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
				linkRemote(remote, child);
			}
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

		public void applyPreRemovalRefId(MutationNode mutationNode) {
			domIds.applyPreRemovalRefId(mutationNode.node, mutationNode.refId);
		}
	}

	/*
	 * Bridging class between the server-side remote (NodeRefid) dom and the
	 * client-side LocalDom
	 */
	public class RefidRepresentations {
		public void applyEvent(DomEventData eventData) {
			try {
				EventTarget eventTarget = eventData.event.getEventTarget();
				switch (eventTarget.type) {
				case window: {
					if (eventData.preview) {
						// as below, this special casing is just munge - remove
						// (and handle non-element events as per element events)
						return;//
					}
					Event event = eventData.event;
					switch (event.getType()) {
					case BrowserEvents.PAGEHIDE:
						Window.onPageHide();
						break;
					default:
						// ignore, could be implemented
					}
					return;
				}
				case document:
				case other:
					// not currently handled, could be implemented
					return;
				case element:
					// continue method, most common case
					break;
				}
				if (!Element.is(eventTarget)) {
					// event target (client) has been removed from the
					// canonical dom (server)
					return;
				}
				Element target = Element.as(eventTarget);
				if (eventData.preview) {
					DOM.previewEvent(eventData.event);
				} else {
					Element firstReceiver = (Element) eventData.firstReceiver
							.node();
					if (firstReceiver == null) {
						return;
					}
					if (eventData.eventValue() != null) {
						target.implAccess().refIdRemote().value = eventData
								.eventValue();
					}
					// FIXME - romcom - attach probably not being called.
					// This can probably be removed
					Preconditions
							.checkState(firstReceiver.eventListener != null);
					// um, is it that easy?
					DOM.dispatchEvent(eventData.event, firstReceiver,
							firstReceiver.eventListener);
				}
			} catch (RuntimeException e) {
				// handler exception
				e.printStackTrace();
			}
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
		// a big number
		return 1024000000;
		// get().maxCharsPerTextNode;
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

	static boolean wasRemoved(ElementJso elemJso) {
		return get().wasRemoved0(elemJso);
	}

	boolean wasRemoved0(ElementJso elemJso) {
		int refId = elemJso.getRefId();
		Node node = domIds.getNode(new Refid(refId));
		if (node == null) {
			Preconditions.checkState(refId != 0);
			// domIds.removed will *almost certainly* contain the refId, but
			// because we're async and removed is flushed, it's possible that it
			// wouldn't. so don't check
			return true;
		} else {
			return false;
		}
	}
}
