package cc.alcina.framework.gwt.client.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@EnvironmentRegistration
public abstract class DomContext {
	// transitional, use boundingclientrect for positioning computations
	private boolean useBoundingClientRect;

	public boolean isUseBoundingClientRect() {
		return useBoundingClientRect;
	}

	public void setUseBoundingClientRect(boolean useBoundingClientRect) {
		this.useBoundingClientRect = useBoundingClientRect;
	}

	public static void clearReferences() {
		DomContext domContext = get();
		if (domContext != null) {
			domContext.clearReferences0();
		}
	}

	private static DomContext get() {
		return Registry.impl(DomContext.class);
	}

	public static int getAbsoluteTop(Element elem) {
		return get().getAbsoluteTop0(elem);
	}

	/* ensure cache exists at a given context level */
	public static void ensure() {
		get().ensure0();
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

	public static void putXmlDoc(DomDocument doc) {
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

	public static DomDocument xmlDoc(Document domDocument) {
		return get().getXmlDoc(domDocument);
	}

	public static boolean isZeroOffsetDims(Element elem) {
		return get().isZeroOffsetDims0(elem);
	}

	protected abstract void clearReferences0();

	protected abstract int getAbsoluteTop0(Element parentElement);

	protected abstract Document getDocument0();

	protected void ensure0() {
	};

	protected abstract Element getElementForPositioning0(Element elem);

	protected abstract int getOffsetTop0(Element elem);

	protected abstract DomDocument getXmlDoc(Document domDocument);

	protected abstract boolean isVisibleAncestorChain0(Element elem);

	protected abstract boolean isZeroOffsetDims0(Element elem);

	protected abstract void putXmlDoc0(DomDocument doc);

	protected abstract void scrollIntoView0(Element elem);

	protected abstract void setProperty0(Element elem, String key,
			String value);

	protected abstract void setStyleProperty0(Element elem, String key,
			String value);
}