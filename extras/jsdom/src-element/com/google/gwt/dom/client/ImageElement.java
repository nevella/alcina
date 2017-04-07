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
import com.google.gwt.user.client.ui.AbstractImagePrototype.ImagePrototypeElement;

/**
 * Embedded image.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#edef-IMG">W3C HTML Specification</a>
 */
@TagName(ImageElement.TAG)
public class ImageElement extends ImagePrototypeElement {

  public static final String TAG = "img";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ImageElement as(Element elem) {
    assert is(elem);
    return (ImageElement) elem;
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

  protected ImageElement() {
  }

  /**
   * Alternate text for user agents not rendering the normal content of this
   * element.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt">W3C HTML Specification</a>
   */  public  String getAlt(){
  return this.getPropertyString("alt");
}


  /**
   * Height of the image in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-height-IMG">W3C HTML Specification</a>
   */  public  int getHeight(){
  return this.getPropertyInt("height");
}


  /**
   * URI designating the source of this image.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-src-IMG">W3C HTML Specification</a>
   */  public  String getSrc(){
  return this.getPropertyString("src");
}


  /**
   * The width of the image in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-width-IMG">W3C HTML Specification</a>
   */  public  int getWidth(){
  return this.getPropertyInt("width");
}


  /**
   * Use server-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-ismap">W3C HTML Specification</a>
   */  public  boolean isMap(){
  return this.getPropertyBoolean("isMap");
}

  
  /**
   * Alternate text for user agents not rendering the normal content of this
   * element.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt">W3C HTML Specification</a>
   */  public  void setAlt(String alt){
   this.setPropertyString("alt",alt);
}


  /**
   * Height of the image in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-height-IMG">W3C HTML Specification</a>
   */  public  void setHeight(int height){
   this.setPropertyInt("height",height);
}


  /**
   * Use server-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-ismap">W3C HTML Specification</a>
   */  public  void setIsMap(boolean isMap){
   this.setPropertyBoolean("isMap",isMap);
}


  /**
   * URI designating the source of this image.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-src-IMG">W3C HTML Specification</a>
   */  public  void setSrc(String src){
   this.setPropertyString("src",src);
}


  /**
   * Use client-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-usemap">W3C HTML Specification</a>
   */  public  void setUseMap(boolean useMap){
   this.setPropertyBoolean("useMap",useMap);
}


  /**
   * The width of the image in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-width-IMG">W3C HTML Specification</a>
   */  public  void setWidth(int width){
   this.setPropertyInt("width",width);
}


  /**
   * Use client-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-usemap">W3C HTML Specification</a>
   */
  public final  boolean useMap() {
	  return getPropertyBoolean("useMap");
			  } 
}
