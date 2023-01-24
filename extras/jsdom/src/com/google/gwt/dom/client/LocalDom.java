package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.ElementRemote.ElementRemoteIndex;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.LocalDomDebug;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;

/**
 * FIXME - dirndl 1x1e - Refactoring needs - there's a lot of semi-duplication
 * in the 'link remote to localdom' models - i.e. puts to remoteLookup
 *
 * Probably need just one true path
 *
 * Notes re gc => we maintain a map of remote (browser dom) nodes to local
 * nodes, but that's weak. Strong refs are via node.remote fields
 *
 * Notes re issues with ex-gwt mutations - FIXME - dirndl 1x1e -
 * https://github.com/nevella/alcina/issues/23
 *
 * Does not support IE<11
 */
public class LocalDom {
	private static LocalDom instance = new LocalDom();

	private static LocalDomCollections collections;

	private static Map<String, String> declarativeCssNames;

	public static boolean fastRemoveAll = true;

	private static boolean useRemoteDom = GWT.isClient();

	private static boolean disableRemoteWrite = !GWT.isClient();

	static boolean ie9;

	static boolean emitCommentPisAsText;

	public static LocalDomMutations mutations;

	public static final Topic<Exception> topicException = Topic.create();

	public static final Topic<String> topicUnableToParse = Topic.create();

	public static void debug(ElementRemote elementRemote) {
		get().debug0(elementRemote);
	}

	public static void ensureRemote(Node node) {
		get().ensureRemote0(node);
	}

	public static void ensureRemoteDocument() {
		nodeFor(Document.get().typedRemote().getDocumentElement0());
	}

	public static NodeRemote ensureRemoteNodeMaybePendingResolution(Node node) {
		return get().ensureRemoteNodeMaybePendingResolution0(node);
	}

	public static void eventMod(NativeEvent evt, String eventName) {
		get().eventMod0(evt, eventName);
	}

	public static void flush() {
		get().resolve0();
	}

	public static void invokeExternal(Runnable runnable) {
		flush();
		try {
			mutations.startObserving();
			runnable.run();
		} finally {
			mutations.stopObserving();
		}
	}

	public static boolean isDisableRemoteWrite() {
		return disableRemoteWrite;
	}

	public static boolean isPending(NodeRemote nodeRemote) {
		return get().isPending0(nodeRemote);
	}

	public static boolean isStopPropagation(NativeEvent evt) {
		return get().isStopPropagation0(evt);
	}

	public static boolean isUseRemoteDom() {
		return useRemoteDom;
	}

	public static void log(LocalDomDebug channel, String message,
			Object... args) {
		get().debugImpl.log(channel, message, args);
	}

	public static <T extends Node> T nodeFor(JavaScriptObject jso) {
		return nodeFor((NodeRemote) jso);
	}

	public static <T extends Node> T nodeFor(NodeRemote remote) {
		return (T) get().nodeFor0(remote);
	}

	public static void register(Document doc) {
		if (useRemoteDom) {
			get().linkRemote(doc.typedRemote(), doc);
			get().nodeFor0(doc.typedRemote().getDocumentElement0());
			mutations.startObservingIfNotInEventCycle();
		}
	}

	public static Node resolveExternal(NodeRemote nodeRemote) {
		return get().resolveExternal0(nodeRemote);
	}

	public static void setDisableRemoteWrite(boolean disableRemoteWrite) {
		LocalDom.disableRemoteWrite = disableRemoteWrite;
	}

	public static void setUseRemoteDom(boolean useRemoteDom) {
		LocalDom.useRemoteDom = useRemoteDom;
	}

	public static void syncToRemote(Element element) {
		get().parseAndMarkResolved(element.typedRemote(),
				element.typedRemote().getOuterHtml(), element);
	}

	public static String validateHtml(String html) {
		return get().validateHtml0(html);
	}

	private static LocalDom get() {
		return instance;
	}

	static LocalDomCollections collections() {
		return collections;
	}

	static void consoleLog(String message) {
		if (LocalDomDebugImpl.debugAll) {
			consoleLog0(message);
		}
	}

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

	static boolean hasNode(JavaScriptObject remote) {
		return get().remoteLookup.containsKey(remote);
	}

	static Node nodeForNoResolve(NodeRemote nodeRemote) {
		return get().remoteLookup.get(nodeRemote);
	}

	static void putRemote(Element element, ElementRemote remote) {
		get().putRemote0(element, remote);
	}

	static String safeParseByBrowser(String html) {
		ElementRemote remote = Document.get().typedRemote()
				.generateFromOuterHtml(html);
		return remote.buildOuterHtml();
	}

	static void wasResolved(Element elem) {
		get().wasResolved0(elem);
	}

	LocalDomDebugImpl debugImpl = new LocalDomDebugImpl();

