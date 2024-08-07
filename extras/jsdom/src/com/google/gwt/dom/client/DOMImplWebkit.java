/*
 * Copyright 2011 Google Inc.
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

/**
 * WebKit based implementation of
 * {@link com.google.gwt.dom.client.DOMImplStandardBase}.
 */
class DOMImplWebkit extends DOMImplStandardBase {
	/**
	 * Return true if using Webkit 525.x (Safari 3) or earlier.
	 * 
	 * @return true if using Webkit 525.x (Safari 3) or earlier.
	 */
	private static native boolean isWebkit525OrBefore() /*-{
    var result = /safari\/([\d.]+)/.exec(navigator.userAgent.toLowerCase());
    if (result) {
      var version = (parseFloat(result[1]));
      if (version < 526) {
        return true;
      }
    }
    return false;
	}-*/;

	/**
	 * Webkit events sometimes target the Text_Dom Node_Dom inside of the
	 * Element_Dom instead of the Element_Dom itself, so we need to get the
	 * parent of the Text_Dom Node_Dom.
	 */
	@Override
	public native EventTarget eventGetTarget(NativeEventJso evt) /*-{
    var target = evt.target;
    if (target && target.nodeType == 3) {
      target = target.parentNode;
    }
    var wrapped = @com.google.gwt.dom.client.EventTarget::new(Lcom/google/gwt/core/client/JavaScriptObject;)(target);
    return wrapped;
	}-*/;

	@Override
	Element getLegacyDocumentScrollingElement(DocumentJso doc) {
		// Old WebKit needs body.scrollLeft in both quirks mode and strict mode.
		return doc.getBody();
	}

	/**
	 * Webkit based browsers require that we set the webkit-user-drag style
	 * attribute to make an Element_Dom draggable.
	 */
	@Override
	public void setDraggable(ElementJso elem, String draggable) {
		super.setDraggable(elem, draggable);
		if ("true".equals(draggable)) {
			elem.getStyle().setProperty("webkitUserDrag", "Element_Dom");
		} else {
			elem.getStyle().clearProperty("webkitUserDrag");
		}
	}
}
