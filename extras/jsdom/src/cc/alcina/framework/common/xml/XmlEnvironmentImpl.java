package cc.alcina.framework.common.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.xml.XmlDoc;
import cc.alcina.framework.common.client.xml.XmlEnvironment;
import cc.alcina.framework.common.client.xml.XmlNode;
import cc.alcina.framework.common.client.xml.XmlNode.XpathEvaluator;

public class XmlEnvironmentImpl implements XmlEnvironment {
	@Override
	public XpathEvaluator createXpathEvaluator(XmlNode xmlNode,
			XpathEvaluator xpathEvaluator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node loadFromXml(String xml) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String log(XmlNode xmlNode, boolean pretty) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String prettyPrint(Document domDoc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String prettyToString(XmlNode xmlNode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamespaceResult removeNamespaces(XmlDoc xmlDoc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NamespaceResult restoreNamespaces(XmlDoc xmlDoc, String firstTag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String streamNCleanForBrowserHtmlFragment(Node node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toXml(Node node) {
		// TODO Auto-generated method stub
		return null;
	}
}
