package com.google.gwt.dom.client;

import java.util.List;

class NodeList_Wrapped<T extends Node> implements DomNodeList<T> {
	private List<Node> nodes;

	NodeList_Wrapped(List<Node> nodes) {
		this.nodes = nodes;
	}

	@Override
	public T getItem(int index) {
		return (T)nodes.get(index);
	}

	/**
	 * The number of nodes in the list. The range of valid child node indices is
	 * 0 to length-1 inclusive.
	 */
	@Override
	public int getLength() {
		return nodes.size();
	}
}
