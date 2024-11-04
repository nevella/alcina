package com.google.gwt.dom.client;

import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;

public class TextAttachId extends NodeAttachId implements ClientDomText {
	TextAttachId(Node node) {
		super(node);
	}

	void appendUnescaped(UnsafeHtmlBuilder builder) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteData(int offset, int length) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getData() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLength() {
		throw new UnsupportedOperationException();
	}

	@Override
	public short getNodeType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNodeValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexInParentChildren() {
		throw new UnsupportedOperationException();
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
		setNodeValue(data);
	}

	@Override
	public void setNodeValue(String nodeValue) {
		MutationRecord record = new MutationRecord();
		record.type = MutationRecord.Type.characterData;
		record.target = MutationNode.attachId(node());
		record.newValue = nodeValue;
		emitMutation(record);
	}

	@Override
	public Text splitText(int offset) {
		throw new UnsupportedOperationException();
	}
}
