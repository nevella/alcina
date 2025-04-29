package com.google.gwt.dom.client;

import com.google.gwt.dom.client.mutations.SelectionRecord;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public class SelectionLocal implements ClientDomSelection {
	private Selection selectionObject;

	private SelectionRecord selectionRecord;

	public SelectionRecord getSelectionRecord() {
		if (selectionRecord == null) {
			selectionRecord = selectionObject.remote().getSelectionRecord()
					.copy();
			selectionRecord.populateNodes();
		}
		return selectionRecord;
	}

	public SelectionLocal(Selection selection) {
		this.selectionObject = selection;
	}

	public void onDocumentEventSystemInit() {
		Event.addNativePreviewHandler(this::conditionallyInvalidateSelection);
	}

	void conditionallyInvalidateSelection(NativePreviewEvent event) {
		switch (event.getNativeEvent().getType()) {
		case BrowserEvents.CLICK:
		case BrowserEvents.DBLCLICK:
		case BrowserEvents.MOUSEDOWN:
		case BrowserEvents.MOUSEUP:
		case BrowserEvents.KEYDOWN:
		case BrowserEvents.KEYUP:
		case BrowserEvents.SELECTIONCHANGE:
			selectionRecord = null;
			break;
		}
	}

	@Override
	public Selection selectionObject() {
		return selectionObject;
	}

	@Override
	public void collapse(Node node) {
		collapse(node, 0);
	}

	@Override
	public void collapse(Node node, int offset) {
		SelectionRecord selectionRecord = getSelectionRecord().copy();
		selectionRecord.anchorNode = node;
		selectionRecord.anchorOffset = offset;
		selectionRecord.focusNode = selectionRecord.anchorNode;
		selectionRecord.focusOffset = selectionRecord.anchorOffset;
		selectionRecord.populateNodeIds();
		this.selectionRecord = selectionRecord;
	}

	@Override
	public void extend(Node node) {
		extend(node, 0);
	}

	@Override
	public void extend(Node node, int offset) {
		SelectionRecord selectionRecord = getSelectionRecord();
		selectionRecord.focusNode = node;
		selectionRecord.focusOffset = offset;
		selectionRecord.populateNodeIds();
		this.selectionRecord = selectionRecord;
	}

	@Override
	public Node getAnchorNode() {
		return getSelectionRecord().anchorNode;
	}

	@Override
	public int getAnchorOffset() {
		return getSelectionRecord().anchorOffset;
	}

	@Override
	public DomRect getClientRect() {
		return getSelectionRecord().clientRect;
	}

	@Override
	public Node getFocusNode() {
		return getSelectionRecord().focusNode;
	}

	@Override
	public int getFocusOffset() {
		return getSelectionRecord().focusOffset;
	}

	@Override
	public String getType() {
		return getSelectionRecord().type;
	}

	@Override
	public boolean isCollapsed() {
		return getSelectionRecord().isCollapsed();
	}

	@Override
	public void modify(String alter, String direction, String granularity) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'modify'");
	}

	@Override
	public void removeAllRanges() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'removeAllRanges'");
	}

	void validate() {
		getSelectionRecord().focusOffset = validateOffset(getFocusNode(),
				getFocusOffset());
		getSelectionRecord().anchorOffset = validateOffset(getAnchorNode(),
				getAnchorOffset());
	}

	int validateOffset(Node node, int offset) {
		if (node != null) {
			DomNode domNode = node.asDomNode();
			if (domNode.isText() && offset > domNode.textContent().length()) {
				return domNode.textContent().length();
			}
		}
		return offset;
	}

	@Override
	public String toString() {
		return getSelectionRecord().toNodeString();
	}
}
