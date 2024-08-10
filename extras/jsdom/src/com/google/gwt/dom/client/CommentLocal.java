package com.google.gwt.dom.client;

import cc.alcina.framework.common.client.util.Ax;

public class CommentLocal extends NodeLocal implements ClientDomComment {
	private String text;

	private Comment commentNode;

	CommentLocal(DocumentLocal documentLocal, String text) {
		this.ownerDocument = documentLocal;
		setData(text);
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		builder.appendUnsafeHtml("<!--");
		builder.appendUnsafeHtml(text);
		builder.appendUnsafeHtml("-->");
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		// builder.append(getData());
		// definitely not part of innertext
	}

	void appendUnescaped(UnsafeHtmlBuilder builder) {
		builder.appendUnsafeHtml(text);
	}

	@Override
	public Node cloneNode(boolean deep) {
		return getOwnerDocument().createComment(getData());
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
		return "#comment";
	}

	@Override
	public short getNodeType() {
		return Node.COMMENT_NODE;
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
	public Node node() {
		return commentNode;
	}

	void putComment(Comment commentNode) {
		this.commentNode = commentNode;
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
		setData(nodeValue);
	}

	@Override
	public Text splitText(int offset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return Ax.format("#COMMENT[%s]", getData());
	}
}
