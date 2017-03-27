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
 * IE9 based implementation of {@link com.google.gwt.dom.client.DOMImplStandardBase}.
 */
class DOMImplIE9 extends DOMImplStandardBase {

  @Override
  protected int getAbsoluteLeft(Element_Jso elem) {
    double left = getBoundingClientRectLeft(elem) + getDocumentScrollLeftImpl();
    if (isRTL(elem)) { // in RTL, account for the scroll bar shift if present
      left += getParentOffsetDelta(elem);
    }
    return toInt32(left);
  }

  @Override
  protected int getAbsoluteTop(Element_Jso elem) {
    return toInt32(getBoundingClientRectTop(elem) + getDocumentScrollTopImpl());
  }

  /**
   * Coerce numeric values a string. In IE, some values can be stored as numeric
   * types.
   */
  @Override
  protected native String getNumericStyleProperty(Style_Jso style, String name) /*-{
    return typeof(style[name]) == "number" ? "" + style[name] : style[name];
  }-*/;

  @Override
  protected int getScrollLeft(Document_Jso doc) {
    return toInt32(getDocumentScrollLeftImpl());
  }

  @Override
  protected int getScrollLeft(Element_Jso elem) {
    int left = toInt32(getScrollLeftImpl(elem));
    if (isRTL(elem)) {
      left = -left;
    }
    return left;
  }

  @Override
  protected int getScrollTop(Document_Jso doc) {
    return toInt32(getDocumentScrollTopImpl());
  }

  @Override
  protected native int getTabIndex(Element_Jso elem) /*-{ 
    return elem.tabIndex < 65535 ? elem.tabIndex : -(elem.tabIndex % 65535) - 1;
  }-*/;

  @Override
  protected boolean isOrHasChild(Node_Jso parent, Node_Jso child) {
    // IE9 still behaves like IE8 for this method
    return DOMImplTrident.isOrHasChildImpl(parent, child);
  }

  @Override
  protected native void selectRemoveOption(Element_Jso select, int index) /*-{
    try {
      // IE9 throws if elem at index is an optgroup
      select.remove(index);
    } catch(e) {
      select.removeChild(select.childNodes[index]);
    }
  }-*/;

  @Override
  protected void setScrollLeft(Element_Jso elem, int left) {
    if (isRTL(elem)) {
      left = -left;
    }
    setScrollLeftImpl(elem, left);
  }

  @Override
  protected void setScrollLeft(Document_Jso doc, int left) {
    setScrollLeft(doc.getDocumentElement().typedDomImpl, left);
  }

  private native double getBoundingClientRectLeft(Element_Jso elem) /*-{
    // getBoundingClientRect() throws a JS exception if the elem is not attached
    // to the doc, so we wrap it in a try/catch block
    try {
      return elem.getBoundingClientRect().left;
    } catch (e) {
      // if not attached return 0
      return 0;
    }
  }-*/;

  private native double getBoundingClientRectTop(Element_Jso elem) /*-{
    // getBoundingClientRect() throws a JS exception if the elem is not attached
    // to the doc, so we wrap it in a try/catch block
    try {
      return elem.getBoundingClientRect().top;
    } catch (e) {
      // if not attached return 0
      return 0;
    }
  }-*/;

  private native double getDocumentScrollLeftImpl() /*-{
    return $wnd.pageXOffset;
  }-*/;

  private native double getDocumentScrollTopImpl() /*-{
    return $wnd.pageYOffset;
  }-*/;

  private native double getParentOffsetDelta(Element_Jso elem) /*-{
    var offsetParent = elem.offsetParent;
    if (offsetParent) {
      return offsetParent.offsetWidth - offsetParent.clientWidth;
    }
    return 0;
  }-*/;

  private native double getScrollLeftImpl(Element_Jso elem) /*-{
    return elem.scrollLeft || 0;
  }-*/; 

  private native void setScrollLeftImpl(Element_Jso elem, int left) /*-{
    elem.scrollLeft = left;
  }-*/; 
}