	private Map<NodeRemote, Node> remoteLookup;

	private Map<String, Supplier<Element>> elementCreators;

	Map<NativeEvent, List<String>> eventMods = new LinkedHashMap<>();

	List<Node> pendingResolution = new ArrayList<>();

	ScheduledCommand resolveCommand = null;

	private int resolutionEventId = 1;

	private boolean resolutionEventIdDirty;

	private boolean resolving;

	private boolean useBuiltHtmlValidation;

	public LocalDom() {
		if (GWT.isScript()) {
			remoteLookup = JsUniqueMap.createWeakMap();
		} else {
			remoteLookup = new LinkedHashMap<>();
		}
		ie9 = GWT.isClient() ? BrowserMod.isIE9() : false;
		LocalDom.mutations = GWT.isClient() ? new LocalDomMutations() : null;
		useBuiltHtmlValidation = GWT.isClient()
				? BrowserMod.isInternetExplorer()
				: false;
		emitCommentPisAsText = true;
		if (collections == null) {
			initStatics();
		}
	}

	public void ensurePendingResolved(Node node) {
		Preconditions.checkState(node.linkedToRemote());
		Element element = (Element) node;
		if (element.isPendingResolution()) {
			ElementRemote remote = (ElementRemote) node.remote();
			DomElement local = node.local();
			localToRemote(element, remote, local);
		}
	}

	public Node resolveExternal0(NodeRemote nodeRemote) {
		switch (nodeRemote.getNodeType()) {
		case Node.ELEMENT_NODE:
			ElementRemote elementRemote = (ElementRemote) nodeRemote;
			Element element = Document.get().local()
					.createElement(elementRemote.getTagNameRemote());
			element.putRemote(nodeRemote, false);
			syncToRemote(element);
			linkRemote(elementRemote, element);
			return element;
		case Node.TEXT_NODE:
			Text textNode = Document.get()
					.createTextNode(nodeRemote.getNodeValue());
			textNode.putRemote(nodeRemote, true);
			return textNode;
		default:
			throw new UnsupportedOperationException();
		}
	}

	private Element createElement0(String tagName) {
		Supplier<Element> creator = elementCreators.get(tagName.toLowerCase());
		if (creator == null) {
			return new Element();
		} else {
			return creator.get();
		}
	}

	private void debug0(ElementRemote elementRemote) {
		int debug = 4;
	}

	private void ensureFlush() {
		if (resolveCommand == null) {
			resolveCommand = () -> flush();
			Scheduler.get().scheduleFinally(resolveCommand);
		}
	}

