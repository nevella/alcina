package cc.alcina.framework.entity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomEnvironment;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.XpathEvaluator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.CommonUtils;

@Registration.Singleton(value = DomEnvironment.class, priority = Registration.Priority.PREFERRED_LIBRARY)
public class DomEnvironmentJvmImpl implements DomEnvironment {
	@Override
	public XpathEvaluator createXpathEvaluator(DomNode xmlNode,
			XpathEvaluator xpathEvaluator) {
		XpathHelper xh = null;
		if (xpathEvaluator != null) {
			xh = ((OptimizingXpathEvaluator) xpathEvaluator).getXpathHelper();
		} else {
			xh = new XpathHelper(xmlNode.domNode());
		}
		return xh.createOptimisedEvaluator(xmlNode.domNode());
	}

	@Override
	public DomDocument loadFromUrl(String url) {
		return ResourceUtilities.loadXmlDocFromUrl(url);
	}

	@Override
	public Node loadFromXml(String xml) throws Exception {
		return XmlUtils.loadDocument(xml, true);
	}

	@Override
	public String log(DomNode xmlNode, boolean pretty) {
		try {
			if (pretty) {
				XmlUtils.logToFilePretty(xmlNode.domNode());
			} else {
				XmlUtils.logToFile(xmlNode.domNode());
			}
			return "ok";
		} catch (Exception e) {
			try {
				XmlUtils.logToFile(xmlNode.domNode());
				return "could not log pretty - logged raw instead";
			} catch (Exception e1) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	@Override
	public String prettyPrint(Document domDoc) {
		return XmlUtils.prettyPrintWithDOM3LS(domDoc);
	}

	@Override
	public String prettyToString(DomNode xmlNode) {
		Node node = xmlNode.domNode();
		try {
			if (node.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
				return XmlUtils
						.prettyPrintWithDOM3LSNode((DocumentFragment) node);
			} else {
				return XmlUtils.prettyPrintWithDOM3LSNode((Element) node);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
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
	public String toHtml(DomDocument doc) {
		String xml = doc.prettyToString();
		xml = XmlUtils.expandEmptyElements(xml);
		return XmlUtils.fixStyleNodeContents(xml);
	}

	@Override
	public String toXml(Node node) {
		return XmlUtils.streamXML(node);
	}
}
