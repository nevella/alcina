package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.text.StyledEditorKit.BoldAction;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.LocalDomBridge.LocalDomBridgeCollections;
import com.google.gwt.dom.client.LocalDomBridge.LocalDomBridgeCollections_Script;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.LocalDomDebug;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsUniqueMap;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.ClientUtils;

/**
 * Gotchas:
 * 
 * remove dom nodes when attaching
 * 
 * @author nick@alcina.cc
 *
 */
public class LocalDomBridge {
	static LocalDomBridge bridge = null;

	public static boolean isScript = GWT.isScript();

	static LocalDomBridgeDebug debug = new LocalDomBridgeDebug();

	private static boolean ensuring = false;

	public static boolean resolving;

	static boolean ensuringPendingResolutionNode;

	public static boolean fastRemoveAll = true;

	public static Element_Jso elementJso(Element elem) {
		return elementJso(elem, true);
	}

	public static Element_Jso elementJso(Element elem, boolean maybeEnsure) {
		if (!LocalDomImpl.useLocalImpl && maybeEnsure && elem.domImpl != null) {
			ensureJso(elem);
		}
		return elem.domImpl;
	}

	public static void ensureJso(Element element) {
		ensureJso(element, true);
	}

	public static Node_Jso ensurePendingResolutionNode(Node node) {
		return get().ensurePendingResolutionNode0(node);
	}

	public synchronized static LocalDomBridge get() {
		if (bridge == null) {
			bridge = new LocalDomBridge();
		}
		return bridge;
	}

	public static <N extends Node> N nodeFor(JavaScriptObject o) {
		if (o == null) {
			return null;
		}
		return get().nodeFor0(o);
	}

	public static <N extends Node> N nodeFor(Node_Jvm node_Jvm) {
		if (node_Jvm == null) {
			return null;
		}
		return (N) get().nodeFor0(node_Jvm);
	}

	public static void register(Document doc) {
		get().register0(doc);
	}

	public static void registerId(Element_Jvm element_Jvm) {
		get().registerId0(element_Jvm);
	}

	public static void replaceWithJso(Element element) {
		boolean saveLocalImpl = LocalDomImpl.useLocalImpl;
		Element_Jso element_Jso = get().localDomImpl
				.createDomElement(Document.get(), element.getTagName());
		get().javascriptObjectNodeLookup.put(element_Jso, element);
		element.putDomImpl(element_Jso);
		get().ensureResolved(element);
		if (saveLocalImpl) {
			get().useLocalDom();
		}
	}

	public static boolean shouldUseDomNodes() {
		return !LocalDomBridge.ensuringPendingResolutionNode
				&& get().flushCommand == null;
	}

	public static Style styleObjectFor(JavaScriptObject o) {
		if (o == null) {
			return null;
		}
		return get().styleObjectFor0(o);
	}

	public static Style styleObjectFor(Style_Jvm style_jvm) {
		if (style_jvm == null) {
			return null;
		}
		return get().styleObjectFor0(style_jvm);
	}

	private static void linkTreesLocal(Element element) {
		Element original = element;
		// these will be Element_Jvms
		List<Element> chain = new ArrayList<>();
		// hmmm...ascend to dom, descend...
		while (element.domImpl == null) {
			chain.add(element);
			element = element.getParentElement();
		}
		chain.add(element);
		// don't go to zero (children of this elt)
		for (int idx = chain.size() - 1; idx >= 1; idx--) {
			Element withDom = chain.get(idx);
			Element_Jvm vmImpl = (Element_Jvm) withDom.localImpl();
			NodeList_Jso<Element> childNodes = (NodeList_Jso) withDom.domImpl
					.getChildNodes().impl;
			if (debug.on) {
				Preconditions.checkState(
						childNodes.getLength() == vmImpl.children.size());
			}
			for (int idx2 = 0; idx2 < vmImpl.children.size(); idx2++) {
				Node_Jvm child_jvm = vmImpl.children.get(idx2);
				int len = childNodes.getLength();
				Node_Jso domImpl = childNodes.getItem0(idx2);
				Node_Jso domImplCopy = domImpl;
				if (debug.on) {
					Preconditions.checkState(domImpl.getNodeName()
							.equalsIgnoreCase(child_jvm.getNodeName()));
				}
				get().javascriptObjectNodeLookup.put(domImpl, child_jvm.node);
				child_jvm.node.putDomImpl(domImpl);
				child_jvm.node.putImpl(domImpl);
			}
		}
	}

