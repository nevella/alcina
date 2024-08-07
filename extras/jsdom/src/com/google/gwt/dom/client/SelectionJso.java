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

/*
 * Derived from elemental.js.html.JsSelection.
 */
public class SelectionJso extends JavaScriptObject {
	protected SelectionJso() {
	}

	public final native void collapse(NodeJso node) /*-{
    this.collapse(node);
	}-*/;

	public final native void collapse(NodeJso node, int offset) /*-{
    this.collapse(node, offset);
	}-*/;

	public final native void extend(NodeJso node) /*-{
    this.extend(node);
	}-*/;

	public final native void extend(NodeJso node, int offset) /*-{
    this.extend(node, offset);
	}-*/;

	public final native NodeJso getAnchorNode() /*-{
    return this.anchorNode;
	}-*/;

	public final native int getAnchorOffset() /*-{
    return this.anchorOffset;
	}-*/;

	public final native DomRect getClientRect()/*-{
		var rect = this.getRangeAt(0).getBoundingClientRect();
		 return @com.google.gwt.dom.client.DomRect::new(Lcom/google/gwt/dom/client/DomRectJso;)(rect);
	}-*/;

	public final native NodeJso getFocusNode() /*-{
    return this.focusNode;
	}-*/;

	public final native int getFocusOffset() /*-{
    return this.focusOffset;
	}-*/;

	public final native String getType() /*-{
    return this.type;
	}-*/;

	public final native boolean isCollapsed() /*-{
    return this.isCollapsed;
	}-*/;

	public final native void modify(String alter, String direction,
			String granularity) /*-{
    this.modify(alter, direction, granularity);
	}-*/;

	public final native void removeAllRanges() /*-{
    this.removeAllRanges();
	}-*/;
}
