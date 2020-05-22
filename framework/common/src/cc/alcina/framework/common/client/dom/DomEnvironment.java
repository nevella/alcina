package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cc.alcina.framework.common.client.dom.DomNode.XpathEvaluator;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.HtmlConstants;

public interface DomEnvironment {
	public static StyleResolver contextBlockResolver() {
		return Registry.impl(StyleResolver.class);
	}

	public static DomEnvironment get() {
		return Registry.impl(DomEnvironment.class);
	}

	public static List<Node> nodeListToList(NodeList nl) {
		List<Node> rVal = new ArrayList<Node>();
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			rVal.add(nl.item(i));
		}
		return rVal;
	}

	public XpathEvaluator createXpathEvaluator(DomNode xmlNode,
			XpathEvaluator xpathEvaluator);

	public Node loadFromXml(String xml) throws Exception;

	public String log(DomNode xmlNode, boolean pretty);

	public String prettyPrint(Document domDoc);

	public String prettyToString(DomNode xmlNode);

	public NamespaceResult removeNamespaces(DomDoc xmlDoc);

	public NamespaceResult restoreNamespaces(DomDoc xmlDoc, String firstTag);

	public String streamNCleanForBrowserHtmlFragment(Node node);

	public String toXml(Node node);

	public static class NamespaceResult {
		public String firstTag;

		public String xml;
	}

	public static interface StyleResolver extends Predicate<DomNode> {
		default Optional<DomNode> getContainingBlock(DomNode cursor) {
			return cursor.ancestors().orSelf().match(this);
		}

		default boolean isBlock(Element e) {
			return isBlock(DomNode.from(e));
		}

		boolean isBlock(DomNode node);

		boolean isBold(DomNode node);

		boolean isItalic(DomNode node);

		@Override
		default boolean test(DomNode node) {
			return isBlock(node);
		}
	}

	@RegistryLocation(registryPoint = StyleResolver.class, implementationType = ImplementationType.INSTANCE)
	@ClientInstantiable
	public static class StyleResolverHtml implements StyleResolver {
		DomDoc doc = null;

		@Override
		public boolean isBlock(Element e) {
			if (doc == null) {
				doc = DomNode.from(e).doc;
			}
			return isBlock(doc.nodeFor(e));
		}

		@Override
		public boolean isBlock(DomNode node) {
			return HtmlConstants.isHtmlBlock(node.name());
		}

		@Override
		public boolean isBold(DomNode node) {
			return node.ancestors().orSelf()
					.has(n -> n.name().equalsIgnoreCase("B")
							|| n.name().equalsIgnoreCase("STRONG"));
		}

		@Override
		public boolean isItalic(DomNode node) {
			return node.ancestors().orSelf()
					.has(n -> n.name().equalsIgnoreCase("I")
							|| n.name().equalsIgnoreCase("EMPH"));
		}
	}
}
