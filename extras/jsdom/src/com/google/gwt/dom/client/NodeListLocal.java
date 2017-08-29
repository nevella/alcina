package com.google.gwt.dom.client;

import java.util.List;
import java.util.stream.Stream;

class NodeListLocal<T extends Node> implements DomNodeList<T> {
	private List<NodeLocal> nodes;

	NodeListLocal(List<NodeLocal> nodes) {
		this.nodes = nodes;
	}

	@Override
	public T getItem(int index) {
		return (T) nodes.get(index).nodeFor();
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
		return DomNodeListStatic.stream0(this);
	}
}
