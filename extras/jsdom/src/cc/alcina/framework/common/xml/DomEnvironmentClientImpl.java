package cc.alcina.framework.common.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomEnvironment;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.XpathEvaluator;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = DomEnvironment.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
/*
 * Not yet implemented, but could be without much difficulty
 */
public class DomEnvironmentClientImpl implements DomEnvironment {
	@Override
	public XpathEvaluator createXpathEvaluator(DomNode xmlNode,
			XpathEvaluator xpathEvaluator) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node loadFromXml(String xml) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public String log(DomNode xmlNode, boolean pretty) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String prettyPrint(Document domDoc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String prettyToString(DomNode xmlNode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamespaceResult removeNamespaces(DomDoc xmlDoc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamespaceResult restoreNamespaces(DomDoc xmlDoc, String firstTag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String streamNCleanForBrowserHtmlFragment(Node node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toXml(Node node) {
		throw new UnsupportedOperationException();
	}
}
