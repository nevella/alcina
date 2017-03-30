package com.google.gwt.dom.client;

import java.util.List;
import java.util.stream.Stream;

class NodeList_Jvm<T extends Node> implements DomNodeList<T> {
	private List<Node_Jvm> nodes;

	NodeList_Jvm(List<Node_Jvm> nodes) {
		this.nodes = nodes;
	}

	@Override
	public T getItem(int index) {
		return (T) LocalDomBridge.nodeFor(nodes.get(index));
	}

	/**
	 * The number of nodes in the list. The range of valid child node indices is
	 * 0 to length-1 inclusive.
	 */
	@Override
	public int getLength() {
		return nodes.size();
	}
	@Override
	public Stream<T> stream() {
		return DomNodeList_Static.stream0(this);
	}
}
