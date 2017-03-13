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

import com.google.gwt.core.client.CastableFromJavascriptObject;

/**
 * The Text interface represents textual content.
 */
public class Text extends Node<Text, Text_Dom> implements DomText {
	/**
	 * Assert that the given {@link Node} is of type {@link Node#TEXT_NODE} and
	 * automatically typecast it.
	 */
	public static Text as(Node node) {
		assert node.getNodeType() == Node.TEXT_NODE;
		return (Text) node;
	}

	public Text cast() {
		return this;
	}

	public void deleteData(int offset, int length) {
		this.impl.deleteData(offset, length);
	}

	public String getData() {
		return this.impl.getData();
	}

	public int getLength() {
		return this.impl.getLength();
	}

	public void insertData(int offset, String data) {
		this.impl.insertData(offset, data);
	}

	public void replaceData(int offset, int length, String data) {
		this.impl.replaceData(offset, length, data);
	}

	public void setData(String data) {
		this.impl.setData(data);
	}

	public Text splitText(int offset) {
		return this.impl.splitText(offset);
	}

	protected Text() {
	}
}
