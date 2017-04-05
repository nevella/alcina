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
import com.google.gwt.safehtml.shared.annotations.IsTrustedResourceUri;

/**
 * The LINK element specifies a link to an external resource, and defines this
 * document's relationship to that resource (or vice versa).
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#edef-LINK">W3C HTML Specification</a>
 */
@TagName(LinkElement.TAG)
public class LinkElement extends Element {

  public static final String TAG = "link";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static LinkElement as(Element elem) {
    assert is(elem);
    return (LinkElement) elem;
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

  protected LinkElement() {
  }

  /**
   * Enables/disables the link. This is currently only used for style sheet
   * links, and may be used to activate or deactivate style sheets.
   * @deprecated use {@link #isDisabled()} instead.
   */
  @Deprecated  public  boolean getDisabled(){
  throw new FixmeUnsupportedOperationException();
}


  /**
   * The URI of the linked resource.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">W3C HTML Specification</a>
   */  public  String getHref(){
  return this.getPropertyString("href");
}


  /**
   * Language code of the linked resource.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang">W3C HTML Specification</a>
   */  public  String getHreflang(){
  return this.getPropertyString("hreflang");
}


  /**
   * Designed for use with one or more target media.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-media">W3C HTML Specification</a>
   */  public  String getMedia(){
  return this.getPropertyString("media");
}


  /**
   * Forward link type.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel">W3C HTML Specification</a>
   */  public  String getRel(){
  return this.getPropertyString("rel");
}


  /**
   * Frame to render the resource in.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">W3C HTML Specification</a>
   */  public  String getTarget(){
  return this.getPropertyString("target");
}


  /**
   * Advisory content type.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A">W3C HTML Specification</a>
   */  public  String getType(){
  return this.getPropertyString("type");
}


  /**
   * Enables/disables the link. This is currently only used for style sheet
   * links, and may be used to activate or deactivate style sheets.
   */  public  boolean isDisabled(){
  return this.getPropertyBoolean("disabled");
}


  /**
   * Enables/disables the link. This is currently only used for style sheet
   * links, and may be used to activate or deactivate style sheets.
   */  public  void setDisabled(boolean disabled){
   this.setPropertyBoolean("disabled",disabled);
}


  /**
   * The URI of the linked resource.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">W3C HTML Specification</a>
   */  public  void setHref(@IsTrustedResourceUri String href){
   this.setPropertyString("href",href);
}


  /**
   * Language code of the linked resource.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang">W3C HTML Specification</a>
   */  public  void setHreflang(String hreflang){
   this.setPropertyString("hreflang",hreflang);
}


  /**
   * Designed for use with one or more target media.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-media">W3C HTML Specification</a>
   */  public  void setMedia(String media){
   this.setPropertyString("media",media);
}


  /**
   * Forward link type.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel">W3C HTML Specification</a>
   */  public  void setRel(String rel){
   this.setPropertyString("rel",rel);
}


  /**
   * Frame to render the resource in.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">W3C HTML Specification</a>
   */  public  void setTarget(String target){
   this.setPropertyString("target",target);
}


  /**
   * Advisory content type.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A">W3C HTML Specification</a>
   */  public  void setType(String type){
   this.setPropertyString("type",type);
}

}
