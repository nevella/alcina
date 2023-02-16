package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;

class NodeIndex {
	static NodeIndex forNode(NodeRemote node) {
		NodeIndex result = new NodeIndex();
		NodeRemote cursor = node;
		while (true) {
			result.indices.add(0, cursor.indexInParentChildren());
			cursor = cursor.getParentNodeRemote();
			if (cursor == null) {
				break;
			} else if (cursor.getNodeType() == Node.DOCUMENT_NODE) {
				result.attached = true;
				break;
			}
		}
		return result;
	}

	List<Integer> indices = new ArrayList<>();

	boolean attached;

	Node getNode() {
		Node cursor = Document.get().getDocumentElement();
		int idx = 1;
		while (idx < indices.size()) {
			int index = indices.get(idx++);
			cursor = cursor.getChild(index);
		}
		return cursor;
	}
}