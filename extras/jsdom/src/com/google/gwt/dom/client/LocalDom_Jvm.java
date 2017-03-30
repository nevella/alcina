package com.google.gwt.dom.client;

public class LocalDom_Jvm implements IDOMImpl {
	@Override
	public Element createLocalElement(Document doc, String tag) {
		return LocalDomBridge.nodeFor(createUnwrappedLocalElement(doc, tag));
	}

	@Override
	public InputElement createInputElement(Document doc, String type) {
		Document_Jvm document_Jvm = doc.castLocalImpl();
		Element_Jvm element_Jvm = document_Jvm.createElement_Jvm("input");
		LocalDomBridge.get().createdLocalElement(element_Jvm);
		element_Jvm.setAttribute("type", type);
		return (InputElement) element_Jvm.nodeFor();
	}

	@Override
	public ButtonElement createButtonElement(Document doc, String type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createCheckInputElement(Document doc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputElement createInputRadioElement(Document doc, String name) {
		InputElement inputElement = createInputElement(doc, "radio");
		inputElement.setName(name);
		inputElement.setValue("on");
		return inputElement;
	}

	@Override
	public Element_Jvm createUnwrappedLocalElement(Document doc, String tag) {
		Document_Jvm document_Jvm = doc.castLocalImpl();
		Element_Jvm element_Jvm = document_Jvm.createElement_Jvm(tag);
		LocalDomBridge.get().createdLocalElement(element_Jvm);
		return element_Jvm;
	}
}
