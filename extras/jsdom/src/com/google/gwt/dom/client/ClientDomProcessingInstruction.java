package com.google.gwt.dom.client;

public interface ClientDomProcessingInstruction extends ClientDomNode {
	String getData();

	String getTarget();

	void setData(String data);
}
