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
 * The Comment interface represents textual content.
 */
public class Comment extends Node
		implements ClientDomComment, org.w3c.dom.Comment {
	/**
	 * Assert that the given {@link Node} is of type {@link Node#COMMENT_NODE}
	 * and automatically typecast it.
	 */
	public static Comment as(Node node) {
		assert node.getNodeType() == Node.COMMENT_NODE;
		return (Comment) node;
	}

	private CommentLocal local;

	private ClientDomComment remote;

	protected Comment(CommentLocal local) {
		this.local = local;
	}

	@Override
	public void appendData(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Comment cast() {
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
	public void insertData(int offset, String data) {
		local().insertData(offset, data);
		sync(() -> remote().insertData(offset, data));
	}

	@Override
	public CommentJso jsoRemote() {
		return (CommentJso) remote();
	}

	@Override
	protected CommentLocal local() {
		return local;
	}

	@Override
	public Node node() {
		return this;
	}

	@Override
	protected void putRemote(ClientDomNode remote) {
		this.remote = (ClientDomComment) remote;
	}

	@Override
	protected ClientDomComment remote() {
		return remote;
	}

	@Override
	public void replaceData(int offset, int length, String data) {
		local().replaceData(offset, length, data);
		sync(() -> remote().replaceData(offset, length, data));
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

	@Override
	public Text splitText(int offset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String substringData(int arg0, int arg1) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return Ax.format("#COMMENT[%s]", local().getData());
	}
}
