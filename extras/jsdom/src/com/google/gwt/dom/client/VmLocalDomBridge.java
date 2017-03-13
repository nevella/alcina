package com.google.gwt.dom.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gwt.core.client.JavaScriptObject;

public class VmLocalDomBridge {
	static VmLocalDomBridge bridge = null;

	public static Node_Dom ensurePendingResolutionNode(Node node) {
		return get().ensurePendingResolutionNode0(node);
	}

	public static <N extends Node> N nodeFor(JavaScriptObject o) {
		if (o == null) {
			return null;
		}
		return get().nodeFor0(o);
	}

	public static Node nodeFor(Node_Jvm node_Jvm) {
		if (node_Jvm == null) {
			return null;
		}
		return get().nodeFor0(node_Jvm);
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

	synchronized static VmLocalDomBridge get() {
		if (bridge == null) {
			bridge = new VmLocalDomBridge();
		}
		return bridge;
	}

	Map<JavaScriptObject, Node> javascriptObjectNodeLookup;

	Map<JavaScriptObject, Style> javascriptObjectStyleLookup;

	Map<Node_Jvm, Node> nodeJvmLookup;

	Map<String, Node> idLookup;

	private Document doc;

	private VmLocalDomBridge() {
		// FIXME - weak maps
		javascriptObjectNodeLookup = new LinkedHashMap<>();
		javascriptObjectStyleLookup = new LinkedHashMap<>();
		idLookup = new LinkedHashMap<>();
		elementCreators = new LinkedHashMap<>();
		elementCreators.put(DivElement.TAG, () -> new DivElement());
		elementCreators.put(BodyElement.TAG, () -> new BodyElement());
	}

	Map<String, Supplier<Element>> elementCreators;

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

	private Node_Dom ensurePendingResolutionNode0(Node node) {
		if (node.domImpl != null) {
			return node.domImpl;
		}
		Node_Dom nodeDom = null;
		int nodeType = node.getNodeType();
		switch (nodeType) {
		case Node.TEXT_NODE:
			// FIXME - these go to DomImpl, all the document.create calls get
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
		javascriptObjectNodeLookup.put(nodeDom, node);
		node.domImpl = nodeDom;
		node.vmLocal = false;
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
		node.domImpl = o.cast();
		node.impl = node.domImpl;
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
			node.impl = node_jvm;
			addToIdLookup(node);
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

	public static void ensureResolved(Node node) {
		if (node.domImpl == null) {
			throw new UnsupportedOperationException();
		}
	}
}
