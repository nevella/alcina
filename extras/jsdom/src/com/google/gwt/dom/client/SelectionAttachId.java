package com.google.gwt.dom.client;

import com.google.gwt.dom.client.mutations.SelectionRecord;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;

public class SelectionAttachId implements ClientDomSelection {
	private Selection selectionObject;

	private SelectionRecord selectionRecord;

	public void setSelectionRecord(SelectionRecord selectionRecord) {
		this.selectionRecord = selectionRecord;
	}

	public SelectionRecord getSelectionRecord() {
		return selectionRecord;
	}

	public SelectionAttachId(Selection selection) {
		this.selectionObject = selection;
	}

	@Override
	public Selection selectionObject() {
		return selectionObject;
	}

	@Override
	public void collapse(Node node) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'collapse'");
	}

	@Override
	public void collapse(Node node, int offset) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'collapse'");
	}

	@Override
	public void extend(Node node) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'extend'");
	}

	@Override
	public void extend(Node node, int offset) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'extend'");
	}

	@Override
	public Node getAnchorNode() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getAnchorNode'");
	}

	@Override
	public int getAnchorOffset() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getAnchorOffset'");
	}

	@Override
	public DomRect getClientRect() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getClientRect'");
	}

	@Override
	public Node getFocusNode() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getFocusNode'");
	}

	@Override
	public int getFocusOffset() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getFocusOffset'");
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getType'");
	}

	@Override
	public boolean isCollapsed() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'isCollapsed'");
	}

	@Override
	public void modify(String alter, String direction, String granularity) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'modify'");
	}

	@Override
	public void removeAllRanges() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'removeAllRanges'");
	}
}
