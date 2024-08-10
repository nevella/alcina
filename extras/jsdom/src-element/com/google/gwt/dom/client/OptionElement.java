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
 * A selectable choice.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-OPTION">
 *      W3C HTML Specification</a>
 */
@TagName(OptionElement.TAG)
public class OptionElement extends Element {
	public static final String TAG = "option";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static OptionElement as(Element elem) {
		assert is(elem);
		return (OptionElement) elem;
	}

	/**
	 * Determine whether the given {@link Element} can be cast to this class. A
	 * <code>null</code> node will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(Element elem) {
		return elem != null && elem.hasTagName(TAG);
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

	protected OptionElement() {
	}

	/**
	 * Returns the FORM element containing this control. Returns null if this
	 * control is not within the context of a form.
	 */
	public FormElement getForm() {
		throw new FixmeUnsupportedOperationException();
	}

	/**
	 * The index of this OPTION in its parent SELECT, starting from 0.
	 */
	public int getIndex() {
		return this.getPropertyInt("index");
	}

	/**
	 * Option label for use in hierarchical menus.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-label-OPTION">
	 *      W3C HTML Specification</a>
	 */
	public String getLabel() {
		return this.getPropertyString("label");
	}

	/**
	 * The text contained within the option element.
	 */
	public String getText() {
		return this.getPropertyString("text");
	}

	/**
	 * The current form control value.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-OPTION">
	 *      W3C HTML Specification</a>
	 */
	public String getValue() {
		return this.getPropertyString("value");
	}

	/**
	 * Represents the value of the HTML selected attribute. The value of this
	 * attribute does not change if the state of the corresponding form control,
	 * in an interactive user agent, changes.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-selected">
	 *      W3C HTML Specification</a>
	 */
	public boolean isDefaultSelected() {
		return this.getPropertyBoolean("defaultSelected");
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
	 * Represents the current state of the corresponding form control, in an
	 * interactive user agent. Changing this attribute changes the state of the
	 * form control, but does not change the value of the HTML selected
	 * attribute of the element.
	 */
	public boolean isSelected() {
		return this.getPropertyBoolean("selected");
	}

	/**
	 * Represents the value of the HTML selected attribute. The value of this
	 * attribute does not change if the state of the corresponding form control,
	 * in an interactive user agent, changes.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-selected">
	 *      W3C HTML Specification</a>
	 */
	public void setDefaultSelected(boolean selected) {
		this.setPropertyBoolean("defaultSelected", selected);
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
	 * Option label for use in hierarchical menus.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-label-OPTION">
	 *      W3C HTML Specification</a>
	 */
	public void setLabel(String label) {
		this.setPropertyString("label", label);
	}

	@Override
	public void setPropertyBoolean(String name, boolean value) {
		if (name.equals("selected") && !Boolean.valueOf(value)) {
			local().removeAttribute(name);
		} else {
			local().setPropertyBoolean(name, value);
		}
		sync(() -> remote().setPropertyBoolean(name, value));
	}

	/**
	 * Represents the current state of the corresponding form control, in an
	 * interactive user agent. Changing this attribute changes the state of the
	 * form control, but does not change the value of the HTML selected
	 * attribute of the element.
	 */
	public void setSelected(boolean selected) {
		this.setPropertyBoolean("selected", selected);
	}

	/**
	 * The text contained within the option element.
	 */
	public void setText(String text) {
		local().setInnerText(text);
		sync(() -> remote().setPropertyString("text", text));
	}

	/**
	 * The current form control value.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-OPTION">
	 *      W3C HTML Specification</a>
	 */
	public void setValue(String value) {
		this.setPropertyString("value", value);
	}
}
