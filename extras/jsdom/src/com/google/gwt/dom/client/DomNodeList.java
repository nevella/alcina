package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gwt.core.client.SingleJsoImpl;

public interface DomNodeList<T extends Node> {

	T getItem(int index);

	int getLength();
	
	Stream<T> stream();
	
	
}
