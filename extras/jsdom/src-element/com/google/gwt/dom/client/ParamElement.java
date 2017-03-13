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

/**
 * Parameters fed to the OBJECT element.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#edef-PARAM">W3C HTML Specification</a>
 */
@TagName(ParamElement.TAG)
public class ParamElement extends Element {

  public static final String TAG = "param";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ParamElement as(Element elem) {
    assert is(elem);
    return (ParamElement) elem;
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

  protected ParamElement() {
  }

  /**
   * The name of a run-time parameter.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-name-PARAM">W3C HTML Specification</a>
   */  public  String getName(){
  return this.getPropertyString("name");
}


  /**
   * The value of a run-time parameter.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-value-PARAM">W3C HTML Specification</a>
   */  public  String getValue(){
  return this.getPropertyString("value");
}


  /**
   * The name of a run-time parameter.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-name-PARAM">W3C HTML Specification</a>
   */  public  void setName(String name){
   this.setPropertyString("name",name);
}


  /**
   * The value of a run-time parameter.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-value-PARAM">W3C HTML Specification</a>
   */  public  void setValue(String value){
   this.setPropertyString("value",value);
}

}
