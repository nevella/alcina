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

	private DomText impl;

	private Text_Jso domImpl;

	protected Text() {
	}

	public Text cast() {
		return this;
	}

	public void deleteData(int offset, int length) {
		impl().deleteData(offset, length);
	}

	public String getData() {
		return impl().getData();
	}

	public int getLength() {
		return impl().getLength();
	}

	public void insertData(int offset, String data) {
		impl().insertData(offset, data);
	}

	@Override
	public void putDomImpl(Node_Jso nodeDom) {
		this.domImpl = (Text_Jso) nodeDom;
	}

	@Override
	public void putImpl(DomNode impl) {
		this.impl = (DomText) impl;
	}

	public void replaceData(int offset, int length, String data) {
		impl().replaceData(offset, length, data);
	}

	public void setData(String data) {
		impl().setData(data);
	}

	public Text splitText(int offset) {
		return impl().splitText(offset);
	}

	@Override
	Text_Jso domImpl() {
		return domImpl;
	}

	@Override
	DomText impl() {
		return impl;
	}
	@Override
	DomText implNoResolve() {
		return impl();
	}
}
