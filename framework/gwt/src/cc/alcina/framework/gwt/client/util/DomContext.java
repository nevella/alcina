package cc.alcina.framework.gwt.client.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.xml.XmlDoc;

public abstract class DomContext {
	public static XmlDoc xmlDoc(Document domDocument) {
		return get().getXmlDoc(domDocument);
	}

	public static int getAbsoluteTop(Element parentElement) {
		return get().getAbsoluteTop0(parentElement);
	}

	public static Document getDocument() {
		return get().getDocument0();
	}

	public static Element getElementForPositioning(Element elem) {
		return get().getElementForPositioning0(elem);
	}

	public static int getOffsetTop(Element elem) {
		return get().getOffsetTop0(elem);
	}

	public static boolean isVisibleAncestorChain(Element elem) {
		return get().isVisibleAncestorChain0(elem);
	}

	public static void putXmlDoc(XmlDoc doc) {
		get().putXmlDoc0(doc);
	}

	public static void scrollIntoView(Element elem) {
		get().scrollIntoView0(elem);
	}

	private static DomContext get() {
		return Registry.impl(DomContext.class);
	}

	protected abstract int getAbsoluteTop0(Element parentElement);

	protected abstract Document getDocument0();

	protected abstract Element getElementForPositioning0(Element elem);

	protected abstract int getOffsetTop0(Element elem);

	protected abstract XmlDoc getXmlDoc(Document domDocument);

	protected abstract boolean isVisibleAncestorChain0(Element elem);

	protected abstract void putXmlDoc0(XmlDoc doc);

	protected abstract void scrollIntoView0(Element elem);
}