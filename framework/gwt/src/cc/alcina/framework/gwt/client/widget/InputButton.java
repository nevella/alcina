/*
 * Copyright 2006 Google Inc.
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
package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasHTML;

/**
 * Uses an input-button element, rather than a button
 */
public class InputButton extends FocusWidget implements HasHTML {

	/**
	 * Creates a button with no caption.
	 */
	public InputButton() {
		InputElement button = Document.get().createButtonInputElement();
		setElement(button);
		setStyleName("gwt-Button");
		button.setPropertyString("name", "");
	}

	/**
	 * Creates a button with the given HTML caption.
	 * 
	 * @param html
	 *            the HTML caption
	 */
	public InputButton(String html) {
		this();
		setHTML(html);
	}

	/**
	 * Creates a button with the given HTML caption and click listener.
	 * 
	 * @param html
	 *            the HTML caption
	 */
	public InputButton(String html, ClickHandler clickHandler) {
		this(html);
		addClickHandler(clickHandler);
	}

	/**
	 * Programmatic equivalent of the user clicking the button.
	 */
	public void click() {
		InputElement.as(getElement()).click();
	}

	public void setHTML(String html) {
		setText(html);
	}

	public String getHTML() {
		return getText();
	}

	public String getText() {
		return getElement().getPropertyString("value");
	}

	public void setText(String text) {
		getElement().setPropertyString("value", text);
	}
}
