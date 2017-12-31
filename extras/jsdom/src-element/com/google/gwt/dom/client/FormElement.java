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
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

/**
 * The FORM element encompasses behavior similar to a collection and an element.
 * It provides direct access to the contained form controls as well as the
 * attributes of the form element.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-FORM">
 *      W3C HTML Specification</a>
 */
@TagName(FormElement.TAG)
public class FormElement extends Element {
	public static final String TAG = "form";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static FormElement as(Element elem) {
		assert is(elem);
		return (FormElement) elem;
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

	protected FormElement() {
	}

	/**
	 * List of character sets supported by the server.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accept-charset">
	 *      W3C HTML Specification</a>
	 */
	public final String getAcceptCharset() {
		return getPropertyString("acceptCharset");
	}

	/**
	 * Server-side form handler.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-action">
	 *      W3C HTML Specification</a>
	 */
	public final String getAction() {
		return getPropertyString("action");
	}

	/**
	 * Returns a collection of all form control elements in the form.
	 */
	public final NodeCollection<Element> getElements() {
		throw new FixmeUnsupportedOperationException();
	}

	/**
	 * The content type of the submitted form, generally
	 * "application/x-www-form-urlencoded".
	 * 
	 * Note: The onsubmit even handler is not guaranteed to be triggered when
	 * invoking this method. The behavior is inconsistent for historical reasons
	 * and authors should not rely on a particular one.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-enctype">
	 *      W3C HTML Specification</a>
	 */
	public final String getEnctype() {
		return getPropertyString("enctype");
	}

	/**
	 * HTTP method [IETF RFC 2616] used to submit form.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-method">
	 *      W3C HTML Specification</a>
	 */
	public final String getMethod() {
		return getPropertyString("method");
	}

	/**
	 * Names the form.
	 */
	public final String getName() {
		return getPropertyString("name");
	}

	/**
	 * Frame to render the resource in.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">
	 *      W3C HTML Specification</a>
	 */
	public final String getTarget() {
		return getPropertyString("target");
	}

	public final void reset() {
		callMethod("reset");
	}

	/**
	 * List of character sets supported by the server.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accept-charset">
	 *      W3C HTML Specification</a>
	 */
	public final void setAcceptCharset(String acceptCharset) {
		setPropertyString("acceptCharset", acceptCharset);
	}

	/**
	 * Server-side form handler.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-action">
	 *      W3C HTML Specification</a>
	 */
	public final void setAction(SafeUri action) {
		setAction(action.asString());
	}

	/**
	 * Server-side form handler.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-action">
	 *      W3C HTML Specification</a>
	 */
	public final void setAction(@IsSafeUri String action) {
		setPropertyString("action", action);
	}

	/**
	 * The content type of the submitted form, generally
	 * "application/x-www-form-urlencoded".
	 * 
	 * Note: The onsubmit even handler is not guaranteed to be triggered when
	 * invoking this method. The behavior is inconsistent for historical reasons
	 * and authors should not rely on a particular one.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-enctype">
	 *      W3C HTML Specification</a>
	 */
	public final void setEnctype(String enctype) {
		setPropertyString("enctype", enctype);
	}

	/**
	 * HTTP method [IETF RFC 2616] used to submit form.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-method">
	 *      W3C HTML Specification</a>
	 */
	public final void setMethod(String method) {
		setPropertyString("method", method);
	}

	/**
	 * Names the form.
	 */
	public final void setName(String name) {
		setPropertyString("name", name);
	}

	/**
	 * Frame to render the resource in.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">
	 *      W3C HTML Specification</a>
	 */
	public final void setTarget(String target) {
		setPropertyString("target", target);
	}

	/**
	 * Submits the form. It performs the same action as a submit button.
	 */
	public final void submit() {
		callMethod("submit");
	}
}
