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

import com.google.gwt.core.client.JavascriptObjectEquivalent;

/**
 * The Text interface represents textual content.
 */
public class Text extends Node implements DomText {
	/**
	 * Assert that the given {@link Node} is of type {@link Node#TEXT_NODE} and
	 * automatically typecast it.
	 */
	public static Text as(Node node) {
		assert node.getNodeType() == Node.TEXT_NODE;
		return (Text) node;
	}

	private DomText local;

	private DomText remote;

	protected Text(DomText local) {
		this.local = local;
		this.remote = TextNull.INSTANCE;
	}

	public Text cast() {
		return this;
	}

	public void deleteData(int offset, int length) {
		local().deleteData(offset, length);
		remote().deleteData(offset, length);
	}

	public String getData() {
		return local().getData();
	}

	public int getLength() {
		return local().getLength();
	}

	public void insertData(int offset, String data) {
		local().insertData(offset, data);
		remote().insertData(offset, data);
	}

	public void replaceData(int offset, int length, String data) {
		local().replaceData(offset, length, data);
		remote().replaceData(offset, length, data);
	}

	public void setData(String data) {
		local().setData(data);
		remote().setData(data);
	}

	public Text splitText(int offset) {
		// FIXME - remote must use created text no9de
		Text result = local().splitText(offset);
		remote().splitText(offset);
		return result;
	}

	@Override
	protected DomText remote() {
		return remote;
	}

	@Override
	protected DomText local() {
		return local;
	}

	@Override
	public Node nodeFor() {
		return this;
	}

	@Override
	protected void putRemote(NodeRemote remote) {
		this.remote = (DomText) remote;
	}
}
