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

	final native void collapse0(NodeJso node) /*-{
    this.collapse(node);
	}-*/;

	final native void collapse0(NodeJso node, int offset) /*-{
    this.collapse(node, offset);
	}-*/;

	final native void extend0(NodeJso node) /*-{
    this.extend(node);
	}-*/;

	final native void extend0(NodeJso node, int offset) /*-{
    this.extend(node, offset);
	}-*/;

	final native NodeJso getAnchorNode0() /*-{
    return this.anchorNode;
	}-*/;

	final native int getAnchorOffset0() /*-{
    return this.anchorOffset;
	}-*/;

	final native DomRect getClientRect0()/*-{
		if(this.rangeCount == 0){
			return null;
		}
		var rect = this.getRangeAt(0).getBoundingClientRect();
		 return @com.google.gwt.dom.client.DomRect::new(Lcom/google/gwt/dom/client/DomRectJso;)(rect);
	}-*/;

	final native NodeJso getFocusNode0() /*-{
    return this.focusNode;
	}-*/;

	final native int getFocusOffset0() /*-{
    return this.focusOffset;
	}-*/;

	final native String getType0() /*-{
    return this.type;
	}-*/;

	final native boolean isCollapsed0() /*-{
    return this.isCollapsed;
	}-*/;

	final native void modify0(String alter, String direction,
			String granularity) /*-{
    this.modify(alter, direction, granularity);
	}-*/;

	final native void removeAllRanges0() /*-{
    this.removeAllRanges();
	}-*/;

	public final Selection selectionObject() {
		return Document.get().getSelection();
	}

	@Override
	public final void collapse(Node node) {
		collapse0(node.jsoRemote());
	}

	@Override
	public final void collapse(Node node, int offset) {
		collapse0(node.jsoRemote(), offset);
	}

	@Override
	public final void extend(Node node) {
		extend0(node.jsoRemote());
	}

	@Override
	public final void extend(Node node, int offset) {
		extend0(node.jsoRemote(), offset);
	}

	@Override
	public final Node getAnchorNode() {
		return Node.as(getAnchorNode0());
	}

	@Override
	public final int getAnchorOffset() {
		return getAnchorOffset0();
	}

	@Override
	public final DomRect getClientRect() {
		return getClientRect0();
	}

	@Override
	public final Node getFocusNode() {
		return Node.as(getFocusNode0());
	}

	@Override
	public final int getFocusOffset() {
		return getFocusOffset0();
	}

	@Override
	public final String getType() {
		return getType0();
	}

	@Override
	public final boolean isCollapsed() {
		return isCollapsed0();
	}

	@Override
	public final void modify(String alter, String direction,
			String granularity) {
		modify0(alter, direction, granularity);
	}

	@Override
	public final void removeAllRanges() {
		removeAllRanges0();
	}

	@Override
	public final SelectionRecord getSelectionRecord() {
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
