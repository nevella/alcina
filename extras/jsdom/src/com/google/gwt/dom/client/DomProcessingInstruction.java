package com.google.gwt.dom.client;

public interface DomProcessingInstruction extends ClientNode {
	String getData();

	String getTarget();

	void setData(String data);
}
