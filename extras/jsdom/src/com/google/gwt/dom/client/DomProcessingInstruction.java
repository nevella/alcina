package com.google.gwt.dom.client;

public interface DomProcessingInstruction extends DomNode {
	String getData();

	String getTarget();

	void setData(String data);
}
