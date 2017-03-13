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
 * Generic embedded object.
 * 
 * Note: In principle, all properties on the object element are read-write but
 * in some environments some properties may be read-only once the underlying
 * object is instantiated.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#edef-OBJECT">W3C HTML Specification</a>
 */
@TagName(ObjectElement.TAG)
public class ObjectElement extends Element {

  public static final String TAG = "object";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ObjectElement as(Element elem) {
    assert is(elem);
    return (ObjectElement) elem;
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

  protected ObjectElement() {
  }

  /**
   * Applet class file.
   */  public  String getCode(){
  return this.getPropertyString("code");
}


  /**
   * The document this object contains, if there is any and it is available, or
   * null otherwise.
   */  public  Document getContentDocument(){
  throw new FixmeUnsupportedOperationException();
}


  /**
   * A URI specifying the location of the object's data.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-data">W3C HTML Specification</a>
   */  public  String getData(){
  return this.getPropertyString("data");
}


  /**
   * Returns the FORM element containing this control. Returns null if this
   * control is not within the context of a form.
   */  public  FormElement getForm(){
  throw new FixmeUnsupportedOperationException();
}


  /**
   * Override height.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-height-IMG">W3C HTML Specification</a>
   */  public  String getHeight(){
  return this.getPropertyString("height");
}


  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-INPUT">W3C HTML Specification</a>
   */  public  String getName(){
  return this.getPropertyString("name");
}


  /**
   * Content type for data downloaded via data attribute.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-type-OBJECT">W3C HTML Specification</a>
   */  public  String getType(){
  return this.getPropertyString("type");
}


  /**
   * Override width.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-width-IMG">W3C HTML Specification</a>
   */  public  String getWidth(){
  return this.getPropertyString("width");
}


  /**
   * Applet class file.
   */  public  void setCode(String code){
   this.setPropertyString("code",code);
}


  /**
   * A URI specifying the location of the object's data.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-data">W3C HTML Specification</a>
   */
  @SuppressIsTrustedResourceUriCastCheck
  public final void setData(@IsTrustedResourceUri SafeUri data) {
    setData(data.asString());
  }

  /**
   * A URI specifying the location of the object's data.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-data">W3C HTML Specification</a>
   */  public  void setData(@IsTrustedResourceUri String data){
   this.setPropertyString("data",data);
}


  /**
   * Override height.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-height-IMG">W3C HTML Specification</a>
   */  public  void setHeight(String height){
   this.setPropertyString("height",height);
}


  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-INPUT">W3C HTML Specification</a>
   */  public  void setName(String name){
   this.setPropertyString("name",name);
}


  /**
   * Content type for data downloaded via data attribute.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-type-OBJECT">W3C HTML Specification</a>
   */  public  void setType(String type){
   this.setPropertyString("type",type);
}


  /**
   * Use client-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-usemap">W3C HTML Specification</a>
   */  public  void setUseMap(boolean useMap){
   this.setPropertyBoolean("useMap",useMap);
}


  /**
   * Override width.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-width-IMG">W3C HTML Specification</a>
   */  public  void setWidth(String width){
   this.setPropertyString("width",width);
}


  /**
   * Use client-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-usemap">W3C HTML Specification</a>
   */
	public final boolean useMap() {
		return getPropertyBoolean("useMap");
	}
}