	static void ensureJso(Element element, boolean flush) {
		if (get().flushCommand != null && flush
				&& get().pendingResolution.size() > 0) {
			get().flush();
		}
		if (element.domImpl != null) {
			return;
		}
		if (ensuring) {
			return;
		}
		try {
			ensuring = true;
			if (debug.on) {
				String message = Ax.format("ensure jso - %s\n", element);
				log(LocalDomDebug.ENSURE_JSO, message);
			}
			String id = element.getId();
			if (!id.isEmpty()) {
				Element_Jso domImpl = Document.get().domImpl
						.getElementById0(id);
				if (domImpl != null) {
					get().javascriptObjectNodeLookup.put(domImpl, element);
					element.putDomImpl(domImpl);
					element.putImpl(domImpl);
					return;
				}
			}
			linkTreesLocal(element);
		} finally {
			ensuring = false;
		}
	}

	// FIXME - better map
	private Map<String, String> declarativeCssNames;

	Map<JavaScriptObject, Node> javascriptObjectNodeLookup;

	Map<JavaScriptObject, Style> javascriptObjectStyleLookup;

	Map<DomNode, Node> localNodeLookup;

	Map<DomStyle, Style> localStyleLookup;

	Map<String, Node> idLookup;

	private Document doc;

	Map<String, Supplier<Element>> elementCreators;

	List<Node> pendingResolution = new ArrayList<>();

	LocalDomImpl localDomImpl;

	List<DomElement> createdLocals = new ArrayList<>();

	ScheduledCommand flushCommand = null;

	Map<NativeEvent, List<String>> eventMods = new LinkedHashMap<>();

	LocalDomBridgeCollections collections;

	private LocalDomBridge() {
		if (GWT.isScript()) {
			JavascriptKeyableLookup.initJs();
			collections = new LocalDomBridgeCollections_Script();
		} else {
			collections = new LocalDomBridgeCollections();
		}
		declarativeCssNames = collections.createStringMap();
		javascriptObjectNodeLookup = collections
				.createIdentityEqualsMap(JavaScriptObject.class);
		javascriptObjectStyleLookup = collections
				.createIdentityEqualsMap(JavaScriptObject.class);
		idLookup = collections.createIdentityEqualsMap(String.class);
		elementCreators = collections.createIdentityEqualsMap(String.class);
		initElementCreators();
		localNodeLookup = collections.createIdentityEqualsMap(DomNode.class);
		localStyleLookup = collections.createIdentityEqualsMap(DomStyle.class);
	}

	public void checkInPreconditionList(Element element, DomNode impl) {
		if (impl instanceof JavaScriptObject) {
			if (pendingResolution.contains(element)) {
				Preconditions.checkState(false);
			}
		}
	}

	public void createdLocalElement(DomElement local) {
		createdLocals.add(local);
		ensureFlush();
	}

