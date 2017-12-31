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
 * The anchor element.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#edef-A">
 *      W3C HTML Specification</a>
 */
@TagName(AnchorElement.TAG)
public class AnchorElement extends Element {
	public static final String TAG = "a";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static AnchorElement as(Element elem) {
		assert is(elem);
		return (AnchorElement) elem;
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

	protected AnchorElement() {
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
	 * The absolute URI of the linked resource.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">
	 *      W3C HTML Specification</a>
	 */
	public String getHref() {
		return this.getPropertyString("href");
	}

	/**
	 * Language code of the linked resource.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang">
	 *      W3C HTML Specification</a>
	 */
	public String getHreflang() {
		return this.getPropertyString("hreflang");
	}

	/**
	 * Anchor name.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-name-A">
	 *      W3C HTML Specification</a>
	 */
	public String getName() {
		return this.getPropertyString("name");
	}

	/**
	 * Forward link type.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel">
	 *      W3C HTML Specification</a>
	 */
	public String getRel() {
		return this.getPropertyString("rel");
	}

	/**
	 * Frame to render the resource in.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">
	 *      W3C HTML Specification</a>
	 */
	public String getTarget() {
		return this.getPropertyString("target");
	}

	/**
	 * Advisory content type.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A">
	 *      W3C HTML Specification</a>
	 */
	public String getType() {
		return this.getPropertyString("type");
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
	 * The absolute URI of the linked resource.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">
	 *      W3C HTML Specification</a>
	 */
	public final void setHref(SafeUri href) {
		setHref(href.asString());
	}

	/**
	 * The absolute URI of the linked resource.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">
	 *      W3C HTML Specification</a>
	 */
	public void setHref(@IsSafeUri String href) {
		this.setPropertyString("href", href);
	}

	/**
	 * Language code of the linked resource.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang">
	 *      W3C HTML Specification</a>
	 */
	public void setHreflang(String hreflang) {
		this.setPropertyString("hreflang", hreflang);
	}

	/**
	 * Anchor name.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-name-A">
	 *      W3C HTML Specification</a>
	 */
	public void setName(String name) {
		this.setPropertyString("name", name);
	}

	/**
	 * Forward link type.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel">
	 *      W3C HTML Specification</a>
	 */
	public void setRel(String rel) {
		this.setPropertyString("rel", rel);
	}

	/**
	 * Frame to render the resource in.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">
	 *      W3C HTML Specification</a>
	 */
	public void setTarget(String target) {
		this.setPropertyString("target", target);
	}

	/**
	 * Advisory content type.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A">
	 *      W3C HTML Specification</a>
	 */
	public void setType(String type) {
		this.setPropertyString("type", type);
	}
}
