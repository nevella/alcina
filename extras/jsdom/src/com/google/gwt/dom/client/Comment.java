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

import com.google.common.base.Preconditions;

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
		this.remote = CommentNull.INSTANCE;
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
	public CommentImplAccess implAccess() {
		return new CommentImplAccess();
	}

	@Override
	public void insertData(int offset, String data) {
		ensureRemoteCheck();
		local().insertData(offset, data);
		sync(() -> remote().insertData(offset, data));
	}

	@Override
	protected CommentJso jsoRemote() {
		return (CommentJso) remote();
	}

	@Override
	protected boolean linkedToRemote() {
		return remote != CommentNull.INSTANCE;
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
	protected void putRemote(ClientDomNode remote, boolean resolved) {
		Preconditions.checkState(wasSynced() == resolved);
		this.remote = (ClientDomComment) remote;
	}

	@Override
	protected ClientDomComment remote() {
		return remote;
	}

	@Override
	public void replaceData(int offset, int length, String data) {
		ensureRemoteCheck();
		local().replaceData(offset, length, data);
		sync(() -> remote().replaceData(offset, length, data));
	}

	@Override
	protected void resetRemote() {
		this.remote = CommentNull.INSTANCE;
	}

	@Override
	public void setData(String data) {
		ensureRemoteCheck();
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

	public class CommentImplAccess extends Node.ImplAccess {
		public CommentJso ensureRemote() {
			ensureRemoteCheck();
			return Comment.this.jsoRemote();
		}

		@Override
		public void putRemoteNoSideEffects(ClientDomNode remote) {
			Comment.this.remote = (ClientDomComment) remote;
		}

		public CommentJso typedRemote() {
			return Comment.this.jsoRemote();
		}
	}
}
