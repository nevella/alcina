package com.google.gwt.dom.client;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class Text_Jvm extends Node_Jvm implements DomText {
	private String text;

	Text_Jvm(String text) {
		this.text = text;
	}

	@Override
	public void deleteData(int offset, int length) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getData() {
		return text;
	}

	@Override
	public int getLength() {
		return text.length();
	}

	@Override
	public String getNodeName() {
		return "#text";
	}

	@Override
	public short getNodeType() {
		return Node.TEXT_NODE;
	}

	@Override
	public String getNodeValue() {
		return text;
	}

	@Override
	public void insertData(int offset, String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceData(int offset, int length, String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setData(String data) {
		this.text = data;
	}

	@Override
	public void setNodeValue(String nodeValue) {
		this.text = nodeValue;
	}

	@Override
	public Text splitText(int offset) {
		throw new UnsupportedOperationException();
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		builder.appendEscaped(text);
	}
}
