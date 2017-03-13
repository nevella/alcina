package com.google.gwt.dom.client;

import com.google.gwt.core.client.SingleJsoImpl;

public interface DomNodeList<T extends Node> {

	T getItem(int index);

	int getLength();
}
