package cc.alcina.framework.common.client.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.HtmlConstants;
import cc.alcina.framework.common.client.xml.XmlNode.XpathEvaluator;

public interface XmlEnvironment {
	public static StyleResolver contextBlockResolver() {
		return Registry.impl(StyleResolver.class);
	}

	public static XmlEnvironment get() {
		return Registry.impl(XmlEnvironment.class);
	}

	public static List<Node> nodeListToList(NodeList nl) {
		List<Node> rVal = new ArrayList<Node>();
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			rVal.add(nl.item(i));
		}
		return rVal;
	}

	public XpathEvaluator createXpathEvaluator(XmlNode xmlNode,
			XpathEvaluator xpathEvaluator);

	public Node loadFromXml(String xml) throws Exception;

	public String log(XmlNode xmlNode, boolean pretty);

	public String prettyPrint(Document domDoc);

	public String prettyToString(XmlNode xmlNode);

	public NamespaceResult removeNamespaces(XmlDoc xmlDoc);

	public NamespaceResult restoreNamespaces(XmlDoc xmlDoc, String firstTag);

	public String streamNCleanForBrowserHtmlFragment(Node node);

	public String toXml(Node node);

	public static class NamespaceResult {
		public String firstTag;

		public String xml;
	}

	public static interface StyleResolver extends Predicate<XmlNode> {
		default Optional<XmlNode> getContainingBlock(XmlNode cursor) {
			return cursor.ancestors().orSelf().match(this);
		}

		default boolean isBlock(Element e) {
			return isBlock(XmlNode.from(e));
		}

		boolean isBlock(XmlNode node);

		boolean isBold(XmlNode node);

		boolean isItalic(XmlNode node);

		@Override
		default boolean test(XmlNode node) {
			return isBlock(node);
		}
	}

	@RegistryLocation(registryPoint = StyleResolver.class, implementationType = ImplementationType.INSTANCE)
	@ClientInstantiable
	public static class StyleResolverHtml implements StyleResolver {
		XmlDoc doc = null;

		@Override
		public boolean isBlock(Element e) {
			if (doc == null) {
				doc = XmlNode.from(e).doc;
			}
			return isBlock(doc.nodeFor(e));
		}

		@Override
		public boolean isBlock(XmlNode node) {
			return HtmlConstants.isHtmlBlock(node.name());
		}

		@Override
		public boolean isBold(XmlNode node) {
			return node.ancestors().orSelf()
					.has(n -> n.name().equalsIgnoreCase("B")
							|| n.name().equalsIgnoreCase("STRONG"));
		}

		@Override
		public boolean isItalic(XmlNode node) {
			return node.ancestors().orSelf()
					.has(n -> n.name().equalsIgnoreCase("I")
							|| n.name().equalsIgnoreCase("EMPH"));
		}
	}
}
