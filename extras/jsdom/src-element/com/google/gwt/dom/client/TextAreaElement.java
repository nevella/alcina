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
 * Multi-line text field.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-TEXTAREA">
 *      W3C HTML Specification</a>
 */
@TagName(TextAreaElement.TAG)
public class TextAreaElement extends Element {
	public static final String TAG = "textarea";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static TextAreaElement as(Element elem) {
		assert is(elem);
		return (TextAreaElement) elem;
	}

	/**
	 * Determines whether the given {@link JavaScriptObject} can be cast to this
	 * class. A <code>null</code> object will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(JavaScriptObject o) {
		if (Element.is(o)) {
			return is(Element.as(o));
		}
		return false;
	}

	/**
	 * Determine whether the given {@link Node} can be cast to this class. A
	 * <code>null</code> node will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(Node node) {
		if (Element.is(node)) {
			return is((Element) node);
		}
		return false;
	}

	/**
	 * Determine whether the given {@link Element} can be cast to this class. A
	 * <code>null</code> node will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(Element elem) {
		return elem != null && elem.hasTagName(TAG);
	}

	protected TextAreaElement() {
	}

	/**
	 * A single character access key to give access to the form control.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">
	 *      W3C HTML Specification</a>
	 */
	public String getAccessKey() {
		return this.getPropertyString("accessKey");
	}

	/**
	 * Width of control (in characters).
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-cols-TEXTAREA">
	 *      W3C HTML Specification</a>
	 */
	public int getCols() {
		return this.getPropertyInt("cols");
	}

	/**
	 * Represents the contents of the element. The value of this attribute does
	 * not change if the contents of the corresponding form control, in an
	 * interactive user agent, changes.
	 */
	public String getDefaultValue() {
		return this.getPropertyString("defaultValue");
	}

	/**
	 * @deprecated use {@link #isDisabled()} instead
	 */
	@Deprecated
	public boolean getDisabled() {
		throw new FixmeUnsupportedOperationException();
	}

	/**
	 * Returns the FORM element containing this control. Returns null if this
	 * control is not within the context of a form.
	 */
	public FormElement getForm() {
		throw new FixmeUnsupportedOperationException();
	}

	/**
	 * Form control or object name when submitted with a form.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-TEXTAREA">
	 *      W3C HTML Specification</a>
	 */
	public String getName() {
		return this.getPropertyString("name");
	}

	/**
	 * @deprecated use {@link #isReadOnly()} instead.
	 */
	@Deprecated
	public boolean getReadOnly() {
		throw new FixmeUnsupportedOperationException();
	}

	/**
	 * Number of text rows.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-rows-TEXTAREA">
	 *      W3C HTML Specification</a>
	 */
	public int getRows() {
		return this.getPropertyInt("rows");
	}

	/**
	 * The type of this form control. This the string "textarea".
	 */
	public String getType() {
		return this.getPropertyString("type");
	}

	/**
	 * Represents the current contents of the corresponding form control, in an
	 * interactive user agent. Changing this attribute changes the contents of
	 * the form control, but does not change the contents of the element. If the
	 * entirety of the data can not fit into a single string, the implementation
	 * may truncate the data.
	 */
	public String getValue() {
		return this.getPropertyString("value");
	}

	/**
	 * The control is unavailable in this context.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">
	 *      W3C HTML Specification</a>
	 */
	public boolean isDisabled() {
		return this.getPropertyBoolean("disabled");
	}

	/**
	 * This control is read-only.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-readonly">
	 *      W3C HTML Specification</a>
	 */
	public boolean isReadOnly() {
		return this.getPropertyBoolean("readOnly");
	}

	/**
	 * Select the contents of the TEXTAREA.
	 */
	native void select0(ElementRemote elt) /*-{
        this.select();
	}-*/;

	public final void select() {
		select0(ensureRemote());
	}

	/**
	 * A single character access key to give access to the form control.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">
	 *      W3C HTML Specification</a>
	 */
	public void setAccessKey(String accessKey) {
		this.setPropertyString("accessKey", accessKey);
	}

	/**
	 * Width of control (in characters).
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-cols-TEXTAREA">
	 *      W3C HTML Specification</a>
	 */
	public void setCols(int cols) {
		this.setPropertyInt("cols", cols);
	}

	/**
	 * Represents the contents of the element. The value of this attribute does
	 * not change if the contents of the corresponding form control, in an
	 * interactive user agent, changes.
	 */
	public void setDefaultValue(String defaultValue) {
		this.setPropertyString("defaultValue", defaultValue);
	}

	/**
	 * The control is unavailable in this context.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">
	 *      W3C HTML Specification</a>
	 */
	public void setDisabled(boolean disabled) {
		this.setPropertyBoolean("disabled", disabled);
	}

	/**
	 * Form control or object name when submitted with a form.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-TEXTAREA">
	 *      W3C HTML Specification</a>
	 */
	public void setName(String name) {
		this.setPropertyString("name", name);
	}

	/**
	 * This control is read-only.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-readonly">
	 *      W3C HTML Specification</a>
	 */
	public void setReadOnly(boolean readOnly) {
		this.setPropertyBoolean("readOnly", readOnly);
	}

	/**
	 * Number of text rows.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-rows-TEXTAREA">
	 *      W3C HTML Specification</a>
	 */
	public void setRows(int rows) {
		this.setPropertyInt("rows", rows);
	}

	@Override
	public void setPropertyBoolean(String name, boolean value) {
		if ((name.equals("readOnly") || name.equals("disabled")) && !value) {
			local().removeAttribute(name);
		}else{
			local().setPropertyBoolean(name, value);
		}
		remote().setPropertyBoolean(name, value);
	}

	@Override
	public String getPropertyString(String name) {
		if ("value".equals(name)) {
			if (linkedToRemote()) {
				return typedRemote().getPropertyString(name);
			} else {
				return getInnerText();
			}
		}
		return super.getPropertyString(name);
	}

	@Override
	public void setPropertyString(String name, String value) {
		if ("value".equals(name)) {
			local().setInnerText(value);
		} else {
			local().setPropertyString(name, value);
		}
		remote().setPropertyString(name, value);
	}

	/**
	 * Represents the current contents of the corresponding form control, in an
	 * interactive user agent. Changing this attribute changes the contents of
	 * the form control, but does not change the contents of the element. If the
	 * entirety of the data can not fit into a single string, the implementation
	 * may truncate the data.
	 */
	public void setValue(String value) {
		this.setPropertyString("value", value);
	}
}
