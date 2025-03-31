package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.google.gwt.dom.client.AttachIds.IdList;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.MarkupJso.MarkupToken;
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
	public int maxCharsPerTextNode = 65536;

	Topic<Exception> topicPublishException;

	Topic<String> topicUnableToParse;

	Topic<List<MutationRecord>> topicMutationsAppliedToLocal;

	Topic<Exception> topicReportException;

	private static LocalDomCollections collections;

	private static Map<String, String> declarativeCssNames;

	private static Map<String, String> jsCssNames;

	private static boolean disableRemoteWrite;

	// FIXME - localdom - remove (once there's better general logging)
	private static boolean logParseAndMutationIssues;

	private static Map<String, Supplier<Element>> elementCreators;

	static LocalDomCollections collections() {
		return collections;
	}

	static native void consoleLog0(String message, boolean error) /*-{
    if (error) {
      console.error(message);
    } else {
      console.log(message);
    }
	}-*/;;

	static void consoleLog(String message, boolean error) {
		Ax.sysLogHigh(message);
		consoleLog0(message, error);
	}

	static Element createElement(String tagName) {
		return get().createElement0(tagName);
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

	synchronized static String jsCssName(String key) {
		return jsCssNames.computeIfAbsent(key, k -> {
			StringBuilder builder = new StringBuilder();
			Arrays.stream(k.split("-")).forEach(part -> {
				if (builder.isEmpty()) {
					builder.append(part);
				} else {
					builder.append(
							String.valueOf(part.charAt(0)).toUpperCase());
					builder.append(part.substring(1));
				}
			});
			return builder.toString();
		});
	}

	static void ensureRemoteDocument() {
		nodeFor(Document.get().jsoRemote().getDocumentElement0());
	}

	static <C extends ClientDomNode> C
			ensureRemoteNodeMaybePendingSync(Node node) {
		return (C) get().ensureRemoteNodeMaybePendingSync0(node, true);
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
		jsCssNames = collections.createStringMap();
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

	public static AttachIdRepresentations attachIdRepresentations() {
		return get().attachIdRepresentations;
	}

	public static void register(Document doc) {
		if (Al.isBrowser()) {
			get().initalizeRemoteSync(doc);
			//
		} else {
			doc.setAttached(true, true);
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
		// FIXME - attachId
		throw new UnsupportedOperationException();
	}

	public static void triggerLocalDomException() {
		get().topicReportException
				.publish(new Exception("test exception trigger"));
	}

	public static String validateHtml(String html) {
		if (GWT.isClient()) {
			if (Al.isBrowser()) {
				return Document.get().jsoRemote().validateHtml(html);
			} else {
				// FIXME - romcom - but basically there's no quick way except
				// pre-caching
				return html;
			}
		} else {
			return html;
		}
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

	AttachIdRepresentations attachIdRepresentations = new AttachIdRepresentations();

	boolean applyToRemote = true;

	RemoteMutations remoteMutations;

	LocalMutations localMutations;

	LoggingConfiguration loggingConfiguration;

	DocumentJso docRemote;

	Map<NativeEvent, List<String>> eventMods = new LinkedHashMap<>();

	List<Node> pendingSync = new ArrayList<>();

	ScheduledCommand flushCommand = null;

	boolean markNonStructuralNodesAsSyncedOnSync;

	AttachIds attachIds;

	LocalDom() {
		topicPublishException = Topic.create();
		topicReportException = Topic.create();
		topicUnableToParse = Topic.create();
		topicMutationsAppliedToLocal = Topic.create();
		topicReportException.add(this::handleReportedException);
		attachIds = new AttachIds();
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
			newChild.putRemote(remoteNode);
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

	private void ensureFlush() {
		if (flushCommand == null && GWT.isClient()) {
			flushCommand = () -> flush();
			Scheduler.get().scheduleFinally(flushCommand);
		}
	}

	public void ensurePendingSynced(Node node) {
		Element element = (Element) node;
		if (element.isPendingSync()) {
			ClientDomElement local = node.local();
			localToRemote(element, node.remote(), local);
		}
	}

	void linkSubtreeToAttachIdRemotes(Node node) {
		Node cursor = node;
		while (cursor != null) {
			if (cursor.hasRemote()) {
				break;
			} else {
				cursor.putRemote(NodeAttachId.create(node));
			}
			cursor = cursor.getParentNode();
		}
	}

	ClientDomNode ensureRemoteNodeMaybePendingSync0(Node node,
			boolean ensureFlush) {
		if (node.hasRemote()) {
			return node.remote();
		}
		if (ensureFlush) {
			ensureFlush();
		}
		ClientDomNode remote = null;
		int nodeType = node.getNodeType();
		switch (nodeType) {
		case Node.ELEMENT_NODE:
			Element elem = (Element) node;
			if (isAttachId()) {
				remote = NodeAttachId.create(node);
			} else {
				remote = ((DomDispatchJso) DOMImpl.impl.remote())
						.createElement(elem.getTagName());
			}
			ensurePending(elem);
			break;
		case Node.TEXT_NODE:
			if (isAttachId()) {
				remote = NodeAttachId.create(node);
			} else {
				remote = Document.get().jsoRemote()
						.createTextNode0(((Text) node).getData());
			}
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
		node.putRemote(remote);
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

	// FIXME - attachId - is this needed? verifyMutatingState0?
	boolean syncing;

	boolean applyingDetachedMutationsToLocalDom;

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
			// clear nodes enqueued for sync, then removed
			pendingSync.removeIf(n ->
			/*
			 * this first check will be incorrect (for pending) if the node was
			 * pending, then removed, then attached to some other pending
			 * structure. hasRemote() is correct
			 */
			// !n.isAttached()
			!n.hasRemote());
			List<Node> toSync = new ArrayList<>(pendingSync);
			toSync.stream().forEach(this::ensurePendingSynced);
		} catch (RuntimeException re) {
			topicReportException.publish(re);
			throw re;
		} finally {
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
		attachIds.releaseRemoved();
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

	void initalizeDetachedSync0() {
		remoteMutations = new RemoteMutations(new MutationsAccess(),
				new RemoteMutations.LoggingConfiguration());
		localMutations = new LocalMutations(new MutationsAccess());
		loggingConfiguration = new LoggingConfiguration();
	}

	void initalizeRemoteSync(Document doc) {
		docRemote = doc.jsoRemote();
		loggingConfiguration = new LoggingConfiguration();
		linkRemote(docRemote, doc);
		localMutations = new LocalMutations(new MutationsAccess());
		ElementJso documentElementJso = docRemote.getDocumentElement0();
		Element documentElement = parse(documentElementJso, true);
		documentElement.local().putParent(doc.local());
		walkPutRemote(documentElementJso, documentElement);
		// create
		doc.setAttached(true, true);
		nodeFor0(documentElementJso);
		remoteMutations = new RemoteMutations(new MutationsAccess(),
				loggingConfiguration.asMutationsConfiguration());
	}

	// FIXME - attachId - this process should be the inverse of MarkupJso - and
	// not require a linear number of devmode calls
	void walkPutRemote(ElementJso elemJso, Element elem) {
		elem.putRemote(elemJso);
		DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<>(
				(Node) elem,
				e -> e.streamChildren().collect(Collectors.toList()));
		traversal.forEach(node -> {
			if (node.hasRemote()) {
				return;
			}
			int idx = traversal.getIndexInParent();
			NodeJso parentRemote = node.getParentNode().jsoRemote();
			// edge case - parentRemote will be null inside a <noscript> tag -
			// so that must be mirrored in the local dom
			if (parentRemote == null) {
				throw new IllegalStateException(
						"Local nodes must track browser tree rules");
			}
			NodeJso remote = parentRemote.getChildNodes0().getItem0(idx);
			node.putRemote(remote);
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
		// FIXME - attachId - use MarkupJso
		Element parsed = new HtmlParser().parse(elemJso.getOuterHtml(), null,
				emitHtmlStructuralTags);
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

	boolean isAttachId() {
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

	// FIXME - attachId - maybe remove? simplify call sites?
	private void linkRemote(NodeJso remote, Node node) {
		remote.setAttachId(node.getAttachId());
	}

	void localToRemoteInner(Element element, String markup) {
		IdList subtreeIds = attachIds.getSubtreeIds(element);
		pendingSync.remove(element);
		MarkupToken markupToken = new MarkupToken(element, markup, subtreeIds);
		new MarkupJso().markup(markupToken);
		element.resolvePendingSync();
	}

	void localToRemote(Element element, ElementRemote remote,
			ClientDomElement local) {
		pendingSync.remove(element);
		if (remote instanceof ElementJso) {
			ElementJso jsoRemote = (ElementJso) remote;
			if (local.getChildCount() == 1
					&& local.getFirstChild().getNodeType() == Node.TEXT_NODE) {
				// avoid webkit rewriting as multiple text nodes (if large)
				Text text = (Text) element.getFirstChild();
				ensureRemoteNodeMaybePendingSync0(text, false);
				TextJso textJso = text.jsoRemote();
				jsoRemote.appendChild0(textJso);
				textJso.setData(text.getData());
				int childCount = remote.getChildCount();
				Preconditions.checkState(childCount == 1);
			} else {
				String localMarkup = local.getInnerHTML();
				IdList subtreeIds = attachIds.getSubtreeIds(element);
				MarkupToken markupToken = new MarkupToken(element, localMarkup,
						subtreeIds);
				new MarkupJso().markup(markupToken);
			}
			// doesn't include style
			local.getAttributeMap().entrySet().forEach(e -> {
				String value = e.getValue();
				switch (e.getKey()) {
				case "text":
					jsoRemote.setPropertyString(e.getKey(), value);
					break;
				default:
					jsoRemote.setAttribute(e.getKey(), value);
					break;
				}
			});
			local.getStyle().getProperties().entrySet().forEach(e -> {
				StyleJso remoteStyle = jsoRemote.getStyle0();
				remoteStyle.setProperty(e.getKey(), e.getValue());
			});
		} else {
			// ElementAttachId
			if (!element.isAttached()) {
				return;
			} else {
				// FIXME - localdom - handle empty text nodes here - possibly by
				// reverting to non-markup if we encounter any
				remoteMutations.emitInnerMarkupMutation(element);
				linkSubtreeToAttachIdRemotes(element);
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
	}

	private <T extends Node> T nodeFor0(NodeJso remote) {
		if (remote == null) {
			return null;
		}
		if (remote.getNodeType() == Node.DOCUMENT_NODE) {
			return (T) Document.get();
		} else {
			int attachId = remote.getAttachId();
			if (attachId == 0) {
				throw new IllegalStateException(
						"Remote should always be registered");
			} else {
				Node node = attachIds.getNode(new AttachId(attachId));
				if (node == null) {
					// removed from localdom, but remotedom removal still
					// pending
					return null;
				} else {
					return (T) node;
				}
			}
		}
	}

	private void verifyMutatingState0() {
		Preconditions
				.checkArgument(syncing || (remoteMutations.isObserverConnected()
						|| !remoteMutations.isEnabled()));
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

		public boolean logEvents = false;// !GWT.isScript();

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
			// FIXME - attachId
			throw new UnsupportedOperationException();
		}

		public NodeJso parentNoResolve(NodeJso cursor) {
			return cursor.getParentNodeJso();
		}

		// FIXME - attachId - mutations - goes away
		public void putRemoteChildren(Element elem,
				List<NodeJso> remoteChildrenS0) {
			NodeList<Node> childNodes = elem.getChildNodes();
			for (int idx = 0; idx < remoteChildrenS0.size(); idx++) {
				Node child = childNodes.getItem(idx);
				NodeJso remote = remoteChildrenS0.get(idx);
				// not sure about synced here...
				child.putRemote(remote);
				linkRemote(remote, child);
			}
		}

		public void reportException(Exception exception) {
			topicReportException.publish(exception);
		}

		public void setApplyToRemote(boolean applyToRemote) {
			get().applyToRemote = applyToRemote;
		}

		public void setApplyingDetachedMutationsToLocalDom(
				boolean applyingDetachedMutationsToLocalDom) {
			get().applyingDetachedMutationsToLocalDom = applyingDetachedMutationsToLocalDom;
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

		public void applyPreRemovalAttachId(MutationNode mutationNode) {
			attachIds.applyPreRemovalAttachId(mutationNode.node,
					mutationNode.attachId);
		}

		public void putRemoteAttachId(Node node) {
			ClientDomNode remote = node.remote();
			if (remote != null && remote.getAttachId() == node.getAttachId()) {
				return;
			}
			node.putRemote(NodeAttachId.create(node));
		}

		public void setDetached(Node node) {
			node.traverse().forEach(n -> n.setAttached(false, false));
		}

		public Node getNode(int attachId) {
			return attachIds.getNode(new AttachId(attachId));
		}

		public void insertAttachedBefore(Node newChild, Node refChild) {
			newChild.jsoRemote().getParentElement()
					.insertAttachedBefore(newChild, refChild);
			if (newChild.isElement()) {
				Element newElem = (Element) newChild;
				IdList idList = newElem.getSubtreeIds();
				MarkupToken markupToken = new MarkupToken(newElem, null,
						idList);
				// this applies the local dom attachids to the jso nodes
				new MarkupJso().markup(markupToken);
			} else {
				newChild.jsoRemote().setAttachId(newChild.attachId);
			}
		}

		public Node remoteToLocal(NodeJso nodeJso) {
			return NodeJso.toNode(nodeJso);
		}

		public void onRemoteMutationsApplied(List<MutationRecord> records,
				boolean hadException) {
			if (!hadException) {
				records.forEach(MutationRecord::populateAttachIds);
				topicMutationsAppliedToLocal.publish(records);
			}
		}

		public boolean isApplyingDetachedMutationsToLocalDom() {
			return applyingDetachedMutationsToLocalDom;
		}
	}

	/*
	 * Bridging class between the server-side remote (NodeAttachId) dom and the
	 * client-side LocalDom
	 */
	public class AttachIdRepresentations {
		public void applyEvent(DomEventData eventData) {
			boolean selectionEvent = Objects.equals(eventData.event.getType(),
					BrowserEvents.SELECTIONCHANGE);
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
				if (!selectionEvent) {
					return;
				}
				break;
			case other:
				// not currently handled, could be implemented
				return;
			case element:
				// continue method, most common case
				if (!Element.is(eventTarget)) {
					// event target (client) has been removed from the
					// canonical dom (server)
					return;
				}
				break;
			}
			if (eventData.preview) {
				DOM.previewEvent(eventData.event);
			} else {
				Element target = Element.as(eventTarget);
				Element firstReceiver = (Element) eventData.firstReceiver
						.node();
				if (firstReceiver == null) {
					return;
				}
				if (eventData.eventValue() != null) {
					target.attachIdRemote().value = eventData.eventValue();
				}
				if (eventData.selectedIndex != null) {
					target.attachIdRemote().selectedIndex = eventData.selectedIndex;
				}
				// FIXME - romcom - attach probably not being called.
				// This can probably be removed
				Preconditions.checkState(firstReceiver.eventListener != null);
				// um, is it that easy?
				DOM.dispatchEvent(eventData.event, firstReceiver,
						firstReceiver.eventListener);
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

	public static Topic<List<MutationRecord>> topicMutationsAppliedToLocal() {
		return get().topicMutationsAppliedToLocal;
	}

	void onAttach(Node node) {
		attachIds.onAttach(node);
	}

	void onDetach(Node node) {
		attachIds.onDetach(node);
	}

	static boolean wasRemoved(ElementJso elemJso) {
		return get().wasRemoved0(elemJso);
	}

	boolean wasRemoved0(ElementJso elemJso) {
		int attachId = elemJso.getAttachId();
		if (attachId == 0) {
			return true;
		}
		Node node = attachIds.getNode(new AttachId(attachId));
		return node == null;
	}

	public static boolean isPendingSync(Node node) {
		return get().isPendingSync0(node);
	}

	boolean isPendingSync0(Node node) {
		/*
		 * Given normal dom structure patterns, this check will be infrequent
		 * and pendingSync will be small, so no need to optimise
		 */
		return pendingSync.contains(node);
	}

	static void setInnerHtml(Element element, String html, IdList idList) {
		get().setInnerHtml0(element, html, idList);
	}

	void setInnerHtml0(Element elem, String html, IdList idList) {
		/*
		 * this element can be returned to the 'pending' state (all
		 * manipulations are local-only until flush), since the remote is empty
		 * 
		 * FIXME - attachId - are the domIds calls needed?
		 */
		if (elem.linkedAndNotPending()) {
			ensurePending(elem);
		}
		attachIds.readFromIdList(elem, idList);
		elem.local().setInnerHTML(html);
		try {
			attachIds.verifyIdList(idList);
		} catch (RuntimeException e) {
			String remoteHtml = elem.jsoRemote().getInnerHTML0();
			String localHtml = elem.local().getInnerHTML();
			consoleLog("setHtml::validation issue (probably wonky html)", true);
			/* probably some odd dom - compare to roundtripped
			@formatter:off
			 java.nio.file.Files.write(java.nio.file.Path.of("/g/alcina/tmp/t0.html"),  remoteHtml.replace("&nbsp;","\u00A0").getBytes());
			 java.nio.file.Files.write(java.nio.file.Path.of("/g/alcina/tmp/t1.html"), localHtml.replace("&nbsp;","\u00A0").getBytes());
			 @formatter:on
			 */
			throw e;
		}
	}

	void ensurePending(Element elem) {
		if (!pendingSync.contains(elem)) {
			pendingSync.add(elem);
			ensureFlush();
		}
	}

	static Node toNode(ElementJso elemJso) {
		return get().parse(elemJso, false);
	}
}
