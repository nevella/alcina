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

import java.util.Objects;

import com.google.gwt.dom.client.mutations.SelectionRecord;

/**
 * Models the browser selection object
 *
 */
public class Selection implements ClientDomSelection {
	SelectionLocal local;

	private ClientDomSelection remote = null;

	protected Selection(Document document) {
		local = new SelectionLocal(this);
		remote = document.remote().ensureRemoteSelection(this);
	}

	protected SelectionLocal local() {
		return local;
	}

	/**
	 * The StyleJso remote is only instantiated lazily (it's mostly unused)
	 * 
	 * @return the remote if it exists (essentially if the element has a
	 *         remote), or null
	 */
	protected ClientDomSelection remote() {
		return remote;
	}

	@Override
	public Selection selectionObject() {
		return this;
	}

	@Override
	public void collapse(Node node) {
		local.collapse(node);
		remote.collapse(node);
	}

	@Override
	public void collapse(Node node, int offset) {
		local.collapse(node, offset);
		remote.collapse(node, offset);
	}

	@Override
	public void extend(Node node) {
		local.extend(node);
		remote.extend(node);
	}

	@Override
	public void extend(Node node, int offset) {
		local.extend(node, offset);
		remote.extend(node, offset);
	}

	@Override
	public Node getAnchorNode() {
		return local.getAnchorNode();
	}

	@Override
	public int getAnchorOffset() {
		return local.getAnchorOffset();
	}

	@Override
	public DomRect getClientRect() {
		return local.getClientRect();
	}

	@Override
	public Node getFocusNode() {
		return local.getFocusNode();
	}

	@Override
	public int getFocusOffset() {
		return local.getFocusOffset();
	}

	@Override
	public String getType() {
		return local.getType();
	}

	@Override
	public boolean isCollapsed() {
		return local.isCollapsed();
	}

	@Override
	public void modify(String alter, String direction, String granularity) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'modify'");
	}

	@Override
	public void removeAllRanges() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'removeAllRanges'");
	}

	@Override
	public SelectionRecord getSelectionRecord() {
		return local.getSelectionRecord();
	}

	void onDocumentEventSystemInit() {
		local.onDocumentEventSystemInit();
	}

	public boolean hasSelection() {
		return !Objects.equals("None", getType());
	}

	public void validate() {
		local.validate();
	}
}
