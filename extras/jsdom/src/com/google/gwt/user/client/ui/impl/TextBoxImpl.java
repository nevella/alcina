/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.dom.client.Element;

/**
 * Implementation class used by {@link com.google.gwt.user.client.ui.TextBox}.
 */
public class TextBoxImpl {
	public static native void setTextBoxSelectionRange(Element elem_multi,
			int pos, int length) /*-{

    var elem = elem_multi.@com.google.gwt.dom.client.Element::remote()();
    try {
      elem.setSelectionRange(pos, pos + length);
    } catch (e) {
      // Firefox throws exception if TextBox is not visible, even if attached
    }
	}-*/;

	public native int getCursorPos(Element elem_multi) /*-{
    var elem = elem_multi.@com.google.gwt.dom.client.Element::remote()();
    // Guard needed for FireFox.
    try {
      return elem.selectionStart;
    } catch (e) {
      return 0;
    }
	}-*/;

	public native int getSelectionLength(Element elem_multi) /*-{
    var elem = elem_multi.@com.google.gwt.dom.client.Element::remote()();
    // Guard needed for FireFox.
    try {
      return elem.selectionEnd - elem.selectionStart;
    } catch (e) {
      return 0;
    }
	}-*/;

	public int getTextAreaCursorPos(Element elem) {
		return getCursorPos(elem);
	}

	public int getTextAreaSelectionLength(Element elem) {
		return getSelectionLength(elem);
	}

	public void setSelectionRange(Element elem_multi, int pos, int length) {
		setTextBoxSelectionRange(elem_multi, pos, length);
	}
}
