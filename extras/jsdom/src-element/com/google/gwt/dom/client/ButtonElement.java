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
 * Push button.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-BUTTON">W3C HTML Specification</a>
 */
@TagName(ButtonElement.TAG)
public class ButtonElement extends Element {

  public static final String TAG = "button";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ButtonElement as(Element elem) {
    assert is(elem);
    return (ButtonElement) elem;
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

  protected ButtonElement() {
  }

  /**
   * Simulate a mouse-click.
   */
  public final void click() {
    DOMImpl.impl.buttonClick(this);
  }

  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C HTML Specification</a>
   */  public  String getAccessKey(){
  return this.getPropertyString("accessKey");
}


  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   * @deprecated use {@link #isDisabled()} instead.
   */
  @Deprecated  public  String getDisabled(){
  return this.getPropertyString("disabled");
}


  /**
   * Returns the FORM element containing this control. Returns null if this
   * control is not within the context of a form.
   */  public  FormElement getForm(){
  throw new FixmeUnsupportedOperationException();
}


  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-BUTTON">W3C HTML Specification</a>
   */  public  String getName(){
  return this.getPropertyString("name");
}


  /**
   * The type of button (all lower case).
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-type-BUTTON">W3C HTML Specification</a>
   */  public  String getType(){
  return this.getPropertyString("type");
}


  /**
   * The current form control value.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-BUTTON">W3C HTML Specification</a>
   */  public  String getValue(){
  return this.getPropertyString("value");
}


  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   */  public  boolean isDisabled(){
  return this.getPropertyBoolean("disabled");
}


  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C HTML Specification</a>
   */  public  void setAccessKey(String accessKey){
   this.setPropertyString("accessKey",accessKey);
}


  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   */  public  void setDisabled(boolean disabled){
   this.setPropertyBoolean("disabled",disabled);
}


  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   * @deprecated use {@link #setDisabled(boolean)} instead
   */
  @Deprecated  public  void setDisabled(String disabled){
   this.setPropertyString("disabled",disabled);
}


  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-BUTTON">W3C HTML Specification</a>
   */  public  void setName(String name){
   this.setPropertyString("name",name);
}


  /**
   * The current form control value.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-BUTTON">W3C HTML Specification</a>
   */  public  void setValue(String value){
   this.setPropertyString("value",value);
}
   @Override
	public void setPropertyBoolean(String name, boolean value) {
	if(provideIsLocal()&&(name.equals("disabled"))&&!Boolean.valueOf(value)){
		   removeAttribute(name);
		   return;
	   }
		super.setPropertyBoolean(name, value);
	}

}
