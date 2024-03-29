package com.google.gwt.dom.client;

public interface ClientDomText extends ClientDomNode {
	void deleteData(int offset, int length);

	String getData();

	int getLength();

	void insertData(int offset, String data);

	void replaceData(int offset, int length, String data);

	void setData(String data);

	Text splitText(int offset);
}
