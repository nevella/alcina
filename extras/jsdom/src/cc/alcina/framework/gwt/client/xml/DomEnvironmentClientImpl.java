package cc.alcina.framework.gwt.client.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomEnvironment;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.XpathEvaluator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

@Reflected
@Registration.Singleton(DomEnvironment.class)
public class DomEnvironmentClientImpl implements DomEnvironment {
	@Override
	public XpathEvaluator createXpathEvaluator(DomNode xmlNode,
			XpathEvaluator xpathEvaluator) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEarlierThan(Node o1, Node o2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DomDocument loadFromUrl(String url) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node loadFromXml(String xml, boolean gwtDocument, boolean remote) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String log(DomNode xmlNode, boolean pretty) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toPrettyMarkup(Document domDoc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toPrettyMarkup(DomNode xmlNode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamespaceResult removeNamespaces(DomDocument xmlDoc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamespaceResult restoreNamespaces(DomDocument xmlDoc,
			String firstTag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String streamNCleanForBrowserHtmlFragment(Node node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toMarkup(DomDocument doc, boolean pretty) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toMarkup(Node node) {
		com.google.gwt.dom.client.Node clientNode = (com.google.gwt.dom.client.Node) node;
		short nodeType = clientNode.getNodeType();
		switch (nodeType) {
		case Node.TEXT_NODE:
		case Node.COMMENT_NODE:
			return clientNode.getNodeValue();
		case Node.ELEMENT_NODE:
			return ((com.google.gwt.dom.client.Element) node).getOuterHtml();
		default:
			throw new UnsupportedOperationException();
		}
	}
}
