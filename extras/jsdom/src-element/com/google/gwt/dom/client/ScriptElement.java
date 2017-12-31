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
import com.google.gwt.safehtml.shared.annotations.IsTrustedResourceUri;

/**
 * Script statements.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#edef-SCRIPT">
 *      W3C HTML Specification</a>
 */
@TagName(ScriptElement.TAG)
public class ScriptElement extends Element {
	public static final String TAG = "script";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static ScriptElement as(Element elem) {
		assert is(elem);
		return (ScriptElement) elem;
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

	protected ScriptElement() {
	}

	/**
	 * Indicates that the user agent can defer processing of the script.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-defer">
	 *      W3C HTML Specification</a>
	 */
	public String getDefer() {
		return this.getPropertyString("defer");
	}

	/**
	 * URI designating an external script.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-src-SCRIPT">
	 *      W3C HTML Specification</a>
	 */
	public String getSrc() {
		return this.getPropertyString("src");
	}

	/**
	 * The script content of the element.
	 */
	public String getText() {
		return this.getPropertyString("text");
	}

	/**
	 * The content type of the script language.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-type-SCRIPT">
	 *      W3C HTML Specification</a>
	 */
	public String getType() {
		return this.getPropertyString("type");
	}

	/**
	 * Indicates that the user agent can defer processing of the script.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-defer">
	 *      W3C HTML Specification</a>
	 */
	public void setDefer(String defer) {
		this.setPropertyString("defer", defer);
	}

	/**
	 * URI designating an external script.
	 *
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-src-SCRIPT">
	 *      W3C HTML Specification</a>
	 */
	public void setSrc(@IsTrustedResourceUri String src) {
		this.setPropertyString("src", src);
	}

	/**
	 * The script content of the element.
	 */
	public void setText(String text) {
		this.setPropertyString("text", text);
	}

	/**
	 * The content type of the script language.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-type-SCRIPT">
	 *      W3C HTML Specification</a>
	 */
	public void setType(String type) {
		this.setPropertyString("type", type);
	}
}
