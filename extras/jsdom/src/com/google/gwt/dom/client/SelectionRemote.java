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
 * Derived from elemental.js.html.JsSelection. Includes only desired
 * methods/accessors
 */
public class SelectionRemote extends JavaScriptObject {
	protected SelectionRemote() {
	}

	public final native NodeRemote getAnchorNode() /*-{
    return this.anchorNode;
	}-*/;

	public final native int getAnchorOffset() /*-{
    return this.anchorOffset;
	}-*/;

	public final native NodeRemote getBaseNode() /*-{
    return this.baseNode;
	}-*/;

	public final native int getBaseOffset() /*-{
    return this.baseOffset;
	}-*/;

	public final native DomRect getClientRect()/*-{
    return this.getRangeAt(0).getBoundingClientRect();
	}-*/;

	public final native NodeRemote getExtentNode() /*-{
    return this.extentNode;
	}-*/;

	public final native int getExtentOffset() /*-{
    return this.extentOffset;
	}-*/;

	public final native NodeRemote getFocusNode() /*-{
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