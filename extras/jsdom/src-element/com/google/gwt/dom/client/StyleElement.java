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
 * Style information.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#edef-STYLE">
 *      W3C HTML Specification</a>
 * @see <a href=
 *      "http://www.w3.org/TR/DOM-Level-2-HTML/references.html#DOMStyle">W3C
 *      HTML Specification</a>
 * @see <a href=
 *      "http://www.w3.org/TR/DOM-Level-2-HTML/references.html#DOMStyle-inf">W3C
 *      HTML Specification</a>
 */
@TagName(StyleElement.TAG)
public class StyleElement extends Element {
	public static final String TAG = "style";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static StyleElement as(Element elem) {
		assert is(elem);
		return (StyleElement) elem;
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

	protected StyleElement() {
	}

	/**
	 * The CSS text.
	 */
	public String getCssText() {
		return this.getPropertyString("cssText");
	}

	/**
	 * Enables/disables the style sheet.
	 * 
	 * @deprecated use {@link #isDisabled()} instead
	 */
	@Deprecated
	public boolean getDisabled() {
		throw new FixmeUnsupportedOperationException();
	}

	/**
	 * Designed for use with one or more target media.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-media">
	 *      W3C HTML Specification</a>
	 */
	public String getMedia() {
		return this.getPropertyString("media");
	}

	/**
	 * The content type of the style sheet language.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-type-STYLE">
	 *      W3C HTML Specification</a>
	 */
	public String getType() {
		return this.getPropertyString("type");
	}

	/**
	 * Enables/disables the style sheet.
	 */
	public boolean isDisabled() {
		return this.getPropertyBoolean("disabled");
	}

	/**
	 * Sets the CSS text.
	 */
	public void setCssText(String cssText) {
		this.setPropertyString("cssText", cssText);
	}

	/**
	 * Enables/disables the style sheet.
	 */
	public void setDisabled(boolean disabled) {
		this.setPropertyBoolean("disabled", disabled);
	}

	/**
	 * Designed for use with one or more target media.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-media">
	 *      W3C HTML Specification</a>
	 */
	public void setMedia(String media) {
		this.setPropertyString("media", media);
	}

	/**
	 * The content type of the style sheet language.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-type-STYLE">
	 *      W3C HTML Specification</a>
	 */
	public void setType(String type) {
		this.setPropertyString("type", type);
	}
}
