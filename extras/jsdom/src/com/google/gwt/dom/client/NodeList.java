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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The NodeList interface provides the abstraction of an ordered collection of
 * nodes, without defining or constraining how this collection is implemented.
 * NodeList objects in the DOM are live.
 * 
 * The items in the NodeList are accessible via an integral index, starting from
 * 0.
 * 
 * @param <T>
 *            the type of contained node
 */
public class NodeList<T extends Node>
		implements ClientDomNodeList<T>, Iterable<T>, org.w3c.dom.NodeList {
	public static ClientDomNodeList<? extends Node>
			gwtOnlySubList(ClientDomNodeList<? extends Node> childNodes) {
		return new NodeList<>(childNodes).filteredSubList(n -> {
			switch (n.getNodeType()) {
			case Node.DOCUMENT_NODE:
			case Node.ELEMENT_NODE:
			case Node.TEXT_NODE:
				return true;
			default:
				return false;
			}
		});
	}

	ClientDomNodeList<T> impl;

	public NodeList(ClientDomNodeList<T> impl) {
		this.impl = impl;
	}

	public <V extends Node> NodeList<V>
			filteredSubList(Predicate<T> predicate) {
		return new NodeList<V>(new NodeListWrapped<V>((List) stream()
				.filter(predicate).collect(Collectors.toList())));
	}

	public T getItem(int index) {
		return this.impl.getItem(index);
	}

	public int getLength() {
		return this.impl.getLength();
	}

	@Override
	public Iterator<T> iterator() {
		return new NodeListIterator();
	}

	@Override
	public Stream<T> stream() {
		return ClientDomNodeListStatic.stream0(this);
	}

	private class NodeListIterator implements Iterator<T> {
		int cursor = 0;

		@Override
		public boolean hasNext() {
			return getLength() > cursor;
		}

		@Override
		public T next() {
			if (cursor >= getLength()) {
				throw new NoSuchElementException();
			}
			return getItem(cursor++);
		}
	}

	@Override
	public Node item(int arg0) {
		return getItem(arg0);
	}
}
