package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;

public class VmLocalDomBridge {
	static VmLocalDomBridge bridge = null;

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

	public static Node_Jso ensurePendingResolutionNode(Node node) {
		return get().ensurePendingResolutionNode0(node);
	}

	public static void ensureResolved(Node node) {
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
				dom_elt.setAttribute(e.getKey(), e.getValue());
			});
			vmlocal_elt.getStyle().getProperties().entrySet().forEach(e -> {
				Style domStyle = dom_elt.getStyle();
				domStyle.setProperty(e.getKey(), e.getValue());
			});
			int bits = ((Element_Jvm) vmlocal_elt).orSunkEventsOfAllChildren(0);
			DOM.sinkEvents(elem, bits);
		}
	}

	public synchronized static VmLocalDomBridge get() {
		if (bridge == null) {
			bridge = new VmLocalDomBridge();
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

	// FIXME - better map
	private static Map<String, String> declarativeCssNames = new LinkedHashMap<>();

	Map<JavaScriptObject, Node> javascriptObjectNodeLookup;

	Map<JavaScriptObject, Style> javascriptObjectStyleLookup;

	Map<Node_Jvm, Node> nodeJvmLookup;

	Map<Style_Jvm, Style> styleJvmLookup;

	Map<String, Node> idLookup;

	private Document doc;

	Map<String, Supplier<Element>> elementCreators;

	List<Node> pendingResolution = new ArrayList<>();

	VmLocalDOMImpl vmLocalDomImpl;

	private VmLocalDomBridge() {
		// FIXME - weak maps
		javascriptObjectNodeLookup = new LinkedHashMap<>();
		javascriptObjectStyleLookup = new LinkedHashMap<>();
		idLookup = new LinkedHashMap<>();
		elementCreators = new LinkedHashMap<>();
		initElementCreators();
		nodeJvmLookup = new LinkedHashMap<>();
		styleJvmLookup = new LinkedHashMap<>();
	}

	private void initElementCreators() {
		elementCreators.put(DivElement.TAG, () -> new DivElement());
		elementCreators.put(SpanElement.TAG, () -> new SpanElement());
		elementCreators.put(BodyElement.TAG, () -> new BodyElement());
		elementCreators.put(ButtonElement.TAG, () -> new ButtonElement());
		elementCreators.put(StyleElement.TAG, () -> new StyleElement());
		elementCreators.put(TableElement.TAG, () -> new TableElement());
		elementCreators.put(HeadElement.TAG, () -> new HeadElement());
		elementCreators.put(TableSectionElement.TAG_TBODY, () -> new TableSectionElement());
		elementCreators.put(TableSectionElement.TAG_TFOOT, () -> new TableSectionElement());
		elementCreators.put(TableSectionElement.TAG_THEAD, () -> new TableSectionElement());
		elementCreators.put(TableCaptionElement.TAG, () -> new HeadElement());
		elementCreators.put(TableCellElement.TAG_TD, () -> new TableCellElement());
		elementCreators.put(TableCellElement.TAG_TH, () -> new TableCellElement());
		
		elementCreators.put(TableColElement.TAG_COL, () -> new TableColElement());
		elementCreators.put(TableColElement.TAG_COLGROUP, () -> new TableColElement());
		elementCreators.put(TableRowElement.TAG, () -> new TableRowElement());
		elementCreators.put(HeadElement.TAG, () -> new HeadElement());
		elementCreators.put(HeadElement.TAG, () -> new HeadElement());
		
	}

	public void flush() {
		pendingResolution.stream().forEach(VmLocalDomBridge::ensureResolved);
		pendingResolution.clear();
		vmLocalDomImpl.useVmLocalImpl = false;
	}

	public void useJvmDom() {
		vmLocalDomImpl.setVmLocalImpl(new VmLocalDom_Jvm());
	}

	public void useVmLocalDom() {
		vmLocalDomImpl.useVmLocalImpl = true;
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
				System.out.println("creating: " + nodeName);
				node = elementCreators.get(nodeName.toLowerCase()).get();
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

	private Node_Jso ensurePendingResolutionNode0(Node node) {
		if (node.domImpl != null) {
			return node.domImpl;
		}
		pendingResolution.add(node);
		boolean useVmLocalImpl = vmLocalDomImpl.useVmLocalImpl;
		try {
			vmLocalDomImpl.useVmLocalImpl = false;
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
				nodeDom = doc
						.createElement(((Element) node).getTagName()).domImpl;
				break;
			case Node.DOCUMENT_NODE:
				nodeDom = doc.domImpl;
				break;
			default:
				throw new UnsupportedOperationException();
			}
			javascriptObjectNodeLookup.put(nodeDom, node);
			node.putDomImpl(nodeDom);
			node.vmLocal = false;
			return nodeDom;
		} finally {
			vmLocalDomImpl.useVmLocalImpl = useVmLocalImpl;
		}
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
		node.putDomImpl(o.cast());
		node.putImpl(o.cast());
		node.resolved = true;
		node.vmLocal = false;
		addToIdLookup(node);
		javascriptObjectNodeLookup.put(o, node);
		return (N) node;
	}

	private Node nodeFor0(Node_Jvm node_jvm) {
		return nodeJvmLookup.computeIfAbsent(node_jvm, key -> {
			Node node = createNode(node_jvm.getNodeType(),
					node_jvm.getNodeName());
			node.vmLocal = true;
			node_jvm.node = node;
			node.putImpl(node_jvm);
			return node;
		});
	}

	private void register0(Document doc) {
		this.doc = doc;
		javascriptObjectNodeLookup.put(doc.domImpl, doc);
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
		return styleJvmLookup.computeIfAbsent(style_jvm, key -> {
			Style style = new Style();
			style.impl = style_jvm;
			return style;
		});
	}

	public static Element_Jso elementJso(Element elem) {
		return elem.typedDomImpl;
	}

	public static void registerId(Element_Jvm element_Jvm) {
		get().registerId0(element_Jvm);
	}

	private void registerId0(Element_Jvm element_Jvm) {
		idLookup.put(element_Jvm.getId(), element_Jvm.node);
	}
}
