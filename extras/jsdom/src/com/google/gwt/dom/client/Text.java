/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dom.client;

import org.w3c.dom.DOMException;

import cc.alcina.framework.common.client.util.Ax;

/**
 * The Text interface represents textual content.
 */
public class Text extends Node implements ClientDomText, org.w3c.dom.Text {
	/**
	 * Assert that the given {@link Node} is of type {@link Node#TEXT_NODE} and
	 * automatically typecast it.
	 */
	public static Text as(Node node) {
		assert node.getNodeType() == Node.TEXT_NODE;
		return (Text) node;
	}

	private TextLocal local;

	private ClientDomText remote;

	protected Text(TextLocal local) {
		this.local = local;
	}

	@Override
	public void appendData(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Text cast() {
		return this;
	}

	@Override
	public void deleteData(int offset, int length) {
		local().deleteData(offset, length);
		sync(() -> remote().deleteData(offset, length));
	}

	@Override
	public String getData() {
		return local().getData();
	}

	@Override
	public int getLength() {
		return local().getLength();
	}

	@Override
	public String getTextContent() throws DOMException {
		return getData();
	}

	@Override
	public String getWholeText() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void insertData(int offset, String data) {
		local().insertData(offset, data);
		sync(() -> remote().insertData(offset, data));
	}

	@Override
	public boolean isElementContentWhitespace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TextJso jsoRemote() {
		return (TextJso) remote();
	}

	@Override
	protected TextLocal local() {
		return local;
	}

	@Override
	public Node node() {
		return this;
	}

	@Override
	protected void putRemote(ClientDomNode remote) {
		this.remote = (ClientDomText) remote;
	}

	@Override
	protected ClientDomText remote() {
		return remote;
	}

	@Override
	public void replaceData(int offset, int length, String data) {
		local().replaceData(offset, length, data);
		sync(() -> remote().replaceData(offset, length, data));
	}

	@Override
	public org.w3c.dom.Text replaceWholeText(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetRemote0() {
		this.remote = null;
	}

	@Override
	public void setData(String data) {
		local().setData(data);
		sync(() -> remote().setData(data));
	}

	/**
	 * Because empty text nodes are discouraged (for local/remote sync), this
	 * varies from the w3c node spec by throwing an exception if the created
	 * text node is empty
	 */
	@Override
	public Text splitText(int offset) {
		String contents = getTextContent();
		setTextContent(contents.substring(0, offset));
		String newNodeContents = contents.substring(offset);
		if (newNodeContents.isEmpty()) {
			throw new IllegalStateException();
		}
		Text createdNode = getOwnerDocument().createTextNode(newNodeContents);
		getParentElement().insertAfter(createdNode, this);
		return createdNode;
	}

	@Override
	public String substringData(int arg0, int arg1) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return Ax.format("#TEXT[%s]", local().getData());
	}
}
