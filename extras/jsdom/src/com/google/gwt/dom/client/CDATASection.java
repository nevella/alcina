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
 * The CDATASection interface represents textual content.
 */
public class CDATASection extends Node
		implements ClientDomCDATASection, org.w3c.dom.CDATASection {
	/**
	 * Assert that the given {@link Node} is of type
	 * {@link Node#CDATA_SECTION_NODE} and automatically typecast it.
	 */
	public static CDATASection as(Node node) {
		assert node.getNodeType() == Node.CDATA_SECTION_NODE;
		return (CDATASection) node;
	}

	private CDATASectionLocal local;

	private ClientDomCDATASection remote;

	protected CDATASection(CDATASectionLocal local) {
		this.local = local;
		this.remote = CDATASectionNull.INSTANCE;
	}

	@Override
	public void appendData(String arg0) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public CDATASection cast() {
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
	public CDATASectionImplAccess implAccess() {
		return new CDATASectionImplAccess();
	}

	@Override
	public void insertData(int offset, String data) {
		ensureRemoteCheck();
		local().insertData(offset, data);
		sync(() -> remote().insertData(offset, data));
	}

	@Override
	public boolean isElementContentWhitespace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node node() {
		return this;
	}

	@Override
	public void replaceData(int offset, int length, String data) {
		ensureRemoteCheck();
		local().replaceData(offset, length, data);
		sync(() -> remote().replaceData(offset, length, data));
	}

	@Override
	public org.w3c.dom.Text replaceWholeText(String content)
			throws DOMException {
		// TODO Auto-generated method stub
		return null;
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
		return Ax.format("#TEXT[%s]", local().getData());
	}

	@Override
	protected boolean linkedToRemote() {
		return remote != CDATASectionNull.INSTANCE;
	}

	@Override
	protected CDATASectionLocal local() {
		return local;
	}

	@Override
	protected void putRemote(ClientDomNode remote, boolean resolved) {
		Preconditions.checkState(wasSynced() == resolved);
		this.remote = (ClientDomCDATASection) remote;
	}

	@Override
	protected ClientDomCDATASection remote() {
		return remote;
	}

	@Override
	protected void resetRemote0() {
		this.remote = CDATASectionNull.INSTANCE;
	}

	@Override
	protected CDATASectionJso jsoRemote() {
		return (CDATASectionJso) remote();
	}

	public class CDATASectionImplAccess extends Node.ImplAccess {
		public CDATASectionJso ensureRemote() {
			ensureRemoteCheck();
			return CDATASection.this.jsoRemote();
		}

		public CDATASectionJso typedRemote() {
			return CDATASection.this.jsoRemote();
		}
	}
}
