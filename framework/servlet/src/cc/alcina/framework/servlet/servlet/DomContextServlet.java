package cc.alcina.framework.servlet.servlet;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.util.DomContext;

@Registration.Singleton(DomContext.class)
public class DomContextServlet extends DomContext {
	public static final String CONTEXT_DOCS = DomContextServlet.class.getName()
			+ ".CONTEXT_DOCS";

	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	protected void clearReferences0() {
		xmlDocs().clear();
	}

	@Override
	protected int getAbsoluteTop0(Element parentElement) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Document getDocument0() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Element getElementForPositioning0(Element elem) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int getOffsetTop0(Element elem) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected DomDoc getXmlDoc(Document domDocument) {
		return xmlDocs().get(domDocument);
	}

	@Override
	protected boolean isVisibleAncestorChain0(Element elem) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void putXmlDoc0(DomDoc doc) {
		/*
		 * critical - otherwise very leak-possible
		 */
		if (!xmlDocs().containsKey(doc.domDoc())) {
			xmlDocs().clear();
			xmlDocs().put(doc.domDoc(), doc);
		}
	}

	@Override
	protected void scrollIntoView0(Element elem) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void setProperty0(Element elem, String key, String value) {
		DomNode.from(elem).setAttr(key, value);
	}

	@Override
	protected void setStyleProperty0(Element elem, String key, String value) {
		DomNode.from(elem).style().setProperty(key, value);
	}

	Map<Document, DomDoc> xmlDocs() {
		return LooseContext.ensure(CONTEXT_DOCS, () -> new LinkedHashMap<>());
	}
}