	public String declarativeCssName(String key) {
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

	public void detachDomNode(Node_Jso domImpl) {
		javascriptObjectNodeLookup.remove(domImpl);
		debug.removeAssignment(domImpl);
		if (domImpl instanceof Element_Jso) {
			Element_Jso elem = (Element_Jso) domImpl;
			String id = elem.getId();
			log(LocalDomDebug.DETACH_ELEM_JSO, "detach id:" + id);
			idLookup.remove(id);
			NodeList_Jso<Node> kids = elem.getChildNodes0();
			int length = kids.getLength();
			for (int idx = 0; idx < length; idx++) {
				detachDomNode(kids.getItem0(idx));
			}
		}
	}

	public void ensureResolved(Node node) {
		if (node.domImpl() == null) {
			throw new UnsupportedOperationException();
		}
		if (node.impl() != null && node.impl() != node.domImpl()) {
			Element elem = (Element) node;
			Element_Jso dom_elt = (Element_Jso) node.domImpl();
			DomElement vmlocal_elt = (DomElement) node.impl();
			String innerHTML = vmlocal_elt.getInnerHTML();
			if (innerHTML.contains("__localdom__46")) {
				int debug = 3;
			}
			dom_elt.setInnerHTML(innerHTML);
			// doesn't include style
			vmlocal_elt.getAttributes().entrySet().forEach(e -> {
				switch (e.getKey()) {
				case "text":
					dom_elt.setPropertyString(e.getKey(), e.getValue());
					break;
				default:
					dom_elt.setAttribute(e.getKey(), e.getValue());
					break;
				}
			});
			vmlocal_elt.getStyle().getProperties().entrySet().forEach(e -> {
				Style domStyle = dom_elt.getStyle();
				domStyle.setProperty(e.getKey(), e.getValue());
			});
			int bits = ((Element_Jvm) vmlocal_elt).orSunkEventsOfAllChildren(0);
			bits |= DOM.getEventsSunk(elem);
			DOM.sinkEvents(elem, bits);
			pendingResolution.remove(node);
			node.putImpl(node.domImpl());
		}
	}

	public void eventMod(NativeEvent evt, String eventName) {
		log(LocalDomDebug.EVENT_MOD,
				Ax.format("eventMod - %s %s", evt, eventName));
		if (!eventMods.keySet().contains(evt)) {
			eventMods.clear();
			eventMods.put(evt, new ArrayList<>());
		}
		eventMods.get(evt).add(eventName);
	}

	public void flush() {
		if (flushCommand == null) {
			if(createdLocals.size()>0){
				return;//FIXME - Jade
			}
			Preconditions.checkState(createdLocals.size() == 0);
			return;
		}
		flushCommand = null;
		log(LocalDomDebug.FLUSH, "**flush**");
		if (pendingResolution.size() == 0) {
			boolean detachedAndPending = createdLocals.stream()
					.anyMatch(e -> e.getParentNode() == null);
			if (detachedAndPending) {
				// e.g. dialog box
				return;
			}
			createdLocals.stream().forEach(e -> {
				Element_Jvm el = (Element_Jvm) e;
				if (((Element) el.node).uiObject != null) {
					log(LocalDomDebug.FLUSH,
							((Element) el.node).uiObject.getClass().getName());
				}
			});
		}
		Preconditions.checkState(pendingResolution.size() > 0);
		new ArrayList<>(pendingResolution).stream()
				.forEach(this::ensureResolved);
		debug.checkCreatedLocals(createdLocals);
		createdLocals.clear();
	}

	public boolean isPending(Element element) {
		return pendingResolution.contains(element);
	}

	public boolean isStopPropogation(NativeEvent evt) {
		List<String> list = eventMods.get(evt);
		return list != null && (list.contains("eventStopPropagation")
				|| list.contains("eventCancelBubble"));
	}

	public void useJsoDom() {
		debug.logUseLocal(false);
		LocalDomImpl.useLocalImpl = false;
	}

	public void useJvmDom() {
		localDomImpl.setLocalImpl(new LocalDom_Jvm());
	}

	public void useLocalDom() {
		debug.logUseLocal(true);
		LocalDomImpl.useLocalImpl = true;
	}

	public boolean wasCreatedThisLoop(Element element) {
		return createdLocals.contains(element.implNoResolve());
	}

	private void addToIdLookup(Node node) {
		if (node.provideIsElement()) {
			String id = ((Element) node).getId();
			if (id != null && id.length() > 0) {
				if (idLookup.containsKey(id)&&debug.strict) {
					throw new RuntimeException("duplicate id");
				}
				idLookup.put(id, node);
			}
		}
	}

	private void createJsoNode(Node_Jso item) {
		Node node = null;
		boolean mayHaveId = true;
		try {
			int nodeType = item.getNodeType();
			String nodeName = item.getNodeName();
			node = createNode(nodeType, nodeName);
		} catch (Exception e) {
			// IE9
			if (item.toString().contains("CSSStyle")) {
				StyleElement ieElt = new StyleElement();
				node = ieElt;
			}
			mayHaveId = false;
		}
		javascriptObjectNodeLookup.put(item, node);
		node.putDomImpl(item);
		node.putImpl(item);
		if (mayHaveId) {
			addToIdLookup(node);
		}
	}

	private Node createNode(int nodeType, String nodeName) {
		Node node = null;
		switch (nodeType) {
		case Node.TEXT_NODE:
			node = new Text();
			break;
		case Node.ELEMENT_NODE:
			Supplier<Element> creator = elementCreators
					.get(nodeName.toLowerCase());
			if (creator == null) {
				node = new Element();
			} else {
				node = creator.get();
			}
			break;
		case Node.DOCUMENT_NODE:
			// should already be registered
			throw new UnsupportedOperationException();
		default:
			throw new UnsupportedOperationException();
		}
		return node;
	}

	private void ensureFlush() {
		if (flushCommand == null) {
			if (debug.on) {
				log(LocalDomDebug.ENSURE_FLUSH,
						CommonUtils.highlightForLog("ensure flush"));
			}
			flushCommand = () -> flush();
			Scheduler.get().scheduleFinally(flushCommand);
		}
	}

	private Node_Jso ensurePendingResolutionNode0(Node node) {
		if (node.domImpl() != null) {
			return node.domImpl();
		}
		ensureFlush();
		pendingResolution.add(node);
		boolean useLocalImpl = LocalDomImpl.useLocalImpl;
		ensuringPendingResolutionNode = true;
		useJsoDom();
		Node_Jso nodeDom = null;
		int nodeType = node.getNodeType();
		switch (nodeType) {
		case Node.TEXT_NODE:
			// FIXME - these go to DomImpl, all the document.create calls
			// get
			// rewritten
			nodeDom = localDomImpl.createDomText(Document.get(),
					((Text) node).getData());
			break;
		case Node.ELEMENT_NODE:
			nodeDom = localDomImpl.createDomElement(Document.get(),
					((Element) node).getTagName());
			break;
		case Node.DOCUMENT_NODE:
			nodeDom = doc.domImpl();
			break;
		default:
			throw new UnsupportedOperationException();
		}
		log(LocalDomDebug.CREATED_PENDING_RESOLUTION,
				"created pending resolution node:" + node.implNoResolve().hashCode());
		javascriptObjectNodeLookup.put(nodeDom, node);
		debug.removeAssignment(nodeDom);
		node.putDomImpl(nodeDom);
		ensuringPendingResolutionNode = false;
		if (useLocalImpl) {
			useLocalDom();
		}
		return nodeDom;
	}

	private native String getId(JavaScriptObject obj) /*-{
														return obj.id;
														}-*/;

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

	private Node linkTreesDom(Node_Jso node_jso) {
		Node_Jso original = node_jso;
		// these will be Element_Jvms
		List<Node_Jso> chain = new ArrayList<>();
		// hmmm...ascend to dom, descend...
		Node linkedNode = null;
		while (true) {
			linkedNode = javascriptObjectNodeLookup.get(node_jso);
			chain.add(node_jso);
			if (linkedNode != null) {
				break;
			}
			Node_Jso parentNode0 = node_jso.getParentNode0();
			if (parentNode0 == null) {
				if (chain.size() == 1) {
					int debug = 3;
					// patch for native scroll bars, which use gnarly
					// uibinder
					// innerhtml/element combos
					/*
					 * actually, dropped that code - but datagrid.replace
					 * children arrives here
					 */
					createJsoNode(node_jso);
					return nodeFor0(node_jso);
				} else {
					// unattached dom tree...e.g. mouseout firing on a shadow
					// root
					return null;
				}
			}
			node_jso = parentNode0;
		}
		// don't go to zero (children of this elt)
		for (int idx = chain.size() - 1; idx >= 1; idx--) {
			Node_Jso jso = chain.get(idx);
			Node withDom = (Node) javascriptObjectNodeLookup.get(jso);
			Node_Jvm vmImpl = (Node_Jvm) withDom.localImpl();
			NodeList_Jso childNodesSub = (NodeList_Jso) jso
					.getChildNodes().impl;
			/*
			 * future optimisation - don't create this list if we're mapping to
			 * existing elements - instead have some sort of 'potential state' -
			 * to save getting the whole child list
			 */
			List<Node_Jso> toLink = new ArrayList<>();
			int childCount = childNodesSub.getLength();
			for (int idx2 = 0; idx2 < childCount; idx2++) {
				Node_Jso item0 = childNodesSub.getItem0(idx2);
				switch (item0.getNodeType()) {
				case Node.ELEMENT_NODE:
				case Node.DOCUMENT_NODE:
				case Node.TEXT_NODE:
					toLink.add(item0);
					break;
				}
			}
			/*
			 * either this is:
			 * 
			 * a dom tree with no corresponding element structure (from an
			 * innerhtml) - create all
			 * 
			 * a few top-level dom objects (html, body) - create all
			 * 
			 * a dom tree mapping to an existing element structure - link all
			 * 
			 */
			Document ownerDocument = (Document) (withDom instanceof Document
					? withDom : withDom.getOwnerDocument());
			if (vmImpl != null && (toLink.size() != vmImpl.children.size())
					&& toLink.size() != vmImpl.children.size()) {
				switch (vmImpl.getNodeName()) {
				case "colgroup":
					// frankly don't really care
					toLink.forEach(debug::removeAssignment);
					vmImpl.children.clear();
					break;
				}
			}
			if (vmImpl == null || vmImpl.getChildCount() == 0) {
				// case 1 or 2
				for (int idx2 = 0; idx2 < toLink.size(); idx2++) {
					Node_Jso item = toLink.get(idx2);
					if (!LocalDomImpl.useLocalImpl
							&& javascriptObjectNodeLookup.containsKey(item)) {
						// edge cases in cell widgets
					} else {
						createJsoNode(item);
					}
					// and that's it...subtree created. not currently creating
					// dummy localdomnodes
				}
			} else {
				// if (debug.on) {
				Preconditions
						.checkState(toLink.size() == vmImpl.children.size());
				// }
				for (int idx2 = 0; idx2 < vmImpl.children.size(); idx2++) {
					Node_Jvm child_jvm = vmImpl.children.get(idx2);
					Node_Jso domImpl = toLink.get(idx2);
					Preconditions.checkState(domImpl.getNodeName()
							.equalsIgnoreCase(child_jvm.getNodeName()));
					get().javascriptObjectNodeLookup.put(domImpl,
							child_jvm.node);
					child_jvm.node.putDomImpl(domImpl);
					child_jvm.node.putImpl(domImpl);
				}
			}
		}
		return nodeFor(original);
	}

	private <N extends Node> N nodeFor0(JavaScriptObject jso) {
		Node_Jso o = jso.cast();
		Node node = javascriptObjectNodeLookup.get(o);
		if (node != null) {
			return (N) node;
		}
		String id = getId(o);
		if (id != null && id.length() > 0) {
			node = idLookup.get(id);
			if (node != null) {
				if (node.provideIsLocal()) {
					javascriptObjectNodeLookup.put(o, node);
					node.putDomImpl(o);
					node.putImpl(o);
				}
				return (N) node;
			}
		}
		return (N) linkTreesDom(o);
	}

	private Node nodeFor0(Node_Jvm node_jvm) {
		return localNodeLookup.computeIfAbsent(node_jvm, key -> {
			Node node = createNode(node_jvm.getNodeType(),
					node_jvm.getNodeName());
			node_jvm.node = node;
			node.putImpl(node_jvm);
			return node;
		});
	}

	private void register0(Document doc) {
		this.doc = doc;
		javascriptObjectNodeLookup.put(doc.domImpl(), doc);
		localNodeLookup.put(doc.localImpl, doc);
	}

	private void registerId0(Element_Jvm element_Jvm) {
		String id = element_Jvm.getId();
		if (id.length() > 0) {
			if (idLookup.containsKey(id)) {
				debug.warnDuplicateId(id, idLookup.get(id), element_Jvm);
			}
			idLookup.put(id, element_Jvm.node);
		}
	}

	private Style styleObjectFor0(JavaScriptObject o) {
		Style style = javascriptObjectStyleLookup.get(o);
		if (style != null) {
			return style;
		}
		style = new Style();
		style.impl = o.cast();
		javascriptObjectStyleLookup.put(o, style);
		style.resolved = true;
		return style;
	}

	private Style styleObjectFor0(Style_Jvm style_jvm) {
		return localStyleLookup.computeIfAbsent(style_jvm, key -> {
			Style style = new Style();
			style.impl = style_jvm;
			return style;
		});
	}

	boolean hasPendingResolutionNodes() {
		return pendingResolution.size() > 0;
	}

	public static class LocalDomBridgeCollections {
		public <K, V> Map<K, V> createIdentityEqualsMap(Class<K> keyClass) {
			return new LinkedHashMap<>();
		}

		public Map<String, String> createStringMap() {
			return createIdentityEqualsMap(String.class);
		}
	}

	public static class LocalDomBridgeCollections_Script
			extends LocalDomBridgeCollections {
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

	static class LocalDomBridgeDebug {
		public boolean on = false;

		Set<Node_Jvm> nodesInHierarchy = new LinkedHashSet<>();

		Map<Node_Jso, Element> assigned = new LinkedHashMap<>();

		private boolean strict;

		public void added(Node_Jvm impl) {
			nodesInHierarchy.add(impl);
		}

		public void checkCreatedLocals(List<DomElement> createdLocals) {
			createdLocals.stream().forEach(e -> {
				Node_Jso domImpl = ((Element_Jvm) e).provideAncestorDomImpl();
				if (domImpl == null && ((Element_Jvm) e).parentNode == null) {
					if (nodesInHierarchy.contains(e)) {
						int debug = 3;
					}
				}
			});
		}

		public void checkMultipleAssignment(Element element, Node_Jso nodeDom) {
			if (!get().javascriptObjectNodeLookup.containsKey(nodeDom)) {
				throw new IllegalStateException();
			}
			if (assigned.containsKey(nodeDom)) {
				if (assigned.get(nodeDom) != element &&debug.strict) {
					throw new IllegalStateException();
				}
			}
			assigned.put(nodeDom, element);
		}

		public void logUseLocal(boolean b) {
		}

		public void removeAssignment(Node_Jso nodeDom) {
			assigned.remove(nodeDom);
		}

		public void removed(Node_Jvm oldChild_Jvm) {
			nodesInHierarchy.remove(oldChild_Jvm);
		}

		public void warnDuplicateId(String id, Node node,
				Element_Jvm element_Jvm) {
			log(LocalDomDebug.DUPLICATE_ELT_ID,
					"**warn - duplicate elt id - " + id);
		}

		public void log(LocalDomDebug channel, String message) {
			switch (channel) {
			case DOM_MOUSE_EVENT:
			case DOM_EVENT:
			case DUPLICATE_ELT_ID:
			case DISPATCH_DETAILS:
			case DUMP_LOCAL:
				System.out.println(message);
				break;
			default:
				break;
			}
		}
	}

	public static void log(LocalDomDebug channel, String message) {
		debug.log(channel, message);
	}
}