	private void ensureRemote0(Node node) {
		resolve0(true);
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
			ensureRemoteNodeMaybePendingResolution(root);
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
			NodeRemote remote = withRemote.typedRemote().getChildNodes0()
					.getItem0(idx);
			linkRemote(remote, needsRemote);
			needsRemote.putRemote(remote, true);
			withRemote = needsRemote;
		}
	}

	private NodeRemote ensureRemoteNodeMaybePendingResolution0(Node node) {
		if (node.linkedToRemote()) {
			return node.remote();
		}
		ensureFlush();
		NodeRemote remote = null;
		int nodeType = node.getNodeType();
		switch (nodeType) {
		case Node.ELEMENT_NODE:
			Element element = (Element) node;
			remote = ((DomDispatchRemote) DOMImpl.impl.remote)
					.createElement(element.getTagName());
			element.pendingResolution();
			pendingResolution.add(node);
			log(LocalDomDebug.CREATED_PENDING_RESOLUTION,
					"created pending resolution node:" + element.getTagName());
			break;
		case Node.TEXT_NODE:
			remote = Document.get().typedRemote()
					.createTextNode0(((Text) node).getData());
			break;
		// case Node.DOCUMENT_NODE:
		// nodeDom = doc.domImpl();
		// break;
		default:
			throw new UnsupportedOperationException();
		}
		linkRemote(remote, node);
		node.putRemote(remote, false);
		return remote;
	}

	private void eventMod0(NativeEvent evt, String eventName) {
		log(LocalDomDebug.EVENT_MOD,
				Ax.format("eventMod - %s %s", evt, eventName));
		if (!eventMods.keySet().contains(evt)) {
			eventMods.clear();
			eventMods.put(evt, new ArrayList<>());
		}
		eventMods.get(evt).add(eventName);
	}

	private void initElementCreators() {
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

	private void initStatics() {
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

	private boolean isPending0(NodeRemote nodeRemote) {
		return pendingResolution.size() > 0 && pendingResolution.stream()
				.anyMatch(n -> n.remote() == nodeRemote);
	}

	private boolean isStopPropagation0(NativeEvent evt) {
		List<String> list = eventMods.get(evt);
		return list != null && (list.contains("eventStopPropagation")
				|| list.contains("eventCancelBubble"));
	}

	private void linkRemote(NodeRemote remote, Node node) {
		Preconditions.checkState(!remoteLookup.containsKey(remote));
		remoteLookup.put(remote, node);
	}

	private void localToRemote(Element element, ElementRemote remote,
			DomElement local) {
		String innerHTML = local.getInnerHTML();
		if (ie9) {
			switch (element.getTagName()) {
			case "table":
				remote = writeIe9Table((TableElement) element, remote);
				break;
			case "tr":
				remote = writeIe9Tr((TableRowElement) element, remote);
				break;
			default:
				remote.setInnerHTML(innerHTML);
			}
		} else {
			remote.setInnerHTML(innerHTML);
		}
		log(LocalDomDebug.RESOLVE, "%s - uiobj: %s - \n%s",
				element.getTagName(),
				Optional.ofNullable(element.uiObject)
						.map(ui -> ui.getClass().getSimpleName())
						.orElse("(null)"),
				CommonUtils.trimToWsChars(innerHTML, 1000));
		ElementRemote f_remote = remote;
		// doesn't include style
		local.getAttributeMap().entrySet().forEach(e -> {
			switch (e.getKey()) {
			case "text":
				f_remote.setPropertyString(e.getKey(), e.getValue());
				break;
			default:
				f_remote.setAttribute(e.getKey(), e.getValue());
				break;
			}
		});
		local.getStyle().getProperties().entrySet().forEach(e -> {
			StyleRemote remoteStyle = f_remote.getStyle0();
			remoteStyle.setProperty(e.getKey(), e.getValue());
		});
		int bits = ((ElementLocal) local).orSunkEventsOfAllChildren(0);
		bits |= DOM.getEventsSunk(element);
		DOM.sinkEvents(element, bits);
		pendingResolution.remove(element);
		element.resolvePending();
		wasResolved0(element);
	}

	private <T extends Node> T nodeFor0(NodeRemote remote) {
		return nodeFor0(remote, false);
	}

	private <T extends Node> T nodeFor0(NodeRemote remote,
			boolean postReparse) {
		try {
			return nodeFor1(remote, postReparse);
		} catch (RuntimeException re) {
			topicException.publish(re);
			throw re;
		}
	}

	private <T extends Node> T nodeFor1(NodeRemote remote,
			boolean postReparse) {
		if (remote == null) {
			return null;
		}
		T node = (T) remoteLookup.get(remote);
		if (node != null) {
			return node;
		}
		if (remote.provideIsTextOrComment()) {
			// FIXME - dirndl 1x1e - non-performant, but rare (exception
			// for selectionish)
			ElementRemote parentRemote = (ElementRemote) remote
					.getParentNodeRemote();
			Node parent = nodeFor0(parentRemote);
			int index = remote.indexInParentChildren();
			if (parent.getChildCount() == parentRemote.getChildCount()) {
				Node childNode = parent.getChild(index);
				linkRemote(remote, childNode);
				childNode.putRemote(remote, true);
				return (T) childNode;
			} else {
				if (postReparse) {
					topicUnableToParse.publish(Ax.format(
							"Text node reparse - remote:\n%s\n\nlocal:\n%s\n",
							parentRemote.getOuterHtml(),
							((Element) parent).getOuterHtml()));
					throw new RuntimeException("Text node reparse");
				}
				ElementRemoteIndex remoteIndex = parentRemote
						.provideRemoteIndex(false);
				ElementRemote hasNodeRemote = remoteIndex.hasNode();
				reparseFromRemote(hasNodeRemote, (Element) parent, remoteIndex);
				return nodeFor0(remote, true);
			}
		}
		if (!remote.provideIsElement()) {
			return null;// say, shadowroot...
		}
		ElementRemote elem = (ElementRemote) remote;
		// if (elem.getTagNameRemote().equalsIgnoreCase("iframe")) {
		// return null;// don't handle iframes
		// }
		ElementRemoteIndex remoteIndex = elem.provideRemoteIndex(false);
		ElementRemote hasNodeRemote = remoteIndex.hasNode();
		boolean hadNode = hasNodeRemote != null;
		if (hasNodeRemote == null) {
			ElementRemote root = remoteIndex.root();
			Element hasNode = parseAndMarkResolved(root, root.getOuterHtml(),
					null);
			linkRemote(root, hasNode);
			hasNode.putRemote(root, true);
			hasNodeRemote = root;
		}
		Element hasNode = (Element) remoteLookup.get(hasNodeRemote);
		// if this returns true, we knew the remote element has DOM manipulated
		// outside GWT - parse the tree
		if (hasNode.resolveRemoteDefined()) {
			return nodeFor0(remote);
		}
		// htmlparser will sometimes fail to parse dodgy DOM - reparse from
		// browser DOM
		if (shouldTryReparseFromRemote(elem, hasNode, remoteIndex)
				&& !postReparse) {
			reparseFromRemote(hasNodeRemote, hasNode, remoteIndex);
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
			NodeRemote childRemote = (NodeRemote) ancestors.get(idx);
			linkRemote(childRemote, child);
			child.putRemote(childRemote, true);
			cursor = child;
		}
		debugImpl.debugNodeFor(elem, hasNode, remoteIndex, false);
		return (T) remoteLookup.get(remote);
	}

	private Element parseAndMarkResolved(ElementRemote root, String outerHtml,
			Element replaceContents) {
		Element parsed = null;
		try {
			parsed = new HtmlParser().parse(outerHtml, replaceContents,
					root == Document.get().typedRemote().getDocumentElement0());
		} catch (Exception e) {
			// TODO - possibly log. But maybe not - full support of dodgy dom wd
			// be truly hard
			parsed = new HtmlParser().parse(safeParseByBrowser(outerHtml),
					replaceContents,
					root == Document.get().typedRemote().getDocumentElement0());
		}
		if (parsed != null) {
			wasResolved0(parsed);
		} else {
			topicUnableToParse.publish(outerHtml);
		}
		return parsed;
	}

	private void putRemote0(Element element, ElementRemote remote) {
		flush();
		resolutionEventId++;
		wasResolved(element);
		remoteLookup.put(remote, element);
		element.putRemote(remote, true);
	}

	private void reparseFromRemote(ElementRemote elem, Element hasNode,
			ElementRemoteIndex remoteIndex) {
		List<Integer> sizes = remoteIndex.sizes();
		List<Integer> indicies = remoteIndex.indicies();
		boolean sizesMatch = true;
		Element cursor = hasNode;
		ElementRemote remoteCursor = elem;
		for (int idx = sizes.size() - 1; idx >= 0; idx--) {
			int size = sizes.get(idx);
			boolean invalid = cursor.getChildCount() != size;
			Node node = null;
			NodeRemote remoteNode = null;
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
				parseAndMarkResolved(remoteCursor, builtOuterHtml, cursor);
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
			remoteCursor = (ElementRemote) remoteNode;
		}
		Ax.out("Reparse successful");
	}

	private boolean shouldTryReparseFromRemote(ElementRemote elem,
			Element hasNode, ElementRemoteIndex remoteIndex) {
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
		ElementRemote typedRemote = div.implAccess().typedRemote();
		typedRemote.setInnerHTML(html);
		try {
			if (useBuiltHtmlValidation) {
				String outerHtml = typedRemote.buildOuterHtml();
				RegExp regexp = RegExp.compile("^<div>(.+)</div>$", "i");
				MatchResult exec = regexp.exec(outerHtml);
				return exec.getGroup(1);
			} else {
				return typedRemote.getInnerHTML0();
			}
		} catch (Exception e) {
			topicException.publish(e);
			return html;
		}
	}

	private void wasResolved0(Element elem) {
		elem.local().walk(nl -> nl.node().resolved(resolutionEventId));
		resolutionEventIdDirty = true;
	}

	private ElementRemote writeIe9Table(TableElement elem,
			ElementRemote remote) {
		String outer = elem.local().getOuterHtml();
		remoteLookup.remove(remote);
		remote = Document.get().typedRemote().generateFromOuterHtml(outer);
		elem.replaceRemote(remote);
		return remote;
	}

	private ElementRemote writeIe9Tr(TableRowElement elem,
			ElementRemote remote) {
		Preconditions.checkArgument(elem.getChildNodes().stream()
				.allMatch(n -> n.getNodeName().equals("td")));
		int idx = 0;
		for (Node node : elem.getChildNodes()) {
			Element child = (Element) node;
			ElementRemote remoteCell = elem.insertCellRemote(idx);
			localToRemote(child, remoteCell, child.local());
			idx++;
		}
		return remote;
	}

	void resolve0() {
		resolve0(false);
	}

	void resolve0(boolean force) {
		if (resolving) {
			return;
		}
		if (resolveCommand == null && !force) {
			return;
		}
		resolveCommand = null;
		log(LocalDomDebug.RESOLVE, "**resolve**");
		try {
			resolving = true;
			if (resolutionEventIdDirty) {
				resolutionEventId++;
				resolutionEventIdDirty = false;
			}
			new ArrayList<>(pendingResolution).stream()
					.forEach(this::ensurePendingResolved);
			if (resolutionEventIdDirty) {
				resolutionEventId++;
			}
		} catch (RuntimeException re) {
			topicException.publish(re);
			throw re;
		} finally {
			resolutionEventIdDirty = false;
			resolving = false;
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
}
