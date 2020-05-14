package cc.alcina.framework.gwt.client.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public abstract class DomContext {
	public static void clearReferences() {
		get().clearReferences0();
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

	public static void putXmlDoc(DomDoc doc) {
		get().putXmlDoc0(doc);
	}

	public static void scrollIntoView(Element elem) {
		get().scrollIntoView0(elem);
	}

	public static void setProperty(Element elem, String key, String value) {
		get().setProperty0(elem, key, value);
	}

	public static void setStyleProperty(Element elem, String key,
			String value) {
		get().setStyleProperty0(elem, key, value);
	}

	public static DomDoc xmlDoc(Document domDocument) {
		return get().getXmlDoc(domDocument);
	}

	private static DomContext get() {
		return Registry.impl(DomContext.class);
	}

	protected abstract void clearReferences0();

	protected abstract int getAbsoluteTop0(Element parentElement);

	protected abstract Document getDocument0();

	protected abstract Element getElementForPositioning0(Element elem);

	protected abstract int getOffsetTop0(Element elem);

	protected abstract DomDoc getXmlDoc(Document domDocument);

	protected abstract boolean isVisibleAncestorChain0(Element elem);

	protected abstract void putXmlDoc0(DomDoc doc);

	protected abstract void scrollIntoView0(Element elem);

	protected abstract void setProperty0(Element elem, String key,
			String value);

	protected abstract void setStyleProperty0(Element elem, String key,
			String value);
}