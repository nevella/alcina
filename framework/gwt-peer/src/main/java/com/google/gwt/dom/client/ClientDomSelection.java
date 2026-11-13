package com.google.gwt.dom.client;

import com.google.gwt.dom.client.mutations.SelectionRecord;

public interface ClientDomSelection {
	Selection selectionObject();

	void collapse(Node node);

	void collapse(Node node, int offset);

	void extend(Node node);

	void extend(Node node, int offset);

	Node getAnchorNode();

	int getAnchorOffset();

	DomRect getClientRect();

	Node getFocusNode();

	int getFocusOffset();

	String getType();

	boolean isCollapsed();

	void modify(String alter, String direction, String granularity);

	void removeAllRanges();

	SelectionRecord getSelectionRecord();
}
