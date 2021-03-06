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
 * Client-side image map area definition.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#edef-AREA">
 *      W3C HTML Specification</a>
 */
@TagName(AreaElement.TAG)
public class AreaElement extends Element {
	public static final String TAG = "area";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static AreaElement as(Element elem) {
		assert is(elem);
		return (AreaElement) elem;
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

	protected AreaElement() {
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
	 * Alternate text for user agents not rendering the normal content of this
	 * element.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt">
	 *      W3C HTML Specification</a>
	 */
	public String getAlt() {
		return this.getPropertyString("alt");
	}

	/**
	 * Comma-separated list of lengths, defining an active region geometry. See
	 * also shape for the shape of the region.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-coords">
	 *      W3C HTML Specification</a>
	 */
	public String getCoords() {
		return this.getPropertyString("coords");
	}

	/**
	 * The URI of the linked resource.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">
	 *      W3C HTML Specification</a>
	 */
	public String getHref() {
		return this.getPropertyString("href");
	}

	/**
	 * The shape of the active area. The coordinates are given by coords.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-shape">
	 *      W3C HTML Specification</a>
	 */
	public String getShape() {
		return this.getPropertyString("shape");
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
	 * Alternate text for user agents not rendering the normal content of this
	 * element.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt">
	 *      W3C HTML Specification</a>
	 */
	public void setAlt(String alt) {
		this.setPropertyString("alt", alt);
	}

	/**
	 * Comma-separated list of lengths, defining an active region geometry. See
	 * also shape for the shape of the region.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-coords">
	 *      W3C HTML Specification</a>
	 */
	public void setCoords(String coords) {
		this.setPropertyString("coords", coords);
	}

	/**
	 * The URI of the linked resource.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">
	 *      W3C HTML Specification</a>
	 */
	public void setHref(String href) {
		this.setPropertyString("href", href);
	}

	/**
	 * The shape of the active area. The coordinates are given by coords.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-shape">
	 *      W3C HTML Specification</a>
	 */
	public void setShape(String shape) {
		this.setPropertyString("shape", shape);
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
}
