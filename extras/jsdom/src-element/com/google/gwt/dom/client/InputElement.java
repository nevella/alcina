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
 * Form control.
 * 
 * Note: Depending upon the environment in which the page is being viewed, the
 * value property may be read-only for the file upload input type. For the
 * "password" input type, the actual value returned may be masked to prevent
 * unauthorized use.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-INPUT">W3C HTML Specification</a>
 */
@TagName(InputElement.TAG)
public class InputElement extends Element {

  public static final String TAG = "input";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static InputElement as(Element elem) {
    assert is(elem);
    return (InputElement) elem;
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

  protected InputElement() {
  }

  
  public final  void click() {
	  click0(domImpl);
  }
  
  /**
   * Simulate a mouse-click. For INPUT elements whose type attribute has one of
   * the following values: "button", "checkbox", "radio", "reset", or "submit".
   */
	private native void click0(Element_Dom elt) /*-{
    elt.click();
  }-*/;

  /**
   * A comma-separated list of content types that a server processing this form
   * will handle correctly.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accept">W3C HTML Specification</a>
   */  public  String getAccept(){
  return this.getPropertyString("accept");
}


  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C HTML Specification</a>
   */  public  String getAccessKey(){
  return this.getPropertyString("accessKey");
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
   * When the type attribute of the element has the value "text", "file" or
   * "password", this represents the HTML value attribute of the element. The
   * value of this attribute does not change if the contents of the
   * corresponding form control, in an interactive user agent, changes.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-INPUT">W3C HTML Specification</a>
   */  public  String getDefaultValue(){
  return this.getPropertyString("defaultValue");
}


  /**
   * Returns the FORM element containing this control. Returns null if this
   * control is not within the context of a form.
   */  public  FormElement getForm(){
  throw new FixmeUnsupportedOperationException();
}


  /**
   * Maximum number of characters for text fields, when type has the value
   * "text" or "password".
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-maxlength">W3C HTML Specification</a>
   */  public  int getMaxLength(){
  return this.getPropertyInt("maxLength");
}


  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-INPUT">W3C HTML Specification</a>
   */  public  String getName(){
  return this.getPropertyString("name");
}


  /**
   * Size information. The precise meaning is specific to each type of field.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-size-INPUT">W3C HTML Specification</a>
   */  public  int getSize(){
  return this.getPropertyInt("size");
}


  /**
   * When the type attribute has the value "image", this attribute specifies the
   * location of the image to be used to decorate the graphical submit button.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-src">W3C HTML Specification</a>
   */  public  String getSrc(){
  return this.getPropertyString("src");
}


  /**
   * The type of control created (all lower case).
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-type-INPUT">W3C HTML Specification</a>
   */  public  String getType(){
  return this.getPropertyString("type");
}


  /**
   * When the type attribute of the element has the value "text", "file" or
   * "password", this represents the current contents of the corresponding form
   * control, in an interactive user agent. Changing this attribute changes the
   * contents of the form control, but does not change the value of the HTML
   * value attribute of the element. When the type attribute of the element has
   * the value "button", "hidden", "submit", "reset", "image", "checkbox" or
   * "radio", this represents the HTML value attribute of the element.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-INPUT">W3C HTML Specification</a>
   */  public  String getValue(){
  return this.getPropertyString("value");
}


  /**
   * When the type attribute of the element has the value "radio" or "checkbox",
   * this represents the current state of the form control, in an interactive
   * user agent. Changes to this attribute change the state of the form control,
   * but do not change the value of the HTML checked attribute of the INPUT
   * element.
   * 
   * Note: During the handling of a click event on an input element with a type
   * attribute that has the value "radio" or "checkbox", some implementations
   * may change the value of this property before the event is being dispatched
   * in the document. If the default action of the event is canceled, the value
   * of the property may be changed back to its original value. This means that
   * the value of this property during the handling of click events is
   * implementation dependent.
   */  public  boolean isChecked(){
  return this.getPropertyBoolean("checked");
}


  /**
   * When type has the value "radio" or "checkbox", this represents the HTML
   * checked attribute of the element. The value of this attribute does not
   * change if the state of the corresponding form control, in an interactive
   * user agent, changes.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-checked">W3C HTML Specification</a>
   */  public  boolean isDefaultChecked(){
  return this.getPropertyBoolean("defaultChecked");
}


  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   */  public  boolean isDisabled(){
  return this.getPropertyBoolean("disabled");
}


  /**
   * This control is read-only. Relevant only when type has the value "text" or
   * "password".
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-readonly">W3C HTML Specification</a>
   */  public  boolean isReadOnly(){
  return this.getPropertyBoolean("readOnly");
}


  /**
   * Select the contents of the text area. For INPUT elements whose type
   * attribute has one of the following values: "text", "file", or "password".
   */
  private  native void select0(Element_Dom elt) /*-{
    elt.select();
  }-*/;
  
  public final  void select() {
	  select0(domImpl);
  }
  /**
   * A comma-separated list of content types that a server processing this form
   * will handle correctly.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accept">W3C HTML Specification</a>
   */  public  void setAccept(String accept){
   this.setPropertyString("accept",accept);
}


  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C HTML Specification</a>
   */  public  void setAccessKey(String accessKey){
   this.setPropertyString("accessKey",accessKey);
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
   * When the type attribute of the element has the value "radio" or "checkbox",
   * this represents the current state of the form control, in an interactive
   * user agent. Changes to this attribute change the state of the form control,
   * but do not change the value of the HTML checked attribute of the INPUT
   * element.
   * 
   * Note: During the handling of a click event on an input element with a type
   * attribute that has the value "radio" or "checkbox", some implementations
   * may change the value of this property before the event is being dispatched
   * in the document. If the default action of the event is canceled, the value
   * of the property may be changed back to its original value. This means that
   * the value of this property during the handling of click events is
   * implementation dependent.
   */  public  void setChecked(boolean checked){
   this.setPropertyBoolean("checked",checked);
}


  /**
   * When type has the value "radio" or "checkbox", this represents the HTML
   * checked attribute of the element. The value of this attribute does not
   * change if the state of the corresponding form control, in an interactive
   * user agent, changes.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-checked">W3C HTML Specification</a>
   */  public  void setDefaultChecked(boolean defaultChecked){
   this.setPropertyBoolean("defaultChecked",defaultChecked);
}


  /**
   * When the type attribute of the element has the value "text", "file" or
   * "password", this represents the HTML value attribute of the element. The
   * value of this attribute does not change if the contents of the
   * corresponding form control, in an interactive user agent, changes.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-INPUT">W3C HTML Specification</a>
   */  public  void setDefaultValue(String defaultValue){
   this.setPropertyString("defaultValue",defaultValue);
}


  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   */  public  void setDisabled(boolean disabled){
   this.setPropertyBoolean("disabled",disabled);
}


  /**
   * Maximum number of characters for text fields, when type has the value
   * "text" or "password".
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-maxlength">W3C HTML Specification</a>
   */  public  void setMaxLength(int maxLength){
   this.setPropertyInt("maxLength",maxLength);
}


  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-INPUT">W3C HTML Specification</a>
   */  public  void setName(String name){
   this.setPropertyString("name",name);
}


  /**
   * This control is read-only. Relevant only when type has the value "text" or
   * "password".
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-readonly">W3C HTML Specification</a>
   */  public  void setReadOnly(boolean readOnly){
   this.setPropertyBoolean("readOnly",readOnly);
}


  /**
   * Size information. The precise meaning is specific to each type of field.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-size-INPUT">W3C HTML Specification</a>
   */  public  void setSize(int size){
   this.setPropertyInt("size",size);
}


  /**
   * When the type attribute has the value "image", this attribute specifies the
   * location of the image to be used to decorate the graphical submit button.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-src">W3C HTML Specification</a>
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
   * When the type attribute of the element has the value "text", "file" or
   * "password", this represents the current contents of the corresponding form
   * control, in an interactive user agent. Changing this attribute changes the
   * contents of the form control, but does not change the value of the HTML
   * value attribute of the element. When the type attribute of the element has
   * the value "button", "hidden", "submit", "reset", "image", "checkbox" or
   * "radio", this represents the HTML value attribute of the element.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-INPUT">W3C HTML Specification</a>
   */  public  void setValue(String value){
   this.setPropertyString("value",value);
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
