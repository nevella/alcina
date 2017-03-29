package com.google.gwt.dom.client;

public interface IDOMImpl {
	Element createLocalElement(Document doc, String tag);

	InputElement createInputElement(Document doc, String type);

	ButtonElement createButtonElement(Document doc, String type);

	InputElement createCheckInputElement(Document doc);

	InputElement createInputRadioElement(Document doc, String name);

	default ScriptElement createScriptElement(Document doc, String source) {
		ScriptElement elem = (ScriptElement) createLocalElement(doc, "script");
		elem.setText(source);
		return elem;
	}
}