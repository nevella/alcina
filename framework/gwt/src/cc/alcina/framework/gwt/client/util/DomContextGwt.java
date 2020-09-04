package cc.alcina.framework.gwt.client.util;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.dom.DomDoc;

public class DomContextGwt extends DomContext {
	DomDoc doc = null;

	@Override
	protected void clearReferences0() {
		getXmlDoc(null).clearElementReferences();
	}

	@Override
	protected int getAbsoluteTop0(org.w3c.dom.Element w3cElem) {
		Element elem = (Element) w3cElem;
		return elem.getAbsoluteTop();
	}

	@Override
	protected org.w3c.dom.Document getDocument0() {
		return Document.get();
	}

	@Override
	protected org.w3c.dom.Element
			getElementForPositioning0(org.w3c.dom.Element w3cElem) {
		Element elem = (Element) w3cElem;
		return WidgetUtils.getElementForPositioning(elem);
	}

	@Override
	protected int getOffsetTop0(org.w3c.dom.Element w3cElem) {
		Element elem = (Element) w3cElem;
		return elem.getOffsetTop();
	}

	@Override
	protected DomDoc getXmlDoc(org.w3c.dom.Document domDocument) {
		if (doc == null) {
			doc = DomDoc.from(getDocument0());
		}
		return doc;
	}

	@Override
	protected boolean isVisibleAncestorChain0(org.w3c.dom.Element w3cElem) {
		Element elem = (Element) w3cElem;
		return WidgetUtils.isVisibleAncestorChain(elem);
	}

	@Override
	protected void putXmlDoc0(DomDoc doc) {
		// multi-document environments only
		throw new UnsupportedOperationException();
	}

	@Override
	protected void scrollIntoView0(org.w3c.dom.Element w3cElem) {
		Element elem = (Element) w3cElem;
		elem.scrollIntoView();
	}

	@Override
	protected void setProperty0(org.w3c.dom.Element w3cElem, String key,
			String value) {
		Element elem = (Element) w3cElem;
		elem.setPropertyString(key, value);
	}

	@Override
	protected void setStyleProperty0(org.w3c.dom.Element w3cElem, String key,
			String value) {
		Element elem = (Element) w3cElem;
		elem.getStyle().setProperty(key, value);
	}
}