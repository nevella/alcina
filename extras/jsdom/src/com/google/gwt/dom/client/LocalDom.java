package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.ElementRemote.ElementRemoteIndex;
import com.google.gwt.dom.client.mutations.LocalDomMutations2;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.LocalDomDebug;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Topic;

/**
 * <p>
 * FIXME - dirndl 1x1e - Refactoring needs - there's a lot of semi-duplication
 * in the 'link remote to localdom' models - i.e. puts to remoteLookup
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
 * FIXME - dirndl - 1x3 - move all init out of statics if possible (to avoid
 * clinit)
 *
 * Does not support IE<11
 */
public class LocalDom {
	private static LocalDom instance;

	private static LocalDomCollections collections;

	private static Map<String, String> declarativeCssNames;

	public static boolean fastRemoveAll;

	private static boolean disableRemoteWrite;

	public static Topic<Exception> topicException;

	public static Topic<String> topicUnableToParse;

	// FIXME - localdom - remove
	private static boolean logParseAndMutationIssues;

	public static int maxCharsPerTextNode = 65536;

	public static void debug(ElementRemote elementRemote) {
		get().debug0(elementRemote);
	};

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

	public static LocalDomMutations2 getMutations() {
		return get().mutations;
	}

	public static void initalize() {
		Preconditions.checkState(instance == null);
		fastRemoveAll = true;
		disableRemoteWrite = !GWT.isClient();
		topicException = Topic.create();
		topicUnableToParse = Topic.create();
		instance = new LocalDom();
	}

	public static void invokeExternal(Runnable runnable) {
		get().invokeExternal0(runnable);
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

	public static void log(Level level, String template, Object... args) {
		String message = Ax.format(template, args);
		boolean error = level.intValue() > Level.INFO.intValue();
		if (error) {
			Ax.err(message);
		} else {
			Ax.out(message);
		}
		consoleLog(message, error);
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

	public static void onRelatedException(RuntimeException e) {
		if (logParseAndMutationIssues) {
			throw e;
		} else {
			// devmode only
			Ax.simpleExceptionOut(e);
		}
	}

	public static void register(Document doc) {
		if (isUseRemoteDom()) {
			initalize();
			get().initalizeSync(doc);
		}
	}

	public static Node resolveExternal(NodeRemote nodeRemote) {
		return get().resolveExternal0(nodeRemote);
	}

	public static void setDisableRemoteWrite(boolean disableRemoteWrite) {
		LocalDom.disableRemoteWrite = disableRemoteWrite;
	}

	public static void syncToRemote(Element element) {
		get().parseAndMarkResolved(element.typedRemote(),
				element.typedRemote().getOuterHtml(), element);
	}

	public static String validateHtml(String html) {
		return get().validateHtml0(html);
	}

	private static native void consoleLog(String message, boolean error) /*-{
    if (error) {
      console.error(message);
    } else {
      console.log(message);
    }
	}-*/;

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

	static boolean isUseRemoteDom() {
		return GWT.isClient();
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

	LocalDomMutations2 mutations;

	private Configuration configuration;

	public BrowserBehaviour browserBehaviour;

	private DocumentRemote docRemote;

	LocalDomDebugImpl debugImpl = new LocalDomDebugImpl();

	private Map<NodeRemote, Node> remoteLookup;

	private Map<String, Supplier<Element>> elementCreators;

	Map<NativeEvent, List<String>> eventMods = new LinkedHashMap<>();

	List<Node> pendingResolution = new ArrayList<>();

	ScheduledCommand resolveCommand = null;

	private int resolutionEventId = 1;

	private boolean resolutionEventIdDirty;

	private boolean resolving;

	private LocalDom() {
		if (GWT.isScript()) {
			remoteLookup = JsUniqueMap.createWeakMap();
		} else {
			remoteLookup = new LinkedHashMap<>();
		}
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

	private void initalizeSync(Document doc) {
		docRemote = doc.typedRemote();
		linkRemote(docRemote, doc);
		nodeFor0(docRemote.getDocumentElement0());
		configuration = new Configuration();
		browserBehaviour = new BrowserBehaviour();
		browserBehaviour.test();
		mutations = GWT.isClient()
				? new LocalDomMutations2(new MutationsAccess(),
						configuration.asMutationsConfiguration())
				: null;
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

	private void invokeExternal0(Runnable runnable) {
		flush();
		try {
			mutations.startObserving();
			runnable.run();
		} finally {
			mutations.syncMutationsAndStopObserving();
		}
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
		remote.setInnerHTML(innerHTML);
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

	private <T extends Node> T nodeFor0(NodeRemote remote, boolean postReparse)
			throws LocalDomException {
		try {
			return nodeFor1(remote, postReparse);
		} catch (RuntimeException re) {
			topicException.publish(re);
			throw new LocalDomException(re);
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
		if (remote.getNodeName().equalsIgnoreCase("iframe")) {
			return null;// SEP
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
			return typedRemote.getInnerHTML0();
		} catch (Exception e) {
			topicException.publish(e);
			return html;
		}
	}

	private void wasResolved0(Element elem) {
		elem.local().walk(nl -> nl.node().resolved(resolutionEventId));
		resolutionEventIdDirty = true;
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
			ElementRemote div = docRemote.createElementNode0("div");
			TextRemote text = docRemote.createTextNode0(lengthTest);
			div.appendChild0(text);
			int childNodesLengthNodeOperation = div.getChildNodes0()
					.getLength();
			div.setInnerHTML(lengthTest);
			int childNodesLengthHtmlOperation = div.getChildNodes0()
					.getLength();
			if (childNodesLengthHtmlOperation == 1) {
				maxCharsPerTextNode = Integer.MAX_VALUE;
			} else {
				maxCharsPerTextNode = div.getChildNodes0().getItem(0)
						.getNodeValue().length();
			}
			log(Level.INFO,
					"test text length: %s\n\tchildNodesLengthNodeOperation: %s"
							+ "\n\tchildNodesLengthHtmlOperation: %s\n\tmaxCharsPerTextNode: %s",
					lengthTest.length(), childNodesLengthNodeOperation,
					childNodesLengthHtmlOperation, maxCharsPerTextNode);
		}
	}

	public static class Configuration {
		public boolean mutationLogDoms = true;

		public boolean mutationLogEvents = true;

		public boolean logEvents = true;

		public Configuration() {
		}

		public LocalDomMutations2.Configuration asMutationsConfiguration() {
			LocalDomMutations2.Configuration result = new LocalDomMutations2.Configuration();
			result.logDoms = mutationLogDoms;
			result.logEvents = mutationLogEvents;
			return result;
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

	public class MutationsAccess {
		public Stream<NodeRemote> streamChildren(NodeRemote node) {
			return node.getChildNodes0().streamRemote();
		}

		public NodeRemote typedRemote(Node n) {
			return n.typedRemote();
		}
	}
}
