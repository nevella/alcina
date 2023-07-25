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
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
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

	public boolean isEarlierThan(Node o1, Node o2);

	public DomDocument loadFromUrl(String url);

	public Node loadFromXml(String xml) throws Exception;

	public String log(DomNode xmlNode, boolean pretty);

	public String prettyPrint(Document domDoc);

	public String prettyToString(DomNode xmlNode);

	public NamespaceResult removeNamespaces(DomDocument xmlDoc);

	public NamespaceResult restoreNamespaces(DomDocument xmlDoc,
			String firstTag);

	public String streamNCleanForBrowserHtmlFragment(Node node);

	public String toHtml(DomDocument doc);

	public String toXml(Node node);

	public static class NamespaceResult {
		public String firstTag;

		public String xml;
	}

	public static interface StyleResolver extends Predicate<DomNode> {
		default Optional<DomNode> getContainingBlock(DomNode cursor) {
			return cursor.ancestors().orSelf().match(this);
		}

		boolean isBlock(DomNode node);

		default boolean isBlock(Element e) {
			return isBlock(DomNode.from(e));
		}

		boolean isBold(DomNode node);

		boolean isItalic(DomNode node);

		@Override
		default boolean test(DomNode node) {
			return isBlock(node);
		}
	}

	@Reflected
	@Registration(StyleResolver.class)
	public static class StyleResolverHtml implements StyleResolver {
		@Override
		public boolean isBlock(DomNode node) {
			return HtmlConstants.isHtmlBlock(node.name());
		}

		@Override
		public boolean isBlock(Element e) {
			return isBlock(DomNode.from(e));
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
