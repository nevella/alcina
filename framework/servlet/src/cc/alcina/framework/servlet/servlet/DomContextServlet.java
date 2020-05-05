package cc.alcina.framework.servlet.servlet;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.xml.XmlDoc;
import cc.alcina.framework.gwt.client.util.DomContext;

@RegistryLocation(registryPoint = DomContext.class, implementationType = ImplementationType.SINGLETON)
public class DomContextServlet extends DomContext {
	public static final String CONTEXT_DOCS = DomContextServlet.class.getName()
			+ ".CONTEXT_DOCS";

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
	protected XmlDoc getXmlDoc(Document domDocument) {
		return xmlDocs().get(domDocument);
	}

	@Override
	protected boolean isVisibleAncestorChain0(Element elem) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void putXmlDoc0(XmlDoc doc) {
		xmlDocs().put(doc.domDoc(), doc);
	}

	@Override
	protected void scrollIntoView0(Element elem) {
		throw new UnsupportedOperationException();
	}

	Map<Document, XmlDoc> xmlDocs() {
		return LooseContext.ensure(CONTEXT_DOCS, () -> new LinkedHashMap<>());
	}
}
