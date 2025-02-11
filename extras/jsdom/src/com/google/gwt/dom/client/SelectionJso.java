/*
 * Copyright 2012 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.mutations.SelectionRecord;

/*
 * Derived from elemental.js.html.JsSelection.
 */
public final class SelectionJso extends JavaScriptObject
		implements ClientDomSelection {
	protected SelectionJso() {
	}

	native void collapse0(NodeJso node) /*-{
    this.collapse(node);
	}-*/;

	native void collapse0(NodeJso node, int offset) /*-{
    this.collapse(node, offset);
	}-*/;

	native void extend0(NodeJso node) /*-{
    this.extend(node);
	}-*/;

	native void extend0(NodeJso node, int offset) /*-{
    this.extend(node, offset);
	}-*/;

	native NodeJso getAnchorNode0() /*-{
    return this.anchorNode;
	}-*/;

	native int getAnchorOffset0() /*-{
    return this.anchorOffset;
	}-*/;

	native DomRect getClientRect0()/*-{
		if(this.rangeCount == 0){
			return null;
		}
		var rect = this.getRangeAt(0).getBoundingClientRect();
		 return @com.google.gwt.dom.client.DomRect::new(Lcom/google/gwt/dom/client/DomRectJso;)(rect);
	}-*/;

	native NodeJso getFocusNode0() /*-{
    return this.focusNode;
	}-*/;

	native int getFocusOffset0() /*-{
    return this.focusOffset;
	}-*/;

	native String getType0() /*-{
    return this.type;
	}-*/;

	native boolean isCollapsed0() /*-{
    return this.isCollapsed;
	}-*/;

	native void modify0(String alter, String direction, String granularity) /*-{
    this.modify(alter, direction, granularity);
	}-*/;

	native void removeAllRanges0() /*-{
    this.removeAllRanges();
	}-*/;

	public Selection selectionObject() {
		return Document.get().getSelection();
	}

	@Override
	public void collapse(Node node) {
		collapse0(node.jsoRemote());
	}

	@Override
	public void collapse(Node node, int offset) {
		collapse0(node.jsoRemote(), offset);
	}

	@Override
	public void extend(Node node) {
		extend0(node.jsoRemote());
	}

	@Override
	public void extend(Node node, int offset) {
		extend0(node.jsoRemote(), offset);
	}

	@Override
	public Node getAnchorNode() {
		return Node.as(getAnchorNode0());
	}

	@Override
	public int getAnchorOffset() {
		return getAnchorOffset0();
	}

	@Override
	public DomRect getClientRect() {
		return getClientRect0();
	}

	@Override
	public Node getFocusNode() {
		return Node.as(getFocusNode0());
	}

	@Override
	public int getFocusOffset() {
		return getFocusOffset0();
	}

	@Override
	public String getType() {
		return getType0();
	}

	@Override
	public boolean isCollapsed() {
		return isCollapsed0();
	}

	@Override
	public void modify(String alter, String direction, String granularity) {
		modify0(alter, direction, granularity);
	}

	@Override
	public void removeAllRanges() {
		removeAllRanges0();
	}

	@Override
	public SelectionRecord getSelectionRecord() {
		SelectionRecord record = new SelectionRecord();
		record.anchorNode = getAnchorNode();
		record.anchorOffset = getAnchorOffset();
		record.focusNode = getFocusNode();
		record.focusOffset = getFocusOffset();
		record.clientRect = getClientRect();
		record.type = getType();
		record.populateNodeIds();
		return record;
	}
}
