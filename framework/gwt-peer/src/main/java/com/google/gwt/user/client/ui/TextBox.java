// Source code is decompiled from a .class file using FernFlower decompiler.
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;

public class TextBox extends TextBoxBase {
	public static TextBox wrap(Element element) {
		assert Document.get().getBody().isOrHasChild(element);
		TextBox textBox = new TextBox(element);
		textBox.onAttach();
		RootPanel.detachOnWindowClose(textBox);
		return textBox;
	}

	public TextBox() {
		this(Document.get().createTextInputElement(), "gwt-TextBox");
	}

	protected TextBox(Element element) {
		super(element);
		assert InputElement.as(element).getType().equalsIgnoreCase("text");
	}

	TextBox(Element element, String styleName) {
		super(element);
		if (styleName != null) {
			this.setStyleName(styleName);
		}
		getElement().setAttribute("autocomplete", "off");
	}

	public int getMaxLength() {
		return this.getInputElement().getMaxLength();
	}

	public int getVisibleLength() {
		return this.getInputElement().getSize();
	}

	public void setMaxLength(int length) {
		this.getInputElement().setMaxLength(length);
	}

	public void setVisibleLength(int length) {
		this.getInputElement().setSize(length);
	}

	private InputElement getInputElement() {
		return (InputElement) this.getElement().cast();
	}
}
