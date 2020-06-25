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
 * The Text interface represents textual content.
 */
public class Text extends Node implements DomText, org.w3c.dom.Text {
	/**
	 * Assert that the given {@link Node} is of type {@link Node#TEXT_NODE} and
	 * automatically typecast it.
	 */
	public static Text as(Node node) {
		assert node.getNodeType() == Node.TEXT_NODE;
		return (Text) node;
	}

	private TextLocal local;

	private DomText remote;

	protected Text(TextLocal local) {
		this.local = local;
		this.remote = TextNull.INSTANCE;
	}

	@Override
	public Text cast() {
		return this;
	}

	@Override
	public void deleteData(int offset, int length) {
		local().deleteData(offset, length);
		remote().deleteData(offset, length);
	}

	@Override
	public String getData() {
		return local().getData();
	}

	@Override
	public int getLength() {
		return local().getLength();
	}

	public TextImplAccess implAccess() {
		return new TextImplAccess();
	}

	@Override
	public void insertData(int offset, String data) {
		ensureRemoteCheck();
		local().insertData(offset, data);
		remote().insertData(offset, data);
	}

	@Override
	public Node node() {
		return this;
	}

	@Override
	public void replaceData(int offset, int length, String data) {
		ensureRemoteCheck();
		local().replaceData(offset, length, data);
		remote().replaceData(offset, length, data);
	}

	@Override
	public void setData(String data) {
		ensureRemoteCheck();
		local().setData(data);
		remote().setData(data);
	}

	@Override
	public Text splitText(int offset) {
		throw new UnsupportedOperationException();
		// // FIXME - remote must use created text no9de
		// Text result = local().splitText(offset);
		// remote().splitText(offset);
		// return result;
	}

	@Override
	public String toString() {
		return Ax.format("#TEXT[%s]", local().getData());
	}

	@Override
	protected boolean linkedToRemote() {
		return remote != TextNull.INSTANCE;
	}

	@Override
	protected TextLocal local() {
		return local;
	}

	@Override
	protected void putRemote(NodeRemote remote, boolean resolved) {
		Preconditions.checkState(wasResolved() == resolved);
		this.remote = (DomText) remote;
	}

	@Override
	protected DomText remote() {
		return remote;
	}

	@Override
	protected void resetRemote0() {
		this.remote = TextNull.INSTANCE;
	}

	@Override
	protected TextRemote typedRemote() {
		return (TextRemote) remote();
	}

	public class TextImplAccess {
		public TextRemote ensureRemote() {
			ensureRemoteCheck();
			return Text.this.typedRemote();
		}

		public TextRemote typedRemote() {
			return Text.this.typedRemote();
		}
	}

	@Override
	public void appendData(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String substringData(int arg0, int arg1) throws DOMException {
		throw new UnsupportedOperationException();
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
	public boolean isElementContentWhitespace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public org.w3c.dom.Text replaceWholeText(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}
}
