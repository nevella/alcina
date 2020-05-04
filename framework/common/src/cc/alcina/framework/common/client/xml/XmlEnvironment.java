package cc.alcina.framework.common.client.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonConstants;
import cc.alcina.framework.common.client.xml.XmlNode.XpathEvaluator;

public interface XmlEnvironment {
	public static BlockResolver contextBlockResolver() {
		return new BlockResolverHtml();
	}

	public static XmlEnvironment get() {
		return Registry.impl(XmlEnvironment.class);
	}

	public static boolean isHtmlBlockTag(String tagName) {
		return CommonConstants.HTML_BLOCKS.contains("," + tagName + ",");
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

	public static interface BlockResolver extends Predicate<XmlNode> {
		default Optional<XmlNode> getContainingBlock(XmlNode cursor) {
			return cursor.ancestors().orSelf().match(this);
		}

		default boolean isBlock(Element e) {
			return isBlock(XmlNode.from(e));
		}

		boolean isBlock(XmlNode node);

		@Override
		default boolean test(XmlNode node) {
			return isBlock(node);
		}
	}

	public static class BlockResolverHtml implements BlockResolver {
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
			return isHtmlBlockTag(node.name());
		}
	}

	public static class NamespaceResult {
		public String firstTag;

		public String xml;
	}
}
