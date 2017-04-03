package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.DOM;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class LocalDomBridge {
	static LocalDomBridge bridge = null;

	// FIXME - better map
	private static Map<String, String> declarativeCssNames = new LinkedHashMap<>();

	static LocalDomBridgeDebug debug = new LocalDomBridgeDebug();

	private static boolean ensuring = false;

	public static boolean resolving;

	static boolean ensuringPendingResolutionNode;

	public static String declarativeCssName(String key) {
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

	public static Element_Jso elementJso(Element elem) {
		return elementJso(elem, true);
	}

	public static Element_Jso elementJso(Element elem, boolean maybeEnsure) {
		if (!LocalDomImpl.useLocalImpl && maybeEnsure
				&& elem.typedDomImpl != null) {
			ensureJso(elem);
		}
		return elem.typedDomImpl;
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

	static void ensureJso(Element element, boolean flush) {
		if (get().flushCommand != null && flush
				&& get().pendingResolution.size() > 0) {
			get().flush();
		}
		if (element.typedDomImpl != null) {
			return;
		}
		if (ensuring) {
			return;
		}
		try {
			ensuring = true;
			if (debug.on) {
				Ax.format("ensure jso - %s\n", element);
				new Exception().printStackTrace();
				System.out.println("\n\n*****\n\n");
			}
			String id = element.getId();
			if (!id.isEmpty()) {
				Element_Jso domImpl = Document.get().typedDomImpl
						.getElementById0(id);
				get().javascriptObjectNodeLookup.put(domImpl, element);
				element.putDomImpl(domImpl);
				element.putImpl(domImpl);
				return;
			}
			Element original = element;
			// these will be Element_Jvms
			List<Element> chain = new ArrayList<>();
			// hmmm...ascend to dom, descend...
			while (element.typedDomImpl == null) {
				chain.add(element);
				element = element.getParentElement();
			}
			chain.add(element);
			// don't go to zero (children of this elt)
			for (int idx = chain.size() - 1; idx >= 1; idx--) {
				Element withDom = chain.get(idx);
				Element_Jvm vmImpl = (Element_Jvm) withDom.localImpl();
				NodeList_Jso<Element> childNodes = (NodeList_Jso) withDom.typedDomImpl
						.getChildNodes().impl;
				if (debug.on) {
					Preconditions.checkState(
							childNodes.getLength() == vmImpl.children.size());
				}
				for (int idx2 = 0; idx2 < vmImpl.children.size(); idx2++) {
					Element_Jvm child_jvm = (Element_Jvm) vmImpl.children
							.get(idx2);
					int len = childNodes.getLength();
					Node_Jso domImpl = childNodes.getItem0(idx2);
					Node_Jso domImplCopy = domImpl;
					if (debug.on) {
						Preconditions.checkState(domImpl.getNodeName()
								.equalsIgnoreCase(child_jvm.getNodeName()));
					}
					get().javascriptObjectNodeLookup.put(domImpl,
							child_jvm.node);
					((Element) child_jvm.node).putDomImpl(domImpl);
					((Element) child_jvm.node).putImpl(domImpl);
				}
			}
		} finally {
			ensuring = false;
		}
	}

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

	private LocalDomBridge() {
		// FIXME - weak maps
		javascriptObjectNodeLookup = new LinkedHashMap<>();
		javascriptObjectStyleLookup = new LinkedHashMap<>();
		idLookup = new LinkedHashMap<>();
		elementCreators = new LinkedHashMap<>();
		initElementCreators();
		localNodeLookup = new LinkedHashMap<>();
		localStyleLookup = new LinkedHashMap<>();
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

	public void ensureResolved(Node node) {
		if (node.domImpl == null) {
			throw new UnsupportedOperationException();
		}
		if (node.impl != null && node.impl != node.domImpl) {
			Element elem = (Element) node;
			Element_Jso dom_elt = (Element_Jso) node.domImpl;
			DomElement vmlocal_elt = (DomElement) node.impl;
			dom_elt.setInnerHTML(vmlocal_elt.getInnerHTML());
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
			node.putImpl(node.domImpl);
		}
	}

	public void flush() {
		if (flushCommand == null) {
			Preconditions.checkState(createdLocals.size() == 0);
			return;
		}
		flushCommand = null;
		System.out.println("**flush**");
		if (pendingResolution.size() == 0) {
			createdLocals.stream().forEach(e -> {
				Element_Jvm el = (Element_Jvm) e;
				if (((Element) el.node).uiObject != null) {
					System.out.println(
							((Element) el.node).uiObject.getClass().getName());
				}
			});
			int debug = 3;
		}
		Preconditions.checkState(pendingResolution.size() > 0);
		new ArrayList<>(pendingResolution).stream()
				.forEach(this::ensureResolved);
		debug.checkCreatedLocals(createdLocals);
		createdLocals.clear();
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
		return createdLocals.contains(element.impl);
	}

	private void addToIdLookup(Node node) {
		if (node.provideIsElement()) {
			String id = ((Element) node).getId();
			if (id != null && id.length() > 0) {
				if (idLookup.containsKey(id)) {
					throw new RuntimeException("duplicate id");
				}
				idLookup.put(id, node);
			}
		}
	}

	private Node createNode(int nodeType, String nodeName) {
		Node node = null;
		switch (nodeType) {
		case Node.TEXT_NODE:
			node = new Text();
			break;
		case Node.ELEMENT_NODE:
			switch (nodeName.toLowerCase()) {
			case "html":
				node = new Element();
				break;
			default:
				Supplier<Element> creator = elementCreators
						.get(nodeName.toLowerCase());
				if (creator == null) {
					GWT.log(CommonUtils.highlightForLog(
							"Missing element creator - %s", nodeName));
				}
				node = creator.get();
				break;
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
				System.out.println(CommonUtils.highlightForLog("ensure flush"));
				new Exception().printStackTrace(System.out);
			}
			flushCommand = () -> flush();
			Scheduler.get().scheduleFinally(flushCommand);
		}
	}

	private Node_Jso ensurePendingResolutionNode0(Node node) {
		if (node.domImpl != null) {
			return node.domImpl;
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
			nodeDom = doc.createTextNode(((Text) node).getData()).domImpl;
			break;
		case Node.ELEMENT_NODE:
			nodeDom = doc.createElement(((Element) node).getTagName()).domImpl;
			break;
		case Node.DOCUMENT_NODE:
			nodeDom = doc.domImpl;
			break;
		default:
			throw new UnsupportedOperationException();
		}
		System.out.println("pending:" + node.impl.hashCode());
		if (!javascriptObjectNodeLookup.containsKey(nodeDom)) {
			Preconditions.checkState(false);
			javascriptObjectNodeLookup.put(nodeDom, node);
		}
		debug.removeAssignment(nodeDom);
		node.putDomImpl(nodeDom);
		ensuringPendingResolutionNode = false;
		if(useLocalImpl){
			useLocalDom();
		}
		return nodeDom;
	}

	private native String getId(JavaScriptObject obj) /*-{
        return obj.id;
	}-*/;

	private native String getNodeName(JavaScriptObject obj) /*-{
        return obj.nodeName;
	}-*/;

	private native int getNodeType(JavaScriptObject obj) /*-{
        return obj.nodeType;
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
		elementCreators.put("SVG", () -> new Element());
	}

	private <N extends Node> N nodeFor0(JavaScriptObject o) {
		Node node = javascriptObjectNodeLookup.get(o);
		if (node != null) {
			return (N) node;
		}
		String id = getId(o);
		if (id != null && id.length() > 0) {
			node = idLookup.get(id);
			if (node != null) {
				return (N) node;
			}
		}
		int nodeType = getNodeType(o);
		String nodeName = getNodeName(o);
		node = createNode(nodeType, nodeName);
		node.resolved = true;
		javascriptObjectNodeLookup.put(o, node);
		node.putDomImpl(o.cast());
		node.putImpl(o.cast());
		addToIdLookup(node);
		return (N) node;
	}

	private Node nodeFor0(Node_Jvm node_jvm) {
		return localNodeLookup.computeIfAbsent(node_jvm, key -> {
			Node node = createNode(node_jvm.getNodeType(),
					node_jvm.getNodeName());
			node.local = true;
			node_jvm.node = node;
			node.putImpl(node_jvm);
			return node;
		});
	}

	private void register0(Document doc) {
		this.doc = doc;
		javascriptObjectNodeLookup.put(doc.domImpl, doc);
		localNodeLookup.put(doc.localImpl, doc);
	}

	private void registerId0(Element_Jvm element_Jvm) {
		idLookup.put(element_Jvm.getId(), element_Jvm.node);
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

	static class LocalDomBridgeDebug {
		public boolean on = false;

		Set<Node_Jvm> nodesInHierarchy = new LinkedHashSet<>();

		Map<Node_Jso, Element> assigned = new LinkedHashMap<>();

		public void added(Node_Jvm impl) {
			nodesInHierarchy.add(impl);
			// System.out.println("add:" + impl.hashCode());
		}

		public void logUseLocal(boolean b) {
//			System.out.println("use local:"+b);
			
		}

		public void removeAssignment(Node_Jso nodeDom) {
			assigned.remove(nodeDom);
		}

		public void checkCreatedLocals(List<DomElement> createdLocals) {
			createdLocals.stream().forEach(e -> {
				Node_Jso domImpl = ((Element_Jvm) e).provideAncestorDomImpl();
				if (domImpl == null && ((Element_Jvm) e).parentNode == null) {
					if (nodesInHierarchy.contains(e)) {
						// System.out.println("Orphan:" + e.hashCode());
						int debug = 3;
					}
				}
			});
		}

		public void removed(Node_Jvm oldChild_Jvm) {
			nodesInHierarchy.remove(oldChild_Jvm);
			// System.out.println("remove:" + oldChild_Jvm.hashCode());
		}

		public void checkMultipleAssignment(Element element, Node_Jso nodeDom) {
			// System.out.println("check:" + nodeDom.hashCode());
			// System.out.println("check:" + nodeDom);
			// new Exception().printStackTrace(System.out);
			if (!get().javascriptObjectNodeLookup.containsKey(nodeDom)) {
				int debug = 3;
			}
			if (assigned.containsKey(nodeDom)) {
				if (assigned.get(nodeDom) != element) {
					int debug = 3;
				}
			}
			assigned.put(nodeDom, element);
		}
	}

	public static void replaceWithJso(Element element) {
		boolean saveLocalImpl = LocalDomImpl.useLocalImpl;
		get().useJsoDom();
		LocalDomElement localDomElement = element.provideLocalDomElement();
		Element_Jso element_Jso = get().localDomImpl
				.createDomElement(Document.get(), element.getTagName());
		element_Jso.setInnerHTML(localDomElement.getPendingInnerHtml());
		get().javascriptObjectNodeLookup.put(element_Jso, element);
		element.putDomImpl(element_Jso);
		element.putImpl(element_Jso);
		if(saveLocalImpl){
			get().useLocalDom();
		}
	}
}
