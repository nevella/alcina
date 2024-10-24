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
 * Form field label text.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-LABEL">
 *      W3C HTML Specification</a>
 */
@TagName(LabelElement.TAG)
public class LabelElement extends Element {
	public static final String TAG = "label";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static LabelElement as(Element elem) {
		assert is(elem);
		return (LabelElement) elem;
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

	protected LabelElement() {
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
	 * Returns the FORM element containing this control. Returns null if this
	 * control is not within the context of a form.
	 */
	public FormElement getForm() {
		throw new FixmeUnsupportedOperationException();
	}

	/**
	 * This attribute links this label with another form control by id
	 * attribute.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-for">
	 *      W3C HTML Specification</a>
	 */
	public String getHtmlFor() {
		return this.getPropertyString("htmlFor");
	}

	@Override
	public String getPropertyString(String name) {
		if ("htmlFor".equals(name)) {
			if (hasRemote()) {
				return jsoRemote().getPropertyString(name);
			} else {
				return getAttribute("for");
			}
		}
		return super.getPropertyString(name);
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
	 * This attribute links this label with another form control by id
	 * attribute.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-for">
	 *      W3C HTML Specification</a>
	 */
	public void setHtmlFor(String htmlFor) {
		this.setPropertyString("htmlFor", htmlFor);
	}

	@Override
	public void setPropertyString(String name, String value) {
		if ("htmlFor".equals(name)) {
			local().setAttribute("for", value);
		} else {
			local().setPropertyString(name, value);
		}
		sync(() -> remote().setPropertyString(name, value));
	}
}
