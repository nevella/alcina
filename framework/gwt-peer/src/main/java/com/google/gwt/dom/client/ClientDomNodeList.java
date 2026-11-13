package com.google.gwt.dom.client;

import java.util.stream.Stream;

public interface ClientDomNodeList<T extends Node> {
	T getItem(int index);

	int getLength();

	Stream<T> stream();
}
