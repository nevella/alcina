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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.annotations.IsTrustedResourceUri;
import com.google.gwt.safehtml.shared.annotations.SuppressIsTrustedResourceUriCastCheck;

/**
 * Create a frame.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#edef-FRAME">W3C HTML Specification</a>
 */
@TagName(FrameElement.TAG)
public class FrameElement extends Element {

  public static final String TAG = "frame";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static FrameElement as(Element elem) {
    assert is(elem);
    return (FrameElement) elem;
  }

  /**
   * Determines whether the given {@link JavaScriptObject} can be cast to
   * this class. A <code>null</code> object will cause this method to
   * return <code>false</code>.
   */
  public static boolean is(JavaScriptObject o) {
    if (Element.is(o)) {
      return is(Element.as(o));
    }
    return false;
  }

  /**
   * Determine whether the given {@link Node} can be cast to this class.
   * A <code>null</code> node will cause this method to return
   * <code>false</code>.
   */
  public static boolean is(Node node) {
    if (Element.is(node)) {
      return is((Element) node);
    }
    return false;
  }
  
  /**
   * Determine whether the given {@link Element} can be cast to this class.
   * A <code>null</code> node will cause this method to return
   * <code>false</code>.
   */
  public static boolean is(Element elem) {
    return elem != null && elem.hasTagName(TAG);
  }

  protected FrameElement() {
  }

  
  public final  Document getContentDocument() {
	  return LocalDom.nodeFor(getContentDocument0(typedRemote()));
  }
  /**
   * The document this frame contains, if there is any and it is available, or
   * null otherwise.
   */
  private final native DocumentRemote getContentDocument0(ElementRemote element) /*-{
     // This is known to work on all modern browsers.
     return this.contentWindow.document;
   }-*/;

  /**
   * Request frame borders.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-frameborder">W3C HTML Specification</a>
   */  public  int getFrameBorder(){
  return this.getPropertyInt("frameBorder");
}


  /**
   * URI designating a long description of this image or frame.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-longdesc-FRAME">W3C HTML Specification</a>
   */  public  String getLongDesc(){
  return this.getPropertyString("longDesc");
}


  /**
   * Frame margin height, in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginheight">W3C HTML Specification</a>
   */  public  int getMarginHeight(){
  return this.getPropertyInt("marginHeight");
}


  /**
   * Frame margin width, in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginwidth">W3C HTML Specification</a>
   */  public  int getMarginWidth(){
  return this.getPropertyInt("marginWidth");
}


  /**
   * The frame name (object of the target attribute).
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-name-FRAME">W3C HTML Specification</a>
   */  public  String getName(){
  return this.getPropertyString("name");
}


  /**
   * Specify whether or not the frame should have scrollbars.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-scrolling">W3C HTML Specification</a>
   */  public  String getScrolling(){
  return this.getPropertyString("scrolling");
}


  /**
   * A URI designating the initial frame contents.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C HTML Specification</a>
   */  public  String getSrc(){
  return this.getPropertyString("src");
}


  /**
   * When true, forbid user from resizing frame.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-noresize">W3C HTML Specification</a>
   */  public  boolean isNoResize(){
  return this.getPropertyBoolean("noResize");
}


  /**
   * Request frame borders.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-frameborder">W3C HTML Specification</a>
   */  public  void setFrameBorder(int frameBorder){
   this.setPropertyInt("frameBorder",frameBorder);
}


  /**
   * URI designating a long description of this image or frame.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-longdesc-FRAME">W3C HTML Specification</a>
   */
  public final void setLongDesc(SafeUri longDesc) {
    setLongDesc(longDesc.asString());
  }

  /**
   * URI designating a long description of this image or frame.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-longdesc-FRAME">W3C HTML Specification</a>
   */  public  void setLongDesc(String longDesc){
   this.setPropertyString("longDesc",longDesc);
}


  /**
   * Frame margin height, in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginheight">W3C HTML Specification</a>
   */  public  void setMarginHeight(int marginHeight){
   this.setPropertyInt("marginHeight",marginHeight);
}


  /**
   * Frame margin width, in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginwidth">W3C HTML Specification</a>
   */  public  void setMarginWidth(int marginWidth){
   this.setPropertyInt("marginWidth",marginWidth);
}


  /**
   * The frame name (object of the target attribute).
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-name-FRAME">W3C HTML Specification</a>
   */  public  void setName(String name){
   this.setPropertyString("name",name);
}


  /**
   * When true, forbid user from resizing frame.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-noresize">W3C HTML Specification</a>
   */  public  void setNoResize(boolean noResize){
   this.setPropertyBoolean("noResize",noResize);
}


  /**
   * Specify whether or not the frame should have scrollbars.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-scrolling">W3C HTML Specification</a>
   */  public  void setScrolling(String scrolling){
   this.setPropertyString("scrolling",scrolling);
}


  /**
   * A URI designating the initial frame contents.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C HTML Specification</a>
   */
  @SuppressIsTrustedResourceUriCastCheck
  public final void setSrc(@IsTrustedResourceUri SafeUri src) {
    setSrc(src.asString());
  }

  /**
   * A URI designating the initial frame contents.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C HTML Specification</a>
   */  public  void setSrc(@IsTrustedResourceUri String src){
   this.setPropertyString("src",src);
}

}
