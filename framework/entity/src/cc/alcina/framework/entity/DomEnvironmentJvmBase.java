package cc.alcina.framework.entity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.gwt.dom.client.Document.RemoteType;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomEnvironment;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.XpathEvaluator;
import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class DomEnvironmentJvmBase implements DomEnvironment {
	@Override
	public XpathEvaluator createXpathEvaluator(DomNode xmlNode,
			XpathEvaluator xpathEvaluator) {
		XpathHelper xh = null;
		if (xpathEvaluator != null) {
			xh = ((OptimizingXpathEvaluator) xpathEvaluator).getXpathHelper();
		} else {
			xh = new XpathHelper(xmlNode.w3cNode());
		}
		return xh.createOptimisedEvaluator(xmlNode.w3cNode());
	}

	@Override
	public boolean isEarlierThan(Node o1, Node o2) {
		return XmlUtils.isEarlierThan(o1, o2);
	}

	@Override
	public DomDocument loadFromUrl(String url) {
		return Io.read().url(url).asDomDocument();
	}

	@Override
	public Node loadFromXml(String xml, boolean gwtDocument) throws Exception {
		if (gwtDocument) {
			com.google.gwt.dom.client.Document document = com.google.gwt.dom.client.Document.contextProvider
					.createFrame(RemoteType.REF_ID);
			document.createDocumentElement(xml, true);
			return document;
		} else {
			return XmlUtils.loadDocument(xml, true);
		}
	}

	boolean isGwtDocument(Document document) {
		return document instanceof com.google.gwt.dom.client.Document;
	}

	com.google.gwt.dom.client.Document asGwtDocument(Document document) {
		return (com.google.gwt.dom.client.Document) document;
	}

	@Override
	public String log(DomNode domNode, boolean pretty) {
		if (isGwtDocument(domNode.document.domDoc())) {
			if (domNode.isElement()) {
				return domNode.gwtElement().getOuterHtml(pretty);
			} else if (domNode.isDocumentNode()) {
				return log(domNode.document.getDocumentElementNode(), pretty);
			} else {
				return domNode.w3cNode().toString();
			}
		}
		try {
			LooseContext.pushWithTrue(XmlUtils.CONTEXT_MUTE_XML_SAX_EXCEPTIONS);
			if (pretty) {
				XmlUtils.logToFilePretty(domNode.w3cNode());
			} else {
				XmlUtils.logToFile(domNode.w3cNode());
			}
			return "ok";
		} catch (Exception e) {
			try {
				XmlUtils.logToFile(domNode.w3cNode());
				return "could not log pretty - logged raw instead";
			} catch (Exception e1) {
				throw new WrappedRuntimeException(e);
			}
		} finally {
			LooseContext.pop();
		}
	}

	@Override
	public String prettyPrint(Document w3cDoc) {
		if (isGwtDocument(w3cDoc)) {
			return asGwtDocument(w3cDoc).getDocumentElement()
					.getOuterHtml(true);
		} else {
			return XmlUtils.prettyPrintWithDOM3LS(w3cDoc);
		}
	}

	@Override
	public String prettyToString(DomNode xmlNode) {
		if (isGwtDocument(xmlNode.document.domDoc())) {
			return log(xmlNode, true);
		} else {
			Node node = xmlNode.w3cNode();
			try {
				if (node.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
					return XmlUtils
							.prettyPrintWithDOM3LSNode((DocumentFragment) node);
				} else if (node.getNodeType() == Node.ELEMENT_NODE) {
					return XmlUtils.prettyPrintWithDOM3LSNode((Element) node);
				} else {
					return XmlUtils.streamXML(node);
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	@Override
	public NamespaceResult removeNamespaces(DomDocument xmlDoc) {
		NamespaceResult result = new NamespaceResult();
		String xml = xmlDoc.fullToString();
		Pattern p = Pattern.compile("(?s)<([A-Za-z]\\S+) .+?>");
		Matcher m = p.matcher(xml);
		m.find();
		result.firstTag = m.group();
		xml = m.replaceFirst("<$1>");
		result.xml = xml;
		return result;
	}

	@Override
	public NamespaceResult restoreNamespaces(DomDocument xmlDoc,
			String firstTag) {
		NamespaceResult result = new NamespaceResult();
		result.xml = xmlDoc.fullToString();
		Pattern p = Pattern.compile("(?s)<[A-Za-z]\\S+>");
		Matcher m = p.matcher(result.xml);
		result.xml = m.replaceFirst(CommonUtils.escapeRegex(firstTag));
		return result;
	}

	@Override
	public String streamNCleanForBrowserHtmlFragment(Node node) {
		return XmlUtils.streamNCleanForBrowserHtmlFragment(node);
	}

	@Override
	public String toHtml(DomDocument doc, boolean pretty) {
		if (isGwtDocument(doc.domDoc())) {
			return log(doc, pretty);
		} else {
			String xml = pretty ? doc.prettyToString() : doc.fullToString();
			xml = XmlUtils.expandEmptyElements(xml);
			return XmlUtils.fixStyleNodeContents(xml);
		}
	}

	@Override
	public String toXml(Node node) {
		if (isGwtDocument(node.getOwnerDocument())) {
			return log(DomNode.from(node), true);
		} else {
			return XmlUtils.streamXML(node);
		}
	}
}
