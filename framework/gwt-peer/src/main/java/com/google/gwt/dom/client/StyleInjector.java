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

/*
 * These were optimised to batch element creation - that's redundant, since
 * localdom does that at a lower level
 */
public class StyleInjector {
	public static StyleElement createAndAttachElement(String contents) {
		StyleElement style = Document.get().createStyleElement();
		style.setPropertyString("language", "text/css");
		setContents(style, contents);
		getHead().appendChild(style);
		return style;
	}

	private static HeadElement getHead() {
		Element elt = (Element) Document.get().getDocumentElement()
				.asDomNode().children.byTag("head").get(0).w3cElement();
		assert elt != null : "The host HTML page does not have a <head> element"
				+ " which is required by StyleInjector";
		return HeadElement.as(elt);
	}

	public static void inject(String contents) {
		createAndAttachElement(contents);
	}

	public static void injectNow(String contents) {
		inject(contents);
	}

	public static void setContents(StyleElement style, String contents) {
		style.setInnerText(contents);
	}
}
