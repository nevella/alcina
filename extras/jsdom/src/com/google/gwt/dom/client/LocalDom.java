package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.ElementRemote.ElementRemoteIndex;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.LocalDomDebug;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class LocalDom {
	private static LocalDom instance = new LocalDom();

	private static LocalDomCollections collections;

	private static Map<String, String> declarativeCssNames;

	public static boolean fastRemoveAll = true;

	private static boolean useRemoteDom = true;

	private static boolean disableRemoteWrite;

	public static NodeRemote ensurePendingResolutionNode(Node node) {
		return get().ensurePendingResolutionNode0(node);
	}

	public static void ensureRemote(Element element) {
		get().ensureRemote0(element);
	}

	public static void eventMod(NativeEvent evt, String eventName) {
		get().eventMod0(evt, eventName);
	}

	public static void flush() {
		get().flush0();
	}

	public static boolean isDisableRemoteWrite() {
		return disableRemoteWrite;
	}

	public static boolean isStopPropogation(NativeEvent evt) {
		return get().isStopPropogation0(evt);
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
		}
	}

	private void linkRemote(NodeRemote remote, Node node) {
		Preconditions.checkState(!remoteLookup.containsKey(remote));
		// Ax.out("put: %s %s - %s", node.getNodeName(), remote.hashCode(),
		// node.hashCode());
		// if (node.getNodeName().equals("tfoot")) {
		// int debug = 3;
		// }
		remoteLookup.put(remote, node);
	}

	public static void setDisableRemoteWrite(boolean disableRemoteWrite) {
		LocalDom.disableRemoteWrite = disableRemoteWrite;
	}

	public static void setUseRemoteDom(boolean useRemoteDom) {
		LocalDom.useRemoteDom = useRemoteDom;
	}

	private static LocalDom get() {
		return instance;
	}

	static LocalDomCollections collections() {
		return collections;
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

	static boolean hasNode(JavaScriptObject remote) {
		return get().remoteLookup.containsKey(remote);
	}

	LocalDomDebugImpl debugImpl = new LocalDomDebugImpl();

	private Map<NodeRemote, Node> remoteLookup;

	private Map<String, Supplier<Element>> elementCreators;

	Map<NativeEvent, List<String>> eventMods = new LinkedHashMap<>();

	List<Node> pendingResolution = new ArrayList<>();

	ScheduledCommand flushCommand = null;

	private int wasResolvedEventId = 1;

	private boolean wasResolvedEventIdDirty;

	public LocalDom() {
		remoteLookup = new LinkedHashMap<>();
		if (collections == null) {
			initStatics();
		}
	}

	public void ensureResolved(Node node) {
		Preconditions.checkState(node.linkedToRemote());
		Element element = (Element) node;
		if (element.isPendingResolution()) {
			Element elem = (Element) node;
			ElementRemote remote = (ElementRemote) node.remote();
			DomElement local = node.local();
			String innerHTML = local.getInnerHTML();
			if (innerHTML.contains("class=\"workspaceViews")) {
				int debug = 3;
			}
			remote.setInnerHTML(innerHTML);
			// doesn't include style
			local.getAttributes().entrySet().forEach(e -> {
				switch (e.getKey()) {
				case "text":
					remote.setPropertyString(e.getKey(), e.getValue());
					break;
				default:
					remote.setAttribute(e.getKey(), e.getValue());
					break;
				}
			});
			log(LocalDomDebug.FLUSH, "%s - uiobj: %s - \n%s",
					element.getTagName(),
					Optional.ofNullable(element.uiObject)
							.map(ui -> ui.getClass().getSimpleName())
							.orElse("(null)"),
					CommonUtils.trimToWsChars(innerHTML, 1000));
			local.getStyle().getProperties().entrySet().forEach(e -> {
				StyleRemote remoteStyle = remote.getStyle0();
				remoteStyle.setProperty(e.getKey(), e.getValue());
			});
			int bits = ((ElementLocal) local).orSunkEventsOfAllChildren(0);
			bits |= DOM.getEventsSunk(elem);
			DOM.sinkEvents(elem, bits);
			pendingResolution.remove(node);
			wasResolved(elem);
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

	private void ensureFlush() {
		if (flushCommand == null) {
			flushCommand = () -> flush();
			Scheduler.get().scheduleFinally(flushCommand);
		}
	}

	private NodeRemote ensurePendingResolutionNode0(Node node) {
		if (node.linkedToRemote()) {
			return node.remote();
		}
		ensureFlush();
		pendingResolution.add(node);
		NodeRemote remote = null;
		int nodeType = node.getNodeType();
		switch (nodeType) {
		case Node.ELEMENT_NODE:
			Element element = (Element) node;
			remote = ((DomDispatchRemote) DOMImpl.impl.remote)
					.createElement(element.getTagName());
			element.pendingResolution();
			log(LocalDomDebug.CREATED_PENDING_RESOLUTION,
					"created pending resolution node:" + element.getTagName());
			break;
		// case Node.TEXT_NODE:
		// nodeDom = localDomImpl.createDomText(Document.get(),
		// ((Text) node).getData());
		// break;
		// case Node.DOCUMENT_NODE:
		// nodeDom = doc.domImpl();
		// break;
		default:
			throw new UnsupportedOperationException();
		}
		linkRemote(remote, node);
		node.putRemote(remote);
		return remote;
	}

	private void ensureRemote0(Element element) {
		List<Element> ancestors = new ArrayList<>();
		Element cursor = element;
		Element withRemote = null;
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
			// attaching with-remote to without-remote (say, a popup)
			Element root = ancestors.get(0);
			ensurePendingResolutionNode(root);
			flush();
			ensureRemote0(element);
			return;
		}
		for (Element needsRemote : ancestors) {
			int idx = needsRemote.indexInParentChildren();
			debugImpl.debugPutRemote(needsRemote, idx, withRemote);
			NodeRemote remote = withRemote.typedRemote().getChildNodes0()
					.getItem0(idx);
			linkRemote(remote, needsRemote);
			needsRemote.putRemote(remote);
			withRemote = needsRemote;
		}
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

	private boolean isStopPropogation0(NativeEvent evt) {
		List<String> list = eventMods.get(evt);
		return list != null && (list.contains("eventStopPropagation")
				|| list.contains("eventCancelBubble"));
	}

	private <T extends Node> T nodeFor0(NodeRemote remote) {
		T node = (T) remoteLookup.get(remote);
		if (node != null) {
			return node;
		}
		ElementRemote elem = (ElementRemote) remote;
		ElementRemoteIndex remoteIndex = elem.provideRemoteIndex(false);
		ElementRemote hasNodeRemote = remoteIndex.hasNode();
		boolean hadNode = hasNodeRemote != null;
		if (hasNodeRemote == null) {
			ElementRemote root = remoteIndex.root();
			Element hasNode = parse(root, null);
			linkRemote(root, hasNode);
			hasNode.putRemote(root);
			hasNodeRemote = root;
		}
		Element hasNode = (Element) remoteLookup.get(hasNodeRemote);
		List<Integer> indicies = remoteIndex.indicies();
		List<Boolean> remoteDefined = remoteIndex.remoteDefined();
		JsArray ancestors = remoteIndex.ancestors();
		debugImpl.debugNodeFor0(elem, hasNode, remoteIndex, true);
		Element cursor = hasNode;
		for (int idx = indicies.size() - 1; idx >= 0; idx--) {
			int nodeIndex = indicies.get(idx);
			cursor.resolveRemoteDefined();
			Element child = (Element) cursor.getChild(nodeIndex);
			NodeRemote childRemote = (NodeRemote) ancestors.get(idx);
			child.putRemote(childRemote);
			linkRemote(childRemote, child);
			cursor = child;
		}
		debugImpl.debugNodeFor0(elem, hasNode, remoteIndex, false);
		return (T) remoteLookup.get(remote);
	}

	private Element parse(ElementRemote root, Element replaceContents) {
		Element parsed = new HtmlParser().parse(root, replaceContents);
		wasResolved(parsed);
		return parsed;
	}

	private void wasResolved(Element elem) {
		elem.local().walk(nl -> nl.node.wasResolved(wasResolvedEventId));
		wasResolvedEventIdDirty = true;
	}

	void flush0() {
		if (flushCommand == null) {
			return;
		}
		flushCommand = null;
		log(LocalDomDebug.FLUSH, "**flush**");
		try {
			if (wasResolvedEventIdDirty) {
				wasResolvedEventId++;
				wasResolvedEventIdDirty = false;
			}
			new ArrayList<>(pendingResolution).stream()
					.forEach(this::ensureResolved);
			requiresSync.removeIf(elem -> resolveSync(elem));
			if (wasResolvedEventIdDirty) {
				wasResolvedEventId++;
			}
		} finally {
			wasResolvedEventIdDirty = false;
		}
	}

	private boolean resolveSync(Element element) {
		element.ensureRemoteCheck();
		if (element.linkedToRemote()) {
			LocalDom.log(LocalDomDebug.REQUIRES_SYNC, "%s: %s --> resolved",
					element.hashCode(), element.getTagName());
			Preconditions.checkState(!element.local().hasChildNodes());
			syncToRemote(element);
			return true;
		} else {
			return false;
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
		public <K, V> Map<K, V> createIdentityEqualsMap(Class<K> keyClass) {
			if (JsUniqueMap.supportsJsMap()) {
				// element.attributes will need entryset, not yet supported in
				// nativemap
				return JsUniqueMap.create(keyClass, keyClass != String.class);
			} else {
				return super.createIdentityEqualsMap(keyClass);
			}
		}
	}

	public static void syncToRemote(Element element) {
		get().parse(element.typedRemote(), element);
	}

	Set<Element> requiresSync = new LinkedHashSet<>();

	public static void checkRequiresSync(ElementLocal elementLocal) {
		if (disableRemoteWrite || !elementLocal.requiresSync) {
			return;
		}
		get().requiresSync0(elementLocal);
	}

	private void requiresSync0(ElementLocal elementLocal) {
		elementLocal.requiresSync = false;
		// this may not be in a pendingresolution tree
		Element element = elementLocal.elementFor();
		if (element.linkedToRemote()) {
			syncToRemote(element);
		} else {
			requiresSync.add(element);
		}
	}

	public static boolean isPending(NodeRemote nodeRemote) {
		return get().isPending0(nodeRemote);
	}

	private boolean isPending0(NodeRemote nodeRemote) {
		return pendingResolution.size() > 0 && pendingResolution.stream()
				.anyMatch(n -> n.remote() == nodeRemote);
	}
}
