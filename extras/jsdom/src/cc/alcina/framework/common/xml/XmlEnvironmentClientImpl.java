package cc.alcina.framework.common.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.xml.XmlDoc;
import cc.alcina.framework.common.client.xml.XmlEnvironment;
import cc.alcina.framework.common.client.xml.XmlNode;
import cc.alcina.framework.common.client.xml.XmlNode.XpathEvaluator;

@RegistryLocation(registryPoint = XmlEnvironment.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
/*
 * Not yet implemented, but could be without much difficulty
 */
public class XmlEnvironmentClientImpl implements XmlEnvironment {
	@Override
	public XpathEvaluator createXpathEvaluator(XmlNode xmlNode,
			XpathEvaluator xpathEvaluator) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node loadFromXml(String xml) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public String log(XmlNode xmlNode, boolean pretty) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String prettyPrint(Document domDoc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String prettyToString(XmlNode xmlNode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamespaceResult removeNamespaces(XmlDoc xmlDoc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NamespaceResult restoreNamespaces(XmlDoc xmlDoc, String firstTag) {
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
