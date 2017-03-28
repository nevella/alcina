package com.google.gwt.dom.client;

public class VmLocalDom_Jvm implements IDOMImpl {
	@Override
	public Element createVmLocalElement(Document doc, String tag) {
		Element_Jvm jvmElement = doc.vmLocalImpl.createElement_Jvm(tag);
		return VmLocalDomBridge.nodeFor(jvmElement);
	}

	@Override
	public InputElement createInputElement(Document doc, String type) {
		Element_Jvm element_Jvm = doc.vmLocalImpl.createElement_Jvm("input");
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
		throw new UnsupportedOperationException();
	}
}
